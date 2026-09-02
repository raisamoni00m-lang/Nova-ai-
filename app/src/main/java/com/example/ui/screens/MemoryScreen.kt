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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Surface
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
import com.example.data.local.entity.MemoryEntity
import com.example.ui.NovaViewModel
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.ambientGlowBackdrop
import com.example.ui.theme.GlassCyanAccent
import com.example.ui.theme.GlassCyanLight
import com.example.ui.theme.GlassIndigoDeep
import com.example.ui.theme.GlassIndigoLight
import com.example.ui.theme.GlassIndigoPrimary
import com.example.ui.theme.GlassObsidianBackground
import com.example.ui.theme.GlassRoseError
import com.example.ui.theme.GlassTextSlate100
import com.example.ui.theme.GlassTextSlate200
import com.example.ui.theme.GlassTextSlate400
import com.example.ui.theme.GlassVioletAccent

@Composable
fun MemoryScreen(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.memories.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val filteredMemories = remember(memories, searchQuery) {
        if (searchQuery.isBlank()) memories
        else memories.filter {
            it.key.contains(searchQuery, ignoreCase = true) ||
                    it.value.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassIndigoDeep.copy(alpha = 0.35f))
                            .border(1.dp, GlassIndigoLight.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = GlassIndigoLight
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Nova Memory",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = GlassTextSlate100
                            )
                        )
                        Text(
                            text = "On-device user knowledge graph",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTextSlate400
                        )
                    }
                }

                if (memories.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.testTag("clear_memory_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear All Memories",
                            tint = GlassRoseError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frosted Glass Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { 
                    Text("Search memories...", style = MaterialTheme.typography.bodyMedium.copy(color = GlassTextSlate400)) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GlassIndigoLight
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_memory_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GlassIndigoPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                    focusedTextColor = GlassTextSlate100,
                    unfocusedTextColor = GlassTextSlate200
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredMemories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = GlassIndigoLight.copy(alpha = 0.35f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No memories stored yet." else "No memories match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlassTextSlate200
                        )
                        Text(
                            text = "Nova learns your preferences automatically from chats, or add manual memories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTextSlate400,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMemories, key = { it.id }) { memory ->
                        MemoryItemCard(
                            memory = memory,
                            onDelete = { viewModel.deleteMemory(memory.id) }
                        )
                    }
                }
            }
        }

        // Add memory FAB (Glowing Frosted Glass style)
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = GlassIndigoPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .shadow(12.dp, CircleShape, spotColor = GlassIndigoPrimary)
                .testTag("add_memory_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Memory")
        }

        // Add Memory Dialog
        if (showAddDialog) {
            AddMemoryDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { key, value, category ->
                    viewModel.saveMemory(key, value, category)
                    showAddDialog = false
                }
            )
        }

        // Clear confirm dialog
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                containerColor = Color(0xFF131728),
                title = { 
                    Text("Clear All Memories?", style = MaterialTheme.typography.titleLarge.copy(color = GlassTextSlate100)) 
                },
                text = { 
                    Text(
                        "This will permanently remove all stored knowledge, preferences, and personal details saved on this device.",
                        color = GlassTextSlate200
                    ) 
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllMemories()
                            showClearConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassRoseError)
                    ) {
                        Text("Clear Everything", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("Cancel", color = GlassTextSlate400)
                    }
                }
            )
        }
    }
}

@Composable
fun MemoryItemCard(
    memory: MemoryEntity,
    onDelete: () -> Unit
) {
    FrostedGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_card_${memory.id}"),
        backgroundColor = Color.White.copy(alpha = 0.07f),
        borderColor = Color.White.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = memory.key,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlassTextSlate100
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GlassIndigoDeep.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassIndigoLight.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = memory.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = GlassIndigoLight,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memory.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTextSlate200
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Memory",
                    tint = GlassRoseError.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("preference") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131728),
        title = { 
            Text("Add New Memory", style = MaterialTheme.typography.titleLarge.copy(color = GlassTextSlate100)) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Memory Topic (e.g. Favorite Food)") },
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
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Detail (e.g. Biryani, Khichuri)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GlassTextSlate100,
                        unfocusedTextColor = GlassTextSlate200,
                        focusedBorderColor = GlassIndigoPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (preference, fact, note)") },
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
                    if (key.isNotBlank() && value.isNotBlank()) {
                        onAdd(key, value, category)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlassIndigoPrimary)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GlassTextSlate400)
            }
        }
    )
}

