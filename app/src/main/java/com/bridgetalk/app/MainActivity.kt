package com.bridgetalk.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgetalk.app.sender.ConfirmDraftMessageSender
import com.bridgetalk.app.sender.LocalTtsVoice
import com.bridgetalk.app.sender.OkHttpSendTransport
import com.bridgetalk.app.sender.VoiceSpeaker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private val Navy = Color(0xFF0B1630)
private val Blue = Color(0xFF2457D6)
private val SurfaceBlue = Color(0xFFEAF0FF)
private val Green = Color(0xFF0C6B4D)
private val AppBackground = Color(0xFFF4F6FA)
private val MutedText = Color(0xFF4C5870)
private val WarningSurface = Color(0xFFFFF4E5)
private val WarningText = Color(0xFF7A4300)
private const val MorseDashThresholdMillis = 250L

internal data class SendDraftRequest(
    val message: String,
    val idempotencyKey: String,
)

internal sealed interface SendDraftResult {
    data class Accepted(val userMessage: String) : SendDraftResult
    data class Rejected(val userMessage: String) : SendDraftResult
}

internal fun interface DraftMessageSender {
    suspend fun send(request: SendDraftRequest): SendDraftResult
}

private sealed interface SendUiState {
    data object Idle : SendUiState
    data class Sending(val request: SendDraftRequest) : SendUiState
    data class Sent(val message: String) : SendUiState
    data class Failed(val request: SendDraftRequest, val message: String) : SendUiState
}

private fun morseTokenForPress(durationMillis: Long): String =
    if (durationMillis < MorseDashThresholdMillis) "." else "-"

private class SoundFeedback {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)

    fun morseToken(isDash: Boolean) {
        val tone = if (isDash) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP
        toneGenerator.startTone(tone, 110)
    }

    fun action() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 140)
    }

    fun close() {
        toneGenerator.release()
    }
}

private class SpeechInputController(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onStatus: (String) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null

    fun start(languageTag: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStatus("Chưa có dịch vụ nhận dạng giọng nói trên thiết bị")
            return
        }

        stop()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { service ->
            service.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onStatus("Đang nghe… nói xong hãy dừng lại")
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    onStatus("Đang chuyển giọng nói thành chữ…")
                }

                override fun onError(error: Int) {
                    onStatus("Chưa nhận được giọng nói · bạn có thể thử lại")
                    stop()
                }

                override fun onResults(results: Bundle?) {
                    val result = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!result.isNullOrBlank()) onFinalResult(result)
                    onStatus(if (result.isNullOrBlank()) "Chưa nhận được nội dung" else "Đã nhận nội dung bằng giọng nói")
                    stop()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.let(onPartialResult)
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            service.startListening(intent)
        }
    }

    fun stop() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val voice = remember { LocalTtsVoice(context) }
            DisposableEffect(voice) {
                onDispose { voice.close() }
            }
            val messageSender: DraftMessageSender = remember(voice) {
                ConfirmDraftMessageSender(
                    transport = OkHttpSendTransport(
                        baseUrl = BuildConfig.BACKEND_BASE_URL,
                    ),
                    callId = BuildConfig.DEFAULT_CALL_ID,
                    offlineSpeaker = VoiceSpeaker { text -> voice.speak(text) },
                )
            }
            BridgeTalkTheme {
                BridgeTalkApp(messageSender = messageSender)
            }
        }
    }
}

private enum class AppScreen {
    Welcome,
    Profile,
    Contacts,
    IncomingCall,
    Call,
    Consent,
}

@Composable
private fun BridgeTalkApp(
    messageSender: DraftMessageSender,
) {
    var screen by remember { mutableStateOf(AppScreen.Welcome) }
    var displayName by remember { mutableStateOf("Mai") }
    var bridgeId by remember { mutableStateOf("mai-2026") }
    var friendAdded by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F9FC)) {
        when (screen) {
            AppScreen.Welcome -> WelcomeScreen { screen = AppScreen.Profile }
            AppScreen.Profile -> ProfileScreen(
                displayName = displayName,
                bridgeId = bridgeId,
                onDisplayNameChange = { displayName = it },
                onBridgeIdChange = { bridgeId = it },
                onDone = { screen = AppScreen.Contacts },
            )
            AppScreen.Contacts -> ContactsScreen(
                displayName = displayName,
                friendAdded = friendAdded,
                onAddFriend = { friendAdded = true },
                onCall = { screen = AppScreen.IncomingCall },
            )
            AppScreen.IncomingCall -> IncomingCallScreen(
                onDecline = { screen = AppScreen.Contacts },
                onAccept = { screen = AppScreen.Call },
            )
            AppScreen.Call -> AccessibleCallScreenV2(
                onEnd = { screen = AppScreen.Consent },
                messageSender = messageSender,
            )
            AppScreen.Consent -> ConsentScreen(
                onDone = { screen = AppScreen.Contacts },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
        color = Navy,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
    )
    if (subtitle != null) {
        Spacer(Modifier.height(8.dp))
        Text(text = subtitle, color = Color(0xFF44506A), fontSize = 17.sp, lineHeight = 25.sp)
    }
}

@Composable
private fun WelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("BridgeTalk", color = Blue, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        SectionHeader(
            title = "Kết nối theo cách của bạn",
            subtitle = "Một cuộc gọi 1-1, nơi lời nói được nhìn thấy và câu trả lời được phát ra khi bạn sẵn sàng.",
        )
        Spacer(Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceBlue)) {
            Text(
                "Nghe trực tiếp, đọc caption hoặc trả lời bằng giọng nói và Morse — bạn chọn cách phù hợp nhất.",
                modifier = Modifier.padding(16.dp),
                color = Navy,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            )
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
            shape = RoundedCornerShape(18.dp),
        ) { Text("Đăng nhập để bắt đầu", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        Text(
            "BridgeTalk giúp mọi người giữ trọn cuộc trò chuyện theo cách họ muốn.",
            color = Color(0xFF44506A),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProfileScreen(
    displayName: String,
    bridgeId: String,
    onDisplayNameChange: (String) -> Unit,
    onBridgeIdChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader("Tạo hồ sơ", "Một Bridge ID để bạn bè tìm thấy bạn, không cần chia sẻ số điện thoại.")
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tên hiển thị") },
            singleLine = true,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = bridgeId,
            onValueChange = onBridgeIdChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Bridge ID") },
            supportingText = { Text("Dùng mã này để kết bạn.") },
            singleLine = true,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onDone,
            enabled = displayName.isNotBlank() && bridgeId.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
        ) { Text("Hoàn tất hồ sơ", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ContactsScreen(
    displayName: String,
    friendAdded: Boolean,
    onAddFriend: () -> Unit,
    onCall: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { SectionHeader("Danh bạ", "Xin chào, $displayName") }
            Text("Hồ sơ", color = Blue, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
        Spacer(Modifier.height(24.dp))
        if (!friendAdded) {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceBlue)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Chưa có bạn bè", color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Thêm một Bridge ID để thử cuộc gọi 1-1.", color = Color(0xFF44506A), fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onAddFriend, colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                        Text("Thêm An Nam")
                    }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).background(Blue, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("A", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("An Nam", color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Bridge ID: an-nam", color = Color(0xFF44506A))
                    }
                    Button(onClick = onCall, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                        Text("Gọi")
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingCallScreen(onDecline: () -> Unit, onAccept: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Cuộc gọi BridgeTalk", color = Blue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Text("An Nam", color = Navy, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("đang gọi cho bạn", color = Color(0xFF44506A), fontSize = 18.sp)
        Spacer(Modifier.height(36.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDecline, modifier = Modifier.height(60.dp)) { Text("Từ chối", fontSize = 17.sp) }
            Button(
                onClick = onAccept,
                modifier = Modifier.height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green),
            ) { Text("Chấp nhận", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun AccessibleCallScreen(onEnd: () -> Unit) {
    val context = LocalContext.current
    var rawMorse by remember { mutableStateOf("") }
    var currentCode by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Cuộc gọi âm thanh đang hoạt động") }
    var sourceLanguage by remember { mutableStateOf("VI") }
    var targetLanguage by remember { mutableStateOf("EN") }
    var isListening by remember { mutableStateOf(false) }
    var isMorsePressed by remember { mutableStateOf(false) }
    val suggestions = listOf(
        "Có",
        "Không",
        "Chờ tôi",
        "Tôi cần giúp đỡ",
        "Cảm ơn bạn",
    )
    val sound = remember { SoundFeedback() }
    val speechController = remember(context) {
        SpeechInputController(
            context = context,
            onPartialResult = { partial -> if (partial.isNotBlank()) draft = partial },
            onFinalResult = { result -> draft = result; sound.action() },
            onStatus = { message ->
                status = message
                isListening = message.startsWith("Đang nghe") || message.startsWith("Đang chuyển")
            },
        )
    }
    val requestAudioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            isListening = true
            speechController.start(if (sourceLanguage == "VI") "vi-VN" else "en-US")
        } else {
            status = "Cần quyền microphone để nhập bằng giọng nói"
        }
    }
    DisposableEffect(sound, speechController) {
        onDispose {
            sound.close()
            speechController.stop()
        }
    }
    LaunchedEffect(currentCode) {
        if (currentCode.isNotBlank()) {
            delay(700)
            MorseDecoder.decode(currentCode)?.let { decoded ->
                draft += decoded
                sound.action()
                status = "Đã tự giải mã: $decoded"
                currentCode = ""
            } ?: run {
                status = "Chuỗi Morse chưa hợp lệ · bạn có thể sửa lại"
            }
        }
    }

    fun appendMorse(token: String) {
        currentCode += token
        rawMorse += token
        sound.morseToken(token == "-")
        status = if (token == ".") "Đã nhận chấm · có âm báo" else "Đã nhận gạch · có âm báo"
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("An Nam", color = Navy, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Đang kết nối", color = Green, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = sourceLanguage == "VI", onClick = { sourceLanguage = "VI" }, label = { Text("Nguồn: VI") })
            FilterChip(selected = targetLanguage == "EN", onClick = { targetLanguage = "EN" }, label = { Text("Dịch: EN") })
        }
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Âm thanh cuộc gọi", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Bạn có thể nghe người gọi nói trực tiếp.", color = Color(0xFF44506A), fontSize = 14.sp)
                }
                OutlinedButton(
                    onClick = {
                        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            isListening = true
                            speechController.start(if (sourceLanguage == "VI") "vi-VN" else "en-US")
                        } else {
                            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.height(54.dp),
                ) { Text(if (isListening) "Đang nghe" else "Nói để nhập") }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                Text("CAPTION TRỰC TIẾP", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("Tôi cần thêm thời gian để trả lời.", color = Navy, fontSize = 22.sp, lineHeight = 30.sp)
                Text("I need more time to reply.", color = Color(0xFF44506A), fontSize = 20.sp, lineHeight = 28.sp)
                Spacer(Modifier.height(8.dp))
                Text("Caption cuối · bản dịch tùy chọn", color = Green, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceBlue)) {
            Column(Modifier.padding(16.dp)) {
                Text("NHẬP BẰNG MORSE", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(if (rawMorse.isBlank()) "Chưa có tín hiệu" else rawMorse, color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("MessageDraft · B kiểm tra trước khi gửi") },
                    minLines = 2,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                        .semantics {
                            contentDescription = "Nút Morse. Chạm nhanh dưới một phần tư giây để nhập chấm. Giữ ngắn từ một phần tư giây để nhập gạch."
                            role = Role.Button
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isMorsePressed = true
                                    try {
                                        val start = SystemClock.elapsedRealtime()
                                        tryAwaitRelease()
                                        val duration = SystemClock.elapsedRealtime() - start
                                        val token = morseTokenForPress(duration)
                                        currentCode += token
                                        rawMorse += token
                                        sound.morseToken(token == "-")
                                        status = if (token == ".") "Đã nhận chấm · có âm báo" else "Đã nhận gạch · có âm báo"
                                    } finally {
                                        isMorsePressed = false
                                    }
                                },
                            )
                        }
                        .background(if (isMorsePressed) Navy else Blue, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("CHẠM NHANH = ·\nGIỮ NGẮN = -\n${if (currentCode.isBlank()) "thả để nhập" else currentCode}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { currentCode = ""; status = "Đã xóa ký tự đang nhập" }) { Text("Xóa ký tự") }
                    OutlinedButton(onClick = { draft += " "; rawMorse += " / "; currentCode = ""; sound.action() }) { Text("Khoảng trắng") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            status,
            color = Green,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(14.dp))
        Text("GỢI Ý TRẢ LỜI", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        suggestions.forEach { suggestion ->
            FilterChip(
                selected = draft == suggestion,
                onClick = { draft = suggestion; sound.action(); status = "Đã chọn câu trả lời · chưa gửi" },
                label = { Text(suggestion, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { sound.action(); status = "Đã gửi nội dung đã xác nhận" },
            enabled = draft.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .semantics {
                    contentDescription = "Phát ngay nội dung đã nhập cho người gọi"
                },
            colors = ButtonDefaults.buttonColors(containerColor = Green),
        ) { Text("PHÁT NGAY", fontSize = 21.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onEnd, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Kết thúc cuộc gọi") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessibleCallScreenV2(
    onEnd: () -> Unit,
    messageSender: DraftMessageSender,
) {
    val context = LocalContext.current
    var rawMorse by remember { mutableStateOf("") }
    var currentCode by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Cuộc gọi âm thanh đang hoạt động") }
    var sendUiState: SendUiState by remember { mutableStateOf(SendUiState.Idle) }
    var sourceLanguage by remember { mutableStateOf("VI") }
    var targetLanguage by remember { mutableStateOf("EN") }
    var isListening by remember { mutableStateOf(false) }
    val suggestions = listOf("Có", "Không", "Chờ tôi", "Tôi cần giúp đỡ", "Cảm ơn bạn")
    val sound = remember { SoundFeedback() }
    val sendScope = rememberCoroutineScope()
    val speechController = remember(context) {
        SpeechInputController(
            context = context,
            onPartialResult = { partial ->
                if (partial.isNotBlank()) {
                    draft = partial
                    sendUiState = SendUiState.Idle
                }
            },
            onFinalResult = { result ->
                draft = result
                sendUiState = SendUiState.Idle
                sound.action()
            },
            onStatus = { message ->
                status = message
                isListening = message.startsWith("Đang nghe") || message.startsWith("Đang chuyển")
            },
        )
    }
    val requestAudioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            isListening = true
            speechController.start(if (sourceLanguage == "VI") "vi-VN" else "en-US")
        } else {
            status = "Cần quyền microphone để nhập bằng giọng nói"
        }
    }
    DisposableEffect(sound, speechController) {
        onDispose {
            sound.close()
            speechController.stop()
        }
    }
    LaunchedEffect(currentCode) {
        if (currentCode.isNotBlank()) {
            delay(700)
            MorseDecoder.decode(currentCode)?.let { decoded ->
                draft += decoded
                sendUiState = SendUiState.Idle
                sound.action()
                status = "Đã tự giải mã: $decoded"
                currentCode = ""
            } ?: run {
                status = "Chuỗi Morse chưa hợp lệ · bạn có thể sửa lại"
            }
        }
    }

    fun appendMorse(token: String) {
        currentCode += token
        rawMorse += token
        sendUiState = SendUiState.Idle
        sound.morseToken(token == "-")
        status = if (token == ".") "Đã nhận chấm · có âm báo" else "Đã nhận gạch · có âm báo"
    }

    fun sendDraft() {
        val normalizedDraft = draft.trim()
        if (normalizedDraft.isBlank() || currentCode.isNotBlank() || sendUiState is SendUiState.Sending) return

        val request = (sendUiState as? SendUiState.Failed)
            ?.request
            ?.takeIf { it.message == normalizedDraft }
            ?: SendDraftRequest(
                message = normalizedDraft,
                idempotencyKey = UUID.randomUUID().toString(),
            )

        sendUiState = SendUiState.Sending(request)
        status = "Đang gửi nội dung đã xác nhận…"
        sendScope.launch {
            val result = runCatching { messageSender.send(request) }
                .getOrElse {
                    SendDraftResult.Rejected(
                        "Tin nhắn chưa được gửi. Vui lòng kiểm tra kết nối và thử lại.",
                    )
                }
            when (result) {
                is SendDraftResult.Accepted -> {
                    sound.action()
                    sendUiState = SendUiState.Sent(request.message)
                    status = result.userMessage
                    draft = ""
                    rawMorse = ""
                    currentCode = ""
                }

                is SendDraftResult.Rejected -> {
                    sendUiState = SendUiState.Failed(request, result.userMessage)
                    status = result.userMessage
                }
            }
        }
    }

    val isSendable = draft.isNotBlank() && currentCode.isBlank() && sendUiState !is SendUiState.Sending
    val sendButtonText = when (sendUiState) {
        is SendUiState.Sending -> "ĐANG GỬI…"
        is SendUiState.Failed -> "THỬ GỬI LẠI"
        else -> "GỬI"
    }

    var videoEnabled by remember { mutableStateOf(false) }
    var showCheatSheet by remember { mutableStateOf(false) }
    val cheatSheetState = rememberModalBottomSheetState()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Navy),
            shape = RoundedCornerShape(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("BridgeTalk", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("An Nam", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Cuộc gọi đang kết nối", color = Color(0xFFD7E2FF), fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ÂM THANH", color = Color(0xFFB9F2D9), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.semantics {
                            contentDescription = if (videoEnabled)
                                "Camera đang bật · chạm để tắt"
                            else
                                "Camera đang tắt · chạm để bật"
                        },
                    ) {
                        Text(
                            text = if (videoEnabled) "Camera BẬT" else "Camera TẮT",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = videoEnabled,
                            onCheckedChange = { videoEnabled = it },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = sourceLanguage == "VI",
                onClick = { sourceLanguage = "VI" },
                label = { Text("Tiếng Việt") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    selectedContainerColor = SurfaceBlue,
                    selectedLabelColor = Blue,
                ),
            )
            FilterChip(
                selected = targetLanguage == "EN",
                onClick = { targetLanguage = "EN" },
                label = { Text("English") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    selectedContainerColor = SurfaceBlue,
                    selectedLabelColor = Blue,
                ),
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { showCheatSheet = true },
                modifier = Modifier.height(54.dp).semantics {
                    contentDescription = "Mở bảng mã Morse từ A đến Z"
                },
            ) { Text("MORSE A-Z") }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CAPTION TRỰC TIẾP", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("● ĐANG NGHE", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Text("Tôi cần thêm thời gian để trả lời.", color = Navy, fontSize = 22.sp, lineHeight = 30.sp)
                Text("I need more time to reply.", color = Color(0xFF44506A), fontSize = 20.sp, lineHeight = 28.sp)
                Spacer(Modifier.height(8.dp))
                Text("Bản gốc và bản dịch được giữ cùng nhau", color = Green, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceBlue),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("TRẢ LỜI", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Chạm chấm hoặc gạch để nhập Morse", color = Color(0xFF44506A), fontSize = 14.sp)
                    }
                    Text(if (rawMorse.isBlank()) "—" else rawMorse.replace("-", "—"), color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        draft = it
                        sendUiState = SendUiState.Idle
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("MessageDraft · kiểm tra trước khi gửi") },
                    supportingText = { Text("Tin nhắn chỉ được gửi khi bạn nhấn GỬI") },
                    minLines = 2,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { appendMorse(".") },
                        modifier = Modifier.weight(1f).height(132.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("·", fontSize = 52.sp, fontWeight = FontWeight.Bold)
                            Text("CHẤM", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { appendMorse("-") },
                        modifier = Modifier.weight(1f).height(132.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4FD3)),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("—", fontSize = 52.sp, fontWeight = FontWeight.Bold)
                            Text("GẠCH", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Mỗi lần chạm đều có âm báo · ký tự tự hoàn tất sau một khoảng dừng", color = Color(0xFF44506A), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { currentCode = ""; status = "Đã xóa ký tự đang nhập" }) { Text("Xóa ký tự") }
                    OutlinedButton(onClick = {
                        draft += " "
                        rawMorse += " / "
                        currentCode = ""
                        sendUiState = SendUiState.Idle
                        sound.action()
                    }) { Text("Thêm khoảng trắng") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            status,
            color = Green,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("GỢI Ý TRẢ LỜI", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Chọn một câu", color = Color(0xFF44506A), fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        suggestions.forEach { suggestion ->
            FilterChip(
                selected = draft == suggestion,
                onClick = {
                    draft = suggestion
                    sendUiState = SendUiState.Idle
                    sound.action()
                    status = "Đã chọn câu trả lời · chưa gửi"
                },
                label = { Text(suggestion, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    selectedContainerColor = SurfaceBlue,
                    selectedLabelColor = Blue,
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onEnd, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Kết thúc cuộc gọi") }
        }
        Surface(
            color = Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Button(
                    onClick = ::sendDraft,
                    enabled = isSendable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .semantics {
                            contentDescription = when (sendUiState) {
                                is SendUiState.Failed -> "Thử gửi lại nội dung đã xác nhận"
                                is SendUiState.Sending -> "Đang gửi nội dung đã xác nhận"
                                else -> "Gửi nội dung đã xác nhận cho người nhận"
                            }
                        },
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(sendButtonText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                if (sendUiState is SendUiState.Failed) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = (sendUiState as SendDraftResult.Rejected).userMessage,
                        color = WarningText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WarningSurface, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                }
            }
        }
        if (showCheatSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCheatSheet = false },
                sheetState = cheatSheetState,
            ) {
                MorseCheatSheet()
            }
        }
    }
}

@Composable
private fun ConsentScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        SectionHeader("Lưu transcript?", "Mặc định BridgeTalk không lưu audio, caption, bản dịch, Morse hay MessageDraft.")
        Spacer(Modifier.height(20.dp))
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceBlue)) {
            Text(
                "Transcript chỉ được lưu khi cả hai người tham gia đều đồng ý.",
                modifier = Modifier.padding(18.dp),
                color = Navy,
                fontSize = 17.sp,
                lineHeight = 25.sp,
            )
        }
        Spacer(Modifier.height(22.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
            Text("Không lưu · quay về danh bạ", fontSize = 17.sp)
        }
    }
}

private object MorseDecoder {
    private val dictionary = mapOf(
        ".-" to "A", "-..." to "B", "-.-." to "C", "-.." to "D", "." to "E", "..-." to "F",
        "--." to "G", "...." to "H", ".." to "I", ".---" to "J", "-.-" to "K", ".-.." to "L",
        "--" to "M", "-." to "N", "---" to "O", ".--." to "P", "--.-" to "Q", ".-." to "R",
        "..." to "S", "-" to "T", "..-" to "U", "...-" to "V", ".--" to "W", "-..-" to "X",
        "-.--" to "Y", "--.." to "Z",
    )

    fun decode(code: String): String? = dictionary[code]
}

@Composable
private fun BridgeTalkTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

private data class MorseEntry(val letter: String, val code: String)

private val MORSE_CHEAT_SHEET: List<MorseEntry> = listOf(
    MorseEntry("A", ".-"),
    MorseEntry("B", "-..."),
    MorseEntry("C", "-.-."),
    MorseEntry("D", "-.."),
    MorseEntry("E", "."),
    MorseEntry("F", "..-."),
    MorseEntry("G", "--."),
    MorseEntry("H", "...."),
    MorseEntry("I", ".."),
    MorseEntry("J", ".---"),
    MorseEntry("K", "-.-"),
    MorseEntry("L", ".-.."),
    MorseEntry("M", "--"),
    MorseEntry("N", "-."),
    MorseEntry("O", "---"),
    MorseEntry("P", ".--."),
    MorseEntry("Q", "--.-"),
    MorseEntry("R", ".-."),
    MorseEntry("S", "..."),
    MorseEntry("T", "-"),
    MorseEntry("U", "..-"),
    MorseEntry("V", "...-"),
    MorseEntry("W", ".--"),
    MorseEntry("X", "-..-"),
    MorseEntry("Y", "-.--"),
    MorseEntry("Z", "--.."),
)

@Composable
private fun MorseCheatSheet() {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(
            text = "Mã Morse A–Z",
            color = Navy,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Chấm · và gạch —. Nhấn CHẤM cho · và GẠCH cho —. Ký tự tự hoàn tất sau khoảng dừng.",
            color = Color(0xFF44506A),
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(16.dp))
        MORSE_CHEAT_SHEET.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .semantics {
                                    contentDescription = "${entry.letter}: ${entry.code.replace(".", "chấm ").replace("-", "gạch ").trim()}"
                                },
                        ) {
                            Text(
                                text = entry.letter,
                                color = Navy,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = entry.code.replace(".", "·").replace("-", "—"),
                                color = Blue,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
