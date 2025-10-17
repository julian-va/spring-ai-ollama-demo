package jva.cloud.infrastructure.config

import io.netty.channel.ChannelOption
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class OllamaConfig(
    @param:Value("\${ollama.base-url}")
    private val baseUrl: String
) {
    companion object {
        const val OLLAMA_CHAT_CLIENT: String = "ollama"
        private const val OLLAMA_WEBCLIENT: String = "ollama-webclient"
    }

    @Bean(name = [OLLAMA_CHAT_CLIENT])
    fun deepseekChatClient(
        api: OllamaApi,
        @Value("\${ollama.llama.model}") model: String,
        @Value("\${ollama.llama.temperature}") temperature: Double,
        @Value("\${ollama.llama.numPredict}") numPredict: Int,
        @Value("\${ollama.llama.keepAlive}") keepAlive: String,
        @Value("\${ollama.llama.numGPU}") numGPU: Int
    ): OllamaChatModel {
        return OllamaChatModel.builder()
            .ollamaApi(api)
            .defaultOptions(ollamaOptions(model, temperature, numPredict, keepAlive, numGPU))
            .build()
    }

    @Bean
    fun ollamaApiBuilder(@Qualifier(OLLAMA_WEBCLIENT) webClient: WebClient.Builder): OllamaApi {
        return OllamaApi.builder()
            .baseUrl(baseUrl).webClientBuilder(webClient)
            .build()
    }

    private fun ollamaOptions(
        model: String,
        temperature: Double,
        numPredict: Int,
        keepAlive: String,
        numGPU: Int
    ): OllamaOptions {
        return OllamaOptions
            .builder()
            .model(model)
            .temperature(temperature)
            .numPredict(numPredict)
            .keepAlive(keepAlive)
            .numGPU(numGPU)
            .build()
    }

    @Bean(name = [OLLAMA_WEBCLIENT])
    fun webClient(
        @Value("\${ollama.connect-timeout-ms}") connectTimeoutMs: Int,
        @Value("\${ollama.response-timeout-s}") responseTimeoutSeconds: Long
    ): WebClient.Builder {

        val httpClient: HttpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds))
        /*.doOnConnected { connection ->
            connection.addHandlerLast(ReadTimeoutHandler(responseTimeoutSeconds, TimeUnit.SECONDS))
            connection.addHandlerLast(WriteTimeoutHandler(responseTimeoutSeconds, TimeUnit.SECONDS))
        }*/


        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
    }
}