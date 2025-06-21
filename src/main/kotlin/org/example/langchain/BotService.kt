package org.example.langchain

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.model.image.ImageModel
import org.springframework.stereotype.Service

@Service
class BotService(
    private val bot: Bot,
    private val imagePromptGenerator: ImagePromptGenerator,
    private val memory: ChatMemory,
    private val imageModel: ImageModel
) {

    fun talk(message: String): ChatMessage {
        val rep = bot.chat(message)
        val prompt = imagePromptGenerator.generateImage(
            "Generate the prompt using this response info:" +
                    "<response>${rep.content()}</response>" +
                    "<tools>${rep.toolExecutions()}</tools>"
        )
        val image = imageModel.generate(prompt)
        return ChatMessage(1, rep.content(), image.content().url().toString(), prompt);
    }

    fun getHistory(): List<ChatMessage> {
        return memory.messages().map {
            when (it) {
                is AiMessage -> ChatMessage(1, it.text() ?: "empty", null, null)
                is UserMessage -> ChatMessage(0, it.singleText() ?: "empty", null, null)
                else -> ChatMessage(1, "unknown", null, null)
            }
        }

    }
}
