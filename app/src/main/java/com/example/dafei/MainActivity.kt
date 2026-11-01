package com.example.dafei


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
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
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.zIndex
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
import androidx.core.content.edit

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

fun compressImage(bytes: ByteArray): ByteArray {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    var scale = 1
    val maxDimension = 800
    if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
        scale = maxOf(1, maxOf(options.outWidth / maxDimension, options.outHeight / maxDimension))
    }
    val decodeOptions = BitmapFactory.Options()
    decodeOptions.inSampleSize = scale
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
    return outputStream.toByteArray()
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

class MainActivity : ComponentActivity() {
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
        setContent { MaterialTheme { AppRoot(connectedDeviceName) } }
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
        // 修复连接：加上 this 上下文
        if (BluetoothPrinter.connect(this, device)) {
            connectedDeviceName = device.name ?: "未知设备"
            Toast.makeText(this, "已连接打印机", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show()
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
        MainAppScreen(connectedDeviceName, currentUser!!) {
            prefs.edit { remove("logged_in_user") }
            currentUser = null
        }
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
fun MainAppScreen(
    connectedDeviceName: String?,
    currentUser: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val gson = remember { Gson() }
    val prefs =
        remember { context.getSharedPreferences("dafei_orders_$currentUser", Context.MODE_PRIVATE) }
    val historyOrders = remember {
        val json = prefs.getString("history_orders", null)
        if (json != null) {
            val type = object : TypeToken<List<CutOrderData>>() {}.type
            gson.fromJson<List<CutOrderData>>(json, type).toMutableStateList()
        } else mutableStateListOf()
    }

    fun saveHistory() {
        prefs.edit { putString("history_orders", gson.toJson(historyOrders)) }
    }

    var currentTab by remember { mutableIntStateOf(0) }
    var showBluetoothDialog by remember { mutableStateOf(false) }
    val pairedDevices = remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var editingOrder by remember { mutableStateOf<CutOrderData?>(null) }

    var isInDetailPage by remember { mutableStateOf(false) }

    // 👇 新增：多选导出模式状态
    var isExportMode by remember { mutableStateOf(false) }
    var selectedExportIds by remember { mutableStateOf(setOf<String>()) }
    var ordersToExportMulti by remember { mutableStateOf<List<CutOrderData>>(emptyList()) }

    // --- 单条导出 Launcher ---
    var orderToExportSingle by remember { mutableStateOf<CutOrderData?>(null) }
    val exportSingleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            orderToExportSingle?.let { order ->
                try {
                    val jsonStr = gson.toJson(listOf(order))
                    context.contentResolver.openOutputStream(it)
                        ?.use { os -> os.write(jsonStr.toByteArray()) }
                    Toast.makeText(context, "导出单条成功", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("打菲助手", "导出失败: ${e.message}", e)
                    Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
        orderToExportSingle = null
    }

    // --- 多条导出 Launcher ---
    val exportMultiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                val jsonStr = gson.toJson(ordersToExportMulti)
                context.contentResolver.openOutputStream(it)
                    ?.use { os -> os.write(jsonStr.toByteArray()) }
                Toast.makeText(
                    context,
                    "成功导出 ${ordersToExportMulti.size} 条",
                    Toast.LENGTH_SHORT
                ).show()
                isExportMode = false // 导出成功后退出多选模式
                selectedExportIds = emptySet()
            } catch (e: Exception) {
                Log.e("打菲助手", "导出失败: ${e.message}", e)
                Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- 导入 Launcher ---
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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
                                Toast.makeText(
                                    context,
                                    "成功导入 ${newOrders.size} 条数据",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(context, "没有新数据需要导入", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("打菲助手", "导入失败: ${e.message}", e)
                    Toast.makeText(context, "导入失败，文件格式不正确", Toast.LENGTH_SHORT).show()
                }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 解码并显示logo
                        val logoBitmap = remember(logoBase64) { decodeBase64ToImage(logoBase64) }
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap,
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(end = 8.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // 原本的文字逻辑不变
                        if (isExportMode) {
                            Text("选择要导出的记录")
                        } else if (currentTab == 0) {
                            Text(if (editingOrder != null) "编辑裁床单" else "打菲助手")
                        } else {
                            Text("历史裁床单")
                        }
                    }
                },
                actions = {
                    if (isExportMode) {
                        // 多选导出模式：只显示取消按钮
                        TextButton(onClick = {
                            isExportMode = false; selectedExportIds = emptySet()
                        }) {
                            Text("取消", color = Color.White)
                        }
                    } else if (currentTab == 0) {
                        // 👇 打菲页专属：显示测试、蓝牙连接、退出
                        if (connectedDeviceName != null) {
                            TextButton(onClick = {
                                if (BluetoothPrinter.isConnected()) {
                                    BluetoothPrinter.printTest()
                                    Toast.makeText(context, "已发送测试指令", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }) { Text("测试", color = Color.Yellow) }
                        }

                        // 蓝牙连接按钮
                        TextButton(onClick = {
                            if (activity.getPairedDevices().isEmpty()) Toast.makeText(
                                context,
                                "请先配对蓝牙",
                                Toast.LENGTH_SHORT
                            ).show()
                            else {
                                pairedDevices.value =
                                    activity.getPairedDevices(); showBluetoothDialog = true
                            }
                        }) { Text(text = connectedDeviceName ?: "未连接", color = Color.White) }

                        // 退出按钮
                        TextButton(onClick = onLogout) { Text("退出", color = Color.White) }

                    } else if (currentTab == 1 && !isInDetailPage) {
                        // 👇 历史列表页专属：只显示导入、导出
                        TextButton(onClick = {
                            isExportMode = true
                            selectedExportIds = emptySet()
                        }) { Text("导出", color = Color.White) }

                        TextButton(onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }) { Text("导入", color = Color.White) }
                    }
                    // 如果 currentTab == 1 且 isInDetailPage == true（在明细页），右侧不显示任何按钮
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1565C0))
            )
        },
        bottomBar = {
            // 👇 多选模式下，底部替换为全选和导出操作栏
            if (isExportMode) {
                BottomAppBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.clickable {
                                selectedExportIds = if (selectedExportIds.size == historyOrders.size) {
                                    emptySet()
                                } else {
                                    historyOrders.map { it.id }.toSet()
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedExportIds.size == historyOrders.size && historyOrders.isNotEmpty(),
                                onCheckedChange = null
                            )
                            Text(
                                "全选 (${selectedExportIds.size}/${historyOrders.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (selectedExportIds.isNotEmpty()) {
                                    ordersToExportMulti =
                                        historyOrders.filter { selectedExportIds.contains(it.id) }
                                    val dateStr = SimpleDateFormat(
                                        "yyyyMMdd_HHmm",
                                        Locale.CHINA
                                    ).format(Date())
                                    exportMultiLauncher.launch("裁床单_批量导出_$dateStr.json")
                                }
                            },
                            enabled = selectedExportIds.isNotEmpty()
                        ) {
                            Text("确认导出")
                        }
                    }
                }
            } else {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        label = { Text("打菲") },
                        icon = { Text("1") })
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        label = { Text("历史") },
                        icon = { Text("2") })
                }
            }
        }
    ) { padding ->
        Box(Modifier
            .padding(padding)
            .imePadding()) {
            if (currentTab == 0) {
                GenerateScreen(historyOrders, editingOrder) { newOrder ->
                    if (editingOrder != null) {
                        val oldIndex = historyOrders.indexOfFirst { it.id == editingOrder!!.id }
                        if (oldIndex != -1) historyOrders.removeAt(oldIndex)
                        editingOrder = null
                    }
                    historyOrders.add(0, newOrder); saveHistory(); currentTab = 1
                }
            } else {
                HistoryListScreen(
                    historyOrders = historyOrders,
                    isExportMode = isExportMode, // 👈 传入多选状态
                    selectedExportIds = selectedExportIds, // 👈 传入已选 ID
                    onToggleSelection = { id -> // 👈 切换选中状态
                        selectedExportIds = if (selectedExportIds.contains(id)) {
                            selectedExportIds - id
                        } else {
                            selectedExportIds + id
                        }
                    },
                    onDetailStatusChanged = { isInDetail -> isInDetailPage = isInDetail },
                    onDelete = { index ->
                        historyOrders.removeAt(index)
                        saveHistory()
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                    },
                    onEdit = { order ->
                        editingOrder = order
                        currentTab = 0
                    },
                    onExportSingle = { order ->
                        orderToExportSingle = order
                        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA).format(Date())
                        exportSingleLauncher.launch("裁床单_${order.styleNo}_${order.bedNo}_$dateStr.json")
                    }
                )
            }
        }

        // 蓝牙弹窗 (保持原样)
        if (showBluetoothDialog) {
            AlertDialog(
                onDismissRequest = { showBluetoothDialog = false },
                title = { Text("选择打印机") },
                text = {
                    Column {
                        pairedDevices.value.forEach { device ->
                            Text(
                                device.name ?: "未知",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activity.connectToDevice(device); showBluetoothDialog =
                                        false
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showBluetoothDialog = false
                    }) { Text("取消") }
                }
            )
        }
    }
}

@SuppressLint("UseKtx", "MutableCollectionMutableState", "DefaultLocale")
@OptIn(ExperimentalLayoutApi::class)
@Composable // 如果你有 @Composable 注解的话
fun GenerateScreen(
    historyOrders: List<CutOrderData>,
    editingOrder: CutOrderData? = null,
    onGenerate: (CutOrderData) -> Unit
) {
    // 👇 新增：控制是否横向自动复制
    var enableAutoCopy by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dafei_prefs", Context.MODE_PRIVATE) }

    // --- 基础信息：如果是编辑模式则回显数据 ---
    var customer by remember(editingOrder) { mutableStateOf(editingOrder?.customer ?: "") }
    var styleNo by remember(editingOrder) { mutableStateOf(editingOrder?.styleNo ?: "") }
    var dateStr by remember(editingOrder) {
        mutableStateOf(
            editingOrder?.dateStr ?: SimpleDateFormat("yy-MM-dd", Locale.CHINA).format(Date())
        )
    }
    var bedNo by remember(editingOrder) { mutableStateOf(editingOrder?.bedNo ?: "") }
    var imageBase64 by remember(editingOrder) { mutableStateOf(editingOrder?.imageBase64) }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var continuousBundle by remember(editingOrder) { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    var isStyleNoFocused by remember { mutableStateOf(false) }

    // 检索历史款号的逻辑：去重、按时间倒序、最多显示5条
    val matchedHistory by remember(styleNo, historyOrders) {
        derivedStateOf {
            if (styleNo.isBlank()) {
                emptyList()
            } else {
                historyOrders
                    .filter { it.styleNo.contains(styleNo, ignoreCase = true) }
                    .distinctBy { it.styleNo } // 款号去重
                    .sortedByDescending { it.time } // 最近录入的排前面
                    .take(5) // 最多显示5条，防止遮挡太多
            }
        }
    }

    // --- 图片选择相关 ---
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes(); inputStream?.close()
                if (bytes != null) imageBase64 =
                    Base64.encodeToString(compressImage(bytes), Base64.DEFAULT)
            }
        }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) cameraUri?.let { uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes(); inputStream?.close()
                if (bytes != null) imageBase64 =
                    Base64.encodeToString(compressImage(bytes), Base64.DEFAULT)
            }
        }
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                val file = File(context.cacheDir, "temp_photo.jpg")
                cameraUri =
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                cameraLauncher.launch(cameraUri!!)
            } else Toast.makeText(context, "需要相机权限", Toast.LENGTH_SHORT).show()
        }

    var showImageSourceDialog by remember { mutableStateOf(false) }

    // --- 床号自动递增逻辑 (仅在新建时生效，编辑时不触发) ---
    LaunchedEffect(dateStr, historyOrders) {
        if (editingOrder == null && bedNo.isEmpty()) {
            val currentMonthPrefix = dateStr.take(5)
            val monthOrders = historyOrders.filter { it.dateStr.startsWith(currentMonthPrefix) }
            val maxBedNo = monthOrders.mapNotNull { it.bedNo.toIntOrNull() }.maxOrNull() ?: 0
            bedNo = (maxBedNo + 1).toString()
        }
    }

    // --- 预设码数与颜色 ---
    val presetSizes = remember {
        mutableStateOf(
            prefs.getStringSet("sizes", setOf("S", "M", "L", "XL"))?.toMutableList()
                ?: mutableListOf()
        )
    }
    val presetColors = remember {
        mutableStateOf(
            prefs.getStringSet("colors", setOf("黑色", "白色", "杏色"))?.toMutableList()
                ?: mutableListOf()
        )
    }

    // --- 表格数据：如果是编辑模式则恢复 ---
    val selectedSizes =
        remember(editingOrder) { mutableStateOf(editingOrder?.sizesOrder ?: listOf()) }

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

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 👇 款号输入框及下拉历史列表部分
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = styleNo,
                    onValueChange = { styleNo = it },
                    label = { Text("款号") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            // 记录焦点状态，失焦时收起列表
                            isStyleNoFocused = focusState.isFocused
                        }
                )

                // 悬浮的下拉列表
                if (isStyleNoFocused && matchedHistory.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 56.dp) // 避开输入框的高度
                            .zIndex(10f), // 确保覆盖在下方的床号或图片区域上方
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(matchedHistory) { order ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // 点击后填入款号和客户
                                            styleNo = order.styleNo
                                            customer = order.customer
                                            isStyleNoFocused = false
                                            focusManager.clearFocus() // 收起键盘
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = order.styleNo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (order.customer.isNotBlank()) {
                                        Text(
                                            text = "客户: ${order.customer}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // 床号输入框保持原样
            OutlinedTextField(
                bedNo,
                { bedNo = it },
                label = { Text("床号") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                customer,
                { customer = it },
                label = { Text("客户") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                dateStr,
                { dateStr = it },
                label = { Text("日期") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { continuousBundle = !continuousBundle }) {
                Checkbox(checked = continuousBundle, onCheckedChange = { continuousBundle = it })
                Text("连床编菲", fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .border(1.dp, Color.Gray, MaterialTheme.shapes.medium)
                .clickable { showImageSourceDialog = true }, contentAlignment = Alignment.Center
        ) {
            if (imageBase64 == null) Text(
                "+ 点击拍照或上传图片",
                color = Color.Gray,
                fontSize = 14.sp
            )
            else decodeBase64ToBitmap(imageBase64!!)?.let {
                Image(
                    it.asImageBitmap(),
                    "图片",
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: Text("解析失败", color = Color.Red)
        }
        Spacer(Modifier.height(16.dp))

        // 颜色、码数按钮并排 (颜色在左，码数在右)
        // 👇 替换这里的 Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("颜色 / 件数", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 👇 新增：行复制开关（放在按钮前面）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { enableAutoCopy = !enableAutoCopy }
                ) {
                    Text("行复制", fontSize = 12.sp, color = Color.DarkGray)
                    Spacer(Modifier.width(2.dp))
                    // 使用 Switch 并缩小，防止挤占按钮空间
                    Switch(
                        checked = enableAutoCopy,
                        onCheckedChange = { enableAutoCopy = it },
                        modifier = Modifier.scale(0.7f) // 缩小到 70%
                    )
                }

                Spacer(Modifier.width(4.dp))

                // 添加颜色按钮
                Button(
                    onClick = {
                        tempSelectedColors = selectedColorsOrder.toSet(); showSelectColorDialog =
                        true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { Text("+ 颜色", fontSize = 12.sp) }

                Spacer(Modifier.width(8.dp))

                // 添加码数按钮
                Button(
                    onClick = {
                        tempSelectedSizes = selectedSizes.value.toList(); showSelectSizeDialog =
                        true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { Text("+ 码数", fontSize = 12.sp) }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (selectedColorsOrder.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedColorsOrder.forEach { color ->
                    AssistChip(
                        { rowList.add(RowInputData(color)) },
                        { Text("+ $color", fontSize = 12.sp) })
                }
            }
        }

        // 表头
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F0F0))
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "颜色",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(start = 8.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            selectedSizes.value.forEach { size ->
                Text(
                    size,
                    modifier = Modifier.weight(1.1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(36.dp))
        }

        // 表格内容 (修复输入框被遮挡 + 恢复横向复制)
        rowList.forEachIndexed { index, row ->
            key(index) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 👇 颜色显示框
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(42.dp) // 统一高度 42dp
                            .border(1.dp, Color.Gray, MaterialTheme.shapes.extraSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(row.color, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                    selectedSizes.value.forEachIndexed { colIndex, size ->
                        val currentKey = "$size-$colIndex"
                        // 👇 将原来的 OutlinedTextField 替换为 BasicTextField
                        BasicTextField(
                            value = row.quantities[currentKey] ?: "",
                            onValueChange = { newVal ->
                                if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                                    val newMap = row.quantities.toMutableMap()
                                    newMap[currentKey] = newVal
                                    // 恢复横向自动复制
                                    // 👇 这里的开关控制
                                    if (enableAutoCopy && newVal.isNotEmpty()) {
                                        for (i in colIndex + 1 until selectedSizes.value.size) {
                                            newMap["${selectedSizes.value[i]}-$i"] = newVal
                                        }
                                    }
                                    rowList[index] = row.copy(quantities = newMap)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center, // 文字居中
                                fontSize = 16.sp,
                                color = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .padding(horizontal = 2.dp)
                                .height(42.dp) // 高度自己定
                                .border(
                                    1.dp,
                                    Color.Gray,
                                    MaterialTheme.shapes.extraSmall
                                ), // 自己画一个边框
                            decorationBox = { innerTextField ->
                                // 👇 使用 Box 包裹，确保文字绝对垂直居中
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    innerTextField()
                                }
                            }
                        )
                    }
                    IconButton(
                        { rowList.removeAt(index) },
                        modifier = Modifier.width(36.dp)
                    ) { Text("X", color = Color.Red, fontWeight = FontWeight.Bold) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 生成按钮
        Button(
            onClick = {
                if (styleNo.isBlank()) {
                    Toast.makeText(context, "款号不能为空", Toast.LENGTH_SHORT)
                        .show(); return@Button
                }
                if (bedNo.isBlank()) {
                    Toast.makeText(context, "床号不能为空", Toast.LENGTH_SHORT)
                        .show(); return@Button
                }
                if (selectedSizes.value.isEmpty()) {
                    Toast.makeText(context, "请先选择码数", Toast.LENGTH_SHORT)
                        .show(); return@Button
                }

                val cutId = editingOrder?.id ?: ("C" + System.currentTimeMillis())
                val newTickets = mutableListOf<TicketData>()
                var totalQty = 0
                var globalBundleNo = 1

                if (continuousBundle && editingOrder == null) {
                    val currentMonthPrefix = dateStr.take(5)
                    val prevOrder = historyOrders.filter {
                        it.styleNo == styleNo && it.dateStr.startsWith(currentMonthPrefix)
                    }.maxByOrNull { it.time }
                    if (prevOrder != null) globalBundleNo =
                        (prevOrder.tickets.maxOfOrNull { it.bundleNo } ?: 0) + 1
                }

                val rowBundles = mutableMapOf<Int, MutableMap<Int, Int>>()
                selectedSizes.value.forEachIndexed { colIndex, size ->
                    rowList.forEachIndexed { rowIdx, row ->
                        val qty = row.quantities["$size-$colIndex"]?.toIntOrNull() ?: 0
                        if (row.color.isNotBlank() && qty > 0) {
                            rowBundles.getOrPut(rowIdx) { mutableMapOf() }[colIndex] =
                                globalBundleNo
                            val no = "$styleNo-$size-" + String.format("%02d", globalBundleNo)
                            newTickets.add(
                                TicketData(
                                    no,
                                    cutId,
                                    customer,
                                    styleNo,
                                    bedNo,
                                    dateStr,
                                    row.color,
                                    size,
                                    qty,
                                    globalBundleNo
                                )
                            )
                            totalQty += qty
                            globalBundleNo++
                        }
                    }
                }

                if (newTickets.isNotEmpty()) {
                    val finalTickets = newTickets.map {
                        it.copy(
                            totalBundles = newTickets.size,
                            totalQuantity = totalQty
                        )
                    }
                    val finalRawRows =
                        rowList.map { it.copy(quantities = it.quantities.toMutableMap()) }
                    // 如果是编辑，时间可以保留旧的，也可以更新，这里设为更新时间
                    val time = editingOrder?.time ?: System.currentTimeMillis()

                    onGenerate(
                        CutOrderData(
                            cutId,
                            customer,
                            styleNo,
                            bedNo,
                            dateStr,
                            time,
                            totalQty,
                            finalTickets,
                            selectedSizes.value,
                            finalRawRows,
                            rowBundles,
                            imageBase64
                        )
                    )

                    Toast.makeText(
                        context,
                        if (editingOrder != null) "已修改保存" else "已保存：${finalTickets.size}张",
                        Toast.LENGTH_SHORT
                    ).show()

                    // 如果不是编辑状态，则清空表单
                    if (editingOrder == null) {
                        rowList.clear(); styleNo = ""; customer = ""; imageBase64 =
                            null; selectedColorsOrder.clear(); selectedSizes.value = emptyList()
                    }
                } else Toast.makeText(context, "没有有效件数", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) { Text(if (editingOrder != null) "保存修改" else "生成并保存裁床单", fontSize = 16.sp) }

        Spacer(Modifier.height(40.dp))
    }

    // ===== 弹窗部分 =====

    // 1. 图片选择弹窗
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("选择图片") },
            text = {
                Column {
                    Text(
                        "拍照",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceDialog =
                                    false; cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                            .padding(12.dp),
                        fontSize = 16.sp
                    )
                    HorizontalDivider()
                    Text(
                        "相册",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceDialog = false; galleryLauncher.launch("image/*")
                            }
                            .padding(12.dp),
                        fontSize = 16.sp
                    )
                }
            },
            confirmButton = { TextButton({ showImageSourceDialog = false }) { Text("取消") } }
        )
    }

    // 2. 选择码数弹窗 (支持重复选择)
    if (showSelectSizeDialog) {
        AlertDialog(
            onDismissRequest = { showSelectSizeDialog = false },
            title = { Text("选择码数") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    // 👇 顶部：已选码数常驻显示，允许重复，按标准顺序展示
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), MaterialTheme.shapes.small)
                            .padding(8.dp)
                    ) {
                        Text(
                            "已选码数（支持重复）：",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))

                        // 使用 sortedBy 排序，且不去重
                        val pickedSorted = tempSelectedSizes.sortedBy { SizeOrder.rank(it) }

                        if (pickedSorted.isEmpty()) {
                            Text(
                                "暂未选择",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pickedSorted.forEach { s ->
                                    Row(
                                        Modifier
                                            .background(Color.White, MaterialTheme.shapes.small)
                                            .border(1.dp, Color.Gray, MaterialTheme.shapes.small)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(s, fontSize = 14.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "x",
                                            color = Color.Red,
                                            modifier = Modifier.clickable {
                                                // 注意：这里使用 minus 运算符只会移除 List 中的第一个匹配项，正好符合“删掉其中一个”的逻辑
                                                tempSelectedSizes = tempSelectedSizes - s
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 👇 预设区：改为 AssistChip，点击即添加（不再切换状态）
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SizeOrder.sorted(presetSizes.value).forEach { size ->
                            AssistChip(
                                onClick = { tempSelectedSizes = tempSelectedSizes + size },
                                label = { Text(size, fontSize = 14.sp) }
                            )
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            newSizeText,
                            { newSizeText = it.uppercase() },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("新码数") },
                            singleLine = true
                        )
                        OutlinedButton(
                            onClick = {
                                if (newSizeText.isNotBlank()) {
                                    val trimmed = newSizeText.trim()
                                    // 预设列表里没有就保存到预设
                                    if (!presetSizes.value.contains(trimmed)) {
                                        presetSizes.value =
                                            (presetSizes.value + trimmed).toMutableList()
                                        prefs.edit {
                                            putStringSet("sizes", presetSizes.value.toSet())
                                        }
                                    }
                                    // 直接添加到已选列表，不再判断是否重复
                                    tempSelectedSizes = tempSelectedSizes + trimmed
                                    newSizeText = ""
                                }
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) { Text("添加") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    // 表格保持原顺序（包含重复项）
                    selectedSizes.value = tempSelectedSizes
                    showSelectSizeDialog = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton({ showSelectSizeDialog = false }) { Text("取消") } }
        )
    }

    // 3. 选择颜色弹窗 (横向流式 + 保持原顺序 + 持久化)
    if (showSelectColorDialog) {
        AlertDialog(
            onDismissRequest = { showSelectColorDialog = false },
            title = { Text("选择颜色") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    // 预设颜色：横向流式排列，保持录入顺序，不对齐
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        presetColors.value.forEach { color ->
                            val selected = tempSelectedColors.contains(color)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    tempSelectedColors = if (selected) tempSelectedColors - color
                                    else tempSelectedColors + color
                                },
                                label = { Text(color, fontSize = 14.sp) }
                            )
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            newColorInput,
                            { newColorInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("新颜色") },
                            singleLine = true
                        )
                        OutlinedButton(
                            onClick = {
                                if (newColorInput.isNotBlank()) {
                                    val trimmed = newColorInput.trim()
                                    if (!presetColors.value.contains(trimmed)) {
                                        // 重新赋值，确保 state 变更触发重组
                                        presetColors.value =
                                            (presetColors.value + trimmed).toMutableList()
                                    }
                                    tempSelectedColors = tempSelectedColors + trimmed
                                    newColorInput = ""
                                    prefs.edit {
                                        putStringSet("colors", presetColors.value.toSet())
                                    }
                                }
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) { Text("添加") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    tempSelectedColors.forEach {
                        if (!selectedColorsOrder.contains(it)) selectedColorsOrder.add(
                            it
                        )
                    }
                    showSelectColorDialog = false
                }) { Text("确定加入") }
            },
            dismissButton = { TextButton({ showSelectColorDialog = false }) { Text("取消") } }
        )
    }
}

@Composable//裁床单列表页面
fun HistoryListScreen(
    historyOrders: List<CutOrderData>,
    isExportMode: Boolean, // 👈 接收是否处于导出模式
    selectedExportIds: Set<String>, // 👈 接收已选列表
    onToggleSelection: (String) -> Unit, // 👈 接收点击切换事件
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

    if (selectedOrder != null && !isExportMode) { // 导出模式下不进入明细页
        val currentOrder = historyOrders.find { it.id == selectedOrder!!.id } ?: selectedOrder!!

        DetailScreen(
            order = currentOrder,
            onBack = { selectedOrder = null },
            onEdit = { onEdit(currentOrder) },
            onExport = { onExportSingle(currentOrder) }
        )
    } else {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(historyOrders.size) { index ->
                val order = historyOrders[index]
                val isSelected = selectedExportIds.contains(order.id)

                Card(
                    // 👇 核心逻辑：如果在导出模式，点击是选中/取消；否则是进详情
                    onClick = {
                        if (isExportMode) {
                            onToggleSelection(order.id)
                        } else {
                            selectedOrder = order
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExportMode && isSelected) Color(0xFFE3F2FD) else Color.White
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // 👇 如果在导出模式，最前面显示 Checkbox
                            if (isExportMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null // 点击事件被最外层的 Card onClick 拦截
                                )
                                Spacer(Modifier.width(8.dp))
                            }

                            Column(Modifier.weight(1f)) {
                                Text(
                                    "款号: ${order.styleNo}  床号: ${order.bedNo}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "客户: ${order.customer.ifBlank { "无" }}  |  日期: ${order.dateStr}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            // 导出模式下隐藏列表卡片里的删除按钮，防止误触
                            if (!isExportMode) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${order.totalQty} 件",
                                        color = Color(0xFF1565C0),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "删除",
                                        color = Color.Red,
                                        fontSize = 13.sp,
                                        modifier = Modifier.clickable {
                                            orderToDelete = Pair(index, order)
                                        })
                                }
                            } else {
                                Text(
                                    "${order.totalQty} 件",
                                    color = Color(0xFF1565C0),
                                    fontWeight = FontWeight.Bold
                                )
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
            confirmButton = {
                TextButton({
                    onDelete(orderToDelete!!.first); orderToDelete = null
                }) { Text("删除", color = Color.Red) }
            },
            dismissButton = { TextButton({ orderToDelete = null }) { Text("取消") } }
        )
    }
}

@Composable//裁床单明细页面
fun DetailScreen(
    order: CutOrderData,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit // 👈 1. 新增导出回调参数
) {
    var showPreview by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showPreview || showImageDialog) {
        if (showPreview) showPreview = false
        if (showImageDialog) showImageDialog = false
    }

    if (showPreview) {
        TicketPreviewScreen(order.tickets) { showPreview = false }; return
    }
    if (showImageDialog && order.imageBase64 != null) {
        ImageViewerDialog(order.imageBase64) { showImageDialog = false }
    }

    Column(Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)) {
        // 👇 顶部导航栏：左侧返回，中间标题，右侧导出 + 编辑
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton({ onBack() }) { Text("< 返回") }
            Text(
                "裁床单明细",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // 👇 2. 替换为调用外部传入的 onExport()
            TextButton(onClick = onExport) {
                Text("导出", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
            }

            // 编辑按钮保持不变
            TextButton({ onEdit() }) {
                Text("编辑", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
            }
        }

        Text(
            "${order.dateStr}  款号: ${order.styleNo}  床号: ${order.bedNo}",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (order.imageBase64 != null) Button(
            { showImageDialog = true },
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE3F2FD),
                contentColor = Color(0xFF1565C0)
            )
        ) { Text("📷 查看关联图片 (支持缩放)") }

        Text("💡 左右滑动查看", fontSize = 11.sp, color = Color.Gray)
        TableView(order)
        Spacer(Modifier.height(24.dp))
        Button(
            { showPreview = true },
            Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) { Text("预览并打印标签", fontSize = 16.sp) }

        // 👇 3. 顺带修正了一个潜在编译错误：你的 CutOrderData 里应该叫 totalQuantity 而不是 totalQty
        Text(
            "共 ${order.tickets.size} 张，总计 ${order.totalQty} 件",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun ImageViewerDialog(base64Str: String, onDismiss: () -> Unit) {
    val bitmap = decodeBase64ToBitmap(base64Str)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier
            .fillMaxSize()
            .background(Color.Black)) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "大图",
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offset += pan
                                } else {
                                    offset = androidx.compose.ui.geometry.Offset.Zero
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    // 双击放大/还原
                                    if (scale > 1f) {
                                        scale = 1f; offset =
                                            androidx.compose.ui.geometry.Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    "图片解析失败",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x88FFFFFF),
                    contentColor = Color.Black
                )
            ) {
                Text("✕ 关闭", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun TableView(order: CutOrderData) {
    val sizeTotals = mutableMapOf<Int, Int>()
    Box(Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())) {
        Column(Modifier.border(1.dp, Color.Black)) {
            Row(Modifier.background(Color(0xFFF5F5F5))) {
                Box(Modifier
                    .width(70.dp)
                    .border(1.dp, Color.Black)
                    .padding(4.dp)) {
                    Text(
                        "颜色",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                order.sizesOrder.forEachIndexed { _, size ->
                    Box(
                        Modifier
                            .width(36.dp)
                            .border(1.dp, Color.Black)
                            .padding(4.dp)
                    ) {
                        Text(
                            "扎号",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(Modifier
                        .width(40.dp)
                        .border(1.dp, Color.Black)
                        .padding(4.dp)) {
                        Text(
                            size,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Box(Modifier
                    .width(50.dp)
                    .border(1.dp, Color.Black)
                    .padding(4.dp)) {
                    Text(
                        "合计",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            order.rawRows.forEachIndexed { rowIdx, row ->
                var rowTotal = 0
                Row(Modifier.border(1.dp, Color.Black)) {
                    Box(
                        Modifier
                            .width(70.dp)
                            .border(1.dp, Color.Black)
                            .padding(4.dp)
                    ) { Text(row.color, fontSize = 12.sp) }
                    order.sizesOrder.forEachIndexed { colIndex, _ ->
                        val qty =
                            row.quantities["${order.sizesOrder[colIndex]}-$colIndex"]?.toIntOrNull()
                                ?: 0
                        val bundleNo = order.rowBundles[rowIdx]?.get(colIndex)
                        Box(
                            Modifier
                                .width(36.dp)
                                .border(1.dp, Color.Black)
                                .padding(4.dp)
                        ) {
                            if (bundleNo != null) Text(
                                String.format("%02d", bundleNo),
                                color = Color(0xFF1565C0),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        rowTotal += qty; sizeTotals[colIndex] = (sizeTotals[colIndex] ?: 0) + qty
                        Box(
                            Modifier
                                .width(40.dp)
                                .border(1.dp, Color.Black)
                                .padding(4.dp)
                        ) {
                            Text(
                                if (qty > 0) qty.toString() else "",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Box(
                        Modifier
                            .width(50.dp)
                            .border(1.dp, Color.Black)
                            .padding(4.dp)
                    ) {
                        Text(
                            "${rowTotal}件",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Row(Modifier.background(Color(0xFFF5F5F5))) {
                Box(Modifier
                    .width(70.dp)
                    .border(1.dp, Color.Black)
                    .padding(4.dp)) {
                    Text(
                        "合计",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                var grandTotal = 0
                order.sizesOrder.forEachIndexed { colIndex, _ ->
                    val st = sizeTotals[colIndex] ?: 0; grandTotal += st
                    Box(Modifier
                        .width(36.dp)
                        .border(1.dp, Color.Black)
                        .padding(4.dp)) { Text("") }
                    Box(
                        Modifier
                            .width(40.dp)
                            .border(1.dp, Color.Black)
                            .padding(4.dp)
                    ) {
                        Text(
                            "${st}件",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Box(
                    Modifier
                        .width(50.dp)
                        .border(1.dp, Color.Black)
                        .padding(4.dp)
                ) {
                    Text(
                        "${grandTotal}件",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
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

    // 👇 新增：浓度状态，默认 8
    var density by remember { mutableIntStateOf(8) }
    var showDensityMenu by remember { mutableStateOf(false) }

    Column(Modifier
        .fillMaxSize()
        .background(Color(0xFFE0E0E0))) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton({ onBack() }) { Text("< 返回") }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { isRotated = !isRotated }) {
                Checkbox(
                    checked = isRotated,
                    onCheckedChange = { isRotated = it },
                    modifier = Modifier.size(28.dp)
                )
                Text("180°", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }

            // 👇 新增：浓度下拉菜单
            Box {
                TextButton(onClick = { showDensityMenu = true }) {
                    Text(
                        "浓度: $density",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold
                    )
                }
                DropdownMenu(
                    expanded = showDensityMenu,
                    onDismissRequest = { showDensityMenu = false }
                ) {
                    // 提供 4 到 15 的浓度选项（4 比较淡，15 最黑）
                    for (i in 4..15) {
                        DropdownMenuItem(
                            text = { Text("浓度 $i") },
                            onClick = {
                                density = i
                                showDensityMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (BluetoothPrinter.isConnected()) {
                        isPrinting = true
                        coroutineScope.launch(Dispatchers.IO) {
                            // 👇 传入浓度参数
                            BluetoothPrinter.printBatchTickets(tickets, isRotated, density)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "已全部发送", Toast.LENGTH_SHORT).show()
                                isPrinting = false
                            }
                        }
                    } else Toast.makeText(context, "请先连接打印机", Toast.LENGTH_SHORT).show()
                },
                enabled = !isPrinting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) { Text(if (isPrinting) "发送中..." else "全部打印") }
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tickets) { ticket ->
                Card(
                    Modifier
                        .fillMaxWidth(0.85f)
                        .align(Alignment.CenterHorizontally),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Column(Modifier
                        .fillMaxWidth()
                        .padding(12.dp)) {
                        Text(
                            "款号: ${ticket.styleNo}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "床号: ${ticket.bedNo}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            "扎号: ${String.format("%02d", ticket.bundleNo)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            "码数: ${ticket.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            "数量: ${ticket.quantity}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            "颜色: ${ticket.color}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            "------------------------",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        Text(
                            "共 ${ticket.totalBundles} 扎, 总 ${ticket.totalQuantity} 件",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            "${ticket.customer}  ${ticket.dateStr}",
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        HorizontalDivider(Modifier.padding(vertical = 8.dp))

                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            TextButton(
                                onClick = {
                                    if (BluetoothPrinter.isConnected()) {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            // 👇 单扎补打也传入浓度参数
                                            BluetoothPrinter.printBatchTickets(
                                                listOf(ticket),
                                                isRotated,
                                                density
                                            )
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "已补打扎号: ${
                                                        String.format("%02d",ticket.bundleNo)
                                                    }",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else Toast.makeText(
                                        context,
                                        "请先连接打印机",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Text("🖨 补打此扎", color = Color(0xFF1565C0), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
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