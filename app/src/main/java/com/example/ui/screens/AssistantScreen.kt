package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.ChatMessageEntity
import com.example.domain.voice.SpeechState
import com.example.ui.NovaViewModel
import com.example.ui.components.AssistantVisualState
import com.example.ui.components.CameraVisionDialog
import com.example.ui.components.ExecutionOverlayHUD
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.FrostedGlassInnerBox
import com.example.ui.components.OrbVisualizer
import com.example.ui.components.ambientGlowBackdrop
import com.example.ui.theme.GlassAmberWarning
import com.example.ui.theme.GlassCardBackground
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCyanAccent
import com.example.ui.theme.GlassCyanLight
import com.example.ui.theme.GlassEmeraldGreen
import com.example.ui.theme.GlassIndigoDeep
import com.example.ui.theme.GlassIndigoLight
import com.example.ui.theme.GlassIndigoPrimary
import com.example.ui.theme.GlassObsidianBackground
import com.example.ui.theme.GlassRoseError
import com.example.ui.theme.GlassTextSlate100
import com.example.ui.theme.GlassTextSlate200
import com.example.ui.theme.GlassTextSlate300
import com.example.ui.theme.GlassTextSlate400
import com.example.ui.theme.GlassTextSlate500
import com.example.ui.theme.GlassVioletAccent

@Composable
fun AssistantScreen(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val visualState by viewModel.visualState.collectAsState()
    val activeTool by viewModel.activeExecutingTool.collectAsState()
    val speechState by viewModel.speechState.collectAsState()
    val audioRmsDb by viewModel.audioRmsDb.collectAsState()
    val languagePreference by viewModel.languagePreference.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val isWakeWordActive by viewModel.isWakeWordActive.collectAsState()
    val isVisionAnalyzing by viewModel.isVisionAnalyzing.collectAsState()
    val visionAnalysisResult by viewModel.visionAnalysisResult.collectAsState()
    val showCameraVisionDialog by viewModel.showCameraVisionDialog.collectAsState()
    val voiceEngine by viewModel.voiceEngine.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openCameraVision()
        }
    }

    val onMicAction: () -> Unit = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            viewModel.toggleVoiceListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val onCameraVisionAction: () -> Unit = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            viewModel.openCameraVision()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GlassObsidianBackground)
            .ambientGlowBackdrop()
    ) {
        // --- Top Status & Quick Language Selector Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Engine status indicator with luminous dot
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isAccessibilityActive) GlassEmeraldGreen else GlassCyanAccent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAccessibilityActive) "Engine Active" else "Standard Mode",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = GlassTextSlate400
                )
            }

            // Frosted Glass Language Switcher
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("auto" to "Auto", "bn" to "বাংলা", "en" to "EN").forEach { (code, label) ->
                    val isSelected = languagePreference == code
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.updateLanguage(code) }
                            .testTag("lang_chip_$code"),
                        color = if (isSelected) GlassIndigoPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) GlassIndigoLight.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) GlassIndigoLight else GlassTextSlate400
                            )
                        )
                    }
                }
            }
        }

        // --- Center Animated AI Orb Hub ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OrbVisualizer(
                    state = visualState,
                    audioAmplitude = audioRmsDb,
                    size = 175.dp,
                    onClick = onMicAction
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Assistant status label in uppercase tracker style
                val statusTitle = when (visualState) {
                    AssistantVisualState.LISTENING -> "LISTENING (বলুন...)"
                    AssistantVisualState.THINKING -> "PLANNING ACTION"
                    AssistantVisualState.SPEAKING -> "SPEAKING (ChatGPT Sky Voice)"
                    AssistantVisualState.EXECUTING -> "EXECUTING COMMAND"
                    AssistantVisualState.IDLE -> "READY (Tap to Speak)"
                }

                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = when (visualState) {
                            AssistantVisualState.LISTENING -> GlassCyanAccent
                            AssistantVisualState.SPEAKING -> GlassVioletAccent
                            AssistantVisualState.THINKING -> GlassIndigoLight
                            AssistantVisualState.EXECUTING -> GlassEmeraldGreen
                            AssistantVisualState.IDLE -> GlassIndigoLight
                        }
                    )
                )

                // Error State Feedback Pill
                AnimatedVisibility(
                    visible = speechState is SpeechState.Error,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val err = (speechState as? SpeechState.Error)?.message ?: ""
                    if (err.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onMicAction() },
                            color = GlassRoseError.copy(alpha = 0.20f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassRoseError.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = GlassRoseError, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                                )
                            }
                        }
                    }
                }

                // Live partial speech transcription bubble
                AnimatedVisibility(
                    visible = speechState is SpeechState.Processing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val partial = (speechState as? SpeechState.Processing)?.partialText ?: ""
                    if (partial.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            color = GlassIndigoDeep.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassIndigoLight.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "“$partial”",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(color = GlassIndigoLight)
                            )
                        }
                    }
                }
            }
        }

        // --- Quick Suggested Command Pills (Frosted Glass Chips) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "📷 Camera Vision (কি দেখছে বলবে)" to "camera vision",
                "📱 Screen Reader (স্ক্রিন পড়বে)" to "read screen",
                "🎙️ OmniVoice Neural" to "switch to omnivoice",
                "👑 Owner k?" to "Who is your owner?",
                "💡 Flashlight on" to "flashlight on",
                "🔊 Volume 70%" to "set volume to 70%",
                "💬 WhatsApp message" to "Open WhatsApp",
                "🔔 Read notifications" to "Read notifications",
                "📍 Open Google Maps" to "Open maps to coffee shop",
                "🧠 আমার মেমোরি দেখাও" to "Tell me what you remember about me"
            )

            suggestions.forEach { (label, command) ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (command == "camera vision") {
                                onCameraVisionAction()
                            } else if (command == "read screen") {
                                viewModel.performScreenReader()
                            } else {
                                viewModel.processUserPrompt(command)
                            }
                        }
                        .testTag("suggestion_${label.filter { it.isLetterOrDigit() }}"),
                    color = if (label.contains("Camera") || label.contains("Screen")) GlassIndigoPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (label.contains("Camera") || label.contains("Screen")) GlassCyanAccent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.10f)
                    )
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (label.contains("Camera") || label.contains("Screen")) GlassCyanLight else GlassTextSlate200,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // --- Active Execution Overlay HUD with Glow & Shimmer Animation ---
        ExecutionOverlayHUD(
            activeTool = activeTool,
            isExecuting = visualState == AssistantVisualState.EXECUTING
        )

        // --- Conversation History Feed ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    // Frosted Glass Context Card & Starter Greeting
                    FrostedGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        backgroundColor = Color.White.copy(alpha = 0.05f),
                        borderColor = Color.White.copy(alpha = 0.10f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(GlassEmeraldGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "CURRENT CONTEXT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp,
                                            color = GlassTextSlate400
                                        )
                                    )
                                }
                                Text(
                                    text = "Ready to assist",
                                    style = MaterialTheme.typography.labelSmall.copy(color = GlassTextSlate500)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FrostedGlassInnerBox(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BrightnessMedium,
                                            contentDescription = null,
                                            tint = GlassIndigoLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Accessibility",
                                                style = MaterialTheme.typography.labelSmall.copy(color = GlassTextSlate400)
                                            )
                                            Text(
                                                text = if (isAccessibilityActive) "Active" else "Standby",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAccessibilityActive) GlassEmeraldGreen else GlassTextSlate100
                                                )
                                            )
                                        }
                                    }
                                }

                                FrostedGlassInnerBox(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isWakeWordActive) Icons.Default.CheckCircle else Icons.Default.BatteryFull,
                                            contentDescription = null,
                                            tint = if (isWakeWordActive) GlassIndigoLight else GlassEmeraldGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Wake Word",
                                                style = MaterialTheme.typography.labelSmall.copy(color = GlassTextSlate400)
                                            )
                                            Text(
                                                text = if (isWakeWordActive) "Hey Nova ON" else "Standby",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = GlassTextSlate100
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "“Hey Nova, flashlight on koro, WhatsApp a message pathao, ba reminders check koro.”",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = GlassIndigoLight,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                ChatMessageItem(message = message, onActionClick = {
                    if (message.toolName != null) {
                        viewModel.processUserPrompt("Repeat ${message.toolName}")
                    }
                })
            }
        }

        // --- Bottom Interaction Bar: Frosted Glass Text Input + Mic ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = GlassObsidianBackground.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onCameraVisionAction() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .testTag("camera_vision_quick_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Vision",
                        tint = GlassCyanLight,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { 
                        Text(
                            "Ask, vision or command...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = GlassTextSlate500)
                        ) 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistant_text_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassIndigoPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = Color.White.copy(alpha = 0.06f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        focusedTextColor = GlassTextSlate100,
                        unfocusedTextColor = GlassTextSlate200
                    ),
                    trailingIcon = {
                        if (textInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val query = textInput
                                    textInput = ""
                                    viewModel.processUserPrompt(query)
                                },
                                modifier = Modifier.testTag("send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Command",
                                    tint = GlassCyanAccent
                                )
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Glowing Indigo/Cyan Mic trigger button
                val isListening = speechState is SpeechState.Listening
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(
                            elevation = if (isListening) 16.dp else 6.dp,
                            shape = CircleShape,
                            spotColor = if (isListening) GlassCyanAccent else GlassIndigoPrimary
                        )
                        .clip(CircleShape)
                        .background(
                            brush = if (isListening) {
                                Brush.linearGradient(listOf(GlassCyanLight, GlassIndigoDeep))
                            } else {
                                Brush.linearGradient(listOf(GlassIndigoDeep, GlassIndigoPrimary))
                            }
                        )
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .clickable { onMicAction() }
                        .testTag("voice_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Microphone Toggle",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Camera & Screen Vision Dialog
        CameraVisionDialog(
            isOpen = showCameraVisionDialog,
            isAnalyzing = isVisionAnalyzing,
            analysisResult = visionAnalysisResult,
            isSpeaking = viewModel.ttsManager.isSpeaking.collectAsState().value,
            onDismiss = { viewModel.closeCameraVision() },
            onAnalyzeBitmap = { bitmap, prompt ->
                viewModel.analyzeCameraBitmap(bitmap, prompt)
            },
            onPerformScreenReader = {
                viewModel.performScreenReader()
            },
            onRepeatSpeech = { text ->
                viewModel.speakResponse(text)
            },
            onStopSpeech = {
                viewModel.stopSpeaking()
            }
        )
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    onActionClick: () -> Unit
) {
    val isUser = message.sender.equals("USER", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GlassIndigoDeep.copy(alpha = 0.35f))
                    .border(1.dp, GlassIndigoLight.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Nova Assistant",
                    tint = GlassIndigoLight,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) GlassIndigoDeep.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.08f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) GlassIndigoLight.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)
            ),
            modifier = Modifier
                .widthIn(max = 310.dp)
                .testTag("chat_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isUser) GlassTextSlate100 else GlassTextSlate200,
                        lineHeight = 20.sp
                    )
                )

                // Tool action status badge if executed
                if (!message.toolName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (message.toolStatus) {
                            "SUCCESS" -> GlassEmeraldGreen.copy(alpha = 0.15f)
                            "FAILED" -> GlassRoseError.copy(alpha = 0.15f)
                            "PENDING_CONFIRMATION" -> GlassAmberWarning.copy(alpha = 0.15f)
                            else -> Color.Black.copy(alpha = 0.25f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (message.toolStatus) {
                                "SUCCESS" -> GlassEmeraldGreen.copy(alpha = 0.35f)
                                "FAILED" -> GlassRoseError.copy(alpha = 0.35f)
                                "PENDING_CONFIRMATION" -> GlassAmberWarning.copy(alpha = 0.35f)
                                else -> Color.White.copy(alpha = 0.1f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (message.toolStatus) {
                                    "SUCCESS" -> Icons.Default.CheckCircle
                                    "FAILED" -> Icons.Default.Error
                                    else -> Icons.Default.FlashlightOn
                                },
                                contentDescription = null,
                                tint = when (message.toolStatus) {
                                    "SUCCESS" -> GlassEmeraldGreen
                                    "FAILED" -> GlassRoseError
                                    else -> GlassAmberWarning
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tool: ${message.toolName} (${message.toolStatus ?: "COMPLETED"})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassTextSlate200
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

