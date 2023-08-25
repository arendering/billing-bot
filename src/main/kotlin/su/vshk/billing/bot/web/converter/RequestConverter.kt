package su.vshk.billing.bot.web.converter

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.springframework.stereotype.Component

@Component
class RequestConverter(
    private val xmlMapper: XmlMapper
) {
    companion object {
        private const val METHOD_PREFIX = "urn:"
        private const val REQUEST_TEMPLATE =
            """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:urn="urn:api3">
                  <soap:Header/>
                  <soap:Body>
                    %s
                  </soap:Body>
                </soap:Envelope>
            """
    }

    fun <T> convert(method: String, payload: T): String =
        xmlMapper.writer()
            .withRootName(METHOD_PREFIX + method)
            .writeValueAsString(payload)
            .let { REQUEST_TEMPLATE.format(it) }
}