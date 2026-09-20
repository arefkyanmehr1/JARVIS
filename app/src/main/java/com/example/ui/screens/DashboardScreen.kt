package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ActivityLogEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.JarvisHudCard
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.StatusBadge
import com.example.ui.components.ThinkingLiveCard
import com.example.ui.navigation.MainTab
import com.example.ui.navigation.SubScreen
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: JarvisViewModel,
    onRequestPermissions: () -> Unit
) {
    val isAutoReplyOn by viewModel.isAutoReplyEnabled.collectAsState()
    val todaySmsCount by viewModel.todayMessagesCount.collectAsState()
    val todayAiReplies by viewModel.todayAiRepliesCount.collectAsState()
    val todayErrors by viewModel.todayErrorsCount.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val selectedSim by viewModel.selectedSimId.collectAsState()
    val currentStatus by viewModel.currentStatus.collectAsState()
    val statusTimestamp by viewModel.statusTimestamp.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val liveAnalysisState by viewModel.liveAnalysisState.collectAsState()

    val isOnline = viewModel.isNetworkAvailable()
    val hasReceiveSms = viewModel.hasSmsReceivePermission()
    val hasSendSms = viewModel.hasSmsSendPermission()
    val hasContacts = viewModel.hasContactsPermission()
    val hasNotifications = viewModel.hasNotificationPermission()
    val allPermissionsGranted = hasReceiveSms && hasSendSms && hasContacts && hasNotifications

    val simLabel = when (selectedSim) {
        -1 -> "همه خطوط"
        0 -> "خط ۱"
        1 -> "خط ۲"
        else -> "خط #$selectedSim"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        // Compact & Balanced Master Reactor Card
        JarvisHudCard(
            borderColor = if (isAutoReplyOn) JarvisCyan.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        JarvisCyan.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.dp, JarvisCyan.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Psychology,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingStatusDot(isOnline = isOnline && isAutoReplyOn)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "دستیار هوشمند پیامک",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = if (isAutoReplyOn) "پاسخ خودکار فعال" else "پاسخ خودکار غیرفعال",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isAutoReplyOn) JarvisEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SimCard,
                                        contentDescription = null,
                                        tint = JarvisCyan,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = simLabel,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Switch(
                    checked = isAutoReplyOn,
                    onCheckedChange = { viewModel.setAutoReplyEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisCyan,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("auto_reply_toggle_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Permissions Banner (if missing)
        AnimatedVisibility(visible = !allPermissionsGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("permissions_warning_card"),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisAmber.copy(alpha = 0.12f)),
                border = BorderStroke(0.8.dp, JarvisAmber.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = JarvisAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "برای کارکرد خودکار و شناسایی مخاطبین، دسترسی‌های پیامک و مخاطبین لازم است.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("grant_permissions_btn")
                    ) {
                        Text("اعطا", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live AI Cognitive Analysis & Delayed Thinking Card
        ThinkingLiveCard(
            analysisState = liveAnalysisState,
            onSendImmediately = { viewModel.triggerSendImmediately() },
            onCancelSend = { viewModel.cancelScheduledReply() },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Live Status Banner (وضعیت لحظه‌ای صاحب گوشی)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clickable { viewModel.navigateToTab(MainTab.SETTINGS) },
            shape = RoundedCornerShape(10.dp),
            color = if (currentStatus.isNotBlank()) JarvisCyan.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.8.dp, if (currentStatus.isNotBlank()) JarvisCyan.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = if (currentStatus.isNotBlank()) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "وضعیت لحظه‌ای «$userName»",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (currentStatus.isNotBlank() && statusTimestamp > 0) {
                                val elapsedMin = ((System.currentTimeMillis() - statusTimestamp) / (60 * 1000)).coerceAtLeast(0)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "($elapsedMin دقیقه پیش)",
                                    fontSize = 9.sp,
                                    color = JarvisCyan
                                )
                            }
                        }
                        Text(
                            text = if (currentStatus.isNotBlank()) currentStatus else "وضعیتی ثبت نشده است. برای ثبت وضعیت لحظه‌ای لمس کنید.",
                            fontSize = 10.sp,
                            color = if (currentStatus.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (currentStatus.isNotBlank()) "تغییر" else "ثبت",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Metrics Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "آمار امروز",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = { viewModel.refreshMetrics() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "به‌روزرسانی آمار",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3 Balanced Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricHudBox(
                title = "پیامک‌ها",
                value = todaySmsCount.toString(),
                accentColor = JarvisCyan,
                modifier = Modifier.weight(1f)
            )
            MetricHudBox(
                title = "پاسخ‌های AI",
                value = todayAiReplies.toString(),
                accentColor = JarvisEmerald,
                modifier = Modifier.weight(1f)
            )
            MetricHudBox(
                title = "خطاها",
                value = todayErrors.toString(),
                accentColor = if (todayErrors > 0) MaterialTheme.colorScheme.error else Color.Gray,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Actions Section
        Text(
            text = "دسترسی سریع",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionTile(
                icon = Icons.Filled.Key,
                title = "کلیدهای API",
                subtitle = "چرخش و تست",
                onClick = { viewModel.navigateToSubScreen(SubScreen.ApiManager) },
                modifier = Modifier.weight(1f),
                testTag = "quick_action_api_manager"
            )
            QuickActionTile(
                icon = Icons.Filled.Memory,
                title = "حافظه و تربیت",
                subtitle = "زمینه‌های پایدار",
                onClick = { viewModel.navigateToSubScreen(SubScreen.MemoryManager) },
                modifier = Modifier.weight(1f),
                testTag = "quick_action_memory"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionTile(
                icon = Icons.Filled.Rule,
                title = "قوانین پاسخ",
                subtitle = "فیلترها و شرط‌ها",
                onClick = { viewModel.navigateToTab(MainTab.RULES) },
                modifier = Modifier.weight(1f),
                testTag = "quick_action_rules"
            )
            QuickActionTile(
                icon = Icons.Filled.ListAlt,
                title = "لاگ‌های سیستم",
                subtitle = "گزارش زنده",
                onClick = { viewModel.navigateToSubScreen(SubScreen.ActivityLogs) },
                modifier = Modifier.weight(1f),
                testTag = "quick_action_logs"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Recent Activity Live Feed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "آخرین فعالیت‌ها",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "مشاهده لاگ‌ها",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { viewModel.navigateToSubScreen(SubScreen.ActivityLogs) }
                    .padding(2.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (recentLogs.isEmpty()) {
            JarvisHudCard {
                Text(
                    text = "هنوز فعالیتی در سامانه ثبت نشده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                recentLogs.take(4).forEach { log ->
                    ActivityLogItem(log = log)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MetricHudBox(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActivityLogItem(log: ActivityLogEntity) {
    val statusColor = when (log.status) {
        "SUCCESS" -> JarvisEmerald
        "FAILED" -> MaterialTheme.colorScheme.error
        "WARNING" -> JarvisAmber
        else -> MaterialTheme.colorScheme.primary
    }

    val timeFormatted = rememberTimeFormat(log.timestamp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(text = log.type, color = statusColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = log.description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = timeFormatted,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun rememberTimeFormat(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
