package su.vshk.billing.bot.dao.repository

import org.assertj.core.api.Assertions.assertThat
import su.vshk.billing.bot.dao.model.UserEntity
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
            uid = 13L,
            login = "user",
            password = "1234",
            agrmId = 999L,
            paymentNotificationEnabled = false
        ).let { entityManager.persist(it) }

        entityManager.flush()
        entityManager.clear()

        val user = userRepository.findById(42L).get()
        assertThat(user.uid).isEqualTo(13L)
        assertThat(user.login).isEqualTo("user")
        assertThat(user.password).isEqualTo("1234")
        assertThat(user.agrmId).isEqualTo(999L)
        assertThat(user.paymentNotificationEnabled).isFalse
    }

    @Test
    fun testFindByPaymentNotificationEnabledTrue() {
        listOf(
            UserEntity(
                telegramId = 1L,
                uid = 1L,
                login = "-",
                password = "-",
                agrmId = 999L,
                paymentNotificationEnabled = true
            ),
            UserEntity(
                telegramId = 2L,
                uid = 2L,
                login = "-",
                password = "-",
                agrmId = 999L,
                paymentNotificationEnabled = true
            ),
            UserEntity(
                telegramId = 3L,
                uid = 3L,
                login = "-",
                password = "-",
                agrmId = 999L,
                paymentNotificationEnabled = false
            )
        ).forEach { entityManager.persist(it) }

        entityManager.flush()
        entityManager.clear()

        val users = userRepository.findByPaymentNotificationEnabledTrue()
        assertThat(users.size).isEqualTo(2)
        assertThat(users.any { it.telegramId == 1L }).isTrue
        assertThat(users.any { it.telegramId == 2L }).isTrue
        assertThat(users.none { it.telegramId == 3L }).isTrue
    }
}