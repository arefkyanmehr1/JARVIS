package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SmsMessageEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.components.ThinkingLiveCard
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SmsScreen(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val messages by viewModel.filteredSmsList.collectAsState()
    val activeFilter by viewModel.smsFilter.collectAsState()
    val searchQuery by viewModel.smsSearchQuery.collectAsState()
    val liveAnalysisState by viewModel.liveAnalysisState.collectAsState()

    var showSendDialog by remember { mutableStateOf(false) }
    var showAiGeneratingDialog by remember { mutableStateOf(false) }
    var aiDialogText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search and Clear Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSmsSearchQuery(it) },
                    placeholder = { Text("جستجو در شماره یا متن پیامک...", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sms_search_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (messages.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.clearAllSms() },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("clear_all_sms_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "پاک‌سازی تمام پیام‌ها",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    SmsFilterChip(
                        title = "همه پیام‌ها",
                        selected = activeFilter == "ALL",
                        onClick = { viewModel.setSmsFilter("ALL") }
                    )
                }
                item {
                    SmsFilterChip(
                        title = "دریافتی",
                        selected = activeFilter == "INCOMING",
                        onClick = { viewModel.setSmsFilter("INCOMING") }
                    )
                }
                item {
                    SmsFilterChip(
                        title = "ارسالی",
                        selected = activeFilter == "OUTGOING",
                        onClick = { viewModel.setSmsFilter("OUTGOING") }
                    )
                }
                item {
                    SmsFilterChip(
                        title = "پاسخ‌های هوشمند AI",
                        selected = activeFilter == "AI_REPLIES",
                        onClick = { viewModel.setSmsFilter("AI_REPLIES") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Live AI Cognitive Analysis & Delayed Thinking Card
            ThinkingLiveCard(
                analysisState = liveAnalysisState,
                onSendImmediately = { viewModel.triggerSendImmediately() },
                onCancelSend = { viewModel.cancelScheduledReply() },
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // List of SMS Messages
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = "پیامکی یافت نشد",
                        description = "با دریافت یا ارسال پیامک، سوابق به صورت زنده در این بخش نمایش داده می‌شوند.",
                        actionButtonText = "ارسال پیامک دستی",
                        onActionClick = { showSendDialog = true }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        SmsItemCard(
                            message = message,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("SMS", message.body)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "متن پیامک کپی شد", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = { viewModel.deleteSms(message.id) },
                            onTriggerAiReply = {
                                showAiGeneratingDialog = true
                                aiDialogText = "در حال تحلیل پیام و تولید پاسخ هوشمند با لحن شما..."
                                viewModel.generateAndSendAiReplyForSms(
                                    destination = message.address,
                                    originalMessage = message.body
                                ) { success, resultText ->
                                    aiDialogText = resultText
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // Floating Action Button to Send Manual SMS
        FloatingActionButton(
            onClick = { showSendDialog = true },
            containerColor = JarvisCyan,
            contentColor = Color(0xFF00363D),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
                .size(48.dp)
                .testTag("new_sms_fab")
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "ارسال پیامک جدید", modifier = Modifier.size(22.dp))
        }
    }

    // Send SMS Dialog
    if (showSendDialog) {
        SendSmsDialog(
            onDismiss = { showSendDialog = false },
            onSend = { destination, body ->
                viewModel.sendManualSms(destination, body) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) showSendDialog = false
                }
            }
        )
    }

    // AI Generation Progress Dialog
    if (showAiGeneratingDialog) {
        AlertDialog(
            onDismissRequest = { showAiGeneratingDialog = false },
            title = { Text("پاسخ هوشمند شخص‌محور", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(aiDialogText, fontSize = 12.sp, lineHeight = 18.sp)
            },
            confirmButton = {
                Button(
                    onClick = { showAiGeneratingDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363D)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("بستن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun SmsFilterChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(title, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) JarvisCyan else MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun SmsItemCard(
    message: SmsMessageEntity,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onTriggerAiReply: () -> Unit
) {
    val isIncoming = message.direction == "INCOMING"
    val isAi = message.isAiReply

    val statusColor = when {
        isAi -> JarvisCyan
        isIncoming -> JarvisEmerald
        else -> MaterialTheme.colorScheme.secondary
    }

    val typeLabel = when {
        isAi -> "پاسخ هوش مصنوعی"
        isIncoming -> "دریافتی"
        else -> "ارسالی"
    }

    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            0.8.dp,
            if (isAi) JarvisCyan.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Address & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = message.address,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        StatusBadge(text = typeLabel, color = statusColor)
                    }

                    if (!message.ruleAppliedName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusBadge(text = message.ruleAppliedName, color = Color(0xFFF59E0B))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = timeFormatted,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isIncoming) {
                    OutlinedButton(
                        onClick = onTriggerAiReply,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        border = BorderStroke(0.8.dp, JarvisCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ارسال پاسخ با هوش مصنوعی", fontSize = 10.sp, color = JarvisCyan, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "کپی",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SendSmsDialog(
    onDismiss: () -> Unit,
    onSend: (String, String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ارسال پیامک جدید", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = { Text("شماره گیرنده (مثلاً 09121234567)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("متن پیامک...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 4,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 18.sp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(phone, text) },
                enabled = phone.isNotBlank() && text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363D)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ارسال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", fontSize = 11.sp)
            }
        }
    )
}
