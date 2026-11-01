package com.example.dafei.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dafei.ui.component.TagFlow

/**
 * @param colorMap 可选：颜色名 -> 颜色值，用于在 chip 前显示色块。没有就传 emptyMap()
 */
@Composable
fun ColorPickerDialog(
    existingColors: List<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onAddNew: (String) -> Unit,
    colorMap: Map<String, Color> = emptyMap()
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existingColors.isEmpty()) {
                    Text(
                        "暂无颜色，可在下方输入后添加",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // 关键：FlowRow，横向排列、内容自适应宽度，不对齐
                    TagFlow(items = existingColors) { name ->
                        val dot = colorMap[name]
                        AssistChip(
                            onClick = { onPick(name) },
                            label = { Text(name) },
                            leadingIcon = if (dot != null) {
                                {
                                    Box(
                                        Modifier
                                            .size(14.dp)
                                            .background(dot, CircleShape)
                                    )
                                }
                            } else null
                        )
                    }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("新增颜色") },
                    placeholder = { Text("如 藏青 / 米白") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = input.trim()
                    if (v.isNotEmpty()) onAddNew(v)
                    input = ""
                },
                enabled = input.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}