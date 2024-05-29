package su.vshk.billing.bot.dialog.transformer

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.service.PaymentNotificationDaoService
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.option.NotificationOptions
import su.vshk.billing.bot.dialog.step.NotificationStep
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.NotificationMessageService
import su.vshk.billing.bot.service.VgroupsService

@Component
class NotificationStateTransformer(
    private val vgroupsService: VgroupsService,
    private val paymentNotificationDaoService: PaymentNotificationDaoService,
    private val notificationMessageService: NotificationMessageService
): DialogStateTransformer {

    override fun getCommand(): Command =
        Command.NOTIFICATION

    override fun initializeState(request: RequestMessageItem, user: UserEntity): Mono<DialogState> =
        getInitResponseMessageItem(user)
            .map { responseMessageItem ->
                DialogState(
                    command = getCommand(),
                    options = NotificationOptions(),
                    steps = listOf(NotificationStep.SWITCH),
                    response = DialogState.Response.next(responseMessageItem)
                )
            }

    override fun processOption(request: RequestMessageItem, user: UserEntity, state: DialogState): Mono<DialogState> =
        Mono.fromCallable {
            when (val step = state.currentStep()) {
                NotificationStep.SWITCH -> addSwitchOption(state = state, option = request.input)
                else -> throw IllegalStateException("unknown step: '$step'")
            }
        }

    private fun getInitResponseMessageItem(user: UserEntity): Mono<ResponseMessageItem> =
        paymentNotificationDaoService.findById(user.telegramId)
            .flatMap { notificationEntityOpt ->
                vgroupsService.getInternetVgroups(user.userId!!)
                    .map { vgroups ->
                        val notificationEntity = notificationEntityOpt.orElse(null)
                        val userHasSingleAgreement = vgroups.size == 1
                        val isNotificationEnabled = notificationEntityOpt.isPresent

                        when {
                            isNotificationEnabled && userHasSingleAgreement ->
                                notificationMessageService.disable()

                            isNotificationEnabled && notificationEntity.isSingleType() ->
                                notificationMessageService.enableForAllAgreementsOrDisable()

                            isNotificationEnabled && notificationEntity.isAllType() ->
                                notificationMessageService.enableForSingleAgreementOrDisable()

                            !isNotificationEnabled && userHasSingleAgreement ->
                                notificationMessageService.enable()

                            !isNotificationEnabled && !userHasSingleAgreement ->
                                notificationMessageService.enableForSingleAgreementOrEnableForAllAgreements()

                            else -> throw IllegalStateException("unreachable code")
                        }
                    }
            }

    private fun addSwitchOption(state: DialogState, option: String): DialogState =
        (state.options as NotificationOptions)
            .copy(switch = option)
            .let { state.finish(it) }
}