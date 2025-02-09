package com.flesiy.Lotus.utils

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

object GroqApi {
    const val BASE_URL = "https://api.groq.com/openai/v1/"
    const val API_KEY = "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"
}

data class Message(
    val role: String,
    val content: String
)

data class GroqRequest(
    val messages: List<Message>,
    val model: String,
    val temperature: Int,
    val max_completion_tokens: Int,
    val top_p: Int,
    val stream: Boolean,
    val stop: String? = null
)

data class GroqResponse(
    val id: String,
    val choices: List<Choice>,
    val created: Long,
    val model: String,
    val usage: Usage
)

data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

interface GroqService {
    @POST("chat/completions")
    suspend fun generateResponse(@Body request: GroqRequest): Response<GroqResponse>
}

object RetrofitClient {
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${GroqApi.API_KEY}")
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(GroqApi.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val groqService: GroqService = retrofit.create(GroqService::class.java)
} 