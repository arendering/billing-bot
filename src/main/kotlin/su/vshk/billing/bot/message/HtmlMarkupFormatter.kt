package su.vshk.billing.bot.message

/**
 * Класс для создания форматированного текста ответа.
 * Не является потокобезопасным.
 */
class HtmlMarkupFormatter {

    private val sb = StringBuilder()
    //TODO: добавь тест, проверяющий корректность работы этой переменной
    private var addBreakLineAvailable: Boolean = false

    companion object {
        private const val LINE_SEPARATOR = "\n"
        private const val SPACE = " "
    }

    /**
     * Добавляет ключ-значение.
     *
     * Пример:
     * <i>ФИО:</i> <b>Иван Иванов</b>
     */
    fun addEntry(
        key: String,
        keyType: TextType = TextType.BOLD,
        value: String?,
        valueType: TextType = TextType.ITALIC
    ): HtmlMarkupFormatter =
        value
            ?.let { v ->
                addBreakLineAvailable = true
                sb.append(doMarkup("${key}:", keyType))
                sb.append(SPACE)
                sb.append(doMarkup(v, valueType))
                sb.append(LINE_SEPARATOR)
                this
            }
            ?: this

    /**
     * Добавляет ключ-значения.
     *
     * Пример:
     * <b>Телефоны:</b>
     * <i>+7-999-111-22-33</i>
     * <i>+7-888-555-22-33</i>
     */
    fun addEntries(
        key: String,
        keyType: TextType = TextType.BOLD,
        values: List<String>?,
        valueType: TextType = TextType.ITALIC
    ): HtmlMarkupFormatter =
        if (values.isNullOrEmpty()) {
            this
        } else {
            addBreakLineAvailable = true
            sb.append(doMarkup("${key}:", keyType))
            values.forEach { v ->
                sb.append(LINE_SEPARATOR)
                sb.append(doMarkup(v, valueType))
            }
            sb.append(LINE_SEPARATOR)
            this
        }

    /**
     * Добавляет текст.
     *
     * Пример:
     * <b>Обещанный платеж выдается сроком на 3 дня</b>
     */
    fun addText(
        text: String?,
        textType: TextType = TextType.PLAIN
    ): HtmlMarkupFormatter =
        text
            ?.let { t ->
                addBreakLineAvailable = true
                sb.append(doMarkup(t, textType))
                sb.append(LINE_SEPARATOR)
                this
            }
            ?: this

    /**
     * Добавляет ссылку, которая скрыта текстом.
     */
    fun addHref(
        text: String,
        textType: TextType = TextType.PLAIN,
        href: String?
    ): HtmlMarkupFormatter =
        href
            ?.let {
                addBreakLineAvailable = true
                sb.append(doMarkup(text, textType, href))
                sb.append(LINE_SEPARATOR)
                this
            }
            ?: this

    //TODO: можно использовать метод ниже
    fun addFormattedText(
        text: String,
        textType: TextType = TextType.PLAIN,
        value: String,
        valueType: TextType = TextType.ITALIC
    ): HtmlMarkupFormatter {
        addBreakLineAvailable = true
        sb.append(
            doMarkup(
                text.format(doMarkup(value, valueType)),
                textType
            )
        )
        sb.append(LINE_SEPARATOR)
        return this
    }

    fun addFormattedText(
        textType: TextType = TextType.PLAIN,
        text: String,
        valueType: TextType = TextType.ITALIC,
        vararg values: String
    ): HtmlMarkupFormatter {
        addBreakLineAvailable = true
        sb.append(
            doMarkup(
                text.format(*values.map { v -> doMarkup(v, valueType) }.toTypedArray()),
                textType
            )
        )
        sb.append(LINE_SEPARATOR)
        return this
    }

    fun addFormattedPair(
        text: String,
        textType: TextType = TextType.PLAIN,
        first: String,
        firstType: TextType = TextType.ITALIC,
        second: String,
        secondType: TextType = TextType.ITALIC
    ): HtmlMarkupFormatter {
        addBreakLineAvailable = true
        sb.append(
            doMarkup(
                text.format(
                    doMarkup(first, firstType),
                    doMarkup(second, secondType)
                ),
                textType
            )

        )
        sb.append(LINE_SEPARATOR)
        return this
    }

    /**
     * Добавляет перевод строки.
     */
    fun addLineBreak(): HtmlMarkupFormatter {
        if (addBreakLineAvailable) {
            sb.append(LINE_SEPARATOR)
            addBreakLineAvailable = false
        }
        return this
    }

    fun build(): String =
        sb.toString()

    private fun doMarkup(text: String, type: TextType, href: String? = null): String {
        val modified = href
            ?.let { "<a href=\"${href}\">${text}</a>" }
            ?: text

        return when (type) {
            TextType.PLAIN -> modified
            TextType.BOLD -> "<b>${modified}</b>"
            TextType.ITALIC -> "<i>${modified}</i>"
            TextType.BOLD_UNDERLINED -> "<b><u>${modified}</u></b>"
            TextType.PLAIN_UNDERLINED -> "<u>${modified}</u>"
            TextType.BOLD_ITALIC -> "<b><i>${modified}</i></b>"
        }
    }
}

enum class TextType {
    PLAIN, // обычный
    BOLD, // жирный
    ITALIC, // курсив
    PLAIN_UNDERLINED, // обычный подчеркнутый
    BOLD_UNDERLINED, // жирный подчеркнутый
    BOLD_ITALIC // жирный курсив
}