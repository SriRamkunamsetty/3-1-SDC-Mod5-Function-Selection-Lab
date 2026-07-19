package com.example.ui.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.network.RetrofitClient
import com.example.domain.tool.LabTool
import com.example.domain.tool.LabToolRegistry
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimelineStep(
    val title: String,
    val description: String,
    val type: StepType,
    val content: String? = null,
    val isPending: Boolean = false
)

enum class StepType {
    REQUEST, ANALYZING, TOOL_SELECTED, DIRECT_ANSWER, TOOL_EXECUTED, MODEL_SYNTHESIS, FINAL_RESPONSE, ERROR
}

data class LabUiState(
    val userPrompt: String = "",
    val isLoading: Boolean = false,
    val timelineSteps: List<TimelineStep> = emptyList(),
    val rawJsonLogs: List<Pair<String, String>> = emptyList(),
    val errorMessage: String? = null,
    // Playground tool testing
    val selectedPlaygroundTool: LabTool? = null,
    val playgroundArgs: Map<String, String> = emptyMap(),
    val playgroundResult: String? = null
)

class LabViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LabUiState())
    val uiState = _uiState.asStateFlow()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val requestAdapter = moshi.adapter(GenerateContentRequest::class.java).indent("  ")
    private val responseAdapter = moshi.adapter(GenerateContentResponse::class.java).indent("  ")

    fun selectPlaygroundTool(tool: LabTool) {
        val initialArgs = tool.schema.properties?.mapValues { "" } ?: emptyMap()
        _uiState.update { it.copy(
            selectedPlaygroundTool = tool,
            playgroundArgs = initialArgs,
            playgroundResult = null
        ) }
    }

    fun updatePlaygroundArg(key: String, value: String) {
        _uiState.update { state ->
            val updated = state.playgroundArgs.toMutableMap().apply { put(key, value) }
            state.copy(playgroundArgs = updated)
        }
    }

    fun executePlaygroundTool() {
        val state = _uiState.value
        val tool = state.selectedPlaygroundTool ?: return
        
        try {
            // Map strings to appropriate schema type
            val typedArgs = state.playgroundArgs.mapValues { (key, value) ->
                val propSchema = tool.schema.properties?.get(key)
                if (propSchema?.type == "NUMBER" || propSchema?.type == "INTEGER") {
                    value.toDoubleOrNull() ?: 0.0
                } else {
                    value
                }
            }
            val result = tool.execute(typedArgs)
            val formattedResult = result.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            _uiState.update { it.copy(playgroundResult = formattedResult) }
        } catch (e: Exception) {
            _uiState.update { it.copy(playgroundResult = "Error: ${e.localizedMessage}") }
        }
    }

    fun sendRequest(prompt: String) {
        if (prompt.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true, 
                userPrompt = prompt,
                timelineSteps = listOf(
                    TimelineStep(
                        title = "User Request Sent",
                        description = "Real-time query received by the sandbox.",
                        type = StepType.REQUEST,
                        content = prompt
                    ),
                    TimelineStep(
                        title = "LLM Tool Selection",
                        description = "Gemini is evaluating your request against 5 registered tools...",
                        type = StepType.ANALYZING,
                        isPending = true
                    )
                ),
                rawJsonLogs = emptyList(),
                errorMessage = null
            ) }

            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    addTimelineStep(
                        TimelineStep(
                            title = "API Key Warning",
                            description = "No custom API key found in BuildConfig. Using fallback.",
                            type = StepType.ERROR,
                            content = "Fallback key is active. Responses may be rate-limited."
                        )
                    )
                }

                // 1. Prepare Content list
                val initialContent = Content(role = "user", parts = listOf(Part(text = prompt)))
                val contentsList = mutableListOf(initialContent)

                // 2. Prepare Tools
                val toolsDeclarations = LabToolRegistry.getGeminiToolDeclarations()

                // 3. Create Request
                val request = GenerateContentRequest(
                    contents = contentsList,
                    tools = toolsDeclarations,
                    generationConfig = GenerationConfig(temperature = 0.5f)
                )

                // Log raw request
                logJson("API Request (Turn 1 - Send Request & Tool Declarations)", requestAdapter.toJson(request))

                // 4. API Call
                val response = RetrofitClient.service.generateContent(apiKey, request)

                // Log raw response
                logJson("API Response (Turn 1 - Selection Decision)", responseAdapter.toJson(response))

                // 5. Inspect response
                val candidate = response.candidates?.firstOrNull()
                if (candidate == null) {
                    updateAnalyzingStep(false, "No response candidate returned by Gemini.", StepType.ERROR)
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val responseParts = candidate.content.parts
                val firstPart = responseParts.firstOrNull()

                if (firstPart?.functionCall != null) {
                    val call = firstPart.functionCall
                    val toolName = call.name
                    val toolArgs = call.args ?: emptyMap()

                    updateAnalyzingStep(
                        isPending = false,
                        description = "Gemini completed dynamic tool selection.",
                        type = StepType.TOOL_SELECTED
                    )

                    addTimelineStep(
                        TimelineStep(
                            title = "Tool Selected: $toolName",
                            description = "LLM extracted arguments and requested a local callback.",
                            type = StepType.TOOL_SELECTED,
                            content = "Selected Tool: $toolName\nArguments:\n${toolArgs.entries.joinToString("\n") { "  ${it.key}: ${it.value}" }}"
                        )
                    )

                    // Execute the tool locally!
                    addTimelineStep(
                        TimelineStep(
                            title = "Executing Local Tool...",
                            description = "Invoking Kotlin implementation in the domain layer.",
                            type = StepType.TOOL_EXECUTED,
                            isPending = true
                        )
                    )

                    val labTool = LabToolRegistry.getByName(toolName)
                    if (labTool == null) {
                        updateLastStep(
                            title = "Execution Failed",
                            description = "Tool '$toolName' not found in registry.",
                            type = StepType.ERROR,
                            content = "Error: Tool name mismatch"
                        )
                        _uiState.update { it.copy(isLoading = false) }
                        return@launch
                    }

                    // Run execution
                    val executionResult = try {
                        labTool.execute(toolArgs)
                    } catch (e: Exception) {
                        mapOf("error" to (e.message ?: "Unknown error"))
                    }

                    updateLastStep(
                        title = "Tool Executed Successfully",
                        description = "Local function returned raw results back to the environment.",
                        type = StepType.TOOL_EXECUTED,
                        content = "Local Result Map:\n${executionResult.entries.joinToString("\n") { "  ${it.key}: ${it.value}" }}"
                    )

                    // 6. Follow-up Turn to Model with Tool output
                    addTimelineStep(
                        TimelineStep(
                            title = "Sending Output back to LLM",
                            description = "Gemini is synthesizing a final answer using the tool's results...",
                            type = StepType.MODEL_SYNTHESIS,
                            isPending = true
                        )
                    )

                    // Build follow-up turns
                    // Turn 2 Content (Model requesting FunctionCall)
                    contentsList.add(candidate.content)

                    // Turn 3 Content (User providing FunctionResponse)
                    val functionResponsePart = Part(
                        functionResponse = FunctionResponse(
                            name = toolName,
                            response = executionResult
                        )
                    )
                    val responseContent = Content(role = "user", parts = listOf(functionResponsePart))
                    contentsList.add(responseContent)

                    val followUpRequest = GenerateContentRequest(
                        contents = contentsList,
                        tools = toolsDeclarations,
                        generationConfig = GenerationConfig(temperature = 0.3f)
                    )

                    // Log raw request 2
                    logJson("API Request (Turn 2 - Send Function Response)", requestAdapter.toJson(followUpRequest))

                    // Call API again
                    val followUpResponse = RetrofitClient.service.generateContent(apiKey, followUpRequest)

                    // Log raw response 2
                    logJson("API Response (Turn 2 - Final Synthetic Answer)", responseAdapter.toJson(followUpResponse))

                    val finalCandidate = followUpResponse.candidates?.firstOrNull()
                    val finalResponseParts = finalCandidate?.content?.parts
                    val finalResponseText = finalResponseParts?.firstOrNull()?.text 
                        ?: "Gemini completed tool execution but did not generate a summary text."

                    updateLastStep(
                        title = "Synthesis Complete",
                        description = "Gemini processed the tool data successfully.",
                        type = StepType.MODEL_SYNTHESIS
                    )

                    addTimelineStep(
                        TimelineStep(
                            title = "Final Answer Synthesized",
                            description = "Model has contextualized the tool results into a user-friendly response.",
                            type = StepType.FINAL_RESPONSE,
                            content = finalResponseText
                        )
                    )

                } else {
                    // Direct response without tool calling
                    updateAnalyzingStep(
                        isPending = false,
                        description = "Gemini determined no tool is required for this query.",
                        type = StepType.DIRECT_ANSWER
                    )

                    val responseText = firstPart?.text ?: "No response text"
                    addTimelineStep(
                        TimelineStep(
                            title = "Direct Text Response",
                            description = "Model responded directly without invoking any local functions.",
                            type = StepType.FINAL_RESPONSE,
                            content = responseText
                        )
                    )
                }

            } catch (e: Exception) {
                updateAnalyzingStep(false, "An error occurred during communication: ${e.message}", StepType.ERROR)
                addTimelineStep(
                    TimelineStep(
                        title = "Lab System Error",
                        description = e.localizedMessage ?: "Unknown connection or parsing failure.",
                        type = StepType.ERROR,
                        content = e.stackTraceToString()
                    )
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun logJson(title: String, json: String) {
        _uiState.update { state ->
            state.copy(rawJsonLogs = state.rawJsonLogs + (title to json))
        }
    }

    private fun addTimelineStep(step: TimelineStep) {
        _uiState.update { state ->
            state.copy(timelineSteps = state.timelineSteps + step)
        }
    }

    private fun updateAnalyzingStep(isPending: Boolean, description: String, type: StepType) {
        _uiState.update { state ->
            val updated = state.timelineSteps.map { step ->
                if (step.type == StepType.ANALYZING) {
                    step.copy(isPending = isPending, description = description, type = type)
                } else {
                    step
                }
            }
            state.copy(timelineSteps = updated)
        }
    }

    private fun updateLastStep(title: String, description: String, type: StepType, content: String? = null) {
        _uiState.update { state ->
            if (state.timelineSteps.isEmpty()) return@update state
            val lastIdx = state.timelineSteps.lastIndex
            val updated = state.timelineSteps.toMutableList().apply {
                this[lastIdx] = this[lastIdx].copy(
                    title = title,
                    description = description,
                    type = type,
                    content = content,
                    isPending = false
                )
            }
            state.copy(timelineSteps = updated)
        }
    }
}
