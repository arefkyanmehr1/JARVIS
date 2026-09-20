package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatMessageEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisEmerald

@Composable
fun ChatScreen(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val messages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isChatGenerating.collectAsState()
    val trainingFeedback by viewModel.trainingFeedback.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showTrainDialog by remember { mutableStateOf(false) }
    val userName by viewModel.userName.collectAsState()

    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Compact Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "گفتگو و تربیت هوش مصنوعی",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "هوش مصنوعی از مکالمات شما یاد می‌گیرد",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showTrainDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("train_dialog_trigger_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = "تربیت هوش مصنوعی",
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (messages.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "پاک‌سازی تاریخچه گفتگو",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Training feedback toast banner (when AI learns a rule)
        AnimatedVisibility(
            visible = trainingFeedback != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = JarvisEmerald.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, JarvisEmerald.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = JarvisEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = trainingFeedback ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { viewModel.dismissTrainingFeedback() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Quick Training Shortcuts Chip Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickPrompts = listOf(
                "یادت باشه اسمم $userName هست" to "نام من",
                "همیشه خیلی کوتاه و صمیمی جواب بده" to "لحن صمیمی",
                "هرگز نگو من هوش مصنوعی هستم" to "عدم معرفی AI",
                "اگر فامیلی پیام داد رسمی سلام کن" to "قانون احترام"
            )
            items(quickPrompts) { (trainPhrase, chipLabel) ->
                Surface(
                    modifier = Modifier.clickable {
                        textInput = trainPhrase
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ModelTraining,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = chipLabel,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Messages List or Empty State
        if (messages.isEmpty() && !isGenerating) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Filled.Psychology,
                    title = "آماده تربیت و یادگیری",
                    description = "با هوش مصنوعی چت کنید، هویت و قوانین خود را به او بیاموزید. او تمام دستورات شما را به خاطر می‌سپارد و در پاسخ پیامک‌ها مانند شما عمل می‌کند."
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        message = msg,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("JARVIS", msg.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "متن در حافظه کپی شد", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (isGenerating) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.8.dp,
                                color = JarvisCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "در حال تحلیل و به خاطرسپاری...",
                                fontSize = 11.sp,
                                color = JarvisCyan
                            )
                        }
                    }
                }
            }
        }

        // Compact Input Field Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("آموزش، نکته یا پیامی بنویسید...", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val prompt = textInput
                            textInput = ""
                            viewModel.sendChatMessage(prompt)
                        }
                    },
                    enabled = textInput.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (textInput.isNotBlank() && !isGenerating) JarvisCyan
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("send_chat_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "ارسال پیام",
                        tint = if (textInput.isNotBlank() && !isGenerating) Color(0xFF00363D) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Modal Dialog to explicitly train persona facts/rules
    if (showTrainDialog) {
        var ruleTitle by remember { mutableStateOf("") }
        var ruleContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showTrainDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.School, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تربیت مستقیم شخصیت هوش مصنوعی", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "دستورات و اطلاعاتی که اینجا می‌نویسید، برای همیشه در حافظه هوش مصنوعی ثبت می‌شود تا در پاسخ پیامک‌ها مانند شما فکر و صحبت کند:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = ruleTitle,
                        onValueChange = { ruleTitle = it },
                        placeholder = { Text("موضوع (مثلاً: نحوه سلام، دوستان، کار)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    OutlinedTextField(
                        value = ruleContent,
                        onValueChange = { ruleContent = it },
                        placeholder = { Text("قانون یا اطلاعات شخصی (مثلاً: من مهندس عمران هستم، اگر حسن زنگ زد بگو فردا می‌آیم)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        minLines = 3,
                        maxLines = 5,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 18.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ruleContent.isNotBlank()) {
                            viewModel.trainPersonaFact(ruleTitle, ruleContent)
                            showTrainDialog = false
                            Toast.makeText(context, "در مغز هوش مصنوعی ثبت شد!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363D)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = ruleContent.isNotBlank()
                ) {
                    Text("ثبت در حافظه دائم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showTrainDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("انصراف", fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    onCopy: () -> Unit
) {
    val isUser = message.sender == "USER"
    val isError = message.isError

    val bubbleColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        isError -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        isError -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = when {
        isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
        else -> JarvisCyan.copy(alpha = 0.25f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 290.dp),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 3.dp,
                bottomEnd = if (isUser) 3.dp else 14.dp
            ),
            color = bubbleColor,
            border = BorderStroke(0.8.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "شما" else "پاسخ‌یار من",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) MaterialTheme.colorScheme.primary else JarvisCyan
                    )

                    if (!isUser) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "کپی پاسخ",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = message.content,
                    fontSize = 12.sp,
                    color = textColor,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
