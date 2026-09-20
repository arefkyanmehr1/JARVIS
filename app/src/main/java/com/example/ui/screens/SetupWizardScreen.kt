package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.components.JarvisHudCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisEmerald

@Composable
fun SetupWizardScreen(
    viewModel: JarvisViewModel,
    onRequestPermissions: () -> Unit
) {
    val hasReceiveSms = viewModel.hasSmsReceivePermission()
    val hasSendSms = viewModel.hasSmsSendPermission()
    val hasContacts = viewModel.hasContactsPermission()
    val hasNotifications = viewModel.hasNotificationPermission()
    val isOnline = viewModel.isNetworkAvailable()

    val apiKeys by viewModel.allApiKeys.collectAsState()
    var quickApiKey by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "راهنمای راه‌اندازی JARVIS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "بررسی پیش‌نیازها و فعال‌سازی دستیار هوشمند",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 1: Intro Card
        JarvisHudCard(borderColor = JarvisCyan) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Psychology, contentDescription = null, tint = JarvisCyan)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "JARVIS چیست؟",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "JARVIS پیامک‌های ورودی را با استفاده از مدل هوش مصنوعی Gemini پردازش کرده و بر اساس قوانین و حافظه شما، پاسخ مناسب ارسال می‌کند.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Permissions Check
        Text(
            text = "مرحله ۱: دسترسی‌های اندروید",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionStatusRow(
            icon = Icons.Filled.Sms,
            title = "دریافت پیامک (RECEIVE_SMS)",
            subtitle = "شناسایی پیامک‌های ورودی برای ارسال به هوش مصنوعی",
            isGranted = hasReceiveSms
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionStatusRow(
            icon = Icons.Filled.Sms,
            title = "ارسال پیامک (SEND_SMS)",
            subtitle = "ارسال خودکار پاسخ‌های تولیدشده توسط هوش مصنوعی",
            isGranted = hasSendSms
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionStatusRow(
            icon = Icons.Filled.Notifications,
            title = "ارسال اعلان‌ها (POST_NOTIFICATIONS)",
            subtitle = "اطلاع‌رسانی پیام‌های جدید و پاسخ‌های ارسالی",
            isGranted = hasNotifications
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionStatusRow(
            icon = Icons.Filled.Contacts,
            title = "خواندن مخاطبین (READ_CONTACTS)",
            subtitle = "شناسایی نام مخاطبین جهت شخصی‌سازی و صمیمیت پاسخ هوش مصنوعی",
            isGranted = hasContacts
        )

        if (!hasReceiveSms || !hasSendSms || !hasContacts || !hasNotifications) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363D)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wizard_grant_permissions_btn")
            ) {
                Text("درخواست دسترسی‌های لازم", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Step 3: API Key Setup
        Text(
            text = "مرحله ۲: اتصال به هوش مصنوعی Gemini",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (apiKeys.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${apiKeys.size} کلید API فعال در سیستم موجود است.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisEmerald
                        )
                        StatusBadge(text = "متصل", color = JarvisEmerald)
                    }
                } else {
                    Text(
                        text = "یک کلید ChatGPT (sk-proj-...)، AIML API، DeepSeek یا Gemini وارد کنید:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = quickApiKey,
                        onValueChange = { quickApiKey = it },
                        placeholder = { Text("sk-proj-... / AIML / sk-... / AIzaSy...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (quickApiKey.isNotBlank()) {
                                viewModel.addApiKey("کلید راه‌اندازی اولیه", quickApiKey, 1)
                                quickApiKey = ""
                            }
                        },
                        enabled = quickApiKey.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363D)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ذخیره کلید API", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Step 4: Finish Action
        Button(
            onClick = { viewModel.navigateBack() },
            colors = ButtonDefaults.buttonColors(containerColor = JarvisEmerald, contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("finish_setup_wizard_button")
        ) {
            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تکمیل راه‌اندازی و ورود به داشبورد", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PermissionStatusRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isGranted) JarvisEmerald.copy(alpha = 0.4f) else JarvisAmber.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) JarvisEmerald else JarvisAmber,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

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

            StatusBadge(
                text = if (isGranted) "مجاز" else "نیاز به دسترسی",
                color = if (isGranted) JarvisEmerald else JarvisAmber
            )
        }
    }
}
