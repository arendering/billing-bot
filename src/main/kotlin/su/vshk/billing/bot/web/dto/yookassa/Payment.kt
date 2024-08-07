package su.vshk.billing.bot.web.dto.yookassa

import com.fasterxml.jackson.annotation.JsonProperty

data class YookassaPayment(
    @JsonProperty("amount")
    val amount: YookassaPaymentAmount? = null,

    @JsonProperty("payment_method_data")
    val paymentMethodData: YookassaPaymentMethodData? = null,

    @JsonProperty("confirmation")
    val confirmation: YookassaPaymentConfirmation? = null,

    /**
     * Автоматический прием поступившего платежа
     */
    @JsonProperty("capture")
    val capture: Boolean? = null,

    /**
     * Описание транзакции (не более 128 символов), которое вы увидите в личном кабинете ЮKassa, а пользователь — при оплате.
     * Например: «Оплата заказа № 72 для user@yoomoney.ru».
     */
    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("metadata")
    val metadata: YookassaPaymentMetadata? = null,

    @JsonProperty("receipt")
    val receipt: YookassaPaymentReceipt? = null
)

data class YookassaPaymentAmount(
    /**
     * Сумма в выбранной валюте.
     * Всегда дробное значение.
     * Разделитель дробной части — точка, разделитель тысяч отсутствует.
     * Количество знаков после точки зависит от выбранной валюты.
     * Пример: 1000.00.
     */
    @JsonProperty("value")
    val value: String? = null,

    /**
     * Трехбуквенный код валюты в формате ISO-4217.
     * Пример: RUB.
     */
    @JsonProperty("currency")
    val currency: String? = null
)

data class YookassaPaymentMethodData(
    /**
     * Код способа оплаты.
     */
    @JsonProperty("type")
    val type: String? = null
)

data class YookassaPaymentConfirmation(
    /**
     * Код сценария подтверждения
     */
    @JsonProperty("type")
    val type: String? = null,

    /**
     * URL, на который вернется пользователь после подтверждения или отмены платежа на веб-странице.
     * Не более 2048 символов.
     */
    @JsonProperty("return_url")
    val returnUrl: String? = null,

    /**
     * URL, на который необходимо перенаправить пользователя для подтверждения оплаты.
     */
    @JsonProperty("confirmation_url")
    val confirmationUrl: String? = null
)

data class YookassaPaymentMetadata(
    /**
     * Идентификатор предварительного платежа в биллинге.
     */
    @JsonProperty("payment_id")
    val prePaymentId: String? = null
)

data class YookassaPaymentReceipt(
    /**
     * Список товаров в заказе.
     */
    @JsonProperty("items")
    val items: List<YookassaPaymentReceiptItem>? = null,

    /**
     * Система налогообложения магазина.
     */
    @JsonProperty("tax_system_code")
    val taxSystemCode: Int? = null,

    /**
     * Информация о пользователе.
     */
    @JsonProperty("customer")
    val customer: YookassaPaymentReceiptCustomer? = null
)

data class YookassaPaymentReceiptItem(
    /**
     * Название товара.
     */
    @JsonProperty("description")
    val description: String? = null,

    /**
     * Количество товара.
     */
    @JsonProperty("quantity")
    val quantity: String? = null,

    /**
     * Цена товара.
     */
    @JsonProperty("amount")
    val amount: YookassaPaymentAmount? = null,

    /**
     * Ставка НДС.
     */
    @JsonProperty("vat_code")
    val vatCode: Int? = null,

    /**
     * Признак предмета расчета.
     */
    @JsonProperty("payment_subject")
    val paymentSubject: String? = null,

    /**
     * Признак способа расчета.
     */
    @JsonProperty("payment_mode")
    val paymentMode: String? = null
)

data class YookassaPaymentReceiptCustomer(
    /**
     * Электронная почта для отправки чека.
     */
    @JsonProperty("email")
    val email: String? = null,

    /**
     * Телефон пользователя для отправки чека.
     */
    @JsonProperty("phone")
    val phone: String? = null
)