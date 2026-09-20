package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ApiKeyEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.JarvisHudCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisEmerald

@Composable
fun ApiManagerScreen(viewModel: JarvisViewModel) {
    val apiKeys by viewModel.allApiKeys.collectAsState()
    val testStatus by viewModel.apiKeyTestStatus.collectAsState()
    val isTesting by viewModel.isTestingKey.collectAsState()
    val isRelayEnabled by viewModel.isGeminiRelayEnabled.collectAsState()
    val relayUrl by viewModel.geminiRelayUrl.collectAsState()
    val isCheckingRelay by viewModel.isCheckingRelayHealth.collectAsState()
    val relayHealthStatus by viewModel.relayHealthStatus.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Screen Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مدیریت کلیدهای API (DeepSeek و Gemini)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "پشتیبانی از DeepSeek Flash V4.1، چرخش خودکار کلیدها و رفع محدودیت‌های اتصال",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gemini Relay Bridge Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, if (isRelayEnabled) JarvisCyan.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Router,
                                contentDescription = null,
                                tint = if (isRelayEnabled) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "رله بدون فیلترشکن Gemini",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isRelayEnabled) "اتصال مستقیم از طریق هاست ($relayUrl)" else "غیرفعال (اتصال مستقیم به گوگل)",
                                    fontSize = 10.sp,
                                    color = if (isRelayEnabled) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Switch(
                            checked = isRelayEnabled,
                            onCheckedChange = { viewModel.setGeminiRelayEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyan,
                                checkedTrackColor = JarvisCyan.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (isRelayEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "درخواست‌های Gemini از این پل رد می‌شوند و فیلترینگ و تحریم ایران را دور می‌زنند.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.checkRelayHealth() },
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isCheckingRelay,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, JarvisCyan)
                            ) {
                                if (isCheckingRelay) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = JarvisCyan)
                                } else {
                                    Icon(imageVector = Icons.Filled.NetworkCheck, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تست رله", fontSize = 10.sp, color = JarvisCyan)
                                }
                            }
                        }

                        if (relayHealthStatus != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = relayHealthStatus ?: "",
                                fontSize = 10.sp,
                                color = if (relayHealthStatus?.contains("موفق") == true) JarvisEmerald else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rotation Info Card
            JarvisHudCard(borderColor = JarvisCyan.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "JARVIS از کلیدهای DeepSeek (مدل deepseek-flash) و Gemini پشتیبانی می‌کند. در صورت پر شدن محدودیت (۴۲۹) یا بروز هرگونه اختلال در اتصال، سیستم بلافاصله به کلید بعدی سوئیچ می‌کند.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test Status Result if active
            if (testStatus != null || isTesting) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, JarvisCyan)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = testStatus ?: "در حال پردازش...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!isTesting) {
                            TextButton(onClick = { viewModel.clearApiTestStatus() }) {
                                Text("بستن", fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (apiKeys.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Filled.Key,
                        title = "هیچ کلید API اضافه نشده است",
                        description = "برای فعال شدن پاسخ هوشمند، حداقل یک کلید DeepSeek (sk-...) یا Gemini اضافه کنید.",
                        actionButtonText = "افزودن کلید API",
                        onActionClick = { showAddDialog = true }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(apiKeys, key = { it.id }) { keyEntity ->
                        ApiKeyItemCard(
                            keyEntity = keyEntity,
                            onToggle = { viewModel.toggleApiKey(keyEntity) },
                            onDelete = { viewModel.deleteApiKey(keyEntity.id) },
                            onTest = { viewModel.testApiKey(keyEntity.apiKey, keyEntity.provider) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = JarvisCyan,
            contentColor = Color(0xFF00363D),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .testTag("add_api_key_fab")
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "افزودن کلید")
        }
    }

    if (showAddDialog) {
        AddApiKeyDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, key, priority, provider ->
                viewModel.addApiKey(name, key, priority, provider)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ApiKeyItemCard(
    keyEntity: ApiKeyEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    val isDeepSeek = keyEntity.isDeepSeek()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (keyEntity.isEnabled) JarvisCyan.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = keyEntity.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val (badgeLabel, badgeColor) = when {
                        keyEntity.isOpenAi() -> "ChatGPT / OpenAI" to Color(0xFF10A37F)
                        keyEntity.isAiml() -> "AIML API" to Color(0xFF9C27B0)
                        keyEntity.isGemini() -> "Gemini" to Color(0xFF4285F4)
                        else -> "DeepSeek" to JarvisCyan
                    }
                    StatusBadge(
                        text = badgeLabel,
                        color = badgeColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(text = "اولویت ${keyEntity.priority}", color = MaterialTheme.colorScheme.primary)
                }

                Switch(
                    checked = keyEntity.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "کد کلید: ${keyEntity.maskedKey}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics: Success, Errors, Rate limits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "موفق: ${keyEntity.successCount}",
                    fontSize = 11.sp,
                    color = JarvisEmerald,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "خطاها: ${keyEntity.errorCount}",
                    fontSize = 11.sp,
                    color = if (keyEntity.errorCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rate Limit: ${keyEntity.rateLimitCount}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!keyEntity.lastError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "آخرین خطا: ${keyEntity.lastError}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTest,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, JarvisCyan)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NetworkCheck,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تست اتصال", fontSize = 11.sp, color = JarvisCyan)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "حذف کلید",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddApiKeyDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("1") }
    var provider by remember { mutableStateOf("AUTO") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن کلید API هوش مصنوعی", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = key,
                    onValueChange = {
                        key = it
                        if (it.startsWith("sk-")) {
                            provider = "DEEPSEEK"
                            if (name.isBlank()) name = "کلید DeepSeek"
                        } else if (it.startsWith("AIza")) {
                            provider = "GEMINI"
                            if (name.isBlank()) name = "کلید Gemini"
                        }
                    },
                    label = { Text("کلید API (DeepSeek: sk-... یا Gemini: AIza...)") },
                    placeholder = { Text("sk-d0833... یا AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام کلید (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it },
                    label = { Text("اولویت چرخش (۱ = پیش‌فرض، ۲، ۳...)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isOpenAiSelected = provider == "OPENAI" || (provider == "AUTO" && key.startsWith("sk-proj-"))
                        OutlinedButton(
                            onClick = { provider = "OPENAI" },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isOpenAiSelected) Color(0xFF10A37F) else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "ChatGPT (OpenAI)",
                                fontSize = 10.sp,
                                fontWeight = if (isOpenAiSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isOpenAiSelected) Color(0xFF10A37F) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val isAimlSelected = provider == "AIML" || (provider == "AUTO" && key.length == 32 && !key.startsWith("sk-"))
                        OutlinedButton(
                            onClick = { provider = "AIML" },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isAimlSelected) Color(0xFF9C27B0) else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "AIML API",
                                fontSize = 10.sp,
                                fontWeight = if (isAimlSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAimlSelected) Color(0xFF9C27B0) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isGeminiSelected = provider == "GEMINI" || (provider == "AUTO" && key.startsWith("AIza"))
                        OutlinedButton(
                            onClick = { provider = "GEMINI" },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isGeminiSelected) Color(0xFF4285F4) else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Google Gemini",
                                fontSize = 10.sp,
                                fontWeight = if (isGeminiSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isGeminiSelected) Color(0xFF4285F4) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val isDeepSeekSelected = provider == "DEEPSEEK" || (provider == "AUTO" && key.startsWith("sk-") && !key.startsWith("sk-proj-"))
                        OutlinedButton(
                            onClick = { provider = "DEEPSEEK" },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isDeepSeekSelected) JarvisCyan else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "DeepSeek",
                                fontSize = 10.sp,
                                fontWeight = if (isDeepSeekSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDeepSeekSelected) JarvisCyan else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, key, priority.toIntOrNull() ?: 1, provider) },
                enabled = key.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363D))
            ) {
                Text("افزودن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
