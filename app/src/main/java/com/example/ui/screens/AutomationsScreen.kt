package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AutomationEntity
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

@Composable
fun AutomationsScreen(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val automations by viewModel.automations.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GlassObsidianBackground)
            .ambientGlowBackdrop()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
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
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = GlassIndigoLight
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Nova Automations",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlassTextSlate100
                        )
                    )
                    Text(
                        text = "Custom phrase triggers and automated phone actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSlate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (automations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GlassIndigoLight.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No custom routines yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlassTextSlate200
                        )
                        Text(
                            text = "Tap + below to build your first voice automation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTextSlate400
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(automations, key = { it.id }) { routine ->
                        AutomationCard(
                            routine = routine,
                            onToggle = { viewModel.toggleAutomation(routine) },
                            onDelete = { viewModel.deleteAutomation(routine.id) },
                            onRunNow = {
                                viewModel.processUserPrompt(routine.triggerPhrase)
                            }
                        )
                    }
                }
            }
        }

        // FAB to add automation (Glowing Frosted Glass style)
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = GlassIndigoPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .shadow(12.dp, CircleShape, spotColor = GlassIndigoPrimary)
                .testTag("add_automation_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Automation")
        }

        if (showCreateDialog) {
            CreateAutomationDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { title, trigger, tool, arg ->
                    viewModel.createAutomation(title, trigger, tool, arg)
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun AutomationCard(
    routine: AutomationEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onRunNow: () -> Unit
) {
    FrostedGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("automation_card_${routine.id}"),
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
                        text = routine.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlassTextSlate100
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Trigger: \"${routine.triggerPhrase}\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = GlassIndigoLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Switch(
                    checked = routine.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GlassIndigoPrimary,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                        uncheckedThumbColor = GlassTextSlate400
                    ),
                    modifier = Modifier.testTag("automation_switch_${routine.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Actions: ${routine.actionsJson}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTextSlate400,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRunNow, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Routine",
                            tint = GlassEmeraldGreen
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Routine",
                            tint = GlassRoseError
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateAutomationDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var trigger by remember { mutableStateOf("") }
    var tool by remember { mutableStateOf("flashlight") }
    var arg by remember { mutableStateOf("state") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131728),
        title = { 
            Text(
                "Create Voice Automation",
                style = MaterialTheme.typography.titleLarge.copy(color = GlassTextSlate100)
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Routine Name (e.g. Night Mode)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GlassTextSlate100,
                        unfocusedTextColor = GlassTextSlate200,
                        focusedBorderColor = GlassIndigoPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("Spoken Trigger (e.g. Good night)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GlassTextSlate100,
                        unfocusedTextColor = GlassTextSlate200,
                        focusedBorderColor = GlassIndigoPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )
                OutlinedTextField(
                    value = tool,
                    onValueChange = { tool = it },
                    label = { Text("Action Tool (e.g. flashlight, volume)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GlassTextSlate100,
                        unfocusedTextColor = GlassTextSlate200,
                        focusedBorderColor = GlassIndigoPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && trigger.isNotBlank()) {
                        onCreate(title, trigger, tool, arg)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlassIndigoPrimary)
            ) {
                Text("Save Routine", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GlassTextSlate400)
            }
        }
    )
}

