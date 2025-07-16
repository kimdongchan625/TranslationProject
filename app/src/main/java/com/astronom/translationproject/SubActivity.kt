package com.astronom.translationproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.astronom.translationproject.ui.theme.TranslationProjectTheme
import com.google.mlkit.vision.text.Text

class SubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var receivedRecognizedText = intent.getStringExtra("recognizedText") ?: "내용없음"
        var receivedTranslatedText = intent.getStringExtra("translatedText") ?: "내용없음"


        setContent {
            TranslationProjectTheme {
                DetailScreen(
                    recognizedText = receivedRecognizedText,
                    translatedText = receivedTranslatedText
                )
            }
        }
    }
}

@Composable
fun DetailScreen(recognizedText: String, translatedText: String) {
    var editableRecognizedText by remember { mutableStateOf(recognizedText) }
    var editableTranslatedText by remember { mutableStateOf(translatedText) }

    Column(
        Modifier.fillMaxSize(),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        Text(text = "Detail Screen", fontSize = 30.sp)
        Row {
            TextField(
                value = editableRecognizedText,
                onValueChange = { newText ->
                    editableRecognizedText = newText
                },
                label = { Text("인식된 텍스트") },
                modifier = Modifier.weight(1f)
            )
            TextField(
                value = editableTranslatedText,
                onValueChange = { newText ->
                    editableTranslatedText = newText
                },
                label = { Text("번역된 텍스트") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}