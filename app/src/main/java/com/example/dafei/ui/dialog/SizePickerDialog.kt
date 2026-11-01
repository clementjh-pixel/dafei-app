package com.example.dafei.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dafei.ui.component.TagFlow
import com.example.dafei.util.SizeOrder

@Composable
fun SizePickerDialog(
    existingSizes: List<String>,            // 已存在的码数（这里自动排序，传原始顺序即可）
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,               // 点击已存在码数
    onAddNew: (String) -> Unit,             // 输入新码数后点「添加」
    onDelete: ((String) -> Unit)? = null    // 可选：长按删除
) {
    var input by remember { mutableStateOf("") }
    val sortedSizes = remember(existingSizes) { SizeOrder.sorted(existingSizes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择码数") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (sortedSizes.isEmpty()) {
                    Text(
                        "暂无码数，可在下方输入后添加",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // 关键：FlowRow，内容自适应宽度、自然换行，不再对齐
                    TagFlow(items = sortedSizes) { size ->
                        AssistChip(
                            onClick = { onPick(size) },
                            label = { Text(size) }
                        )
                    }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("新增码数") },
                    placeholder = { Text("如 XS / 2XL / 110") },
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