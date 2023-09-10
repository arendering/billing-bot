package su.vshk.billing.bot.dialog.transformer

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.option.ExitAvailableOptions
import su.vshk.billing.bot.dialog.step.ExitStep
import su.vshk.billing.bot.message.ResponseMessageService
import su.vshk.billing.bot.message.dto.RequestMessageItem

@Component
class ExitStateTransformer(
    private val responseMessageService: ResponseMessageService
): DialogStateTransformer {

    override fun getCommand(): Command =
        Command.EXIT

    override fun initializeState(request: RequestMessageItem, user: UserEntity): Mono<DialogState> =
        Mono.fromCallable {
            DialogState(
                command = getCommand(),
                steps = listOf(ExitStep.WARNING),
                response = DialogState.Response.next(responseMessageService.logoutWarningMessage()),
            )
        }

    override fun processOption(request: RequestMessageItem, user: UserEntity, state: DialogState): Mono<DialogState> =
        Mono.fromCallable {
            when (val step = state.currentStep()) {
                ExitStep.WARNING -> processWarningOption(state = state, option = request.input)
                else -> throw IllegalStateException("unknown step: '$step'")
            }
        }

    private fun processWarningOption(state: DialogState, option: String): DialogState =
        if (option == ExitAvailableOptions.YES) {
            state.finish(null)
        } else {
            state.invalidOption(responseMessageService.logoutInvalidWarningMessage())
        }
}