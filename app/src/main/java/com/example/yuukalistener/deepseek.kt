package com.example.yuukalistener

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class ChatMessage(val role: String, val content: String)
data class ChatRequest(
    val model: String,
    val content: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false
)

data class ChatResponse(val choices: List<Choice>)
data class Choice(val message: ChatMessage)

interface DeepSeekApi {
    @POST("v1/chat/completions")
    fun chat(
        @Body request: ChatRequest,
        @Header("Authorization") token: String
    ): Call<ChatResponse>
}
