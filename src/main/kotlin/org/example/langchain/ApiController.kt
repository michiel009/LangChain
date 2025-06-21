package org.example.langchain

import org.springframework.web.bind.annotation.*


@CrossOrigin
@RestController
class ApiController(private val botService: BotService) {

    @PostMapping("/talk")
    fun create(@RequestBody message: String): ChatMessage {
        return botService.talk(message)
    }

    @GetMapping("/history")
    fun getHistory(): List<ChatMessage> {
        return botService.getHistory();
    }

}
