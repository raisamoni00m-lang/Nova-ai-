package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.PrivacyAuditEntity
import com.example.ui.NovaViewModel
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.ambientGlowBackdrop
import com.example.ui.theme.GlassCyanAccent
import com.example.ui.theme.GlassEmeraldGreen
import com.example.ui.theme.GlassIndigoDeep
import com.example.ui.theme.GlassIndigoLight
import com.example.ui.theme.GlassIndigoPrimary
import com.example.ui.theme.GlassObsidianBackground
import com.example.ui.theme.GlassRoseError
import com.example.ui.theme.GlassTextSlate100
import com.example.ui.theme.GlassTextSlate200
import com.example.ui.theme.GlassTextSlate400
import com.example.ui.theme.GlassVioletAccent
import com.example.ui.theme.GlassWarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrivacyCenterScreen(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audits by viewModel.audits.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val isNotificationActive by viewModel.isNotificationListenerActive.collectAsState()

    val hasMic = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val hasContacts = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    val hasPhone = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    val hasCamera = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GlassObsidianBackground)
            .ambientGlowBackdrop()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Security Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassEmeraldGreen.copy(alpha = 0.2f))
                        .border(1.dp, GlassEmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = GlassEmeraldGreen
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Nova Privacy & Trust Center",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlassTextSlate100
                        )
                    )
                    Text(
                        text = "Strict local memory, zero telemetry, full transparent audits",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSlate400
                    )
                }
            }
        }

        // Privacy Guarantee Card (Glassmorphism highlight)
        item {
            FrostedGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = GlassIndigoPrimary.copy(alpha = 0.15f),
                borderColor = GlassIndigoLight.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = GlassIndigoLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Built-in Sensitive Data Redaction",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = GlassIndigoLight
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "OTPs, passwords, CVVs, and credit cards are automatically filtered before reaching AI models. Risky operations (calls, messages) require confirmation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTextSlate200
                        )
                    }
                }
            }
        }

        // Permission Status Grid
        item {
            Text(
                text = "System Permissions & Access",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlassTextSlate100
                )
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PermissionRow(
                    icon = Icons.Default.Mic,
                    title = "Microphone (Voice Recognition)",
                    isGranted = hasMic,
                    onManage = { openAppSettings(context) }
                )
                PermissionRow(
                    icon = Icons.Default.AccessibilityNew,
                    title = "Accessibility Service (UI & Taps)",
                    isGranted = isAccessibilityActive,
                    onManage = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
                PermissionRow(
                    icon = Icons.Default.Notifications,
                    title = "Notification Listener (Alerts)",
                    isGranted = isNotificationActive,
                    onManage = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
                PermissionRow(
                    icon = Icons.Default.Contacts,
                    title = "Contacts (Call & SMS Resolution)",
                    isGranted = hasContacts,
                    onManage = { openAppSettings(context) }
                )
                PermissionRow(
                    icon = Icons.Default.Phone,
                    title = "Phone & Calling",
                    isGranted = hasPhone,
                    onManage = { openAppSettings(context) }
                )
                PermissionRow(
                    icon = Icons.Default.CameraAlt,
                    title = "Camera & Flashlight",
                    isGranted = hasCamera,
                    onManage = { openAppSettings(context) }
                )
            }
        }

        // Action Data Controls
        item {
            Text(
                text = "Data Sanitation Controls",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlassTextSlate100
                )
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearAllChatMessages() },
                    modifier = Modifier.weight(1f).testTag("clear_chat_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = GlassRoseError
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassRoseError.copy(alpha = 0.35f))
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Chat Log", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.clearAllMemories() },
                    modifier = Modifier.weight(1f).testTag("clear_all_mem_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = GlassRoseError
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassRoseError.copy(alpha = 0.35f))
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Memories", fontSize = 12.sp)
                }
            }
        }

        // Live Security Audit Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Security Audit Logs (${audits.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlassTextSlate100
                    )
                )
            }
        }

        if (audits.isEmpty()) {
            item {
                FrostedGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color.White.copy(alpha = 0.04f),
                    borderColor = Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "No action audits logged yet. Every assistant tool invocation will appear here transparently.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSlate400,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(audits.take(15), key = { it.id }) { audit ->
                AuditLogItem(audit)
            }
        }
    }
}

@Composable
fun PermissionRow(
    icon: ImageVector,
    title: String,
    isGranted: Boolean,
    onManage: () -> Unit
) {
    FrostedGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onManage() },
        backgroundColor = Color.White.copy(alpha = 0.06f),
        borderColor = Color.White.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) GlassIndigoLight else GlassTextSlate400,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = GlassTextSlate100
                    )
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isGranted) GlassEmeraldGreen.copy(alpha = 0.18f) else GlassWarningAmber.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isGranted) GlassEmeraldGreen.copy(alpha = 0.4f) else GlassWarningAmber.copy(alpha = 0.4f)
                ),
                modifier = Modifier.testTag("perm_badge_${title.take(6)}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isGranted) GlassEmeraldGreen else GlassWarningAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isGranted) "Active" else "Enable",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isGranted) GlassEmeraldGreen else GlassWarningAmber
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AuditLogItem(audit: PrivacyAuditEntity) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = remember(audit.timestamp) { formatter.format(Date(audit.timestamp)) }

    FrostedGlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color.White.copy(alpha = 0.05f),
        borderColor = Color.White.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tool: ${audit.actionName}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlassIndigoLight
                    )
                )
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTextSlate400
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = audit.description,
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSlate200
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Status: ${audit.status}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (audit.status == "COMPLETED") GlassEmeraldGreen else GlassWarningAmber
                )
                Text(
                    text = "Risk: ${audit.riskLevel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (audit.riskLevel == "HIGH") GlassRoseError else GlassTextSlate400
                )
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

