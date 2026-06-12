package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<ContentJson>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfigJson? = null,
    @Json(name = "systemInstruction") val systemInstruction: ContentJson? = null
)

@JsonClass(generateAdapter = true)
data class ContentJson(
    @Json(name = "parts") val parts: List<PartJson>
)

@JsonClass(generateAdapter = true)
data class PartJson(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfigJson(
    @Json(name = "temperature") val temperature: Float? = 0.3f,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 500
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<CandidateJson>? = null
)

@JsonClass(generateAdapter = true)
data class CandidateJson(
    @Json(name = "content") val content: ContentJson? = null
)
