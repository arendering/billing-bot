package su.vshk.billing.bot.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SbssTariffParserTest {

    @Test
    fun testParseInternetTariff() {
        val raw = "tarid: 56, type: internet, name: Свои, speed: 1Gbit/s, rent: 0р/мес, client: физическое лицо."
        val parsed = SbssTariffParser.parse(raw)!!
        assertThat(parsed.id).isEqualTo(56)
        assertThat(parsed.type).isEqualTo("internet")
        assertThat(parsed.name).isEqualTo("Свои")
        assertThat(parsed.speed).isEqualTo("1Gbit/s")
        assertThat(parsed.channels).isNull()
        assertThat(parsed.rent).isEqualTo("0р/мес")
    }

    @Test
    fun testParseTvTariff() {
        val raw = "tarid: 222, type: tv, name: 24ТВ \"Лайт +\", channels: 241 канал, rent: 199р/мес."
        val parsed = SbssTariffParser.parse(raw)!!
        assertThat(parsed.id).isEqualTo(222)
        assertThat(parsed.type).isEqualTo("tv")
        assertThat(parsed.name).isEqualTo("24ТВ \"Лайт +\"")
        assertThat(parsed.speed).isNull()
        assertThat(parsed.channels).isEqualTo("241")
        assertThat(parsed.rent).isEqualTo("199р/мес.")
    }

    @Test
    fun testParseComboTariff() {
        val raw = "tarid: 212, type: internet-tv, name: Мега+ТВ, speed: 500Mbit/s, channels: 144 канала, rent: 1200р/мес."
        val parsed = SbssTariffParser.parse(raw)!!
        assertThat(parsed.id).isEqualTo(212)
        assertThat(parsed.type).isEqualTo("internet-tv")
        assertThat(parsed.name).isEqualTo("Мега+ТВ")
        assertThat(parsed.speed).isEqualTo("500Mbit/s")
        assertThat(parsed.channels).isEqualTo("144")
        assertThat(parsed.rent).isEqualTo("1200р/мес.")
    }

    @Test
    fun testTariffIdNotFoundError() {
        val raw = "type: internet-tv, name: Мега+ТВ, speed: 500Mbit/s, channels: 144 канала, rent: 1200р/мес."
        val parsed = SbssTariffParser.parse(raw)
        assertThat(parsed).isNull()
    }
}