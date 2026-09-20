package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD(
        route = "dashboard",
        title = "داشبورد",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    CHATS(
        route = "chats",
        title = "گفتگوها",
        selectedIcon = Icons.Filled.Chat,
        unselectedIcon = Icons.Outlined.Chat
    ),
    SMS(
        route = "sms",
        title = "پیامک‌ها",
        selectedIcon = Icons.Filled.Message,
        unselectedIcon = Icons.Outlined.Message
    ),
    RULES(
        route = "rules",
        title = "قوانین",
        selectedIcon = Icons.Filled.Gavel,
        unselectedIcon = Icons.Outlined.Gavel
    ),
    SETTINGS(
        route = "settings",
        title = "تنظیمات",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

sealed class SubScreen(val route: String, val title: String) {
    object ApiManager : SubScreen("api_manager", "مدیریت کلیدهای API")
    object MemoryManager : SubScreen("memory_manager", "سیستم حافظه JARVIS")
    object ActivityLogs : SubScreen("activity_logs", "گزارشات فعالیت")
    object SetupWizard : SubScreen("setup_wizard", "راه‌اندازی و دسترسی‌ها")
}
