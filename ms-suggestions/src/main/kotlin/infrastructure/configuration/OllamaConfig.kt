package infrastructure.configuration

import io.netty.channel.ChannelOption
import io.netty.handler.logging.LogLevel
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.slf4j.LoggerFactory
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.transport.logging.AdvancedByteBufFormat
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Spring configuration for Ollama integration.
 *
 * Exposes beans for the Ollama chat model, Ollama API client and a tuned
 * WebClient builder. The configuration provides sensible defaults for
 * connection pooling, timeouts and logging suitable for communicating with
 * the Ollama service.
 *
 * @param baseUrl the base URL of the Ollama server (injected from properties).
 */
@Configuration
class OllamaConfig(
    @param:Value("\${ollama.base-url}")
    private val baseUrl: String
) {
    companion object {
        const val OLLAMA_CHAT_CLIENT: String = "ollama"
        private const val OLLAMA_WEBCLIENT: String = "ollama-webclient"
    }

    /**
     * Create and configure the Ollama chat model bean using provided options.
     */
    @Bean(name = [OLLAMA_CHAT_CLIENT])
    fun ollamaChatModel(
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

    /**
     * Build the OllamaApi using the preconfigured WebClient builder.
     */
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

    /**
     * Create a WebClient.Builder tuned for Ollama requests: connection pool,
     * timeouts, increased in-memory buffer and a timing filter for observability.
     *
     * @param connectTimeoutMs connection timeout in milliseconds
     * @param responseTimeoutSeconds response timeout in seconds
     */
    @Bean(name = [OLLAMA_WEBCLIENT])
    fun webClient(
        @Value("\${ollama.connect-timeout-ms}") connectTimeoutMs: Int,
        @Value("\${ollama.response-timeout-s}") responseTimeoutSeconds: Long
    ): WebClient.Builder {


        val logger = LoggerFactory.getLogger(OllamaConfig::class.java)

        val connectionProvider = ConnectionProvider.builder("ollama-pool")
            .maxConnections(100)
            .pendingAcquireTimeout(Duration.ofSeconds(5))
            .pendingAcquireMaxCount(5000)
            .build()

        val httpClient: HttpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(responseTimeoutSeconds, TimeUnit.SECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(responseTimeoutSeconds, TimeUnit.SECONDS))
            }
            .wiretap("ollama-http", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)

        val strategies = ExchangeStrategies.builder()
            .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
            .build()

        val timingFilter = ExchangeFilterFunction { request, next ->
            val start = System.nanoTime()
            next.exchange(request)
                .doOnTerminate {
                    val elapsedMs = (System.nanoTime() - start) / 1_000_000
                    logger.info("OLLAMA {} {} -> {} ms", request.method(), request.url(), elapsedMs)
                }
        }

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .defaultHeader(HttpHeaders.USER_AGENT, "ms-suggestions/ollama")
            .baseUrl(baseUrl)
            .filter(timingFilter)
    }
}