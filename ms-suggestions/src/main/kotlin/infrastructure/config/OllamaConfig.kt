package jva.cloud.infrastructure.config

import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OllamaConfig(
    @param:Value("\${ollama.base-url}")
    private val baseUrl: String
) {
    companion object {
        const val OLLAMA_CHAT_CLIENT: String = "ollama"
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
    fun ollamaApiBuilder(): OllamaApi {
        return OllamaApi.builder()
            .baseUrl(baseUrl)
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
}