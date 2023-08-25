package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.option.LoginOptions
import su.vshk.billing.bot.message.ResponseMessageService
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.InfoService
import su.vshk.billing.bot.service.LoginMessageService
import su.vshk.billing.bot.dao.service.UserDaoService
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.errorTraceId
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.client.ClientLoginRequest

@Service
class LoginExecutor(
    private val billingWebClient: BillingWebClient,
    private val userDaoService: UserDaoService,
    private val infoService: InfoService,
    private val responseMessageService: ResponseMessageService,
    private val loginMessageService: LoginMessageService
): CommandExecutor {

    companion object {
        private val log = getLogger()
    }

    override fun getCommand(): Command =
        Command.START

    override fun execute(user: UserEntity, options: Any?): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            options as LoginOptions
            log.debugTraceId(context = context, msg = "try to execute command '${getCommand().value}' with options: $options")

            loginMessageService.remove(user.telegramId)
                .flatMap { loginMessageIds ->
                    Mono
                        .defer {
                            billingWebClient.getClientId(ClientLoginRequest(login = options.login, password = options.password))
                                .flatMap {
                                    it.orElse(null)
                                        ?.let { uid ->
                                            infoService.getAgrmId(uid)
                                                .flatMap { agrmId ->
                                                    userDaoService.saveUser(
                                                        telegramId = user.telegramId,
                                                        uid = uid,
                                                        login = options.login!!,
                                                        password = options.password!!,
                                                        agrmId = agrmId
                                                    )
                                                }
                                                .map { responseMessageService.startSuccessfulRegisterMessage(user.telegramId, loginMessageIds) }
                                        }
                                        ?: responseMessageService.startInvalidCredsMessage(user.telegramId, loginMessageIds).toMono()
                                }
                        }
                        .onErrorResume {
                            Mono.deferContextual { context ->
                                log.errorTraceId(context, it.stackTraceToString())
                                responseMessageService.startUnexpectedErrorMessage(user.telegramId, loginMessageIds).toMono()
                            }
                        }
                }
    }
}