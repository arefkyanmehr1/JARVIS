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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.local.entity.RuleEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisEmerald

@Composable
fun RulesScreen(viewModel: JarvisViewModel) {
    val rules by viewModel.allRules.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<RuleEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "قوانین هوشمند پاسخگویی",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "اولویت‌بندی پردازش، بلک‌لیست و رفتارهای خاص",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(
                    text = "${rules.count { it.isEnabled }} فعال",
                    color = JarvisEmerald
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Filled.Gavel,
                        title = "هیچ قانونی تعریف نشده است",
                        description = "می‌توانید قوانینی برای مسدودسازی تبلیغات، پاسخ ثابت، یا تغییر پرامپت هوش مصنوعی اضافه کنید.",
                        actionButtonText = "افزودن قانون جدید",
                        onActionClick = {
                            editingRule = null
                            showAddEditDialog = true
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        RuleItemCard(
                            rule = rule,
                            onToggle = { viewModel.toggleRule(rule) },
                            onEdit = {
                                editingRule = rule
                                showAddEditDialog = true
                            },
                            onDelete = { viewModel.deleteRule(rule) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // FAB to Add Rule
        FloatingActionButton(
            onClick = {
                editingRule = null
                showAddEditDialog = true
            },
            containerColor = JarvisCyan,
            contentColor = Color(0xFF00363D),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
                .size(48.dp)
                .testTag("add_rule_fab")
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "افزودن قانون", modifier = Modifier.size(22.dp))
        }
    }

    if (showAddEditDialog) {
        RuleDialog(
            initialRule = editingRule,
            onDismiss = { showAddEditDialog = false },
            onSave = { ruleToSave ->
                if (ruleToSave.id == 0L) {
                    viewModel.addRule(ruleToSave)
                } else {
                    viewModel.updateRule(ruleToSave)
                }
                showAddEditDialog = false
            }
        )
    }
}

@Composable
fun RuleItemCard(
    rule: RuleEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val actionColor = when (rule.actionType) {
        "AUTO_REPLY" -> JarvisCyan
        "FIXED_REPLY" -> JarvisEmerald
        "IGNORE" -> JarvisAmber
        "BLOCK" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    val actionLabel = when (rule.actionType) {
        "AUTO_REPLY" -> "پاسخ هوش مصنوعی"
        "FIXED_REPLY" -> "پاسخ ثابت"
        "IGNORE" -> "نادیده گرفتن"
        "BLOCK" -> "مسدودسازی"
        else -> rule.actionType
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        border = BorderStroke(0.8.dp, if (rule.isEnabled) actionColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(text = "اولویت ${rule.priority}", color = MaterialTheme.colorScheme.primary)
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Condition details
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (rule.senderPattern.isNotBlank()) {
                    Text(
                        text = "فرستنده: ${rule.senderPattern}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (rule.keywordPatterns.isNotBlank()) {
                    Text(
                        text = "کلمات کلیدی: ${rule.keywordPatterns}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "اقدام: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusBadge(text = actionLabel, color = actionColor)
                }
                if (!rule.fixedReplyText.isNullOrBlank()) {
                    Text(
                        text = "متن: ${rule.fixedReplyText}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${rule.matchCount} بار اجرا",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "ویرایش",
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
}

@Composable
fun RuleDialog(
    initialRule: RuleEntity?,
    onDismiss: () -> Unit,
    onSave: (RuleEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var senderPattern by remember { mutableStateOf(initialRule?.senderPattern ?: "") }
    var keywordPatterns by remember { mutableStateOf(initialRule?.keywordPatterns ?: "") }
    var actionType by remember { mutableStateOf(initialRule?.actionType ?: "AUTO_REPLY") }
    var fixedReplyText by remember { mutableStateOf(initialRule?.fixedReplyText ?: "") }
    var priority by remember { mutableStateOf(initialRule?.priority?.toString() ?: "10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialRule == null) "افزودن قانون جدید" else "ویرایش قانون",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام قانون", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )

                OutlinedTextField(
                    value = senderPattern,
                    onValueChange = { senderPattern = it },
                    label = { Text("الگوی فرستنده (اختیاری، مثلاً: 0912* یا بانک)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )

                OutlinedTextField(
                    value = keywordPatterns,
                    onValueChange = { keywordPatterns = it },
                    label = { Text("کلمات کلیدی با ویرگول (مثلاً: وام، تخفیف، تبلیغ)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )

                Text(
                    text = "نوع اقدام:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val actions = listOf(
                    "AUTO_REPLY" to "پاسخ هوش مصنوعی",
                    "FIXED_REPLY" to "ارسال پاسخ ثابت",
                    "IGNORE" to "نادیده گرفتن",
                    "BLOCK" to "مسدودسازی کامل"
                )

                actions.forEach { (key, label) ->
                    val isSelected = actionType == key
                    Button(
                        onClick = { actionType = key },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) JarvisCyan else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) Color(0xFF00363D) else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }

                if (actionType == "FIXED_REPLY") {
                    OutlinedTextField(
                        value = fixedReplyText,
                        onValueChange = { fixedReplyText = it },
                        label = { Text("متن پاسخ ثابت", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        minLines = 2,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                }

                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it },
                    label = { Text("اولویت اجرا (عدد صحیح)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = (initialRule ?: RuleEntity(name = "")).copy(
                        name = name.ifBlank { "قانون بدون نام" },
                        senderPattern = senderPattern.trim(),
                        keywordPatterns = keywordPatterns.trim(),
                        actionType = actionType,
                        fixedReplyText = if (actionType == "FIXED_REPLY") fixedReplyText.trim() else null,
                        priority = priority.toIntOrNull() ?: 10
                    )
                    onSave(entity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF00363D)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ذخیره قانون", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", fontSize = 11.sp)
            }
        }
    )
}
