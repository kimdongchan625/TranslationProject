package com.astronom.translationproject

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.astronom.translationproject.ui.theme.TranslationProjectTheme
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.mlkit.vision.text.Text

class SubActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
        var receivedRecognizedText = intent.getStringExtra("recognizedText") ?: "내용없음"
        var receivedTranslatedText = intent.getStringExtra("translatedText") ?: "내용없음"


        setContent {
            TranslationProjectTheme {
                DetailScreen(
                    firebaseAnalytics = firebaseAnalytics,
                    recognizedText = receivedRecognizedText,
                    translatedText = receivedTranslatedText
                )
            }
        }
    }
}

@Composable
fun DetailScreen(
    recognizedText: String,
    translatedText: String,
    firebaseAnalytics: FirebaseAnalytics,
) {
    var editableRecognizedText by remember { mutableStateOf(recognizedText) }
    var editableTranslatedText by remember { mutableStateOf(translatedText) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
        Arrangement.Center,
        Alignment.CenterHorizontally,

        ) {
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween
        ) {
            Text(
                text = "Detail Screen", fontSize = 30.sp,
                color = Color(0xFF00FF00)
            )

            Button(
                onClick = {
                    firebaseAnalytics.logEvent("detail_screen_accessed") {
                        param("translated_text_on_detail_access", editableTranslatedText)
                    }
                    Log.d("FirebaseAnalytics", "Event logged: detail_screen_accessed")
                    Log.d("FirebaseAnalytics", "Translated text sent: $editableTranslatedText")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black, // 버튼 배경 검정
                    contentColor = Color(0xFF00FF00) // 텍스트/아이콘 색상 네온 그린
                )
            ) {
                Text(text = "전송")
            }
        }
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