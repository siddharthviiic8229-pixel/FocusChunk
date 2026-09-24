package com.example.focuschunk


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

class MainActivity : ComponentActivity() {

    private val viewModel: ReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ReaderScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color.Black else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFEFE8F4)
    var inputText by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {

        Column(
            modifier = modifier
                .fillMaxSize()
            .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Paste long text here") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = textColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Button(
                onClick = { viewModel.loadArticle(inputText) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Chunk Text")
            }

            if (viewModel.chunks.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Chunk ${viewModel.currentChunkIndex + 1} of ${viewModel.chunks.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor
                        )
                        Text(
                            text = viewModel.chunks[viewModel.currentChunkIndex],
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.previousChunk() },
                        enabled = viewModel.currentChunkIndex > 0
                    ) {
                        Text("Previous")
                    }

                    Button(
                        onClick = { viewModel.explainCurrentChunk() }
                    ) {
                        Text("AI Simplify")
                    }

                    Button(
                        onClick = { viewModel.nextChunk() },
                        enabled = viewModel.currentChunkIndex < viewModel.chunks.size - 1
                    ) {
                        Text("Next")
                    }
                }
                Button(
                    onClick = {
                        viewModel.resetSession()
                        inputText = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Over")
                }
            }


            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (viewModel.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.errorMessage ?: "An unknown error occurred.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (viewModel.currentExplanation.isNotEmpty()) {
                Surface(
                    color = cardBg,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AI Key Takeaways",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = renderMarkdownText(viewModel.currentExplanation),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun renderMarkdownText(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        val cleanedText = text
            .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
            .replace(": *", ":")
            .replace(":*", ":")
            .replace("* ", "• ")
            .replace("*", "")
            .replace(":•", ":")
        val parts = cleanedText.split("**")

        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(
                    style = androidx.compose.ui.text.SpanStyle(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                ) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}