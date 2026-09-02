package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.ambientGlowBackdrop
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.AutomationsScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.PrivacyCenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.GlassCyanAccent
import com.example.ui.theme.GlassCyanLight
import com.example.ui.theme.GlassIndigoDeep
import com.example.ui.theme.GlassIndigoLight
import com.example.ui.theme.GlassIndigoPrimary
import com.example.ui.theme.GlassObsidianBackground
import com.example.ui.theme.GlassTextSlate100
import com.example.ui.theme.GlassTextSlate400
import com.example.ui.theme.NovaTheme
import java.util.Locale

enum class NovaTab(val title: String, val icon: ImageVector) {
    ASSISTANT("Assistant", Icons.Default.SmartToy),
    AUTOMATIONS("Routines", Icons.Default.Bolt),
    MEMORY("Memory", Icons.Default.Psychology),
    PRIVACY("Privacy", Icons.Default.Security),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaApp(
    viewModel: NovaViewModel
) {
    var selectedTab by remember { mutableStateOf(NovaTab.ASSISTANT) }
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmationTool.collectAsState()

    NovaTheme(darkTheme = isDarkMode) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Frosted Indigo Logo Badge from Design HTML
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .shadow(6.dp, RoundedCornerShape(10.dp), spotColor = GlassIndigoPrimary)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GlassIndigoDeep),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Nova",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = GlassTextSlate100
                                )
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = GlassObsidianBackground.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = GlassObsidianBackground.copy(alpha = 0.90f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    NovaTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = GlassIndigoLight,
                                indicatorColor = GlassIndigoPrimary.copy(alpha = 0.35f),
                                unselectedIconColor = GlassTextSlate400,
                                unselectedTextColor = GlassTextSlate400
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GlassObsidianBackground)
                    .ambientGlowBackdrop()
                    .padding(paddingValues)
            ) {
                Crossfade(targetState = selectedTab, label = "TabTransition") { tab ->
                    when (tab) {
                        NovaTab.ASSISTANT -> AssistantScreen(viewModel = viewModel)
                        NovaTab.AUTOMATIONS -> AutomationsScreen(viewModel = viewModel)
                        NovaTab.MEMORY -> MemoryScreen(viewModel = viewModel)
                        NovaTab.PRIVACY -> PrivacyCenterScreen(viewModel = viewModel)
                        NovaTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }

                // Risky Action Confirmation Dialog
                pendingConfirmation?.let { toolCall ->
                    val readableName = toolCall.toolName.replace("_", " ").replaceFirstChar { 
                        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() 
                    }
                    ConfirmationDialog(
                        title = "Confirm $readableName Action",
                        message = "Nova is requesting permission to execute: \"${toolCall.toolName}\" with arguments: ${toolCall.arguments}",
                        confirmLabel = "Authorize & Run",
                        cancelLabel = "Deny Action",
                        onConfirm = { viewModel.confirmPendingAction() },
                        onDismiss = { viewModel.dismissPendingAction() }
                    )
                }
            }
        }
    }
}

