package su.vshk.billing.bot.dao.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.support.TransactionOperations
import su.vshk.billing.bot.dao.model.PaymentNotificationEntity
import su.vshk.billing.bot.dao.model.PaymentNotificationType
import su.vshk.billing.bot.dao.model.UserEntity
import javax.persistence.EntityManager

@ExtendWith(SpringExtension::class)
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JacksonAutoConfiguration::class)
class UserRepositoryTest {

    companion object {
        private fun clear(transactionOperations: TransactionOperations, entityManager: EntityManager) {
            transactionOperations.execute {
                entityManager.createQuery("DELETE FROM UserEntity").executeUpdate()
                entityManager.createQuery("DELETE FROM PaymentNotificationEntity").executeUpdate()
            }
        }
    }

    @Autowired
    private lateinit var transactionOperations: TransactionOperations

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    internal fun setUp() {
        clear(transactionOperations, entityManager)
    }

    @AfterEach
    internal fun tearDown() {
        clear(transactionOperations, entityManager)
    }

    @Test
    fun testFindUserByTelegramId() {
        UserEntity(
            telegramId = 42L,
            userId = 13L,
            login = "user",
            agreementId = 999L
        ).let { entityManager.persist(it) }

        entityManager.flush()
        entityManager.clear()

        val user = userRepository.findById(42L).get()
        assertThat(user.userId).isEqualTo(13L)
        assertThat(user.login).isEqualTo("user")
        assertThat(user.agreementId).isEqualTo(999L)
    }

    @Test
    fun testFindEnabledNotificationUsers() {
        listOf(
            UserEntity(
                telegramId = 42L,
                userId = 13L,
                login = "jack",
                agreementId = 777L
            ),
            UserEntity(
                telegramId = 43L,
                userId = 14L,
                login = "john",
                agreementId = 888L
            ),
            UserEntity(
                telegramId = 44L,
                userId = 15L,
                login = "jason",
                agreementId = 999L
            )
        ).forEach { entityManager.persist(it) }

        listOf(
            PaymentNotificationEntity(
                telegramId = 42L,
                notificationType = PaymentNotificationType.SINGLE
            ),
            PaymentNotificationEntity(
                telegramId = 43L,
                notificationType = PaymentNotificationType.ALL
            )
        ).forEach { entityManager.persist(it) }

        entityManager.flush()
        entityManager.clear()

        val entities = userRepository.findUsersEnabledNotification()
        assertThat(entities.size).isEqualTo(2)
        entities.find { it.telegramId == 42L }.let { e ->
            assertThat(e).isNotNull
            assertThat(e!!.userId).isEqualTo(13)
            assertThat(e.agreementId).isEqualTo(777)
            assertThat(e.notificationType).isEqualTo(PaymentNotificationType.SINGLE)
        }
        entities.find { it.telegramId == 43L }.let { e ->
            assertThat(e).isNotNull
            assertThat(e!!.userId).isEqualTo(14)
            assertThat(e.agreementId).isEqualTo(888)
            assertThat(e.notificationType).isEqualTo(PaymentNotificationType.ALL)
        }
    }

    @Test
    fun testEmptyFindEnabledNotificationUsers() {
        listOf(
            UserEntity(
                telegramId = 42L,
                userId = 13L,
                login = "jack",
                agreementId = 777L
            ),
            UserEntity(
                telegramId = 43L,
                userId = 14L,
                login = "john",
                agreementId = 888L
            ),
            UserEntity(
                telegramId = 44L,
                userId = 15L,
                login = "jason",
                agreementId = 999L
            )
        ).forEach { entityManager.persist(it) }

        entityManager.flush()
        entityManager.clear()

        val entities = userRepository.findUsersEnabledNotification()
        assertThat(entities.size).isEqualTo(0)
    }
}