package org.example.langchain

data class ChatMessage(val sender: Int, val text: String, val image: String?, val prompt: String?);
