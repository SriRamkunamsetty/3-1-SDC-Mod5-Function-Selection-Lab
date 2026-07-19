package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val tools: List<Tool>? = null,
    val toolConfig: ToolConfig? = null,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    @Json(name = "functionCall") val functionCall: FunctionCall? = null,
    @Json(name = "functionResponse") val functionResponse: FunctionResponse? = null
)

data class FunctionCall(
    val name: String,
    val args: Map<String, Any?>? = null
)

data class FunctionResponse(
    val name: String,
    val response: Map<String, Any?>
)

@JsonClass(generateAdapter = true)
data class Tool(
    val functionDeclarations: List<FunctionDeclaration>
)

@JsonClass(generateAdapter = true)
data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Schema
)

@JsonClass(generateAdapter = true)
data class Schema(
    val type: String, // "OBJECT", "STRING", "NUMBER", "INTEGER", "BOOLEAN"
    val properties: Map<String, Schema>? = null,
    val required: List<String>? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ToolConfig(
    val functionCallingConfig: FunctionCallingConfig
)

@JsonClass(generateAdapter = true)
data class FunctionCallingConfig(
    val mode: String // "ANY", "AUTO", "NONE"
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?,
    val promptFeedback: PromptFeedback? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content,
    val finishReason: String? = null,
    val index: Int? = null
)

@JsonClass(generateAdapter = true)
data class PromptFeedback(
    val blockReason: String? = null
)
