package su.vshk.billing.bot.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.math.BigDecimal
import java.math.RoundingMode

class AmountDeserializer: JsonDeserializer<BigDecimal>() {
    override fun deserialize(jsonParser: JsonParser?, context: DeserializationContext?): BigDecimal =
        // положительные числа округляются в бОльшую сторону, а отрицательные - в меньшую
        jsonParser!!.readValueAs(String::class.java)
            .let { BigDecimal(it).setScale(2, RoundingMode.UP) }
}