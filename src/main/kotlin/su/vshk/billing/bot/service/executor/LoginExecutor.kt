package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.option.LoginOptions
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.VgroupsService
import su.vshk.billing.bot.dao.service.UserDaoService
import su.vshk.billing.bot.message.response.LoginMessageService
import su.vshk.billing.bot.service.LoginMessageIdService
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.errorTraceId
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.client.ClientLoginRequest

@Service
class LoginExecutor(
    private val billingWebClient: BillingWebClient,
    private val userDaoService: UserDaoService,
    private val vgroupsService: VgroupsService,
    private val loginMessageIdService: LoginMessageIdService,
    private val loginMessageService: LoginMessageService
): CommandExecutor {

    companion object {
        private val log = getLogger()
    }

    override fun getCommand(): Command =
        Command.LOGIN

    override fun execute(user: UserEntity, options: Any?): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            options as LoginOptions
            log.debugTraceId(context = context, msg = "try to execute command '${getCommand().value}' with options: $options")

            loginMessageIdService.remove(user.telegramId)
                .flatMap { loginMessageIds ->
                    Mono
                        .defer {
                            billingWebClient.getClientId(ClientLoginRequest(login = options.login, password = options.password))
                                .flatMap {
                                    it.orElse(null)
                                        ?.let { userId ->
                                            getAgreementId(userId)
                                                .flatMap { agreementId ->
                                                    userDaoService.saveUser(
                                                        telegramId = user.telegramId,
                                                        userId = userId,
                                                        login = options.login!!,
                                                        agreementId = agreementId
                                                    )
                                                }
                                                .map { loginMessageService.showMainMenu(loginMessageIds) }
                                        }
                                        ?: loginMessageService.showInvalidCredentials(loginMessageIds).toMono()
                                }
                        }
                        .onErrorResume {
                            Mono.deferContextual { context ->
                                log.errorTraceId(context, it.stackTraceToString())
                                loginMessageService.showUnexpectedError(loginMessageIds).toMono()
                            }
                        }
                }
    }

    private fun getAgreementId(userId: Long): Mono<Long> =
        vgroupsService.getInternetVgroups(userId)
            .map { vgroups ->
                vgroups.first().agreementId
                    ?: throw RuntimeException("first agreement id is null")
            }
}