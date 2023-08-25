package su.vshk.billing.bot.web.converter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import su.vshk.billing.bot.web.dto.Fault
import org.springframework.stereotype.Component

@Component
class ResponseConverter(
    private val objectMapper: ObjectMapper,
    private val xmlMapper: XmlMapper
) {

    companion object {
        private const val EMPTY_JSON = "{}"
        private const val BODY_PAYLOAD_TAG = "Body"
        private const val FAULT_PAYLOAD_TAG = "Fault"
        private const val RESPONSE_SUFFIX = "Response"
    }

    fun <T> convert(method: String, httpBody: String, clazz: Class<T>): T {
        val json = readResponsePayload(httpBody, method + RESPONSE_SUFFIX)
        return objectMapper.readValue(json, objectMapper.typeFactory.constructFromCanonical(clazz.name))
    }

    fun convertFault(httpBody: String): Fault {
        val json = readResponsePayload(httpBody, FAULT_PAYLOAD_TAG)
        return objectMapper.readValue(json, Fault::class.java)
    }

    private fun readResponsePayload(body: String, payloadTag: String): String {
        val node = xmlMapper.readTree(body)
            .get(BODY_PAYLOAD_TAG)
            .get(payloadTag)
            ?: throw RuntimeException("response node is null for payload tag '$payloadTag'")

        return if (isEmptyNode(node)) {
            EMPTY_JSON
        } else {
            node.toString()
        }
    }

    private fun isEmptyNode(node: JsonNode): Boolean =
        node.isTextual
            && node.textValue().isEmpty()
}