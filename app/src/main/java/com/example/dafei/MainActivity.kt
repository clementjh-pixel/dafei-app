package com.example.dafei

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.dafei.util.SizeOrder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.edit
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ReceiptLong
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
// 确保有这些 import（如果有飘红请 Alt+Enter 导入）
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusState



//logo图Base64
const val logoBase64 =
    "iVBORw0KGgoAAAANSUhEUgAAAJAAAACQCAYAAADnRuK4AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAACxIAAAsSAdLdfvwAAAtgaVRYdFhNTDpjb20uYWRvYmUueG1wAAAAAAA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVGN6a2M5ZCI/Pg0KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iQWRvYmUgWE1QIENvcmUgNS42LWMxNDIgNzkuMTYwOTI0LCAyMDE3LzA3LzEzLTAxOjA2OjM5ICAgICAgICAiPg0KCTxyZGY6UkRGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+DQoJCTxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6ZGM9Imh0dHA6Ly9wdXJsLm9yZy9kYy9lbGVtZW50cy8xLjEvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyIgeG1sbnM6dGlmZj0iaHR0cDovL25zLmFkb2JlLmNvbS90aWZmLzEuMC8iIHhtbG5zOmV4aWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20vZXhpZi8xLjAvIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDQyAoV2luZG93cykiIHhtcDpDcmVhdGVEYXRlPSIyMDE5LTAzLTE4VDEzOjEwKzA4OjAwIiB4bXA6TWV0YWRhdGFEYXRlPSIyMDE5LTAzLTE4VDE0OjMzOjU0KzA4OjAwIiB4bXA6TW9kaWZ5RGF0ZT0iMjAxOS0wMy0xOFQxNDozMzo1NCswODowMCIgZGM6Zm9ybWF0PSJpbWFnZS9wbmciIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6ZTY1Nzc4M2ItZmM0NS0xMjRlLTg5YzctODVmYmQ2YjEzZDZkIiB4bXBNTTpEb2N1bWVudElEPSJhZG9iZTpkb2NpZDpwaG90b3Nob3A6ZDA3YWZmOTItZDJlOC04OTQ5LWI3ZjUtNjIzNGE2OGMxZmNiIiB4bXBNTTpPcmlnaW5hbERvY3VtZW50SUQ9InhtcC5kaWQ6YmM4MjVkZGEtNmI4ZS04NjQwLWExNWYtMGZiMzcyMjNiNDA5IiBwaG90b3Nob3A6Q29sb3JNb2RlPSIzIiB0aWZmOk9yaWVudGF0aW9uPSIxIiB0aWZmOlhSZXNvbHV0aW9uPSI3MjAwMDAvMTAwMDAiIHRpZmY6WVJlc29sdXRpb249IjcyMDAwMC8xMDAwMCIgdGlmZjpSZXNvbHV0aW9uVW5pdD0iMiIgZXhpZjpDb2xvclNwYWNlPSI2NTUzNSIgZXhpZjpQaXhlbFhEaW1lbnNpb249IjE5MiIgZXhpZjpQaXhlbFlEaW1lbnNpb249IjE5MiI+DQoJCQk8eG1wTU06SGlzdG9yeT4NCgkJCQk8cmRmOlNlcT4NCgkJCQkJPHJkZjpsaSBzdEV2dDphY3Rpb249ImNyZWF0ZWQiIHN0RXZ0Omluc3RhbmNlSUQ9InhtcC5paWQ6YmM4MjVkZGEtNmI4ZS04NjQwLWExNWYtMGZiMzcyMjNiNDA5IiBzdEV2dDp3aGVuPSIyMDE5LTAzLTE4VDEzOjEwKzA4OjAwIiBzdEV2dDpzb2Z0d2FyZUFnZW50PSJBZG9iZSBQaG90b3Nob3AgQ0MgKFdpbmRvd3MpIi8+DQoJCQkJCTxyZGY6bGkgc3RFdnQ6YWN0aW9uPSJzYXZlZCIgc3RFdnQ6aW5zdGFuY2VJRD0ieG1wLmlpZDo3NmNjNmU2NC01YmI3LTNkNDQtYmQ5Ni02YTdjMzFkZjkzMzkiIHN0RXZ0OndoZW49IjIwMTktMDMtMThUMTM6MjE6MTMrMDg6MDAiIHN0RXZ0OnNvZnR3YXJlQWdlbnQ9IkFkb2JlIFBob3Rvc2hvcCBDQyAoV2luZG93cykiIHN0RXZ0OmNoYW5nZWQ9Ii8iLz4NCgkJCQkJPHJkZjpsaSBzdEV2dDphY3Rpb249InNhdmVkIiBzdEV2dDppbnN0YW5jZUlEPSJ4bXAuaWlkOmJkNGE4ZWQ3LTFkMzItZmQ0Yy1iZmM0LWQ0YjQ3MzZiODQ5MSIgc3RFdnQ6d2hlbj0iMjAxOS0wMy0xOFQxNDozMzo1NCswODowMCIgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wIENDIChXaW5kb3dzKSIgc3RFdnQ6Y2hhbmdlZD0iLyIvPg0KCQkJCQk8cmRmOmxpIHN0RXZ0OmFjdGlvbj0iY29udmVydGVkIiBzdEV2dDpwYXJhbWV0ZXJzPSJmcm9tIGFwcGxpY2F0aW9uL3ZuZC5hZG9iZS5waG90b3Nob3AgdG8gaW1hZ2UvcG5nIi8+DQoJCQkJCTxyZGY6bGkgc3RFdnQ6YWN0aW9uPSJkZXJpdmVkIiBzdEV2dDpwYXJhbWV0ZXJzPSJjb252ZXJ0ZWQgZnJvbSBhcHBsaWNhdGlvbi92bmQuYWRvYmUucGhvdG9zaG9wIHRvIGltYWdlL3BuZyIvPg0KCQkJCQk8cmRmOmxpIHN0RXZ0OmFjdGlvbj0ic2F2ZWQiIHN0RXZ0Omluc3RhbmNlSUQ9InhtcC5paWQ6ZTY1Nzc4M2ItZmM0NS0xMjRlLTg5YzctODVmYmQ2YjEzZDZkIiBzdEV2dDp3aGVuPSIyMDE5LTAzLTE4VDE0OjMzOjU0KzA4OjAwIiBzdEV2dDpzb2Z0d2FyZUFnZW50PSJBZG9iZSBQaG90b3Nob3AgQ0MgKFdpbmRvd3MpIiBzdEV2dDpjaGFuZ2VkPSIvIi8+DQoJCQkJPC9yZGY6U2VxPg0KCQkJPC94bXBNTTpIaXN0b3J5Pg0KCQkJPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6YmQ0YThlZDctMWQzMi1mZDRjLWJmYzQtZDRiNDczNmI4NDkxIiBzdFJlZjpkb2N1bWVudElEPSJhZG9iZTpkb2NpZDpwaG90b3Nob3A6MzMyZjg5ZDctZjRkNC0xZjQwLWFjNTYtNTUzMWZhMTg2ZTZlIiBzdFJlZjpvcmlnaW5hbERvY3VtZW50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"

data class TicketData(
    val ticketNo: String, val cutId: String, val customer: String, val styleNo: String,
    val bedNo: String, val dateStr: String, val color: String, val size: String,
    val quantity: Int, val bundleNo: Int, val totalBundles: Int = 0, val totalQuantity: Int = 0
)

data class RowInputData(
    val color: String = "", val quantities: MutableMap<String, String> = mutableMapOf()
)

data class CutOrderData(
    val id: String,
    val customer: String,
    val styleNo: String,
    val bedNo: String,
    val dateStr: String,
    val time: Long,
    val totalQty: Int,
    val tickets: List<TicketData>,
    val sizesOrder: List<String>,
    val rawRows: List<RowInputData>,
    val rowBundles: Map<Int, Map<Int, Int>>,
    val imageBase64: String? = null
)

// 1. 码数排序工具
object SizeOrder {
    private val STANDARD = listOf("XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL", "4XL", "5XL", "6XL")
    private val ALIAS = mapOf("1XL" to "XL", "2XL" to "XXL", "3XL" to "XXXL")

    fun rank(size: String): Int {
        val raw = size.trim().uppercase()
        if (raw.isEmpty()) return Int.MAX_VALUE
        val std = ALIAS[raw] ?: raw
        STANDARD.indexOf(std).takeIf { it >= 0 }?.let { return it }
        Regex("^(\\d+)XL$").find(raw)?.let { m ->
            val n = m.groupValues[1].toIntOrNull() ?: 0
            if (n >= 1) STANDARD.indexOf("X".repeat(n) + "L").takeIf { it >= 0 }?.let { return it }
        }
        raw.toIntOrNull()?.let { return 1000 + it }
        return 100_000
    }

    fun sorted(sizes: Collection<String>): List<String> =
        sizes.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sortedWith(compareBy({ rank(it) }, { it }))
}

// 2. 高质量图片压缩 (适合后续服务器存储)
// 👇 高质量的图片压缩函数：保留清晰度，限制大小，适合服务器和Base64存储
fun compressImage(imageBytes: ByteArray): ByteArray {
    // 1. 先读取原始图片宽高
    val options = android.graphics.BitmapFactory.Options()
    options.inJustDecodeBounds = true
    android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
    val srcWidth = options.outWidth
    val srcHeight = options.outHeight

    // 2. 计算缩放比例（限制最大边长为 1600 像素）
    var scale = 1
    val maxDimension = maxOf(srcWidth, srcHeight)
    if (maxDimension > 1600) {
        scale = (maxDimension / 1600f).toInt()
    }

    // 3. 按比例压缩读取
    val decodeOptions = android.graphics.BitmapFactory.Options()
    decodeOptions.inSampleSize = scale
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions)

    // 4. 质量压缩（动态控制，目标单张在 200KB~400KB 之间）
    val outputStream = java.io.ByteArrayOutputStream()
    var quality = 85
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)

    // 如果压缩后还是大于 500KB（Base64 后会变 600KB+），就继续降低质量
    while (outputStream.toByteArray().size > 500 * 1024 && quality > 50) {
        outputStream.reset()
        quality -= 5
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
    }

    bitmap.recycle()
    return outputStream.toByteArray()
}

// 3. 生成裁床单图片 (给明细页导出用)
// 👇 生成裁床单图片的工具函数
@SuppressLint("UseKtx")
fun generateOrderBitmap(order: CutOrderData): Bitmap {
    val widthColor = 150f    // 颜色列宽
    val widthBundle = 90f    // 扎号列宽
    val widthQty = 110f      // 件数列宽
    val widthTotal = 120f    // 合计列宽
    val rowHeight = 80f      // 行高
    val headerHeight = 180f  // 顶部表头信息区高度

    val sizesCount = order.sizesOrder.size
    val imgWidth = widthColor + sizesCount * (widthBundle + widthQty) + widthTotal + 20f
    val imgHeight = headerHeight + rowHeight * (order.rawRows.size + 2) + 40f

    val bitmap = Bitmap.createBitmap(imgWidth.toInt(), imgHeight.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 32f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val boldPaint = Paint(textPaint).apply { isFakeBoldText = true }
    val bundlePaint = Paint(textPaint).apply { color = android.graphics.Color.parseColor("#1565C0") }
    val linePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    // 4. 绘制顶部备注信息
    val infoPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 36f
        isAntiAlias = true
        isFakeBoldText = true
    }
    val infoPadding = 20f
    var startY = 60f
    canvas.drawText("款号: ${order.styleNo}", infoPadding, startY, infoPaint)
    canvas.drawText("日期: ${order.dateStr}", infoPadding, startY + 50f, infoPaint)
    canvas.drawText("客户: ${order.customer}", infoPadding, startY + 100f, infoPaint)
    canvas.drawText("床号: ${order.bedNo}", infoPadding + 400f, startY, infoPaint)

    // 5. 开始画表格
    var currentY = headerHeight

    // --- 画表头 ---
    val headerBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#F0F0F0") }
    canvas.drawRect(0f, currentY, imgWidth, currentY + rowHeight, headerBgPaint)

    var currentX = 0f
    // 颜色列
    canvas.drawText("颜色", currentX + widthColor / 2, currentY + rowHeight / 2 + 10f, boldPaint)
    canvas.drawLine(currentX, currentY, currentX, currentY + rowHeight, linePaint)
    canvas.drawLine(currentX, currentY, currentX + widthColor, currentY, linePaint)
    // 👇 修复：补上颜色列的右边框
    canvas.drawLine(currentX + widthColor, currentY, currentX + widthColor, currentY + rowHeight, linePaint)
    currentX += widthColor

    // 码数列
    order.sizesOrder.forEach { size ->
        // 扎号列
        canvas.drawText("扎号", currentX + widthBundle / 2, currentY + rowHeight / 2 + 10f, boldPaint)
        canvas.drawLine(currentX + widthBundle, currentY, currentX + widthBundle, currentY + rowHeight, linePaint)
        canvas.drawLine(currentX, currentY, currentX + widthBundle, currentY, linePaint)
        currentX += widthBundle

        // 件数列
        canvas.drawText(size, currentX + widthQty / 2, currentY + rowHeight / 2 + 10f, boldPaint)
        canvas.drawLine(currentX + widthQty, currentY, currentX + widthQty, currentY + rowHeight, linePaint)
        canvas.drawLine(currentX, currentY, currentX + widthQty, currentY, linePaint)
        currentX += widthQty
    }
    // 合计列
    canvas.drawText("合计", currentX + widthTotal / 2, currentY + rowHeight / 2 + 10f, boldPaint)
    canvas.drawLine(currentX + widthTotal, currentY, currentX + widthTotal, currentY + rowHeight, linePaint)
    canvas.drawLine(currentX, currentY, currentX + widthTotal, currentY, linePaint)

    currentY += rowHeight
    canvas.drawLine(0f, currentY, imgWidth, currentY, linePaint) // 表头底线

    // --- 画数据行 ---
    val sizeTotals = mutableMapOf<Int, Int>()
    order.rawRows.forEachIndexed { rowIdx, row ->
        var rowTotal = 0
        currentX = 0f

        // 颜色
        canvas.drawText(row.color, currentX + widthColor / 2, currentY + rowHeight / 2 + 10f, textPaint)
        canvas.drawLine(currentX, currentY, currentX, currentY + rowHeight, linePaint)
        // 👇 修复：补上颜色列的右边框
        canvas.drawLine(currentX + widthColor, currentY, currentX + widthColor, currentY + rowHeight, linePaint)
        currentX += widthColor

        order.sizesOrder.forEachIndexed { colIndex, _ ->
            val qty = row.quantities["${order.sizesOrder[colIndex]}-$colIndex"]?.toIntOrNull() ?: 0
            val bundleNo = order.rowBundles[rowIdx]?.get(colIndex)

            // 扎号
            if (bundleNo != null) {
                canvas.drawText(String.format("%02d", bundleNo), currentX + widthBundle / 2, currentY + rowHeight / 2 + 10f, bundlePaint)
            }
            canvas.drawLine(currentX + widthBundle, currentY, currentX + widthBundle, currentY + rowHeight, linePaint)
            currentX += widthBundle

            // 件数
            if (qty > 0) {
                canvas.drawText(qty.toString(), currentX + widthQty / 2, currentY + rowHeight / 2 + 10f, textPaint)
            }
            rowTotal += qty
            sizeTotals[colIndex] = (sizeTotals[colIndex] ?: 0) + qty
            canvas.drawLine(currentX + widthQty, currentY, currentX + widthQty, currentY + rowHeight, linePaint)
            currentX += widthQty
        }

        // 行合计
        canvas.drawText("${rowTotal}件", currentX + widthTotal / 2, currentY + rowHeight / 2 + 10f, textPaint)
        canvas.drawLine(currentX + widthTotal, currentY, currentX + widthTotal, currentY + rowHeight, linePaint)

        currentY += rowHeight
        canvas.drawLine(0f, currentY, imgWidth, currentY, linePaint) // 行底线
    }

    // --- 画合计行 ---
    currentX = 0f
    canvas.drawText("合计", currentX + widthColor / 2, currentY + rowHeight / 2 + 10f, boldPaint)
    canvas.drawLine(currentX, currentY, currentX, currentY + rowHeight, linePaint)
    // 👇 修复：补上颜色列的右边框
    canvas.drawLine(currentX + widthColor, currentY, currentX + widthColor, currentY + rowHeight, linePaint)
    currentX += widthColor

    var grandTotal = 0
    order.sizesOrder.forEachIndexed { colIndex, _ ->
        val st = sizeTotals[colIndex] ?: 0
        grandTotal += st

        // 扎号列留空
        canvas.drawLine(currentX + widthBundle, currentY, currentX + widthBundle, currentY + rowHeight, linePaint)
        currentX += widthBundle

        // 件数总计
        canvas.drawText("${st}件", currentX + widthQty / 2, currentY + rowHeight / 2 + 10f, boldPaint)
        canvas.drawLine(currentX + widthQty, currentY, currentX + widthQty, currentY + rowHeight, linePaint)
        currentX += widthQty
    }

    // 总计
    canvas.drawText("${grandTotal}件", currentX + widthTotal / 2, currentY + rowHeight / 2 + 10f, boldPaint)
    canvas.drawLine(currentX + widthTotal, currentY, currentX + widthTotal, currentY + rowHeight, linePaint)

    // 整体外框
    canvas.drawRect(0f, headerHeight, imgWidth, currentY + rowHeight, linePaint)

    return bitmap
}

fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        Log.e("打菲助手", "Logo图片解码失败", e) // 👈 加在这里
        null
    }
}

// 只读文字：宽度固定，优先换行，换行超阈值才缩字号，字号有下限，完整显示不截断
@Composable
fun AutoFitWrappedText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 14.sp,
    minFontSize: TextUnit = 10.sp,
    maxPreferredLines: Int = 3,
    textAlign: TextAlign = TextAlign.Center,
    color: Color = Color.Black
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val hPadPx = with(density) { 4.dp.toPx() }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val availWidthPx = (constraints.maxWidth - hPadPx * 2).coerceAtLeast(1f)

        val fontSize = remember(text, availWidthPx, maxFontSize, minFontSize, maxPreferredLines) {
            var size = maxFontSize
            while (true) {
                val layout = measurer.measure(
                    text = AnnotatedString(text),
                    style = TextStyle(fontSize = size, textAlign = textAlign),
                    constraints = Constraints(maxWidth = availWidthPx.toInt()),
                    softWrap = true,
                    maxLines = Int.MAX_VALUE
                )
                if (layout.lineCount <= maxPreferredLines || size.value <= minFontSize.value) break
                size = (size.value - 0.5f).sp
            }
            size
        }

        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            fontSize = fontSize,
            textAlign = textAlign,
            color = color,
            softWrap = true,
            maxLines = Int.MAX_VALUE
        )
    }
}

// 可编辑输入框：同策略，宽度固定、优先换行、超阈值缩字号、下限保可读、完整显示
@Composable
fun AutoFitWrappedBasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 14.sp,
    minFontSize: TextUnit = 10.sp,
    maxPreferredLines: Int = 3,
    textAlign: TextAlign = TextAlign.Center,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onFocusChanged: ((FocusState) -> Unit)? = null
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val hPadPx = with(density) { 4.dp.toPx() }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val availWidthPx = (constraints.maxWidth - hPadPx * 2).coerceAtLeast(1f)

        val fontSize = remember(value, availWidthPx, maxFontSize, minFontSize, maxPreferredLines) {
            var size = maxFontSize
            val sample = if (value.isEmpty()) " " else value
            while (true) {
                val layout = measurer.measure(
                    text = AnnotatedString(sample),
                    style = TextStyle(fontSize = size, textAlign = textAlign),
                    constraints = Constraints(maxWidth = availWidthPx.toInt()),
                    softWrap = true,
                    maxLines = Int.MAX_VALUE
                )
                if (layout.lineCount <= maxPreferredLines || size.value <= minFontSize.value) break
                size = (size.value - 0.5f).sp
            }
            size
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = false,
            keyboardOptions = keyboardOptions,
            textStyle = TextStyle(textAlign = textAlign, fontSize = fontSize, color = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onFocusChanged != null) Modifier.onFocusChanged(onFocusChanged) else Modifier),
            decorationBox = { innerTextField ->
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) { innerTextField() }
            }
        )
    }
}

class MainActivity : ComponentActivity() {

    // 在你的 MainActivity 类里面定义这个广播接收器
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                // 只要收到任何蓝牙设备的断开广播
                if (BluetoothPrinter.isConnected()) {
                    // 如果之前显示的是已连接，现在变成了断开，就更新 UI
                    connectedDeviceName = null
                    Toast.makeText(context, "打印机已断开连接", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }
    private var connectedDeviceName by mutableStateOf<String?>(null)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermissions()

        // 👇 新增：动态注册蓝牙断开广播
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        registerReceiver(bluetoothReceiver, filter)

        setContent {
            MaterialTheme {
                // 注意：如果你之前 AppRoot 需要两个参数，请按你的实际代码补齐
                AppRoot(connectedDeviceName)
            }
        }
    }

    // 👇 新增：在 Activity 销毁时注销广播，防止内存泄漏
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // 👇 记录错误日志
            android.util.Log.e("DafeiApp", "注销蓝牙广播接收器失败: ${e.message}", e)
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        requestPermissionLauncher.launch(permissions)
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e("打菲助手", "获取蓝牙设备失败", e) // 👈 加在这里
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        Toast.makeText(this, "正在连接打印机...", Toast.LENGTH_SHORT).show()

        // 👇 这里不再使用 if，而是等它回调
        BluetoothPrinter.connect(this, device) { isSuccess ->
            if (isSuccess) {
                // 真正连接成功才更新 UI
                connectedDeviceName = device.name ?: "未知设备"
                Toast.makeText(this, "已连接打印机", Toast.LENGTH_SHORT).show()
            } else {
                // 连接失败
                connectedDeviceName = null
                Toast.makeText(this, "连接失败，请检查打印机是否开机", Toast.LENGTH_SHORT).show()
            }
        }
    }

}

@Composable
fun AppRoot(connectedDeviceName: String?) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dafei_auth", Context.MODE_PRIVATE) }
    var currentUser by remember { mutableStateOf(prefs.getString("logged_in_user", null)) }

    if (currentUser == null) {
        LoginScreen { username ->
            prefs.edit { putString("logged_in_user", username) }
            currentUser = username
        }
    } else {
        // 👇 核心修复：严格按照 MainAppScreen 的 4 个参数顺序传参
        MainAppScreen(
            connectedDeviceName = connectedDeviceName,
            currentUser = currentUser!!,
            onLogout = {
                prefs.edit { remove("logged_in_user") }
                currentUser = null
            }
        )
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dafei_users", Context.MODE_PRIVATE) }
    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp)
            .imePadding(),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        Text(
            if (isLogin) "欢迎回来" else "注册新账号",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1565C0)
        )
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            username,
            { username = it },
            label = { Text("账号") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "不能为空", Toast.LENGTH_SHORT).show(); return@Button
                }
                if (isLogin) {
                    if (prefs.getString(username, null) == password) onLoginSuccess(username)
                    else Toast.makeText(context, "账号或密码错误", Toast.LENGTH_SHORT).show()
                } else {
                    if (prefs.contains(username)) Toast.makeText(
                        context,
                        "该账号已存在",
                        Toast.LENGTH_SHORT
                    ).show()
                    else {
                        prefs.edit { putString(username, password) }
                        Toast.makeText(context, "注册成功", Toast.LENGTH_SHORT).show()
                        isLogin = true
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) { Text(if (isLogin) "登录" else "注册", fontSize = 16.sp) }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { isLogin = !isLogin }) { Text(if (isLogin) "去注册" else "去登录") }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(connectedDeviceName: String?, currentUser: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val gson = remember { Gson() }
    val prefs = remember { context.getSharedPreferences("dafei_orders_$currentUser", Context.MODE_PRIVATE) }
    val historyOrders = remember {
        val json = prefs.getString("history_orders", null)
        if (json != null) {
            val type = object : TypeToken<List<CutOrderData>>() {}.type
            gson.fromJson<List<CutOrderData>>(json, type).toMutableStateList()
        } else mutableStateListOf()
    }
    fun saveHistory() { prefs.edit { putString("history_orders", gson.toJson(historyOrders)) } }

    var currentTab by remember { mutableIntStateOf(0) }
    var showBluetoothDialog by remember { mutableStateOf(false) }
    val pairedDevices = remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var editingOrder by remember { mutableStateOf<CutOrderData?>(null) }
    var isInDetailPage by remember { mutableStateOf(false) }

    // 👇 拦截物理返回键，退出编辑
    BackHandler(enabled = editingOrder != null) {
        editingOrder = null
        currentTab = 2
    }

    // 单条导出
    var orderToExportSingle by remember { mutableStateOf<CutOrderData?>(null) }
    val exportSingleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            orderToExportSingle?.let { order ->
                try {
                    val jsonStr = gson.toJson(listOf(order))
                    context.contentResolver.openOutputStream(it)?.use { os -> os.write(jsonStr.toByteArray()) }
                    Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) { Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show() }
            }
        }
        orderToExportSingle = null
    }

    // 多选导出
    var isExportMode by remember { mutableStateOf(false) }
    var selectedExportIds by remember { mutableStateOf(setOf<String>()) }
    var ordersToExportMulti by remember { mutableStateOf<List<CutOrderData>>(emptyList()) }
    val exportMultiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            try {
                val jsonStr = gson.toJson(ordersToExportMulti)
                context.contentResolver.openOutputStream(it)?.use { os -> os.write(jsonStr.toByteArray()) }
                Toast.makeText(context, "成功导出 ${ordersToExportMulti.size} 条", Toast.LENGTH_SHORT).show()
                isExportMode = false
                selectedExportIds = emptySet()
            } catch (e: Exception) { Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show() }
        }
    }

    // 导入
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonStr = inputStream.bufferedReader().use { it.readText() }
                    val type = object : TypeToken<List<CutOrderData>>() {}.type
                    val importedOrders: List<CutOrderData>? = gson.fromJson(jsonStr, type)

                    if (importedOrders != null) {
                        val existingIds = historyOrders.map { it.id }.toSet()
                        val newOrders = importedOrders.filter { it.id !in existingIds }

                        if (newOrders.isNotEmpty()) {
                            historyOrders.addAll(newOrders)
                            historyOrders.sortByDescending { it.time }
                            saveHistory()
                            Toast.makeText(context, "成功导入 ${newOrders.size} 条", Toast.LENGTH_SHORT).show()
                        } else Toast.makeText(context, "没有新数据", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) { Toast.makeText(context, "格式不正确", Toast.LENGTH_SHORT).show() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val logoBase64 = "你的Base64字符串" // 👈 把之前成功的Base64放这里
                        val logoBitmap = remember(logoBase64) { decodeBase64ToImage(logoBase64) }
                        if (logoBitmap != null) {
                            Image(bitmap = logoBitmap, contentDescription = "Logo", modifier = Modifier.size(32.dp).padding(end = 8.dp), contentScale = ContentScale.Fit)
                        }

                        if (isExportMode) Text("选择要导出的记录")
                        else if (currentTab == 0) Text(if (editingOrder != null) "编辑裁床单" else "打菲助手")
                        else if (currentTab == 1) Text("套排打菲")
                        else Text("历史裁床单")
                    }
                },
                actions = {
                    if (isExportMode) {
                        TextButton(onClick = { isExportMode = false; selectedExportIds = emptySet() }) { Text("取消", color = Color.White) }
                    } else if (currentTab == 0) {
                        if (editingOrder != null) {
                            TextButton(onClick = { editingOrder = null; currentTab = 2 }) { Text("取消编辑", color = Color.Yellow, fontWeight = FontWeight.Bold) }
                        } else {
                            if (connectedDeviceName != null) {
                                TextButton(onClick = { if (BluetoothPrinter.isConnected()) BluetoothPrinter.printTest() }) { Text("测试", color = Color.Yellow) }
                            }
                            TextButton(onClick = {
                                if (activity.getPairedDevices().isEmpty()) Toast.makeText(context, "请先配对蓝牙", Toast.LENGTH_SHORT).show()
                                else { pairedDevices.value = activity.getPairedDevices(); showBluetoothDialog = true }
                            }) { Text(text = connectedDeviceName ?: "未连接", color = Color.White) }
                            TextButton(onClick = onLogout) { Text("退出", color = Color.White) }
                        }
                    } else if (currentTab == 1) {
                        if (connectedDeviceName != null) {
                            TextButton(onClick = { if (BluetoothPrinter.isConnected()) BluetoothPrinter.printTest() }) { Text("测试", color = Color.Yellow) }
                        }
                        TextButton(onClick = {
                            if (activity.getPairedDevices().isEmpty()) Toast.makeText(context, "请先配对蓝牙", Toast.LENGTH_SHORT).show()
                            else { pairedDevices.value = activity.getPairedDevices(); showBluetoothDialog = true }
                        }) { Text(text = connectedDeviceName ?: "未连接", color = Color.White) }
                        TextButton(onClick = onLogout) { Text("退出", color = Color.White) }
                    } else if (currentTab == 2 && !isInDetailPage) {
                        TextButton(onClick = { isExportMode = true; selectedExportIds = emptySet() }) { Text("导出", color = Color.White) }
                        TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("导入", color = Color.White) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1565C0), titleContentColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    label = { Text("单排") },
                    icon = { Text("📝", fontSize = 20.sp) } // 用 Emoji 替代图标最省事
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    label = { Text("套排") },
                    icon = { Text("📑", fontSize = 20.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    label = { Text("历史") },
                    icon = { Text("📋", fontSize = 20.sp) }
                )
            }
//            NavigationBar {
//                NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, label = { Text("单排") }, icon = { Icon(Icons.Default.Home, "单排") })
//                NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, label = { Text("套排") }, icon = { Icon(Icons.Default.Menu, "套排") })
//                NavigationBarItem(selected = currentTab == 2, onClick = { currentTab = 2 }, label = { Text("历史") }, icon = { Icon(Icons.Default.List, "历史") })
//            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).imePadding()) {
            if (currentTab == 0) {
                GenerateScreen(
                    historyOrders = historyOrders,
                    editingOrder = editingOrder,
                    onGenerate = { newOrder ->
                        if (editingOrder != null) {
                            val oldIndex = historyOrders.indexOfFirst { it.id == editingOrder!!.id }
                            if (oldIndex != -1) historyOrders.removeAt(oldIndex)
                            editingOrder = null
                        }
                        historyOrders.add(0, newOrder); saveHistory(); currentTab = 2
                    },
                    onCancelEdit = { editingOrder = null; currentTab = 2 }
                )
            } else if (currentTab == 1) {
                CombinedGenerateScreen(historyOrders) { newOrder ->
                    historyOrders.add(0, newOrder); saveHistory(); currentTab = 2
                }
            } else {
                HistoryListScreen(
                    historyOrders = historyOrders,
                    isExportMode = isExportMode,
                    selectedExportIds = selectedExportIds,
                    onToggleSelection = { id -> selectedExportIds = if (selectedExportIds.contains(id)) selectedExportIds - id else selectedExportIds + id },
                    onDetailStatusChanged = { isInDetail -> isInDetailPage = isInDetail },
                    onDelete = { index -> historyOrders.removeAt(index); saveHistory(); Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show() },
                    onEdit = { order -> editingOrder = order; currentTab = 0 },
                    onExportSingle = { order ->
                        orderToExportSingle = order
                        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA).format(Date())
                        exportSingleLauncher.launch("裁床单_${order.styleNo}_${order.bedNo}_$dateStr.json")
                    }
                )
            }
        }

        // 蓝牙弹窗
        if (showBluetoothDialog) {
            AlertDialog(
                onDismissRequest = { showBluetoothDialog = false },
                title = { Text("选择打印机") },
                text = { Column { pairedDevices.value.forEach { device -> Text(device.name ?: "未知", modifier = Modifier.fillMaxWidth().clickable { activity.connectToDevice(device); showBluetoothDialog = false }.padding(vertical = 12.dp)) } } },
                confirmButton = { TextButton(onClick = { showBluetoothDialog = false }) { Text("取消") } }
            )
        }
    }
}

@SuppressLint("UseKtx", "MutableCollectionMutableState", "DefaultLocale")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenerateScreen(
    historyOrders: List<CutOrderData>,
    editingOrder: CutOrderData? = null,
    onGenerate: (CutOrderData) -> Unit,
    onCancelEdit: () -> Unit = {}
) {
    var enableAutoCopy by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember { context.getSharedPreferences("dafei_prefs", Context.MODE_PRIVATE) }

    // --- 基础信息 ---
    var customer by remember(editingOrder) { mutableStateOf(editingOrder?.customer ?: "") }
    var styleNo by remember(editingOrder) { mutableStateOf(editingOrder?.styleNo ?: "") }
    var dateStr by remember(editingOrder) { mutableStateOf(editingOrder?.dateStr ?: SimpleDateFormat("yy-MM-dd", Locale.CHINA).format(Date())) }
    var bedNo by remember(editingOrder) { mutableStateOf(editingOrder?.bedNo ?: "") }

    // 👇 1. 款号联想下拉状态
    var isStyleNoFocused by remember { mutableStateOf(false) }
    val matchedHistory by remember(styleNo, historyOrders) {
        derivedStateOf {
            if (styleNo.isBlank()) emptyList()
            else historyOrders.filter { it.styleNo.contains(styleNo, ignoreCase = true) }
                .distinctBy { it.styleNo }
                .sortedByDescending { it.time }
                .take(5)
        }
    }

    // 👇 2. 多图状态 (解析老数据兼容单图)
    val imageList = remember(editingOrder) {
        val list = mutableStateListOf<String>()
        val oldData = editingOrder?.imageBase64
        if (oldData != null) {
            try {
                if (oldData.startsWith("[")) {
                    list.addAll(Gson().fromJson(oldData, Array<String>::class.java).toList())
                } else {
                    list.add(oldData)
                }
            } catch (e: Exception) { list.add(oldData) }
        }
        list
    }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var continuousBundle by remember(editingOrder) { mutableStateOf(false) }

    // --- 图片选择相关 ---
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val remaining = 5 - imageList.size
            if (remaining <= 0) {
                Toast.makeText(context, "最多只能上传 5 张图片", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val toAdd = uris.take(remaining)
            if (uris.size > remaining) Toast.makeText(context, "最多只能上传 5 张，已保留前 $remaining 张", Toast.LENGTH_SHORT).show()

            toAdd.forEach { uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes(); inputStream?.close()
                if (bytes != null) imageList.add(Base64.encodeToString(compressImage(bytes), Base64.DEFAULT))
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { uri ->
            if (imageList.size >= 5) {
                Toast.makeText(context, "最多只能上传 5 张图片", Toast.LENGTH_SHORT).show()
                return@let
            }
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes(); inputStream?.close()
            if (bytes != null) imageList.add(Base64.encodeToString(compressImage(bytes), Base64.DEFAULT))
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val file = File(context.cacheDir, "temp_photo.jpg")
            cameraUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(cameraUri!!)
        } else Toast.makeText(context, "需要相机权限", Toast.LENGTH_SHORT).show()
    }

    var showImageSourceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(dateStr, historyOrders) {
        if (editingOrder == null && bedNo.isEmpty()) {
            val currentMonthPrefix = dateStr.take(5)
            val monthOrders = historyOrders.filter { it.dateStr.startsWith(currentMonthPrefix) }
            val maxBedNo = monthOrders.mapNotNull { it.bedNo.toIntOrNull() }.maxOrNull() ?: 0
            bedNo = (maxBedNo + 1).toString()
        }
    }

    val presetSizes = remember { mutableStateOf(prefs.getStringSet("sizes", setOf("S", "M", "L", "XL"))?.toMutableList() ?: mutableListOf()) }
    val presetColors = remember { mutableStateOf(prefs.getStringSet("colors", setOf("黑色", "白色", "杏色"))?.toMutableList() ?: mutableListOf()) }

    val selectedSizes = remember(editingOrder) { mutableStateOf(editingOrder?.sizesOrder ?: listOf<String>()) }
    val selectedColorsOrder = remember(editingOrder) {
        val list = mutableStateListOf<String>()
        editingOrder?.rawRows?.forEach { if (!list.contains(it.color)) list.add(it.color) }
        list
    }
    val rowList = remember(editingOrder) {
        val list = mutableStateListOf<RowInputData>()
        editingOrder?.rawRows?.forEach { list.add(it.copy(quantities = it.quantities.toMutableMap())) }
        list
    }

    var showSelectSizeDialog by remember { mutableStateOf(false) }
    var tempSelectedSizes by remember { mutableStateOf(listOf<String>()) }
    var showSelectColorDialog by remember { mutableStateOf(false) }
    var tempSelectedColors by remember { mutableStateOf(setOf<String>()) }
    var newColorInput by remember { mutableStateOf("") }
    var newSizeText by remember { mutableStateOf("") }

    // 👇 核心：锁定字体大小，不受系统缩放影响
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()).imePadding()) {

            // 👇 1. 款号输入框 (带联想下拉)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = styleNo,
                        onValueChange = { styleNo = it },
                        label = { Text("款号") },
                        modifier = Modifier.fillMaxWidth().onFocusChanged { focusState -> isStyleNoFocused = focusState.isFocused },
                        singleLine = true
                    )
                    if (isStyleNoFocused && matchedHistory.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 56.dp).zIndex(10f),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(matchedHistory) { order ->
                                    Column(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            styleNo = order.styleNo
                                            customer = order.customer
                                            isStyleNoFocused = false
                                            focusManager.clearFocus()
                                        }.padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(text = order.styleNo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (order.customer.isNotBlank()) Text(text = "客户: ${order.customer}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(bedNo, { bedNo = it }, label = { Text("床号") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(customer, { customer = it }, label = { Text("客户") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(dateStr, { dateStr = it }, label = { Text("日期") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { continuousBundle = !continuousBundle }) {
                    Checkbox(checked = continuousBundle, onCheckedChange = { continuousBundle = it })
                    Text("连床编菲", fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 👇 2. 多图上传展示区
            if (imageList.isNotEmpty() || imageList.size < 5) {
                LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(imageList.size) { index ->
                        Box(modifier = Modifier.size(80.dp)) {
                            decodeBase64ToBitmap(imageList[index])?.let {
                                Image(it.asImageBitmap(), "图片", Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
                            }
                            IconButton(
                                onClick = { imageList.removeAt(index) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) { Text("X", color = Color.White, fontSize = 10.sp) }
                        }
                    }
                    if (imageList.size < 5) {
                        item {
                            Box(
                                modifier = Modifier.size(80.dp).border(1.dp, Color.Gray, MaterialTheme.shapes.medium).clickable { showImageSourceDialog = true },
                                contentAlignment = Alignment.Center
                            ) { Text("+ 添加\n(${imageList.size}/5)", textAlign = TextAlign.Center, fontSize = 12.sp, color = Color.Gray) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 👇 3. 第一行：标题 + 行复制开关
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("颜色 / 件数", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { enableAutoCopy = !enableAutoCopy }) {
                    Text("行复制", fontSize = 12.sp, color = Color.DarkGray)
                    Spacer(Modifier.width(2.dp))
                    androidx.compose.material3.Switch(checked = enableAutoCopy, onCheckedChange = { enableAutoCopy = it }, modifier = Modifier.scale(0.7f))
                }
            }

            // 👇 4. 第二行：靠右的按钮
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { tempSelectedColors = selectedColorsOrder.toSet(); showSelectColorDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp)
                ) { Text("+ 颜色", fontSize = 12.sp) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { tempSelectedSizes = selectedSizes.value.toList(); showSelectSizeDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp)
                ) { Text("+ 码数", fontSize = 12.sp) }
            }

            Spacer(Modifier.height(8.dp))
            if (selectedColorsOrder.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedColorsOrder.forEach { color -> AssistChip({ rowList.add(RowInputData(color)) }, { Text("+ $color", fontSize = 12.sp) }) }
                }
            }

            // 表头
            Row(Modifier.fillMaxWidth().background(Color(0xFFF0F0F0)).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("颜色", modifier = Modifier.weight(1.5f).padding(start = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                selectedSizes.value.forEach { size -> Text(size, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(36.dp))
            }

            // 👇 5. 表格内容 (橙色边框、矮高度、清空修复)
            rowList.forEachIndexed { index, row ->
                key(index) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
//                        Box(
//                            modifier = Modifier.weight(1.5f).height(36.dp).border(1.dp, Color(0xFFFF9800), MaterialTheme.shapes.extraSmall),
//                            contentAlignment = Alignment.Center
//                        ) { Text(
//                            row.color,
//                            fontSize = 14.sp,
//                            textAlign = TextAlign.Center,
//                            color = Color.Black
//                        )
//                        }

                        AutoFitWrappedText(
                            text = row.color,
                            modifier = Modifier
                                .weight(1.5f)
                                .heightIn(min = 36.dp)   // ← 关键：min 而不是固定 height
                                .border(1.dp, Color(0xFFFF9800), MaterialTheme.shapes.extraSmall),
                            maxFontSize = 14.sp,
                            minFontSize = 10.sp,
                            maxPreferredLines = 3
                        )

                        selectedSizes.value.forEachIndexed { colIndex, size ->
                            val currentKey = "$size-$colIndex"
                            BasicTextField(
                                value = row.quantities[currentKey] ?: "",
                                onValueChange = { newVal ->
                                    if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                                        val newMap = row.quantities.toMutableMap()
                                        newMap[currentKey] = newVal
                                        // 👇 修复：去掉了 newVal.isNotEmpty() 的限制
                                        if (enableAutoCopy) {
                                            for (i in colIndex + 1 until selectedSizes.value.size) {
                                                newMap["${selectedSizes.value[i]}-$i"] = newVal
                                            }
                                        }
                                        rowList[index] = row.copy(quantities = newMap)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, color = Color.Black),
                                modifier = Modifier.weight(1.1f).padding(horizontal = 2.dp).height(36.dp).border(1.dp, Color(0xFFFF9800), MaterialTheme.shapes.extraSmall),
                                decorationBox = { innerTextField -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { innerTextField() } }
                            )
                        }
                        IconButton({ rowList.removeAt(index) }, modifier = Modifier.width(36.dp)) { Text("X", color = Color.Red, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 👇 6. 底部：取消与保存按钮
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editingOrder != null) {
                    OutlinedButton(
                        onClick = onCancelEdit,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                    ) { Text("取消编辑", fontSize = 16.sp) }
                }

                Button(
                    onClick = {
                        if (styleNo.isBlank()) { Toast.makeText(context, "款号不能为空", Toast.LENGTH_SHORT).show(); return@Button }
                        if (bedNo.isBlank()) { Toast.makeText(context, "床号不能为空", Toast.LENGTH_SHORT).show(); return@Button }
                        if (selectedSizes.value.isEmpty()) { Toast.makeText(context, "请先选择码数", Toast.LENGTH_SHORT).show(); return@Button }

                        val cutId = editingOrder?.id ?: ("C" + System.currentTimeMillis())
                        val newTickets = mutableListOf<TicketData>()
                        var totalQty = 0
                        var globalBundleNo = 1

                        if (continuousBundle && editingOrder == null) {
                            val currentMonthPrefix = dateStr.take(5)
                            val prevOrder = historyOrders.filter { it.styleNo == styleNo && it.dateStr.startsWith(currentMonthPrefix) }.maxByOrNull { it.time }
                            if (prevOrder != null) globalBundleNo = (prevOrder.tickets.maxOfOrNull { it.bundleNo } ?: 0) + 1
                        }

                        val rowBundles = mutableMapOf<Int, MutableMap<Int, Int>>()
                        selectedSizes.value.forEachIndexed { colIndex, size ->
                            rowList.forEachIndexed { rowIdx, row ->
                                val qty = row.quantities["$size-$colIndex"]?.toIntOrNull() ?: 0
                                if (row.color.isNotBlank() && qty > 0) {
                                    rowBundles.getOrPut(rowIdx) { mutableMapOf() }[colIndex] = globalBundleNo
                                    val no = "$styleNo-$size-" + String.format("%02d", globalBundleNo)
                                    newTickets.add(TicketData(no, cutId, customer, styleNo, bedNo, dateStr, row.color, size, qty, globalBundleNo))
                                    totalQty += qty
                                    globalBundleNo++
                                }
                            }
                        }

                        if (newTickets.isNotEmpty()) {
                            val finalTickets = newTickets.map { it.copy(totalBundles = newTickets.size, totalQuantity = totalQty) }
                            val finalRawRows = rowList.map { it.copy(quantities = it.quantities.toMutableMap()) }
                            val time = editingOrder?.time ?: System.currentTimeMillis()
                            val finalImageBase64 = if (imageList.isEmpty()) null else Gson().toJson(imageList)

                            onGenerate(CutOrderData(cutId, customer, styleNo, bedNo, dateStr, time, totalQty, finalTickets, selectedSizes.value, finalRawRows, rowBundles, finalImageBase64))
                            Toast.makeText(context, if (editingOrder != null) "已修改保存" else "已保存：${finalTickets.size}张", Toast.LENGTH_SHORT).show()

                            if (editingOrder == null) {
                                rowList.clear(); styleNo = ""; customer = ""; imageList.clear(); selectedColorsOrder.clear(); selectedSizes.value = emptyList()
                            }
                        } else Toast.makeText(context, "没有有效件数", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(if (editingOrder != null) 2f else 1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) { Text(if (editingOrder != null) "保存修改" else "生成并保存裁床单", fontSize = 16.sp) }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // ===== 弹窗部分 =====
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("选择图片") },
            text = {
                Column {
                    Text("拍照", modifier = Modifier.fillMaxWidth().clickable { showImageSourceDialog = false; cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }.padding(12.dp), fontSize = 16.sp)
                    HorizontalDivider()
                    Text("相册", modifier = Modifier.fillMaxWidth().clickable { showImageSourceDialog = false; galleryLauncher.launch("image/*") }.padding(12.dp), fontSize = 16.sp)
                }
            },
            confirmButton = { TextButton({ showImageSourceDialog = false }) { Text("取消") } }
        )
    }

    // 码数弹窗 (支持重复流式)
    if (showSelectSizeDialog) {
        AlertDialog(
            onDismissRequest = { showSelectSizeDialog = false },
            title = { Text("选择码数") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), MaterialTheme.shapes.small).padding(8.dp)) {
                        Text("已选码数（支持重复）：", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        val pickedSorted = tempSelectedSizes.sortedBy { SizeOrder.rank(it) }
                        if (pickedSorted.isEmpty()) {
                            Text("暂未选择", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                pickedSorted.forEach { s ->
                                    Row(Modifier.background(Color.White, MaterialTheme.shapes.small).border(1.dp, Color.Gray, MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(s, fontSize = 14.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Text("x", color = Color.Red, modifier = Modifier.clickable { tempSelectedSizes = tempSelectedSizes - s })
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SizeOrder.sorted(presetSizes.value).forEach { size ->
                            AssistChip(onClick = { tempSelectedSizes = tempSelectedSizes + size }, label = { Text(size, fontSize = 14.sp) })
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(newSizeText, { newSizeText = it.uppercase() }, modifier = Modifier.weight(1f), placeholder = { Text("新码数") }, singleLine = true)
                        OutlinedButton(
                            onClick = {
                                if (newSizeText.isNotBlank()) {
                                    val trimmed = newSizeText.trim()
                                    if (!presetSizes.value.contains(trimmed)) {
                                        presetSizes.value = (presetSizes.value + trimmed).toMutableList()
                                        prefs.edit().putStringSet("sizes", presetSizes.value.toSet()).apply()
                                    }
                                    tempSelectedSizes = tempSelectedSizes + trimmed
                                    newSizeText = ""
                                }
                            }, modifier = Modifier.padding(start = 8.dp)
                        ) { Text("添加") }
                    }
                }
            },
            confirmButton = { Button(onClick = { selectedSizes.value = tempSelectedSizes; showSelectSizeDialog = false }) { Text("确定") } },
            dismissButton = { TextButton({ showSelectSizeDialog = false }) { Text("取消") } }
        )
    }

    // 颜色弹窗 (流式)
    if (showSelectColorDialog) {
        AlertDialog(
            onDismissRequest = { showSelectColorDialog = false },
            title = { Text("选择颜色") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        presetColors.value.forEach { color ->
                            val selected = tempSelectedColors.contains(color)
                            FilterChip(selected = selected, onClick = { tempSelectedColors = if (selected) tempSelectedColors - color else tempSelectedColors + color }, label = { Text(color, fontSize = 14.sp) })
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(newColorInput, { newColorInput = it }, modifier = Modifier.weight(1f), placeholder = { Text("新颜色") }, singleLine = true)
                        OutlinedButton(
                            onClick = {
                                if (newColorInput.isNotBlank()) {
                                    val trimmed = newColorInput.trim()
                                    if (!presetColors.value.contains(trimmed)) {
                                        presetColors.value = (presetColors.value + trimmed).toMutableList()
                                        prefs.edit().putStringSet("colors", presetColors.value.toSet()).apply()
                                    }
                                    tempSelectedColors = tempSelectedColors + trimmed
                                    newColorInput = ""
                                }
                            }, modifier = Modifier.padding(start = 8.dp)
                        ) { Text("添加") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    tempSelectedColors.forEach { color -> if (!selectedColorsOrder.contains(color)) selectedColorsOrder.add(color) }
                    showSelectColorDialog = false
                }) { Text("确定加入") }
            },
            dismissButton = { TextButton({ showSelectColorDialog = false }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CombinedGenerateScreen(historyOrders: List<CutOrderData>, onGenerate: (CutOrderData) -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember { context.getSharedPreferences("dafei_prefs", Context.MODE_PRIVATE) }

    // 基础信息
    var customer by remember { mutableStateOf("") }
    var bedNo by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(SimpleDateFormat("yy-MM-dd", Locale.CHINA).format(Date())) }
    var continuousBundle by remember { mutableStateOf(false) }

    // 👇 1. 多图状态
    val imageList = remember { mutableStateListOf<String>() }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // 图片选择器
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val remaining = 5 - imageList.size
            if (remaining <= 0) {
                Toast.makeText(context, "最多只能上传 5 张图片", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val toAdd = uris.take(remaining)
            if (uris.size > remaining) Toast.makeText(context, "最多只能上传 5 张，已保留前 $remaining 张", Toast.LENGTH_SHORT).show()

            toAdd.forEach { uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes(); inputStream?.close()
                if (bytes != null) imageList.add(Base64.encodeToString(compressImage(bytes), Base64.DEFAULT))
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { uri ->
            if (imageList.size >= 5) {
                Toast.makeText(context, "最多只能上传 5 张图片", Toast.LENGTH_SHORT).show()
                return@let
            }
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes(); inputStream?.close()
            if (bytes != null) imageList.add(Base64.encodeToString(compressImage(bytes), Base64.DEFAULT))
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val file = File(context.cacheDir, "temp_photo_combined.jpg")
            cameraUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(cameraUri!!)
        } else Toast.makeText(context, "需要相机权限", Toast.LENGTH_SHORT).show()
    }

    // 👇 2. 套排款号及解析
    var topStyleNoText by remember { mutableStateOf("") }
    val parsedStyleNos = remember(topStyleNoText) {
        topStyleNoText.split(",", "，").map { it.trim() }.filter { it.isNotBlank() }
    }

    var enableAutoCopy by remember { mutableStateOf(true) }

    // 预设颜色、码数
    val presetSizes = remember { mutableStateOf(prefs.getStringSet("sizes", setOf("S", "M", "L", "XL"))?.toMutableList() ?: mutableListOf()) }
    val presetColors = remember { mutableStateOf(prefs.getStringSet("colors", setOf("黑色", "白色", "杏色"))?.toMutableList() ?: mutableListOf()) }
    val selectedSizes = remember { mutableStateOf(listOf<String>()) }

    val rowList = remember { mutableStateListOf<CombinedRowInputData>() }
    val selectedColorsOrder = remember { mutableStateListOf<String>() }

    // 床号自动递增
    LaunchedEffect(dateStr, historyOrders) {
        if (bedNo.isEmpty()) {
            val currentMonthPrefix = dateStr.take(5)
            val monthOrders = historyOrders.filter { it.dateStr.startsWith(currentMonthPrefix) }
            val maxBedNo = monthOrders.mapNotNull { it.bedNo.toIntOrNull() }.maxOrNull() ?: 0
            bedNo = (maxBedNo + 1).toString()
        }
    }

    // 弹窗状态
    var showSelectSizeDialog by remember { mutableStateOf(false) }
    var tempSelectedSizes by remember { mutableStateOf(listOf<String>()) }
    var newSizeText by remember { mutableStateOf("") }

    var showSelectColorDialog by remember { mutableStateOf(false) }
    var tempSelectedColors by remember { mutableStateOf(setOf<String>()) }
    var newColorInput by remember { mutableStateOf("") }

    // 👇 锁定字体大小
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()).imePadding()) {

            // 1. 套排款号输入框
            OutlinedTextField(
                value = topStyleNoText,
                onValueChange = { topStyleNoText = it },
                label = { Text("套排款号 (支持逗号分割)") },
                placeholder = { Text("例如: A款, B款, C款") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            // 2. 客户和床号
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(customer, { customer = it }, label = { Text("客户") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(bedNo, { bedNo = it }, label = { Text("床号") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Spacer(Modifier.height(8.dp))

            // 3. 日期
            OutlinedTextField(dateStr, { dateStr = it }, label = { Text("日期") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            // 4. 连床编菲
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { continuousBundle = !continuousBundle }) {
                    Checkbox(checked = continuousBundle, onCheckedChange = { continuousBundle = it })
                    Text("连床编菲", fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(8.dp))

            // 5. 多图展示区
            if (imageList.isNotEmpty() || imageList.size < 5) {
                LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(imageList.size) { index ->
                        Box(modifier = Modifier.size(80.dp)) {
                            decodeBase64ToBitmap(imageList[index])?.let {
                                Image(it.asImageBitmap(), "图片", Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
                            }
                            IconButton(
                                onClick = { imageList.removeAt(index) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) { Text("X", color = Color.White, fontSize = 10.sp) }
                        }
                    }
                    if (imageList.size < 5) {
                        item {
                            Box(
                                modifier = Modifier.size(80.dp).border(1.dp, Color.Gray, MaterialTheme.shapes.medium).clickable { showImageSourceDialog = true },
                                contentAlignment = Alignment.Center
                            ) { Text("+ 添加\n(${imageList.size}/5)", textAlign = TextAlign.Center, fontSize = 12.sp, color = Color.Gray) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // 6. 第一行：标题 + 行复制
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("套排明细 (款号+颜色)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { enableAutoCopy = !enableAutoCopy }) {
                    Text("行复制", fontSize = 12.sp, color = Color.DarkGray)
                    Spacer(Modifier.width(2.dp))
                    androidx.compose.material3.Switch(checked = enableAutoCopy, onCheckedChange = { enableAutoCopy = it }, modifier = Modifier.scale(0.7f))
                }
            }

            // 7. 第二行：靠右的按钮
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { tempSelectedColors = emptySet(); showSelectColorDialog = true },
                    modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("+ 颜色", fontSize = 12.sp) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { tempSelectedSizes = selectedSizes.value; showSelectSizeDialog = true },
                    modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) { Text("+ 码数", fontSize = 12.sp) }
            }

            Spacer(Modifier.height(8.dp))

            // 已选颜色标签栏
            if (selectedColorsOrder.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedColorsOrder.forEach { color ->
                        AssistChip(onClick = { rowList.add(CombinedRowInputData(styleNo = "", color = color)) }, label = { Text("+ $color", fontSize = 12.sp) })
                    }
                }
            }

            // 表头
            if (selectedSizes.value.isNotEmpty() || rowList.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF0F0F0)).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("款号", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("颜色", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    selectedSizes.value.forEach { size -> Text(size, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(36.dp))
                }
            }

            // 表格内容
            rowList.forEachIndexed { index, row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    var expanded by remember { mutableStateOf(false) }

                    // 款号输入框（带下拉）
                    Box(modifier = Modifier.weight(1.2f)) {
                        var expanded by remember { mutableStateOf(false) }

                        AutoFitWrappedBasicTextField(
                            value = row.styleNo,
                            onValueChange = { newVal -> rowList[index] = row.copy(styleNo = newVal) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 36.dp)   // ← 关键
                                .border(1.dp, Color(0xFFFF9800), MaterialTheme.shapes.extraSmall),
                            maxFontSize = 14.sp,
                            minFontSize = 10.sp,
                            maxPreferredLines = 3,
                            onFocusChanged = { focusState ->
                                if (focusState.isFocused && parsedStyleNos.isNotEmpty()) expanded = true
                            }
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            parsedStyleNos.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style, fontSize = 14.sp) },
                                    onClick = {
                                        rowList[index] = row.copy(styleNo = style)
                                        expanded = false
                                        focusManager.clearFocus()
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
//                    Box(modifier = Modifier.weight(1.2f)) {
//                        BasicTextField(
//                            value = row.styleNo,
//                            onValueChange = { newVal -> rowList[index] = row.copy(styleNo = newVal) },
//                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 14.sp, color = Color.Black),
//                            singleLine = true,
//                            modifier = Modifier.fillMaxWidth().height(36.dp).border(1.dp, Color(0xFFFF9800), MaterialTheme.shapes.extraSmall).onFocusChanged { focusState ->
//                                if (focusState.isFocused && parsedStyleNos.isNotEmpty()) expanded = true
//                            },
//                            decorationBox = { innerTextField -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { innerTextField() } }
//                        )
//                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White)) {
//                            parsedStyleNos.forEach { style ->
//                                DropdownMenuItem(
//                                    text = { Text(style, fontSize = 14.sp) },
//                                    onClick = { rowList[index] = row.copy(styleNo = style); expanded = false; focusManager.clearFocus() }
//                                )
//                            }
//                        }
//                    }
//                    Spacer(Modifier.width(4.dp))

                    // 颜色输入框
                    AutoFitWrappedBasicTextField(
                        value = row.color,
                        onValueChange = { newVal -> rowList[index] = row.copy(color = newVal) },
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 36.dp)   // ← 关键
                            .border(1.dp, Color(0xFFFF9800), MaterialTheme.shapes.extraSmall),
                        maxFontSize = 14.sp,
                        minFontSize = 10.sp,
                        maxPreferredLines = 3
                    )
//                    BasicTextField(
//                        value = row.color,
//                        onValueChange = { newVal -> rowList[index] = row.copy(color = newVal) },
//                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 14.sp, color = Color.Black),
//                        singleLine = true,
//                        modifier = Modifier.weight(1.2f).height(36.dp).border(1.dp, Color(0xFFFF9800), MaterialTheme.shapes.extraSmall),
//                        decorationBox = { innerTextField -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { innerTextField() } }
//                    )

                    // 各码数件数
                    selectedSizes.value.forEachIndexed { colIndex, size ->
                        val currentKey = "$size-$colIndex"
                        BasicTextField(
                            value = row.quantities[currentKey] ?: "",
                            onValueChange = { newVal ->
                                if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                                    val newMap = row.quantities.toMutableMap()
                                    newMap[currentKey] = newVal
                                    if (enableAutoCopy) {
                                        for (i in colIndex + 1 until selectedSizes.value.size) {
                                            newMap["${selectedSizes.value[i]}-$i"] = newVal
                                        }
                                    }
                                    rowList[index] = row.copy(quantities = newMap)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, color = Color.Black),
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp).height(36.dp).border(1.dp, Color(0xFFFF9800), MaterialTheme.shapes.extraSmall),
                            decorationBox = { innerTextField -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { innerTextField() } }
                        )
                    }
                    IconButton({ rowList.removeAt(index) }, modifier = Modifier.width(36.dp)) { Text("X", color = Color.Red, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 生成按钮
            Button(
                onClick = {
                    if (bedNo.isBlank()) { Toast.makeText(context, "床号不能为空", Toast.LENGTH_SHORT).show(); return@Button }
                    if (selectedSizes.value.isEmpty()) { Toast.makeText(context, "请先选择码数", Toast.LENGTH_SHORT).show(); return@Button }
                    if (rowList.isEmpty()) { Toast.makeText(context, "请添加明细行", Toast.LENGTH_SHORT).show(); return@Button }

                    val cutId = "C" + System.currentTimeMillis()
                    val newTickets = mutableListOf<TicketData>()
                    var totalQty = 0
                    var globalBundleNo = 1
                    if (continuousBundle) {
                        val currentMonthPrefix = dateStr.take(5)
                        val targetStyleNos = rowList.map { it.styleNo }.filter { it.isNotBlank() }
                        val prevOrders = historyOrders.filter { order ->
                            order.dateStr.startsWith(currentMonthPrefix) && targetStyleNos.any { style -> order.styleNo.contains(style) }
                        }
                        if (prevOrders.isNotEmpty()) {
                            val maxBundle = prevOrders.maxOfOrNull { it.tickets.maxOfOrNull { t -> t.bundleNo } ?: 0 } ?: 0
                            globalBundleNo = maxBundle + 1
                        }
                    }

                    val rowBundles = mutableMapOf<Int, MutableMap<Int, Int>>()
                    selectedSizes.value.forEachIndexed { colIndex, size ->
                        rowList.forEachIndexed { rowIdx, row ->
                            val qty = row.quantities["$size-$colIndex"]?.toIntOrNull() ?: 0
                            if (row.color.isNotBlank() && row.styleNo.isNotBlank() && qty > 0) {
                                rowBundles.getOrPut(rowIdx) { mutableMapOf() }[colIndex] = globalBundleNo
                                val no = "${row.styleNo}-$size-" + String.format("%02d", globalBundleNo)
                                newTickets.add(TicketData(no, cutId, customer, row.styleNo, bedNo, dateStr, row.color, size, qty, globalBundleNo))
                                totalQty += qty
                                globalBundleNo++
                            }
                        }
                    }

                    if (newTickets.isNotEmpty()) {
                        val finalTickets = newTickets.map { it.copy(totalBundles = newTickets.size, totalQuantity = totalQty) }
                        val finalRawRows = rowList.map { RowInputData(color = "${it.styleNo}-${it.color}", quantities = it.quantities.toMutableMap()) }
                        val combinedStyleNo = rowList.map { it.styleNo }.filter { it.isNotBlank() }.distinct().joinToString(",")
                        val finalImageBase64 = if (imageList.isEmpty()) null else Gson().toJson(imageList)

                        onGenerate(CutOrderData(cutId, customer, combinedStyleNo, bedNo, dateStr, System.currentTimeMillis(), totalQty, finalTickets, selectedSizes.value, finalRawRows, rowBundles, finalImageBase64))
                        Toast.makeText(context, "已保存：${finalTickets.size}张", Toast.LENGTH_SHORT).show()

                        rowList.clear(); selectedColorsOrder.clear(); continuousBundle = false; imageList.clear(); topStyleNoText = ""
                    } else {
                        Toast.makeText(context, "没有有效件数（请检查款号、颜色、件数）", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) { Text("生成套排裁床单", fontSize = 16.sp) }
        }
    }

    // ===== 弹窗部分 =====
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("选择图片") },
            text = {
                Column {
                    Text("拍照", modifier = Modifier.fillMaxWidth().clickable { showImageSourceDialog = false; cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }.padding(12.dp), fontSize = 16.sp)
                    HorizontalDivider()
                    Text("相册", modifier = Modifier.fillMaxWidth().clickable { showImageSourceDialog = false; galleryLauncher.launch("image/*") }.padding(12.dp), fontSize = 16.sp)
                }
            },
            confirmButton = { TextButton({ showImageSourceDialog = false }) { Text("取消") } }
        )
    }

    if (showSelectColorDialog) {
        AlertDialog(
            onDismissRequest = { showSelectColorDialog = false },
            title = { Text("选择颜色") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        presetColors.value.forEach { color ->
                            val selected = tempSelectedColors.contains(color)
                            FilterChip(selected = selected, onClick = { tempSelectedColors = if (selected) tempSelectedColors - color else tempSelectedColors + color }, label = { Text(color, fontSize = 14.sp) })
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(newColorInput, { newColorInput = it }, modifier = Modifier.weight(1f), placeholder = { Text("新颜色") }, singleLine = true)
                        OutlinedButton(
                            onClick = {
                                if (newColorInput.isNotBlank()) {
                                    val trimmed = newColorInput.trim()
                                    if (!presetColors.value.contains(trimmed)) {
                                        presetColors.value = (presetColors.value + trimmed).toMutableList()
                                        prefs.edit().putStringSet("colors", presetColors.value.toSet()).apply()
                                    }
                                    tempSelectedColors = tempSelectedColors + trimmed
                                    newColorInput = ""
                                }
                            }, modifier = Modifier.padding(start = 8.dp)
                        ) { Text("添加") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    tempSelectedColors.forEach { color -> if (!selectedColorsOrder.contains(color)) selectedColorsOrder.add(color) }
                    showSelectColorDialog = false
                }) { Text("确定加入") }
            },
            dismissButton = { TextButton({ showSelectColorDialog = false }) { Text("取消") } }
        )
    }

    if (showSelectSizeDialog) {
        AlertDialog(
            onDismissRequest = { showSelectSizeDialog = false },
            title = { Text("选择码数") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), MaterialTheme.shapes.small).padding(8.dp)) {
                        Text("已选码数（支持重复）：", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        val pickedSorted = tempSelectedSizes.sortedBy { SizeOrder.rank(it) }
                        if (pickedSorted.isEmpty()) {
                            Text("暂未选择", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                pickedSorted.forEach { s ->
                                    Row(Modifier.background(Color.White, MaterialTheme.shapes.small).border(1.dp, Color.Gray, MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(s, fontSize = 14.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Text("x", color = Color.Red, modifier = Modifier.clickable { tempSelectedSizes = tempSelectedSizes - s })
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SizeOrder.sorted(presetSizes.value).forEach { size ->
                            AssistChip(onClick = { tempSelectedSizes = tempSelectedSizes + size }, label = { Text(size, fontSize = 14.sp) })
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(newSizeText, { newSizeText = it.uppercase() }, modifier = Modifier.weight(1f), placeholder = { Text("新码数") }, singleLine = true)
                        OutlinedButton(
                            onClick = {
                                if (newSizeText.isNotBlank()) {
                                    val trimmed = newSizeText.trim()
                                    if (!presetSizes.value.contains(trimmed)) {
                                        presetSizes.value = (presetSizes.value + trimmed).toMutableList()
                                        prefs.edit().putStringSet("sizes", presetSizes.value.toSet()).apply()
                                    }
                                    tempSelectedSizes = tempSelectedSizes + trimmed
                                    newSizeText = ""
                                }
                            }, modifier = Modifier.padding(start = 8.dp)
                        ) { Text("添加") }
                    }
                }
            },
            confirmButton = { Button(onClick = { selectedSizes.value = tempSelectedSizes; showSelectSizeDialog = false }) { Text("确定") } },
            dismissButton = { TextButton({ showSelectSizeDialog = false }) { Text("取消") } }
        )
    }
}

// 👇 确保这个数据类在你的代码最外层
data class CombinedRowInputData(
    val styleNo: String = "",
    val color: String = "",
    val quantities: MutableMap<String, String> = mutableMapOf()
)

@Composable//裁床单列表页面
fun HistoryListScreen(
    historyOrders: List<CutOrderData>,
    isExportMode: Boolean,
    selectedExportIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    onDelete: (Int) -> Unit,
    onEdit: (CutOrderData) -> Unit,
    onExportSingle: (CutOrderData) -> Unit,
    onDetailStatusChanged: (Boolean) -> Unit
) {
    if (historyOrders.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("暂无历史", color = Color.Gray) }
        return
    }

    var selectedOrder by remember { mutableStateOf<CutOrderData?>(null) }
    var orderToDelete by remember { mutableStateOf<Pair<Int, CutOrderData>?>(null) }

    LaunchedEffect(selectedOrder) {
        onDetailStatusChanged(selectedOrder != null)
    }

    BackHandler(enabled = selectedOrder != null) {
        selectedOrder = null
    }

    if (selectedOrder != null && !isExportMode) {
        val currentOrder = historyOrders.find { it.id == selectedOrder!!.id } ?: selectedOrder!!

        DetailScreen(
            order = currentOrder,
            onBack = { selectedOrder = null },
            onEdit = { onEdit(currentOrder) },
            onExport = { onExportSingle(currentOrder) }
        )
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(historyOrders.size) { index ->
                val order = historyOrders[index]
                val isSelected = selectedExportIds.contains(order.id)

                Card(
                    onClick = {
                        if (isExportMode) onToggleSelection(order.id) else selectedOrder = order
                    },
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExportMode && isSelected) Color(0xFFE3F2FD) else Color.White
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                            if (isExportMode) {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                            }

                            Column(Modifier.weight(1f)) {
                                Text("款号: ${order.styleNo}  床号: ${order.bedNo}", fontWeight = FontWeight.Bold)
                                Text("客户: ${order.customer.ifBlank { "无" }}  |  日期: ${order.dateStr}", fontSize = 12.sp, color = Color.Gray)
                            }

                            if (!isExportMode) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${order.totalQty} 件", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(12.dp))
                                    Text("删除", color = Color.Red, fontSize = 13.sp, modifier = Modifier.clickable { orderToDelete = Pair(index, order) })
                                }
                            } else {
                                Text("${order.totalQty} 件", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (orderToDelete != null) {
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text("删除确认") },
            text = { Text("确定删除【${orderToDelete!!.second.styleNo}】吗？") },
            confirmButton = { TextButton({ onDelete(orderToDelete!!.first); orderToDelete = null }) { Text("删除", color = Color.Red) } },
            dismissButton = { TextButton({ orderToDelete = null }) { Text("取消") } }
        )
    }
}


//裁床单明细页面
@Composable
fun DetailScreen(
    order: CutOrderData,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var currentPreviewIndex by remember { mutableIntStateOf(0) }

    // 解析多图
    val images = remember(order.imageBase64) {
        val list = mutableListOf<String>()
        val oldData = order.imageBase64
        if (oldData != null) {
            try {
                if (oldData.startsWith("[")) list.addAll(Gson().fromJson(oldData, Array<String>::class.java).toList())
                else list.add(oldData)
            } catch (e: Exception) { list.add(oldData) }
        }
        list
    }

    BackHandler(enabled = showPreview || showImageDialog) {
        if (showPreview) showPreview = false
        if (showImageDialog) showImageDialog = false
    }

    if (showPreview) { TicketPreviewScreen(order.tickets) { showPreview = false }; return }
    if (showImageDialog && images.isNotEmpty()) { ImageViewerDialog(images, currentPreviewIndex) { showImageDialog = false } }

    val context = LocalContext.current

    // 导出图片
    val exportImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        uri?.let {
            try {
                val bitmap = generateOrderBitmap(order)
                context.contentResolver.openOutputStream(it)?.use { os -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os) }
                Toast.makeText(context, "图片已保存", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show() }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // 顶部导航栏
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.DarkGray)
            }
            Text("裁床单明细", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)

            TextButton(onClick = { exportImageLauncher.launch("裁床单_${order.styleNo}_${order.bedNo}.png") }) {
                Text("存为图", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onExport) { Text("导出", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold) }
            TextButton(onClick = onEdit) { Text("编辑", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold) }
        }

        Text("${order.dateStr}  款号: ${order.styleNo}  床号: ${order.bedNo}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

        // 多图缩略图横向列表
        if (images.isNotEmpty()) {
            Text("关联图片 (${images.size}张)：", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(images.size) { index ->
                    Card(
                        modifier = Modifier.size(100.dp).clickable { currentPreviewIndex = index; showImageDialog = true },
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            decodeBase64ToBitmap(images[index])?.let { Image(it.asImageBitmap(), "缩略图", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                            Text("🔍", modifier = Modifier.align(Alignment.BottomEnd).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(topStart = 8.dp)).padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Text("💡 左右滑动查看表格", fontSize = 11.sp, color = Color.Gray)
        TableView(order)
        Spacer(Modifier.height(24.dp))
        Button({ showPreview = true }, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
            Text("预览并打印标签", fontSize = 16.sp)
        }
        Text("共 ${order.tickets.size} 张，总计 ${order.totalQty} 件", fontSize = 12.sp, color = Color.Gray)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageViewerDialog(images: List<String>, initialIndex: Int = 0, onDismiss: () -> Unit) {
    if (images.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                ZoomableImage(base64String = images[page])
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) { Text("X", color = Color.White, fontSize = 18.sp) }

            if (images.size > 1) {
                Text(
                    "${pagerState.currentPage + 1} / ${images.size}", color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ZoomableImage(base64String: String) {
    val bitmap = remember(base64String) { decodeBase64ToBitmap(base64String) }
    if (bitmap == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("图片解析失败", color = Color.Red) }
        return
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "图片",
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()

                    if (downTime - lastTapTime < viewConfiguration.doubleTapTimeoutMillis) {
                        if (scale > 1f) { scale = 1f; offset = Offset.Zero } else { scale = 2.5f }
                        lastTapTime = 0L
                        do { val event = awaitPointerEvent(); if (event.changes.all { !it.pressed }) break } while (true)
                        return@awaitEachGesture
                    }
                    lastTapTime = downTime

                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        if (event.changes.size > 1) {
                            scale = (scale * zoomChange).coerceIn(1f, 5f)
                            if (scale > 1f) offset += panChange else offset = Offset.Zero
                            event.changes.forEach { it.consume() }
                        } else if (scale > 1f) {
                            offset += panChange
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
        contentScale = ContentScale.Fit
    )
}

@SuppressLint("DefaultLocale")
@Composable//防塌陷版
fun TableView(order: CutOrderData) {
    val sizeTotals = mutableMapOf<Int, Int>()
    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Column(Modifier.border(1.dp, Color.Black)) {
            Row(Modifier.background(Color(0xFFF5F5F5)), verticalAlignment = Alignment.CenterVertically) {
                TableCell(width = 70.dp, text = "颜色", bold = true)
                order.sizesOrder.forEach { size ->
                    TableCell(width = 36.dp, text = "扎号", bold = true, fontSize = 11.sp)
                    TableCell(width = 40.dp, text = size, bold = true)
                }
                TableCell(width = 50.dp, text = "合计", bold = true)
            }

            order.rawRows.forEachIndexed { rowIdx, row ->
                var rowTotal = 0
                Row(Modifier.border(1.dp, Color.Black), verticalAlignment = Alignment.CenterVertically) {
                    TableCell(width = 70.dp, text = row.color)
                    order.sizesOrder.forEachIndexed { colIndex, _ ->
                        val qty = row.quantities["${order.sizesOrder[colIndex]}-$colIndex"]?.toIntOrNull() ?: 0
                        val bundleNo = order.rowBundles[rowIdx]?.get(colIndex)
                        TableCell(width = 36.dp, text = if (bundleNo != null) String.format("%02d", bundleNo) else "", color = Color(0xFF1565C0), bold = true)
                        rowTotal += qty
                        sizeTotals[colIndex] = (sizeTotals[colIndex] ?: 0) + qty
                        TableCell(width = 40.dp, text = if (qty > 0) qty.toString() else "")
                    }
                    TableCell(width = 50.dp, text = "${rowTotal}件")
                }
            }

            Row(Modifier.background(Color(0xFFF5F5F5)), verticalAlignment = Alignment.CenterVertically) {
                TableCell(width = 70.dp, text = "合计", bold = true)
                var grandTotal = 0
                order.sizesOrder.forEachIndexed { colIndex, _ ->
                    val st = sizeTotals[colIndex] ?: 0
                    grandTotal += st
                    TableCell(width = 36.dp, text = "")
                    TableCell(width = 40.dp, text = "${st}件", bold = true)
                }
                TableCell(width = 50.dp, text = "${grandTotal}件", bold = true)
            }
        }
    }
}

@Composable
private fun TableCell(width: androidx.compose.ui.unit.Dp, text: String, bold: Boolean = false, fontSize: androidx.compose.ui.unit.TextUnit = 12.sp, color: Color = Color.Unspecified) {
    Box(modifier = Modifier.width(width).height(36.dp).border(1.dp, Color.Black), contentAlignment = Alignment.Center) {
        if (text.isNotEmpty()) {
            Text(text = text, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = fontSize, color = color, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun TicketPreviewScreen(tickets: List<TicketData>, onBack: () -> Unit) {
    val context = LocalContext.current
    var isPrinting by remember { mutableStateOf(false) }
    var isRotated by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 连接提示状态
    var showConnectTipDialog by remember { mutableStateOf(false) }
    var showDeviceListDialog by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<android.bluetooth.BluetoothDevice>>(emptyList()) }

    // 浓度与背景色
    var density by remember { mutableIntStateOf(8) }
    var showDensityMenu by remember { mutableStateOf(false) }
    var labelBgColor by remember { mutableStateOf(Color.White) }
    var showBgColorMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFFE0E0E0))) {
        // 顶部工具栏
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.DarkGray)
            }

            // 180° 倒置开关
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isRotated = !isRotated }) {
                Checkbox(checked = isRotated, onCheckedChange = { isRotated = it }, modifier = Modifier.size(28.dp))
                Text("180°", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }

            // 浓度菜单
            Box {
                TextButton(onClick = { showDensityMenu = true }) {
                    Text("浓度: $density", fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = showDensityMenu, onDismissRequest = { showDensityMenu = false }) {
                    for (i in 4..15) {
                        DropdownMenuItem(text = { Text("浓度 $i") }, onClick = { density = i; showDensityMenu = false })
                    }
                }
            }

            // 背景色菜单
            Box {
                TextButton(onClick = { showBgColorMenu = true }) {
                    Text("背景色", fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = showBgColorMenu, onDismissRequest = { showBgColorMenu = false }) {
                    DropdownMenuItem(text = { Text("纯白") }, onClick = { labelBgColor = Color.White; showBgColorMenu = false })
                    DropdownMenuItem(text = { Text("浅黄 (牛皮纸)") }, onClick = { labelBgColor = Color(0xFFFFF9C4); showBgColorMenu = false })
                    DropdownMenuItem(text = { Text("浅蓝") }, onClick = { labelBgColor = Color(0xFFE3F2FD); showBgColorMenu = false })
                    DropdownMenuItem(text = { Text("浅绿") }, onClick = { labelBgColor = Color(0xFFE8F5E9); showBgColorMenu = false })
                }
            }

            Spacer(Modifier.weight(1f))

            // 全部打印
            Button(
                onClick = {
                    if (BluetoothPrinter.isConnected()) {
                        isPrinting = true
                        coroutineScope.launch(Dispatchers.IO) {
                            BluetoothPrinter.printBatchTickets(tickets, isRotated, density)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "已全部发送", Toast.LENGTH_SHORT).show()
                                isPrinting = false
                            }
                        }
                    } else {
                        showConnectTipDialog = true
                    }
                },
                enabled = !isPrinting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) { Text(if (isPrinting) "发送中..." else "全部打印") }
        }

        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tickets) { ticket ->
                Card(
                    Modifier
                        .fillMaxWidth(0.55f) // 👈 宽度改为 0.55f
                        .align(Alignment.CenterHorizontally),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(containerColor = labelBgColor),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {

                        // 👇 顶部孔洞效果
                        Box(modifier = Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.TopCenter) {
                            Box(
                                modifier = Modifier
                                    .width(56.dp).height(14.dp)
                                    .offset(y = (-7).dp)
                                    .background(Color(0xFFE0E0E0), RoundedCornerShape(50))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(50))
                            )
                        }

                        val textColor = Color.Black
                        val subTextColor = Color.DarkGray

                        Text("款号: ${ticket.styleNo}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text("床号: ${ticket.bedNo}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(top = 4.dp))
                        Text("扎号: ${String.format("%02d", ticket.bundleNo)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(top = 4.dp))
                        Text("码数: ${ticket.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(top = 4.dp))
                        Text("数量: ${ticket.quantity}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(top = 4.dp))
                        Text("颜色: ${ticket.color}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(top = 4.dp))

                        Text("------------------------", fontSize = 10.sp, color = if (labelBgColor == Color.White) Color.Gray else Color.Black.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))

                        Text("共 ${ticket.totalBundles} 扎, 总 ${ticket.totalQuantity} 件", fontSize = 11.sp, color = subTextColor)
                        Text("${ticket.customer}  ${ticket.dateStr}", fontSize = 11.sp, color = subTextColor, modifier = Modifier.padding(top = 2.dp))

                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            TextButton(
                                onClick = {
                                    if (BluetoothPrinter.isConnected()) {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            BluetoothPrinter.printBatchTickets(listOf(ticket), isRotated, density)
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "已补打扎号: ${String.format("%02d", ticket.bundleNo)}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        showConnectTipDialog = true
                                    }
                                }
                            ) { Text("🖨 补打此扎", color = Color(0xFF1565C0), fontSize = 14.sp) }
                        }
                    }
                }
            }
        }
    }

    // ===== 弹窗部分 =====
    if (showConnectTipDialog) {
        AlertDialog(
            onDismissRequest = { showConnectTipDialog = false },
            title = { Text("提示") },
            text = { Text("打印机未连接，请先连接蓝牙打印机再打印。") },
            confirmButton = {
                Button(onClick = {
                    showConnectTipDialog = false
                    val activity = context as? MainActivity
                    val devices = activity?.getPairedDevices() ?: emptyList()
                    if (devices.isEmpty()) Toast.makeText(context, "请先在手机系统设置中配对打印机", Toast.LENGTH_SHORT).show()
                    else { pairedDevices = devices; showDeviceListDialog = true }
                }) { Text("去连接") }
            },
            dismissButton = { TextButton(onClick = { showConnectTipDialog = false }) { Text("取消") } }
        )
    }

    if (showDeviceListDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceListDialog = false },
            title = { Text("选择打印机") },
            text = {
                Column {
                    pairedDevices.forEach { device ->
                        Text(
                            device.name ?: "未知设备",
                            modifier = Modifier.fillMaxWidth().clickable {
                                val activity = context as? MainActivity
                                activity?.connectToDevice(device)
                                showDeviceListDialog = false
                            }.padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDeviceListDialog = false }) { Text("取消") } }
        )
    }
}

fun decodeBase64ToImage(base64Str: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        Log.e("打菲助手", "Logo图片解码失败", e) // 👈 加在这里
        null
    }
}

