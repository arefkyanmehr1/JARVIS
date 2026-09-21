package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.JarvisApplication
import com.example.voice.VoiceAssistantController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

@Composable
fun VoiceAssistantScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = remember { MainScope() }
    val app = context.applicationContext as JarvisApplication
    var state by remember { mutableStateOf(VoiceAssistantController.State()) }
    val controller = remember { VoiceAssistantController(context, app.aiRepository, scope) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) controller.start()
        else state = state.copy(error = "برای دستیار صوتی باید مجوز میکروفون را فعال کنی.")
    }

    LaunchedEffect(Unit) {
        controller.onState = { state = it }
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.close()
            scope.cancel()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.PowerSettingsNew, "بستن")
            }
            Text("دستیار صوتی JARVIS", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.size(48.dp))
        }

        Text("Gemini • Voice Assistant", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.height(24.dp))

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(210.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = .35f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(if (state.listening) 150.dp else 130.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.listening) Icons.Default.Mic else Icons.Default.MicOff,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(state.status, fontWeight = FontWeight.SemiBold)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("شما", fontWeight = FontWeight.Bold)
                Text(state.userText.ifBlank { "هنوز چیزی نگفته‌ای." }, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(14.dp))
                Text("JARVIS", fontWeight = FontWeight.Bold)
                Text(state.assistantText.ifBlank { "منتظر فرمان صوتی…" }, modifier = Modifier.padding(top = 6.dp))
            }
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { controller.stop() }) {
                Text("توقف")
            }

            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        controller.start()
                    } else {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            ) {
                Icon(Icons.Default.Mic, null)
                Spacer(Modifier.size(8.dp))
                Text(if (state.listening) "در حال گوش دادن…" else "صحبت با JARVIS")
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "گفتار فارسی → Gemini → پاسخ صوتی فارسی",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}