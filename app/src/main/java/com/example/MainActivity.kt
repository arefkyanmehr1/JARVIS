package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.components.PulsingStatusDot
import com.example.ui.navigation.MainTab
import com.example.ui.navigation.SubScreen
import com.example.ui.screens.ActivityLogsScreen
import com.example.ui.screens.ApiManagerScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MemoryManagerScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.screens.SmsScreen
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            JarvisTheme(darkTheme = isDark) {
                JarvisMainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisMainApp(viewModel: JarvisViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val activeSubScreen by viewModel.activeSubScreen.collectAsState()
    val isAutoReplyOn by viewModel.isAutoReplyEnabled.collectAsState()
    val isOnline = viewModel.isNetworkAvailable()

    // Runtime permissions launcher (SMS, Phone State, Contacts & Notifications)
    val permissionsToRequest = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CONTACTS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshMetrics()
        viewModel.ensureBackgroundServiceRunning()
    }

    val requestAllPermissions: () -> Unit = {
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    LaunchedEffect(Unit) {
        // Ensure continuous background processing service is active
        viewModel.ensureBackgroundServiceRunning()

        // Auto check permissions on launch
        val hasReceive = viewModel.hasSmsReceivePermission()
        val hasSend = viewModel.hasSmsSendPermission()
        val hasContacts = viewModel.hasContactsPermission()
        if (!hasReceive || !hasSend || !hasContacts) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (activeSubScreen == null) {
                JarvisMainTopBar(
                    isOnline = isOnline,
                    isAutoReplyOn = isAutoReplyOn,
                    onHelpClick = { viewModel.navigateToSubScreen(SubScreen.SetupWizard) }
                )
            }
        },
        bottomBar = {
            if (activeSubScreen == null) {
                JarvisBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.navigateToTab(it) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Routing
            when (val sub = activeSubScreen) {
                is SubScreen.ApiManager -> ApiManagerScreen(viewModel = viewModel)
                is SubScreen.MemoryManager -> MemoryManagerScreen(viewModel = viewModel)
                is SubScreen.ActivityLogs -> ActivityLogsScreen(viewModel = viewModel)
                is SubScreen.SetupWizard -> SetupWizardScreen(
                    viewModel = viewModel,
                    onRequestPermissions = requestAllPermissions
                )
                null -> {
                    when (currentTab) {
                        MainTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onRequestPermissions = requestAllPermissions
                        )
                        MainTab.CHATS -> ChatScreen(viewModel = viewModel)
                        MainTab.SMS -> SmsScreen(viewModel = viewModel)
                        MainTab.RULES -> RulesScreen(viewModel = viewModel)
                        MainTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisMainTopBar(
    isOnline: Boolean,
    isAutoReplyOn: Boolean,
    onHelpClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 4.dp
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(JarvisCyan.copy(alpha = 0.3f), Color.Transparent)
                                )
                            )
                            .border(1.dp, JarvisCyan.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Psychology,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "JARVIS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            PulsingStatusDot(isOnline = isOnline && isAutoReplyOn)
                        }
                        Text(
                            text = if (isAutoReplyOn) "دستیار پیامک فعال است" else "پاسخ خودکار غیرفعال",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = onHelpClick, modifier = Modifier.testTag("top_bar_help_btn")) {
                    Icon(
                        imageVector = Icons.Filled.HelpOutline,
                        contentDescription = "راهنمای راه‌اندازی",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
fun JarvisBottomNavigation(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    Surface(
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            MainTab.values().forEach { tab ->
                val selected = currentTab == tab
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title,
                            tint = if (selected) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            text = tab.title,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = JarvisCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                )
            }
        }
    }
}
