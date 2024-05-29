package su.vshk.billing.bot.dialog.transformer

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.option.AgreementAvailableOptions
import su.vshk.billing.bot.dialog.option.AgreementOptions
import su.vshk.billing.bot.dialog.step.AgreementStep
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.response.AgreementMessageService
import su.vshk.billing.bot.service.AccountService
import su.vshk.billing.bot.service.RecommendedPaymentService
import su.vshk.billing.bot.service.VgroupsService
import su.vshk.billing.bot.service.dto.AgreementDto
import su.vshk.billing.bot.service.dto.InfoDto
import su.vshk.billing.bot.util.AddressNormalizer
import su.vshk.billing.bot.web.dto.manager.GetAccountRet
import su.vshk.billing.bot.web.dto.manager.GetVgroupsRet
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

@Component
class AgreementStateTransformer(
    private val vgroupsService: VgroupsService,
    private val recommendedPaymentService: RecommendedPaymentService,
    private val accountService: AccountService,
    private val agreementMessageService: AgreementMessageService
): DialogStateTransformer {

    private val agreementCache = ConcurrentHashMap<Long, List<AgreementDto>>()

    override fun getCommand(): Command =
        Command.AGREEMENTS

    override fun initializeState(request: RequestMessageItem, user: UserEntity): Mono<DialogState> =
        getInfo(user)
            .map { initializeDialogState(it) }

    override fun processOption(request: RequestMessageItem, user: UserEntity, state: DialogState): Mono<DialogState> =
        Mono.defer {
            when (val step = state.currentStep()) {
                AgreementStep.INFO ->
                    processInfoOption(user = user, state = state, option = request.input).toMono()

                AgreementStep.SWITCH_AGREEMENT ->
                    processSwitchAgreementOption(user = user, state = state, option = request.input).toMono()

                else -> IllegalStateException("unknown step: '$step'").toMono()
            }
        }

    private fun getInfo(user: UserEntity): Mono<InfoDto> =
        vgroupsService.getInternetVgroups(user.userId!!)
            .flatMap { vgroups ->
                getAdditionalInfo(userId = user.userId, agreementId = user.agreementId!!)
                    .map { (defaultRecommendedPayment, actualRecommendedPayment, account) ->
                        val info = createCurrentAgreementInfo(
                            agreementId = user.agreementId,
                            vgroups = vgroups,
                            defaultRecommendedPayment = defaultRecommendedPayment,
                            actualRecommendedPayment = actualRecommendedPayment,
                            account = account
                        )

                        if (info.multipleAgreements) {
                            agreementCache[user.telegramId] = createAgreements(vgroups)
                        }

                        info
                    }
            }

    private fun initializeDialogState(info: InfoDto): DialogState =
        if (info.multipleAgreements) {
            DialogState(
                command = getCommand(),
                options = AgreementOptions(),
                steps = listOf(AgreementStep.INFO, AgreementStep.SWITCH_AGREEMENT),
                response = DialogState.Response.next(agreementMessageService.showInfo(info))
            )
        } else {
            DialogState(
                command = getCommand(),
                response = DialogState.Response.cancel(agreementMessageService.showInfo(info))
            )
        }

    private fun getAdditionalInfo(userId: Long, agreementId: Long): Mono<Triple<BigDecimal, BigDecimal, GetAccountRet>> =
        recommendedPaymentService.getDefault(agreementId)
            .flatMap { defaultRecommendedPayment ->
                recommendedPaymentService.getActual(agreementId)
                    .flatMap { actualRecommendedPayment ->
                        accountService.getAccount(userId)
                            .map { account ->
                                Triple(defaultRecommendedPayment, actualRecommendedPayment, account)
                            }
                    }
            }

    private fun createCurrentAgreementInfo(
        agreementId: Long,
        vgroups: List<GetVgroupsRet>,
        defaultRecommendedPayment: BigDecimal,
        actualRecommendedPayment: BigDecimal,
        account: GetAccountRet
    ): InfoDto {
        val vgroup = vgroups.find { it.agreementId == agreementId }
            ?: throw RuntimeException("vgroup not found by agreement id '$agreementId'")

        return InfoDto(
            username = vgroup.username,
            agreementNumber = vgroup.agreementNumber,
            blocked = vgroup.blocked,
            balance = vgroup.balance,
            multipleAgreements = vgroups.size > 1,
            defaultRecommendedPayment = defaultRecommendedPayment,
            actualRecommendedPayment = actualRecommendedPayment,
            promiseCredit = account.agreements?.firstOrNull()?.promiseCredit,
            email = account.account?.email
        )
    }

    private fun createAgreements(vgroups: List<GetVgroupsRet>): List<AgreementDto> =
        vgroups.map {
            AgreementDto(
                agreementId = it.agreementId
                    ?: throw RuntimeException("agreement id is null"),

                agreementNumber = it.agreementNumber
                    ?: throw RuntimeException("agreement number is null"),

                address = it.addresses?.firstOrNull()?.address?.let { a -> AddressNormalizer.agreementNormalize(a) }
                    ?: throw RuntimeException("address is null")
            )
        }

    private fun processInfoOption(user: UserEntity, state: DialogState, option: String): DialogState =
        when (option) {
            AgreementAvailableOptions.CANCEL_INFO -> {
                agreementCache.remove(user.telegramId)
                state.cancel(agreementMessageService.showMainMenu())
            }

            AgreementAvailableOptions.SWITCH_AGREEMENT -> {
                val agreements = agreementCache[user.telegramId]
                    ?: throw RuntimeException("agreements not found in cache by telegram id '${user.telegramId}'")

                state.incrementStep(
                    options = state.options,
                    responseMessageItem = agreementMessageService.showAgreements(agreements = agreements, actualAgreementId = user.agreementId!!)
                )
            }

            else -> throw IllegalStateException("step '${AgreementStep.INFO}': unexpected option '$option'")
        }

    private fun processSwitchAgreementOption(user: UserEntity, state: DialogState, option: String): DialogState =
        if (option == AgreementAvailableOptions.CANCEL_SWITCH_AGREEMENT) {
            agreementCache.remove(user.telegramId)
            state.cancel(agreementMessageService.showMainMenu())
        } else {
            val chosenAgreement = agreementCache.remove(user.telegramId)?.find { it.agreementId == option.toLong() }
                ?: throw RuntimeException("agreementId '$option' not found in cache for user telegramId '${user.telegramId}'")

            state.finish(
                (state.options as AgreementOptions).copy(agreement = chosenAgreement)
            )
        }
}