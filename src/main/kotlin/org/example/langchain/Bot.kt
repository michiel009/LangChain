package org.example.langchain

import dev.langchain4j.service.Result

interface Bot {
    fun chat(userMessage: String): Result<String>
}
