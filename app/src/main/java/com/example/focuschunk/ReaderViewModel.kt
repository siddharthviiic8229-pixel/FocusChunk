package com.example.focuschunk

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    var explanations by mutableStateOf<Map<Int, String>>(emptyMap())
        private set

    val currentExplanation: String
        get() = explanations[currentChunkIndex] ?: ""

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Gemini API Setup
    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.1-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    // Action: Process large text block into smart chunks for readability
    // (avoids splitting on abbreviations like Mr./Mrs./Dr. etc.)
    fun loadArticle(fullText: String) {
        rawText = fullText
        chunks = createSmartChunks(fullText)
        currentChunkIndex = 0
        explanations = emptyMap()
        errorMessage = null
    }

    private fun createSmartChunks(text: String, targetWordCount: Int = 50): List<String> {
        val sentenceRegex =  "(?<!\\b(Mr|Mrs|Ms|Dr|Prof|vs))(?<!\\b[A-Z])\\.\\s+".toRegex()
        val sentences = text.split(sentenceRegex)

        val chunksList = mutableListOf<String>()
        var currentChunk = StringBuilder()
        var currentWordCount = 0

        for (sentence in sentences) {
            val trimmed = sentence.trim()
            if (trimmed.isEmpty()) continue

            val sentenceWords = trimmed.split("\\s+".toRegex()).size

            if (currentWordCount + sentenceWords > targetWordCount && currentChunk.isNotEmpty()) {
                chunksList.add(currentChunk.toString().trim())
                currentChunk = StringBuilder()
                currentWordCount = 0
            }

            if (currentChunk.isNotEmpty()) currentChunk.append(" ")
            currentChunk.append(trimmed)
            if (!trimmed.endsWith(".")) currentChunk.append(".")

            currentWordCount += sentenceWords
        }

        if (currentChunk.isNotEmpty()) {
            chunksList.add(currentChunk.toString().trim())
        }

        return chunksList
    }

    // Action: Move to next/previous chunk
    fun nextChunk() {
        if (currentChunkIndex < chunks.size - 1) {
            currentChunkIndex++

        }
    }

    fun previousChunk() {
        if (currentChunkIndex > 0) {
            currentChunkIndex--

        }
    }
    fun resetSession() {
        rawText = ""
        chunks = emptyList()
        currentChunkIndex = 0
        explanations = emptyMap()
        errorMessage = null
    }

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
                explanations = explanations + (currentChunkIndex to (response.text
                    ?: "No explanation generated."))
            } catch (e: Exception) {
                android.util.Log.e("FocusChunkLog", "API Error: ${e.message}", e)

                val msg = e.message?.lowercase() ?: ""
                val isNetworkError = e is java.net.UnknownHostException ||
                        e is java.net.ConnectException ||
                        msg.contains("unable to resolve host") ||
                        msg.contains("failed to connect") ||
                        msg.contains("network") ||
                        msg.contains("socket") ||
                        msg.contains("unexpected")

                errorMessage = if (isNetworkError) {
                    "Please check internet connection and retry."
                } else {
                    "Unable to generate summary. Please try again later."
                }

            } finally {
                isLoading = false
            }
        }
    }
}