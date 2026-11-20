package com.mindguard.ui.present

import com.mindguard.network.CompilerClient
import com.mindguard.network.InsightResult
import com.mindguard.network.InsightsRequest
import com.mindguard.shared.policy.CompileResponse
import com.mindguard.shared.policy.PresentPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val GENERIC_COMPILE_ERROR = "Something went wrong while compiling. Please try again."
const val GENERIC_INSIGHTS_ERROR = "Something went wrong while fetching your insight. Please try again."

sealed interface CompileUiState {
    data object Idle : CompileUiState
    data object Loading : CompileUiState
    data class Review(val policy: PresentPolicy) : CompileUiState
    data class Rejected(val reason: String) : CompileUiState
    data class Error(val message: String) : CompileUiState
}

class IntentCompiler(private val client: CompilerClient) {

    private val _state = MutableStateFlow<CompileUiState>(CompileUiState.Idle)
    val state: StateFlow<CompileUiState> = _state.asStateFlow()

    suspend fun compile(rawPrompt: String, existing: PresentPolicy?) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _state.value == CompileUiState.Loading) return
        _state.value = CompileUiState.Loading
        _state.value = try {
            when (val response = client.compile(prompt, existing)) {
                is CompileResponse.Compiled -> CompileUiState.Review(response.policy)
                is CompileResponse.Rejected -> CompileUiState.Rejected(response.reason)
                is CompileResponse.Failed   -> CompileUiState.Error(response.message)
            }
        } catch (e: CancellationException) {
            _state.value = CompileUiState.Idle
            throw e
        } catch (e: Exception) {
            CompileUiState.Error(GENERIC_COMPILE_ERROR)
        }
    }

    fun reset() {
        _state.value = CompileUiState.Idle
    }
}

sealed interface InsightsUiState {
    data object Idle : InsightsUiState
    data object Loading : InsightsUiState
    data class Ready(val headline: String, val insight: String, val suggestion: String) : InsightsUiState
    data class Error(val message: String) : InsightsUiState
}

class InsightsLoader(private val client: CompilerClient) {

    private val _state = MutableStateFlow<InsightsUiState>(InsightsUiState.Idle)
    val state: StateFlow<InsightsUiState> = _state.asStateFlow()

    suspend fun load(request: InsightsRequest) {
        if (_state.value == InsightsUiState.Loading) return
        _state.value = InsightsUiState.Loading
        _state.value = try {
            when (val result = client.insights(request)) {
                is InsightResult.Success -> InsightsUiState.Ready(result.headline, result.insight, result.suggestion)
                is InsightResult.Failure -> InsightsUiState.Error(result.message)
            }
        } catch (e: CancellationException) {
            _state.value = InsightsUiState.Idle
            throw e
        } catch (e: Exception) {
            InsightsUiState.Error(GENERIC_INSIGHTS_ERROR)
        }
    }
}
