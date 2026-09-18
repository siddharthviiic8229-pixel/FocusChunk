package com.example.focuschunk

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focuschunk.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

class ReaderViewModel : ViewModel() {

    // UI States
    var rawText by mutableStateOf("")
        private set

    var chunks by mutableStateOf<List<String>>(emptyList())
        private set

    var currentChunkIndex by mutableIntStateOf(0)
        private set

    var aiExplanation by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Gemini API Setup (Replace with your actual API key)
    private val generativeModel = GenerativeModel(
        modelName =  "gemini-3.1-flash-lite" ,
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    // Action: Process large text block into smaller chunks for readability
    fun processInputText(input: String) {
        rawText = input
        // Split by sentences or paragraphs (adjust regex based on formatting needs)
        chunks = input.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        currentChunkIndex = 0
        aiExplanation = ""
        errorMessage = null
    }

    // Action: Move to next/previous chunk
    fun nextChunk() {
        if (currentChunkIndex < chunks.size - 1) {
            currentChunkIndex++
            aiExplanation = ""
        }
    }

    fun previousChunk() {
        if (currentChunkIndex > 0) {
            currentChunkIndex--
            aiExplanation = ""
        }
    }

    // Action: Call Gemini API asynchronously for ADHD/Dyslexia friendly simplification
    // Action: Call Gemini API asynchronously for ADHD/Dyslexia friendly simplification
    fun explainCurrentChunk() {
        val currentText = chunks.getOrNull(currentChunkIndex) ?: return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            android.util.Log.d("FocusChunkLog", "AI Simplify clicked! Text: $currentText")
            try {
                val prompt =
                    "Simplify this text into a concise, easy-to-read summary with key takeaways for a reader with ADHD: \"$currentText\""
                val response = generativeModel.generateContent(prompt)
                android.util.Log.d("FocusChunkLog", "Response received: ${response.text}")
                aiExplanation = response.text ?: "No explanation generated."
            } catch (e: Exception) {
                android.util.Log.e("FocusChunkLog", "API Error: ${e.message}", e)
                errorMessage = "Failed to load explanation: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }
}