package com.aiassistant.ui.screens.roleplay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiassistant.domain.model.PlotAction

@Composable
fun PlotActionBar(
    onAction: (PlotAction, String?) -> Unit,
    onProposeSetting: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "剧情操作",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 常用操作
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onProposeSetting != null) {
                    item {
                        AssistChip(
                            onClick = onProposeSetting,
                            leadingIcon = {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            },
                            label = { Text("识别输入补充设定", color = MaterialTheme.colorScheme.primary) }
                        )
                    }
                }
                val quickActions = listOf(
                    PlotAction.CONTINUE,
                    PlotAction.BRANCH_CHOICES,
                    PlotAction.REGENERATE,
                    PlotAction.REWRITE,
                    PlotAction.EXTEND,
                    PlotAction.SHORTEN,
                    PlotAction.SUMMARY,
                    PlotAction.DIALOGUE_ONLY,
                    PlotAction.NARRATION_ONLY,
                    PlotAction.DIALOGUE_ACTION
                )
                items(quickActions) { action ->
                    ActionChip(
                        action = action,
                        onClick = { onAction(action, null) }
                    )
                }
                item {
                    ActionChip(
                        action = PlotAction.CUSTOM,
                        onClick = { showCustomDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 更多操作
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onAction(PlotAction.CHANGE_PERSPECTIVE, null) },
                    label = { Text("改变视角") }
                )
                AssistChip(
                    onClick = { onAction(PlotAction.CHANGE_TONE, null) },
                    label = { Text("改变语气") }
                )
                AssistChip(
                    onClick = { onAction(PlotAction.BRANCH, null) },
                    label = { Text("创建分支") }
                )
                AssistChip(
                    onClick = { onAction(PlotAction.ROLLBACK, null) },
                    label = { Text("回退版本") }
                )
            }
        }
    }

    // 自定义指令对话框
    if (showCustomDialog) {
        CustomInstructionDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { instruction ->
                showCustomDialog = false
                onAction(PlotAction.CUSTOM, instruction)
            }
        )
    }
}

@Composable
private fun ActionChip(
    action: PlotAction,
    onClick: () -> Unit
) {
    val icon = when (action) {
        PlotAction.CONTINUE -> Icons.Default.PlayArrow
        PlotAction.BRANCH_CHOICES -> Icons.Default.AltRoute
        PlotAction.REGENERATE -> Icons.Default.Refresh
        PlotAction.REWRITE -> Icons.Default.Edit
        PlotAction.EXTEND -> Icons.Default.Add
        PlotAction.SHORTEN -> Icons.Default.Remove
        PlotAction.SUMMARY -> Icons.Default.Summarize
        PlotAction.DIALOGUE_ONLY -> Icons.AutoMirrored.Filled.Chat
        PlotAction.NARRATION_ONLY -> Icons.AutoMirrored.Filled.MenuBook
        PlotAction.DIALOGUE_ACTION -> Icons.Default.RecordVoiceOver
        PlotAction.CUSTOM -> Icons.Default.MoreHoriz
        else -> Icons.Default.MoreHoriz
    }

    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(action.displayName) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
private fun CustomInstructionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var instruction by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义指令") },
        text = {
            Column {
                Text(
                    text = "输入你想要的剧情发展方向：",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("指令内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = {
                        Text("例如：\n- 让他们在雨夜的车站再次相遇\n- 让冲突升级，但不要立即解决\n- 从配角视角继续这一段")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(instruction) },
                enabled = instruction.isNotBlank()
            ) {
                Text("执行")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
