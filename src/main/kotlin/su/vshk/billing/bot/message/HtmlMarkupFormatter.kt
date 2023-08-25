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

    fun addValue(
        header: String,
        headerType: TextType = TextType.BOLD,
        value: String?,
        valueType: TextType = TextType.ITALIC
    ): HtmlMarkupFormatter =
        value
            ?.let { v ->
                addBreakLineAvailable = true
                sb.append(doMarkup("${header}:", headerType))
                sb.append(SPACE)
                sb.append(doMarkup(v, valueType))
                sb.append(LINE_SEPARATOR)
                this
            }
            ?: this

    fun addValues(
        header: String,
        headerType: TextType = TextType.BOLD,
        values: List<String>?,
        valueType: TextType = TextType.ITALIC
    ): HtmlMarkupFormatter =
        if (values.isNullOrEmpty()) {
            this
        } else {
            addBreakLineAvailable = true
            sb.append(doMarkup("${header}:", headerType))
            values.forEach { v ->
                sb.append(LINE_SEPARATOR)
                sb.append(doMarkup(v, valueType))
            }
            sb.append(LINE_SEPARATOR)
            this
        }

    fun addText(
        text: String,
        textType: TextType = TextType.PLAIN
    ): HtmlMarkupFormatter {
        addBreakLineAvailable = true
        sb.append(doMarkup(text, textType))
        sb.append(LINE_SEPARATOR)
        return this
    }

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
    BOLD_UNDERLINED, // жирный подчеркнутый
    PLAIN_UNDERLINED, // обычный подчеркнутый
    BOLD_ITALIC // жирный курсив
}