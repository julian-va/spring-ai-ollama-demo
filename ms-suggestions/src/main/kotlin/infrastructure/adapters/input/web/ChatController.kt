package jva.cloud.infrastructure.adapters.input.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ChatController(private val chatModel: OllamaChatModel) {

    @GetMapping(value = ["/ai/generate"])
    suspend fun generate(
        @RequestParam(
            value = "message",
            defaultValue = "Tell me a joke"
        ) message: String
    ): Map<String, String?> {
        val response: String? = withContext(context = Dispatchers.IO) {
            chatModel.call(message)
        }

        return mapOf("response" to response)
    }

    @GetMapping("/ai/generateStream")
    fun generateStream(
        @RequestParam(
            value = "message",
            defaultValue = "Tell me a joke"
        ) message: String
    ): Flow<String> {
        val prompt: Prompt = Prompt(UserMessage(message))
        return chatModel.stream(prompt).asFlow().map { chatResponse ->
            chatResponse.result.output.text
                ?: ""
        }
    }

}