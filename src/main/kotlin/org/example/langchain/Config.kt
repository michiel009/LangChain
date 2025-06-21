package org.example.langchain

import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.mcp.McpToolProvider
import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.mcp.client.transport.McpTransport
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport
import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.model.anthropic.AnthropicChatModelName
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.image.ImageModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiImageModel
import dev.langchain4j.service.AiServices
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration



@Configuration
class Config {

    //@Bean("antmodel")
    fun antChatModel(): ChatModel {
        val apiKey = System.getenv("ANTHROPIC_API_KEY")
        require(!apiKey.isNullOrEmpty()) { "ANTHROPIC_API_KEY environment variable must be set." }

        return AnthropicChatModel.builder()
            .apiKey(apiKey)
            .modelName(AnthropicChatModelName.CLAUDE_3_7_SONNET_20250219)
            .build()
    }

    @Bean("aimodel")
    fun chatModel(): ChatModel {
        val apiKey = System.getenv("OPENAI_API_KEY")
        require(!apiKey.isNullOrEmpty()) { "OPENAI_API_KEY environment variable must be set." }

        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4o-mini")
            .build()
    }

    //@Bean
    fun ollamaModel(): StreamingChatModel {
        return OllamaStreamingChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("llama3.2")
            .timeout(2.minutes.toJavaDuration())
            .build()
    }

    @Bean
    fun chatMemory(): ChatMemory {
        return MessageWindowChatMemory.withMaxMessages(20);
    }

    @Bean
    fun imageModel(): ImageModel {
        val apiKey = System.getenv("OPENAI_API_KEY")
        require(!apiKey.isNullOrEmpty()) { "OPENAI_API_KEY environment variable must be set." }

        return OpenAiImageModel.builder()
            .apiKey(apiKey)
            .modelName("dall-e-3")
            .build()
    }

    @Bean
    fun imagePrompter(@Qualifier("aimodel") chatModel: ChatModel): ImagePromptGenerator {

        return AiServices.builder(ImagePromptGenerator::class.java)
            .systemMessageProvider { this::class.java.classLoader.getResource("imgPrompter.txt")?.readText() }
            .chatModel(chatModel).build();
    }


    @Bean
    fun assistant(chatMemory: ChatMemory, @Qualifier("aimodel") chatModel: ChatModel): Bot {
        val transport: McpTransport = HttpMcpTransport
            .Builder()
            .sseUrl("http://localhost:8082/sse")
            .logRequests(true) // if you want to see the traffic in the log
            .logResponses(true)
            .timeout(2.minutes.toJavaDuration())
            .build()

        val mcpClient: McpClient = DefaultMcpClient.Builder()
            .transport(transport)
            .build()

        val toolProvider: McpToolProvider = McpToolProvider.builder()
            .mcpClients(mcpClient)
            .build()

        return AiServices.builder(Bot::class.java)
            .systemMessageProvider { this::class.java.classLoader.getResource("prompt.txt")?.readText() }
            .toolProvider(toolProvider)
            .hallucinatedToolNameStrategy { req ->
                ToolExecutionResultMessage(
                    req.id(),
                    req.name(),
                    "Tool ${req.name()} not found"
                )
            }
            .chatModel(chatModel)
            .chatMemory(chatMemory).build();
    }

}
