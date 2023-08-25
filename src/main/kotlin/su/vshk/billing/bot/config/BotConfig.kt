package su.vshk.billing.bot.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.netty.channel.ChannelOption
import io.netty.handler.logging.LogLevel
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat
import java.util.concurrent.TimeUnit

@EnableScheduling
@Configuration
class BotConfig {

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val objectMapper = ObjectMapper()
        objectMapper.registerModule(KotlinModule()) //TODO: replace with builder
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        return objectMapper
    }

    @Bean
    fun xmlMapper(): XmlMapper {
        val xmlMapper = XmlMapper()
        xmlMapper.registerModule(KotlinModule()) //TODO: replace with builder
        xmlMapper.registerModule(JavaTimeModule())
        xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        return xmlMapper
    }

    @Bean
    fun webClient(properties: BotProperties): WebClient {
        val webClientProperties = properties.webClient

        val tcpClient = TcpClient
            .create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webClientProperties.connTimeoutMillis)
            .doOnConnected {
                it.addHandlerLast(ReadTimeoutHandler(webClientProperties.readTimeoutMillis, TimeUnit.MILLISECONDS))
                it.addHandlerLast(WriteTimeoutHandler(webClientProperties.writeTimeoutMillis, TimeUnit.MILLISECONDS))
            }

        val httpClient = HttpClient
            .from(tcpClient) //TODO: refactoring
            .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }

    @Bean
    fun messageSource(): ResourceBundleMessageSource {
        val source = ResourceBundleMessageSource()
        source.setBasename("message")
        source.setUseCodeAsDefaultMessage(false)
        source.setDefaultEncoding("UTF-8")
        return source
    }
}