package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NovaViewModel
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.ambientGlowBackdrop
import com.example.ui.theme.GlassCyanAccent
import com.example.ui.theme.GlassCyanLight
import com.example.ui.theme.GlassEmeraldGreen
import com.example.ui.theme.GlassIndigoDeep
import com.example.ui.theme.GlassIndigoLight
import com.example.ui.theme.GlassIndigoPrimary
import com.example.ui.theme.GlassObsidianBackground
import com.example.ui.theme.GlassTextSlate100
import com.example.ui.theme.GlassTextSlate200
import com.example.ui.theme.GlassTextSlate300
import com.example.ui.theme.GlassTextSlate400
import com.example.ui.theme.GlassVioletAccent

@Composable
fun SettingsScreen(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.languagePreference.collectAsState()
    val pitch by viewModel.voicePitch.collectAsState()
    val speed by viewModel.voiceSpeed.collectAsState()
    val voiceEngine by viewModel.voiceEngine.collectAsState()
    val omnivoicePreset by viewModel.omnivoicePreset.collectAsState()
    val autoSpeak by viewModel.autoSpeak.collectAsState()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()

    var apiKeyInput by remember(customApiKey) { mutableStateOf(customApiKey) }
    var keySavedMessage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GlassObsidianBackground)
            .ambientGlowBackdrop()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassIndigoDeep.copy(alpha = 0.35f))
                        .border(1.dp, GlassIndigoLight.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = GlassIndigoLight
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Nova Voice & Brain Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlassTextSlate100
                        )
                    )
                    Text(
                        text = "Customize neural voice, wake word & AI models",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSlate400
                    )
                }
            }
        }

        // Voice Section
        item {
            Text(
                text = "Voice Synthesis & Speech",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlassTextSlate100
                )
            )
        }

        item {
            FrostedGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.White.copy(alpha = 0.07f),
                borderColor = Color.White.copy(alpha = 0.12f)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Auto-speak toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Auto-Speak Responses",
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTextSlate100
                            )
                            Text(
                                "Read responses aloud with neural female voice",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextSlate400
                            )
                        }
                        Switch(
                            checked = autoSpeak,
                            onCheckedChange = { viewModel.toggleAutoSpeak(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GlassIndigoPrimary,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                                uncheckedThumbColor = GlassTextSlate400
                            ),
                            modifier = Modifier.testTag("auto_speak_switch")
                        )
                    }

                    // Voice Pitch Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Voice Pitch (${String.format("%.2f", pitch)}x)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GlassTextSlate200
                            )
                            Text(
                                "Sweet Female Neural Timbre",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassIndigoLight
                            )
                        }
                        Slider(
                            value = pitch,
                            onValueChange = { viewModel.updateVoicePitch(it) },
                            valueRange = 0.75f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = GlassIndigoLight,
                                activeTrackColor = GlassIndigoPrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.testTag("pitch_slider")
                        )
                    }

                    // Voice Engine Selection (OmniVoice / ChatGPT Sky)
                    Column {
                        Text(
                            text = "Neural TTS Engine",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = GlassTextSlate300
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val engines = listOf(
                                "omnivoice" to "🎙️ OmniVoice\n(k2-fsa)",
                                "chatgpt_sky" to "✨ ChatGPT\n(Sky)",
                                "sweet_bn" to "🌸 Sweet BN\n(বাংলা)"
                            )
                            engines.forEach { (id, label) ->
                                val isSelected = voiceEngine == id
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            1.dp,
                                            if (isSelected) GlassCyanLight else Color.White.copy(alpha = 0.12f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.updateVoiceEngine(id) },
                                    color = if (isSelected) GlassIndigoPrimary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.04f)
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) GlassCyanLight else GlassTextSlate200,
                                            lineHeight = 16.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Quick Female Voice Presets
                    Column {
                        Text(
                            text = "Female Voice Persona Presets",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = GlassTextSlate300
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presets = listOf(
                                Triple("✨ Expressive", 1.18f, 1.00f),
                                Triple("🌸 Sweet BN", 1.22f, 1.02f),
                                Triple("🎙️ Clarity", 1.14f, 1.04f)
                            )
                            presets.forEach { (label, p, s) ->
                                val isSelected = Math.abs(pitch - p) < 0.04f && Math.abs(speed - s) < 0.04f
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            1.dp,
                                            if (isSelected) GlassCyanLight else Color.White.copy(alpha = 0.12f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    color = if (isSelected) GlassIndigoPrimary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.04f)
                                ) {
                                    Button(
                                        onClick = { viewModel.applyVoicePreset(p, s) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) GlassCyanLight else GlassTextSlate200
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Voice Speed Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Speech Speed (${String.format("%.2f", speed)}x)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GlassTextSlate200
                            )
                            Text(
                                "Conversational Cadence",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassIndigoLight
                            )
                        }
                        Slider(
                            value = speed,
                            onValueChange = { viewModel.updateVoiceSpeed(it) },
                            valueRange = 0.75f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = GlassIndigoLight,
                                activeTrackColor = GlassIndigoPrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.testTag("speed_slider")
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.speakResponse("নমস্কার! আমি নোভা। OmniVoice ও ক্যামেরা ভিশন প্রযুক্তি নিয়ে মিজান এবং রাতুলের তৈরি মিষ্টি এআই অ্যাসিস্ট্যান্ট।")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassIndigoPrimary.copy(alpha = 0.25f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassIndigoLight.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = GlassIndigoLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview OmniVoice Female Timbre", color = GlassIndigoLight)
                    }
                }
            }
        }

        // Camera Vision & Screen Reader Section
        item {
            Text(
                text = "Camera Vision & Screen Reader",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlassTextSlate100
                )
            )
        }

        item {
            FrostedGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.White.copy(alpha = 0.07f),
                borderColor = Color.White.copy(alpha = 0.12f)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Visual scene understanding & real-time screen reader",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSlate300
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.openCameraVision() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GlassIndigoPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open Camera Vision", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.performScreenReader() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GlassVioletAccent.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Read Active Screen", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Wake Word Section
        item {
            Text(
                text = "Hands-Free Wake Word",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlassTextSlate100
                )
            )
        }

        item {
            FrostedGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.White.copy(alpha = 0.07f),
                borderColor = Color.White.copy(alpha = 0.12f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Low-Power \"Hey Nova\" Detection",
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTextSlate100
                            )
                            Text(
                                "Runs background energy acoustic buffer to wake up assistant",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextSlate400
                            )
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = { viewModel.toggleWakeWord(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GlassIndigoPrimary,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                                uncheckedThumbColor = GlassTextSlate400
                            ),
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }
                }
            }
        }

        // Gemini Brain API Configuration
        item {
            Text(
                text = "Gemini AI Brain Credentials",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlassTextSlate100
                )
            )
        }

        item {
            FrostedGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.White.copy(alpha = 0.07f),
                borderColor = Color.White.copy(alpha = 0.12f)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter a custom Gemini API Key if you want to override the pre-configured environment key:",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSlate400
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            keySavedMessage = false
                        },
                        placeholder = { 
                            Text("AIzaSy...", color = GlassTextSlate400) 
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = GlassIndigoLight)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlassIndigoPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                            focusedTextColor = GlassTextSlate100,
                            unfocusedTextColor = GlassTextSlate200
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (keySavedMessage) {
                            Text(
                                "Saved successfully!",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassEmeraldGreen
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = {
                                viewModel.updateApiKey(apiKeyInput.trim())
                                keySavedMessage = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassIndigoPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_api_key_button")
                        ) {
                            Text("Save Key", color = Color.White)
                        }
                    }
                }
            }
        }

        // Owners & Creators Section
        item {
            Text(
                text = "Owners & Creators",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlassTextSlate100
                )
            )
        }

        item {
            FrostedGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = GlassIndigoPrimary.copy(alpha = 0.12f),
                borderColor = GlassIndigoLight.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Mizan & Ratul",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GlassIndigoLight
                                )
                            )
                            Text(
                                text = "মিজান এবং রাতুল • Founders & Chief Architects",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextSlate200
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GlassIndigoPrimary.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassCyanAccent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "OWNERS",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GlassCyanLight,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Nova is personalized and loyal to Mizan and Ratul. You can ask Nova anytime: \"Who is your owner?\" or \"Who made you?\" to hear her warm tribute in sweet Bengali or English.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = GlassTextSlate300,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }

        // About & Version
        item {
            FrostedGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.White.copy(alpha = 0.04f),
                borderColor = Color.White.copy(alpha = 0.08f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nova AI Voice Assistant", fontWeight = FontWeight.Bold, color = GlassIndigoLight)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Version 1.0.0 • Crafted with ❤️ by Mizan & Ratul", style = MaterialTheme.typography.bodySmall, color = GlassTextSlate200)
                    Text("Languages: Bengali (বাংলা), English, Banglish", style = MaterialTheme.typography.bodySmall, color = GlassTextSlate400)
                }
            }
        }
    }
}

