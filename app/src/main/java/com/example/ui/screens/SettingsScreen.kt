package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.navigation.SubScreen
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisEmerald

@Composable
fun SettingsScreen(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val currentPrompt by viewModel.systemPrompt.collectAsState()
    var promptDraft by remember(currentPrompt) { mutableStateOf(currentPrompt) }

    val savedName by viewModel.userName.collectAsState()
    var nameDraft by remember(savedName) { mutableStateOf(savedName) }

    val savedBio by viewModel.userBio.collectAsState()
    var bioDraft by remember(savedBio) { mutableStateOf(savedBio) }

    val savedStatus by viewModel.currentStatus.collectAsState()
    val statusTimestamp by viewModel.statusTimestamp.collectAsState()
    var statusDraft by remember(savedStatus) { mutableStateOf(savedStatus) }

    val replyDelayMinutes by viewModel.replyDelayMinutes.collectAsState()
    var customDelayDraft by remember(replyDelayMinutes) { mutableStateOf(replyDelayMinutes.toString()) }

    val isAnalyzingKnowledge by viewModel.isAnalyzingKnowledge.collectAsState()
    val knowledgeFeedback by viewModel.knowledgeAnalysisFeedback.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Text(
            text = "تنظیمات پیشرفته و هویت شخصی",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "اعلام وضعیت لحظه‌ای، بیوگرافی شخصی، زمان پاسخگویی و خطوط فعال",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Current Live Status Card (اعلام وضعیت لحظه‌ای)
        Text(
            text = "اعلام وضعیت لحظه‌ای (Status / Activity)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.8.dp, if (savedStatus.isNotBlank()) JarvisCyan.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.AccessTime, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "وضعیت فعلی شما (برای پاسخگویی هوشمند)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (savedStatus.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = JarvisCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "وضعیت فعال",
                                fontSize = 9.sp,
                                color = JarvisCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "مثال: «الان توی سالن ورزشی هستم دو ساعت دیگه بازی تموم میشه میرم خونه بعدا پیام میدم». هوش مصنوعی ساعت ثبت این وضعیت و گذشت زمان را محاسبه کرده و در پاسخ‌ها به طور طبیعی به مخاطب می‌گوید چقدر از کارتان مانده است.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = statusDraft,
                    onValueChange = { statusDraft = it },
                    placeholder = { Text("وضعیت الان خود را بنویسید...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 2,
                    maxLines = 4,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, lineHeight = 17.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (savedStatus.isNotBlank() && statusTimestamp > 0) {
                    val timeStr = remember(statusTimestamp) {
                        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(statusTimestamp))
                    }
                    val elapsedMin = ((System.currentTimeMillis() - statusTimestamp) / (60 * 1000)).coerceAtLeast(0)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ثبت شده در ساعت $timeStr (حدود $elapsedMin دقیقه پیش)",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (savedStatus.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearCurrentStatus()
                                statusDraft = ""
                                Toast.makeText(context, "اعلام وضعیت پاک شد", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("پاک کردن وضعیت", fontSize = 10.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            viewModel.setCurrentStatus(statusDraft.trim())
                            Toast.makeText(context, "وضعیت با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363D)),
                        shape = RoundedCornerShape(6.dp),
                        enabled = statusDraft.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ذخیره وضعیت فعلی", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Personal Profile & Bio (شخصی‌سازی نام و بیوگرافی)
        Text(
            text = "هویت و بیوگرافی شخصی شما (Personal Profile)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "مشخصات شما به هوش مصنوعی کمک می‌کند تا دقیقاً مثل شما فکر، صحبت و رفتار کند:",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("نام و نام خانوادگی شما (یا لقبتان)", fontSize = 11.sp) },
                    placeholder = { Text("مثلاً: عارف محمدی", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bioDraft,
                    onValueChange = { bioDraft = it },
                    label = { Text("بیوگرافی و سبک زندگی شخصی شما", fontSize = 11.sp) },
                    placeholder = { Text("مثلاً: من ۲۸ سالمه، مهندس کامپیوترم، اهل شوخی و فوتبال، دانشجو دانشگاه تبریز...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 3,
                    maxLines = 5,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, lineHeight = 17.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.setUserName(nameDraft.trim().ifBlank { "کاربر" })
                            viewModel.setUserBio(bioDraft.trim())
                            viewModel.analyzeAndAbsorbKnowledge { feedback ->
                                Toast.makeText(context, "اطلاعات توسط هوش مصنوعی تحلیل و جذب شد", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        enabled = !isAnalyzingKnowledge
                    ) {
                        if (isAnalyzingKnowledge) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = JarvisCyan)
                        } else {
                            Icon(imageVector = Icons.Filled.Psychology, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAnalyzingKnowledge) "در حال تحلیل ادراکی..." else "تحلیل و درک توسط هوش مصنوعی",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisCyan
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.setUserName(nameDraft.trim().ifBlank { "کاربر" })
                            viewModel.setUserBio(bioDraft.trim())
                            Toast.makeText(context, "اطلاعات هویتی با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363D)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ذخیره پروفایل", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (knowledgeFeedback != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = JarvisCyan.copy(alpha = 0.08f),
                        border = BorderStroke(0.6.dp, JarvisCyan.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = knowledgeFeedback ?: "",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Human Response Delay Mechanism (تأخیر طبیعی ارسال پاسخ)
        Text(
            text = "تأخیر طبیعی ارسال پاسخ (شبیه‌ساز رفتار انسانی)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.HourglassTop, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مدت زمان تأخیر پیش از ارسال پیامک",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "ارسال فوری حس رباتیک ایجاد می‌کند. با تنظیم تأخیر (حداقل بالای ۱ دقیقه)، هوش مصنوعی پاسخ را شبیه‌سازی کرده و سوابق مخاطب را زنده تحلیل می‌کند تا رفتار کاملاً انسانی باشد.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                val delayOptions = listOf(
                    0 to "فوری",
                    5 to "۵ دقیقه",
                    10 to "۱۰ دقیقه",
                    15 to "۱۵ دقیقه"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    delayOptions.forEach { (minutes, label) ->
                        val isSelected = replyDelayMinutes == minutes
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.setReplyDelayMinutes(minutes)
                                    customDelayDraft = minutes.toString()
                                    Toast.makeText(context, "تأخیر به $label تنظیم شد", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) JarvisCyan.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(0.8.dp, if (isSelected) JarvisCyan else Color.Transparent)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) JarvisCyan else MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom numeric input for minutes
                Text(
                    text = "یا تنظیم دقیق تأخیر به دقیقه (دلخواه با عدد):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customDelayDraft,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                customDelayDraft = input
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_delay_input"),
                        label = { Text("دقیقه (مثلاً ۱، ۱۰، ۳۰)", fontSize = 10.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Button(
                        onClick = {
                            val parsed = customDelayDraft.toIntOrNull() ?: 0
                            val validMinutes = parsed.coerceIn(0, 1440)
                            viewModel.setReplyDelayMinutes(validMinutes)
                            customDelayDraft = validMinutes.toString()
                            Toast.makeText(context, "تأخیر پاسخ به $validMinutes دقیقه تنظیم شد", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363D)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_custom_delay_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اعمال تأخیر", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Appearance & Theme Card
        val themeMode by viewModel.themeMode.collectAsState()
        Text(
            text = "پوسته ظاهری (Theme)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val themeOptions = listOf(
                    Triple("DARK", "تم تاریک HUD", Icons.Filled.DarkMode),
                    Triple("LIGHT", "تم روشن (Light)", Icons.Filled.LightMode)
                )
                themeOptions.forEach { (modeKey, modeTitle, modeIcon) ->
                    val isSelected = themeMode == modeKey
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.setThemeMode(modeKey)
                                Toast.makeText(context, "پوسته به $modeTitle تغییر یافت", Toast.LENGTH_SHORT).show()
                            },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) JarvisCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.8.dp, if (isSelected) JarvisCyan else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = modeIcon,
                                contentDescription = null,
                                tint = if (isSelected) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = modeTitle,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) JarvisCyan else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SIM / Line Selection Card
        val selectedSim by viewModel.selectedSimId.collectAsState()
        val availableSims = remember { viewModel.getAvailableSims() }

        Text(
            text = "سیم‌کارت پاسخ خودکار (فیلتر خط)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "مشخص کنید هوش مصنوعی فقط به پیام‌های کدام خط شما پاسخ دهد:",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Option: All SIMs
                val isAllSelected = selectedSim == -1
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable { viewModel.setSelectedSimId(-1) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isAllSelected) JarvisCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(0.8.dp, if (isAllSelected) JarvisCyan else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.SimCard,
                                contentDescription = null,
                                tint = if (isAllSelected) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "همه خطوط (هر دو سیم‌کارت ۱ و ۲)",
                                fontSize = 11.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAllSelected) JarvisCyan else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isAllSelected) {
                            Text("فعال", fontSize = 10.sp, color = JarvisCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Dynamic detected SIMs or fallback slots
                if (availableSims.isNotEmpty()) {
                    availableSims.forEach { sim ->
                        val isSimSelected = selectedSim == sim.subscriptionId || selectedSim == sim.slotIndex
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { viewModel.setSelectedSimId(sim.subscriptionId) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSimSelected) JarvisCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(0.8.dp, if (isSimSelected) JarvisCyan else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.SimCard,
                                        contentDescription = null,
                                        tint = if (isSimSelected) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${sim.displayName} (${sim.carrierName}) - خط ${sim.slotIndex + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSimSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSimSelected) JarvisCyan else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSimSelected) {
                                    Text("فعال", fontSize = 10.sp, color = JarvisCyan, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    listOf(0 to "فقط خط شماره ۱ (سیم‌کارت اول)", 1 to "فقط خط شماره ۲ (سیم‌کارت دوم)").forEach { (slot, label) ->
                        val isSlotSelected = selectedSim == slot
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { viewModel.setSelectedSimId(slot) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSlotSelected) JarvisCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(0.8.dp, if (isSlotSelected) JarvisCyan else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.SimCard,
                                        contentDescription = null,
                                        tint = if (isSlotSelected) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSlotSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSlotSelected) JarvisCyan else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSlotSelected) {
                                    Text("فعال", fontSize = 10.sp, color = JarvisCyan, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sub-screens Navigation Menu
        Text(
            text = "ماژول‌های اختصاصی",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        SettingsNavTile(
            icon = Icons.Filled.Key,
            title = "مدیریت کلیدهای API (DeepSeek و Gemini)",
            subtitle = "افزودن، چرخش خودکار و تست اتصال کلیدها",
            onClick = { viewModel.navigateToSubScreen(SubScreen.ApiManager) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        SettingsNavTile(
            icon = Icons.Filled.Memory,
            title = "سامانه حافظه محلی",
            subtitle = "مدیریت اطلاعات و زمینه‌های پایدار",
            onClick = { viewModel.navigateToSubScreen(SubScreen.MemoryManager) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        SettingsNavTile(
            icon = Icons.Filled.ListAlt,
            title = "گزارشات و لاگ‌های زنده",
            subtitle = "مشاهده لاگ‌های SMS، خطاها و AI",
            onClick = { viewModel.navigateToSubScreen(SubScreen.ActivityLogs) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        SettingsNavTile(
            icon = Icons.Filled.HelpOutline,
            title = "راهنمای راه‌اندازی و دسترسی‌ها",
            subtitle = "بررسی دسترسی‌های سیستم و راهنمای کاربری",
            onClick = { viewModel.navigateToSubScreen(SubScreen.SetupWizard) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // AI Model Selection Card
        val activeModel by viewModel.aiModel.collectAsState()

        Text(
            text = "موتور و مدل هوش مصنوعی",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "مدل فعال: $activeModel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisCyan
                )

                Spacer(modifier = Modifier.height(6.dp))

                val availableModels = listOf(
                    "gemini-flash-lite-latest" to "Google Gemini Flash-Lite (کم‌مصرف‌ترین و فوق‌سریع - پایدار)",
                    "gemini-flash-latest" to "Google Gemini Flash (پایدارترین و رسمی‌ترین مدل گوگل)",
                    "gemini-3.5-flash-lite" to "Google Gemini 3.5 Flash-Lite (نسخه اختصاصی ۳.۵)",
                    "gpt-4o-mini" to "ChatGPT: GPT-4o-Mini (بسیار سریع و اقتصادی)",
                    "gpt-5.6-luna" to "ChatGPT: GPT-5.6 Luna (بهینه برای حجم بالا)",
                    "openai/gpt-5-5" to "AIML API: OpenAI GPT-5-5 (پروکسی)",
                    "deepseek-chat" to "DeepSeek Chat (نسخه رسمی V3)"
                )

                availableModels.forEach { (modelKey, modelLabel) ->
                    val isSelected = activeModel.equals(modelKey, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable {
                                viewModel.setAiModel(modelKey)
                                Toast.makeText(context, "مدل به $modelKey تغییر یافت", Toast.LENGTH_SHORT).show()
                            },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) JarvisCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.8.dp, if (isSelected) JarvisCyan else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = modelLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) JarvisCyan else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Text("فعال", fontSize = 9.sp, color = JarvisCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Gemini Relay Bridge (بای‌پس فیلترینگ و تحریم بدون فیلترشکن)
        val isRelayEnabled by viewModel.isGeminiRelayEnabled.collectAsState()
        val currentRelayUrl by viewModel.geminiRelayUrl.collectAsState()
        var relayUrlDraft by remember(currentRelayUrl) { mutableStateOf(currentRelayUrl) }
        val relayHealthStatus by viewModel.relayHealthStatus.collectAsState()
        val isCheckingRelay by viewModel.isCheckingRelayHealth.collectAsState()

        Text(
            text = "پل رله واسط گوگل جمینی (اتصال بدون فیلترشکن)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.8.dp, if (isRelayEnabled) JarvisCyan.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Router,
                            contentDescription = null,
                            tint = if (isRelayEnabled) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "فعال‌سازی رله معکوس هاست (Bypass Proxy)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ارسال درخواست‌های Gemini از هاست و Google Apps Script بدون نیاز به فیلترشکن",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

                    OutlinedTextField(
                        value = relayUrlDraft,
                        onValueChange = { relayUrlDraft = it },
                        label = { Text("آدرس فایل PHP رله روی هاست", fontSize = 10.sp) },
                        placeholder = { Text("https://arefkyanmehr.ir/gemini.php", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.checkRelayHealth()
                            },
                            enabled = !isCheckingRelay,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            if (isCheckingRelay) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = JarvisCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                            } else {
                                Icon(imageVector = Icons.Filled.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp), tint = JarvisCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("تست سلامت رله", fontSize = 10.sp, color = JarvisCyan)
                        }

                        Button(
                            onClick = {
                                viewModel.setGeminiRelayUrl(relayUrlDraft.trim())
                                Toast.makeText(context, "آدرس رله ذخیره شد", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363D)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ذخیره آدرس", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (relayHealthStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = relayHealthStatus ?: "",
                            fontSize = 10.sp,
                            color = if (relayHealthStatus?.contains("موفق") == true) JarvisEmerald else MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { viewModel.clearRelayHealthStatus() }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // System Prompt Customization Card
        Text(
            text = "دستور و شخصیت اصلی (System Prompt)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "تعیین هویت اصلی که هوش مصنوعی در پاسخ به پیامک‌ها اتخاذ می‌کند:",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = promptDraft,
                    onValueChange = { promptDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("system_prompt_input"),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 3,
                    maxLines = 6,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, lineHeight = 18.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.resetSystemPrompt()
                            Toast.makeText(context, "پرامپت به حالت پیش‌فرض بازگشت", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بازنشانی", fontSize = 10.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.updateSystemPrompt(promptDraft)
                            Toast.makeText(context, "تنظیمات پرامپت ذخیره شد", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363D)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("save_system_prompt_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ذخیره پرامپت", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy & Storage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "امنیت و حریم خصوصی ۱۰۰٪ محلی",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "کلیه پیامک‌ها، حافظه و تاریخچه گفتگوها صرفاً در دیتابیس امن داخلی گوشی شما نگهداری می‌شوند.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsNavTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(18.dp)
                )

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

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
