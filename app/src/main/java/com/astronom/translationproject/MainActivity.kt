package com.astronom.translationproject

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.astronom.translationproject.ui.theme.TranslationProjectTheme
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import androidx.camera.core.Preview as CameraPreview


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                0
            )
        }

        setContent {
            TranslationProjectTheme {
                MainScreen()
            }
        }
    }
}

// MainScreen() 밖에 위치 (최상위 레벨)
val recognizedTextState = mutableStateOf("")
val cameraExecutor = Executors.newSingleThreadExecutor()
val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

// 마지막으로 이미지를 분석한 시간을 저장할 변수
private var lastAnalyzedTimestamp = 0L
private const val ANALYSIS_INTERVAL_MILLIS = 3000L // 3초 (3000 밀리초)


@Composable
fun MainScreen() {

    val context = LocalContext.current

    // recognizedTextState를 직접 사용합니다.
    val lifecycleOwner = LocalLifecycleOwner.current
    var flag by remember { mutableStateOf(false) }
    var flagTwo by remember { mutableStateOf(false) }

    // recognizeTextState 값을 가져와 UI에 바인딩

    var recognizedText by recognizedTextState
    var lastRecognizedText: String by remember { mutableStateOf("") }

    val imageAnalyzer = remember {
        ImageAnalysis.Builder()
            // 분석 속도보다 프레임 속도가 빠를 때 최신 이미지만 유지
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                // 수정된 YourImageAnalyzer 인스턴스 사용
                it.setAnalyzer(cameraExecutor, YourImageAnalyzer())
            }
    }
    var isReady by remember { mutableStateOf(false) }

    val enKoTranslator = remember {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.KOREAN)
            .build()
        Translation.getClient(options)
    }

    LaunchedEffect(enKoTranslator) {
        var conditions =
            DownloadConditions.Builder()
//        .requireWifi()
                .build()

        enKoTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                isReady = true
                // Model downloaded successfully. Okay to start translating.
                // (Set a flag, unhide the translation UI, etc.)
            }
            .addOnFailureListener { exception ->
                // Model couldn’t be downloaded or other internal error.
                // ...
            }
    }
    var translated by remember { mutableStateOf("") }
    LaunchedEffect(
        lastRecognizedText,
        flag,
        isReady
    ) { // lastRecognizedText, flag, isReady가 변경될 때 실행
        if (isReady && lastRecognizedText.isNotEmpty()) { // flag가 true이고, 모델이 준비되었고, 인식된 텍스트가 있을 때만 번역
            enKoTranslator.translate(lastRecognizedText)
                .addOnSuccessListener { translatedText ->
                    translated = translatedText
                }
                .addOnFailureListener { exception ->
                    Log.e("Translation", "Translation failed", exception)
                    translated = "번역 실패" // 사용자에게 오류 표시
                }
        } else if (!flag || lastRecognizedText.isEmpty()) {
            translated = "" // 번역 비활성화 또는 인식된 텍스트가 없으면 번역된 텍스트 초기화
        }
    }
    val resultText = translated
        .replace(Regex("[^\\p{IsHangul}]"), " ")   // 한글 제외하고 공백
        .replace(Regex("\\s+"), " ")               // 다중 공백 하나로 줄이기
        .trim()                                     // 앞뒤 공백 제거
    var lastResultText: String by remember { mutableStateOf("") }


    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
        Arrangement.Top,
        Alignment.CenterHorizontally,

        ) {
        Spacer(Modifier.size(50.dp))
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween
        ) {
            Text(
                text = "Text Extractor",
                fontSize = 28.sp,
                color = Color(0xFF00FF00), // 네온 그린
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
            Button(
                onClick = {
                    val intent = Intent(context, SubActivity::class.java).apply {
                        putExtra("recognizedText", lastRecognizedText)
                        putExtra("translatedText", lastResultText)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black, // 버튼 배경 검정
                    contentColor = Color(0xFF00FF00) // 텍스트/아이콘 색상 네온 그린
                )
            ) {
                Text(text = "detail->")
            }
        }
        // 카메라 프리뷰 뷰 추가
        CameraPreviewView(
            modifier = Modifier // 🚩 Modifier 적용
                .fillMaxWidth() // 가로를 최대로 채우고
                .height(400.dp) // 높이를 400dp로 제한 (예시)
                // .size(300.dp) // 또는 가로세로 300dp 정사각형으로 만들 수도 있습니다.
                // .aspectRatio(3f / 4f) // 특정 비율 유지 (예: 3:4)
                .clip(RoundedCornerShape(8.dp)), // 둥근 모서리 추가 (선택 사항)
            context = context,
            lifecycleOwner = lifecycleOwner,
            imageAnalyzer = imageAnalyzer
        )
        Spacer(Modifier.size(20.dp))
        Row {
            Button(
                onClick = {
                    flag = !flag
                },
                colors = ButtonDefaults.buttonColors( // 🚩 버튼 색상 네온 그린
                    containerColor = Color(0xFF00FF00), // 네온 그린 배경
                    contentColor = Color.Black // 텍스트 색상 검정
                ),
                shape = RoundedCornerShape(8.dp), // 둥근 모서리
            ) {
                if (flag) {
                    Text(text = "텍스트 추출 중지")
                } else {
                    Text(text = "텍스트 추출 시작")
                }
            }
            Spacer(Modifier.size(20.dp))
            Button(
                onClick = {
                    flagTwo = !flagTwo
                },
                colors = ButtonDefaults.buttonColors( // 🚩 버튼 색상 네온 그린
                    containerColor = Color(0xFF00FF00), // 네온 그린 배경
                    contentColor = Color.Black // 텍스트 색상 검정
                ),
                shape = RoundedCornerShape(8.dp), // 둥근 모서리

            ) {
                if (flagTwo) {
                    Text(text = "번역 중지")
                } else {
                    Text(text = "번역 시작")
                }
            }
        }
        Spacer(Modifier.size(20.dp))
        Row {
            Text(text = "인식된 텍스트", fontSize = 25.sp, color = Color.Cyan)
            Spacer(Modifier.size(10.dp))
            Text(text = "번역된 텍스트", fontSize = 25.sp, color = Color.Magenta)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            if (flag) {
                lastRecognizedText = recognizedText
            }
            Text(
                text = lastRecognizedText,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = Color.Cyan,
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 16.dp)
            )
            if (flagTwo) {
                lastResultText = resultText
            }
            Text(
                text = lastResultText,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = Color.Magenta,
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 16.dp)
            )
        }

    }
}


@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier, // 🚩 여기에 Modifier 인자 추가
    context: Context,
    lifecycleOwner: LifecycleOwner,
    imageAnalyzer: ImageAnalysis, // ImageAnalysis 유스케이스를 인자로 받도록 추가
) {
    val previewView = remember { PreviewView(context) }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = CameraPreview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                // preview와 imageAnalyzer를 함께 바인딩
                cameraProvider.bindToLifecycle(

                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer // 여기에 ImageAnalysis 유스케이스 추가
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("CameraX", "Error binding camera use cases", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}


@androidx.annotation.OptIn(ExperimentalGetImage::class)
private class YourImageAnalyzer : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()

        // 3초 (3000ms)가 지났을 때만 이미지 분석을 수행
        if (currentTime - lastAnalyzedTimestamp >= ANALYSIS_INTERVAL_MILLIS) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        recognizedTextState.value = visionText.text
                        // Logcat으로 확인
                        Log.d("ImageAnalysis", "Text recognized: ${visionText.text}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ImageAnalysis", "Text recognition failed: ${e.message}", e)
                    }
                    .addOnCompleteListener {
                        // ML Kit 처리가 완료된 후 타임스탬프 업데이트 및 ImageProxy 닫기
                        lastAnalyzedTimestamp = currentTime // 여기서 업데이트해야, 처리가 완료된 후 다음 간격이 시작됨
                        imageProxy.close()
                    }
            } else {
                imageProxy.close() // mediaImage가 null인 경우에도 닫아줘야 함
            }
        } else {
            // 3초 간격이 아직 안 지났으면 이미지 분석을 건너뛰고 바로 닫음
            imageProxy.close()
        }
    }
}