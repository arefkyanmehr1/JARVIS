package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AiAnalysisState
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisEmerald

@Composable
fun ThinkingLiveCard(
    analysisState: AiAnalysisState,
    onSendImmediately: () -> Unit = {},
    onCancelSend: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = analysisState.isAnalyzing,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_thinking_live_card"),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, JarvisCyan.copy(alpha = 0.8f)),
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header: Indicator + Contact/Sender + Live Status
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(JarvisCyan.copy(alpha = 0.18f)),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "فرایند تفکر و تحلیل زنده هوش مصنوعی (Thinking)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = JarvisCyan
                                )
                            }
                            val displayName = if (analysisState.contactName.isNotBlank()) {
                                "${analysisState.contactName} (${analysisState.sender})"
                            } else {
                                analysisState.sender
                            }
                            Text(
                                text = "مخاطب پیام: $displayName",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (analysisState.delayRemainingSeconds > 0) {
                        val minutes = analysisState.delayRemainingSeconds / 60
                        val seconds = analysisState.delayRemainingSeconds % 60
                        val countdownFormatted = String.format("%02d:%02d", minutes, seconds)

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = JarvisEmerald.copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, JarvisEmerald.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.HourglassTop,
                                    contentDescription = null,
                                    tint = JarvisEmerald,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ارسال در $countdownFormatted",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisEmerald
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current Step Badge
                if (analysisState.analysisStep.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = JarvisCyan.copy(alpha = 0.1f),
                        border = BorderStroke(0.5.dp, JarvisCyan.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = analysisState.analysisStep,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = JarvisCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Incoming Message
                if (analysisState.incomingMessage.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = "📩 پیام دریافتی: «${analysisState.incomingMessage}»",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(6.dp),
                            maxLines = 2
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Step-by-step Thinking Console
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "🧠 مراحل استدلال و تحلیل عمیق (Chain of Thought):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (analysisState.historySummary.isNotBlank()) {
                            Text(
                                text = "• سوابق و لحن مخاطب: ${analysisState.historySummary}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 14.sp
                            )
                        }

                        if (analysisState.personaInsight.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "• تطبیق با وضعیت صاحب خط: ${analysisState.personaInsight}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 14.sp
                            )
                        }

                        if (analysisState.decisionReasoning.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "• تصمیم و لحن پاسخ: ${analysisState.decisionReasoning}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // Planned Response and Scheduled Time Statement
                if (analysisState.plannedReply.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = JarvisEmerald.copy(alpha = 0.08f),
                        border = BorderStroke(0.8.dp, JarvisEmerald.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = JarvisEmerald,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val timeText = if (analysisState.scheduledSendTimeStr.isNotBlank()) {
                                    "در زمان مقرر (ساعت ${analysisState.scheduledSendTimeStr}) پیام زیر ارسال می‌شود:"
                                } else {
                                    "پیامک آماده ارسال در زمان مقرر:"
                                }
                                Text(
                                    text = timeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisEmerald
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "«${analysisState.plannedReply}»",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // Action buttons: Send Immediately OR Cancel
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSendImmediately,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_send_now"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = Color(0xFF00363D)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ارسال فوری (بدون معطلی)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onCancelSend,
                            modifier = Modifier
                                .weight(0.7f)
                                .testTag("btn_cancel_send"),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisAmber)
                        ) {
                            Icon(imageVector = Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("لغو ارسال", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
