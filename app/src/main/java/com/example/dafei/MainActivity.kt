package com.example.dafei

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

// ================= 数据模型 =================
data class TicketData(
    val ticketNo: String,
    val cutId: String,
    val customer: String,
    val styleNo: String,
    val bedNo: String,
    val dateStr: String,
    val color: String,
    val size: String,
    val quantity: Int,
    val bundleNo: Int,
    val totalBundles: Int = 0,
    val totalQuantity: Int = 0
)

data class RowInputData(
    val color: String = "",
    val quantities: MutableMap<String, String> = mutableMapOf()
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

// ================= 图片处理工具 =================
fun compressImage(bytes: ByteArray): ByteArray {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    val imageWidth = options.outWidth
    val imageHeight = options.outHeight

    var scale = 1
    val maxDimension = 800
    if (imageWidth > maxDimension || imageHeight > maxDimension) {
        scale = (imageWidth / maxDimension).coerceAtMost(imageHeight / maxDimension)
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
        null
    }
}

// ================= 主界面 =================
class MainActivity : ComponentActivity() {
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var connectedDeviceName by mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermissions()
        
        setContent {
            MaterialTheme {
                AppRoot(
                    connectedDeviceName = connectedDeviceName,
                    onConnectClick = { showDeviceListDialog() }
                )
            }
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
        return try { bluetoothAdapter?.bondedDevices?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        if (BluetoothPrinter.connect(device)) {
            connectedDeviceName = device.name ?: "未知设备"
            Toast.makeText(this, "已连接打印机", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun showDeviceListDialog() {
        if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(this, "请先开启手机蓝牙", Toast.LENGTH_SHORT).show()
        }
    }
}

// ================= 路由与登录拦截 =================
@Composable
fun AppRoot(connectedDeviceName: String?, onConnectClick: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dafei_auth", Context.MODE_PRIVATE) }
    
    var currentUser by remember { mutableStateOf<String?>(prefs.getString("logged_in_user", null)) }

    if (currentUser == null) {
        LoginScreen(onLoginSuccess = { username ->
            prefs.edit().putString("logged_in_user", username).apply()
            currentUser = username
        })
    } else {
        MainAppScreen(
            connectedDeviceName = connectedDeviceName,
            onConnectClick = onConnectClick,
            currentUser = currentUser!!,
            onLogout = {
                prefs.edit().remove("logged_in_user").apply()
                currentUser = null
            }
        )
    }
}

// ================= 登录注册页面 =================
@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dafei_users", Context.MODE_PRIVATE) }

    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (isLogin) "欢迎回来" else "注册新账号", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("账号") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("密码") },
            modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "账号密码不能为空", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (isLogin) {
                    val savedPwd = prefs.getString(username, null)
                    if (savedPwd == password) onLoginSuccess(username)
                    else Toast.makeText(context, "账号或密码错误", Toast.LENGTH_SHORT).show()
                } else {
                    if (prefs.contains(username)) Toast.makeText(context, "该账号已存在", Toast.LENGTH_SHORT).show()
                    else {
                        prefs.edit().putString(username, password).apply()
                        Toast.makeText(context, "注册成功，请登录", Toast.LENGTH_SHORT).show()
                        isLogin = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text(if (isLogin) "登录" else "注册", fontSize = 16.sp) }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { isLogin = !isLogin }) { Text(if (isLogin) "没有账号？去注册" else "已有账号？去登录") }
    }
}

// ================= 主系统界面 =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    connectedDeviceName: String?, onConnectClick: () -> Unit, currentUser: String, onLogout: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as MainActivity
    
    val gson = remember { Gson() }
    val prefs = remember { context.getSharedPreferences("dafei_orders_$currentUser", Context.MODE_PRIVATE) }
    
    val historyOrders = remember {
        val json = prefs.getString("history_orders", null)
        if (json != null) {
            val type = object : TypeToken<List<CutOrderData>>() {}.type
            gson.fromJson<List<CutOrderData>>(json, type).toMutableStateList()
        } else {
            mutableStateListOf()
        }
    }

    fun saveHistory() {
        prefs.edit().putString("history_orders", gson.toJson(historyOrders)).apply()
    }

    var currentTab by remember { mutableIntStateOf(0) }
    var showBluetoothDialog by remember { mutableStateOf(false) }
    val pairedDevices = remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentTab == 0) "打菲助手" else "历史裁床单") },
                actions = {
                    TextButton(onClick = {
                        if (activity.getPairedDevices().isEmpty()) {
                            Toast.makeText(context, "请先在手机设置里配对蓝牙打印机", Toast.LENGTH_LONG).show()
                        } else {
                            pairedDevices.value = activity.getPairedDevices()
                            showBluetoothDialog = true
                        }
                    }) { Text(text = connectedDeviceName ?: "未连接", color = Color.White) }
                    TextButton(onClick = onLogout) { Text("退出", color = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1565C0))
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, label = { Text("打菲") }, icon = { Text("1") })
                NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, label = { Text("历史") }, icon = { Text("2") })
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (currentTab == 0) {
                GenerateScreen(historyOrders) { newOrder ->
                    historyOrders.add(0, newOrder)
                    saveHistory()
                    currentTab = 1
                }
            } else {
                HistoryListScreen(historyOrders) { indexToRemove ->
                    historyOrders.removeAt(indexToRemove)
                    saveHistory() 
                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (showBluetoothDialog) {
            AlertDialog(
                onDismissRequest = { showBluetoothDialog = false },
                title = { Text("选择打印机") },
                text = {
                    Column {
                        pairedDevices.value.forEach { device ->
                            Text(
                                text = device.name ?: "未知设备",
                                modifier = Modifier.fillMaxWidth().clickable {
                                    activity.connectToDevice(device)
                                    showBluetoothDialog = false
                                }.padding(vertical = 12.dp)
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showBluetoothDialog = false }) { Text("取消") } }
            )
        }
    }
}

// ================= 打菲生成界面 =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(historyOrders: List<CutOrderData>, onGenerate: (CutOrderData) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dafei_prefs", Context.MODE_PRIVATE) }

    var customer by remember { mutableStateOf("") }
    var styleNo by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(SimpleDateFormat("yy-MM-dd", Locale.CHINA).format(Date())) }
    var bedNo by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    
    var continuousBundle by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                imageBase64 = Base64.encodeToString(compressImage(bytes), Base64.DEFAULT)
            }
        }
    }

    LaunchedEffect(dateStr, historyOrders) {
        val currentMonthPrefix = dateStr.take(5)
        val monthOrders = historyOrders.filter { it.dateStr.startsWith(currentMonthPrefix) }
        val maxBedNo = monthOrders.mapNotNull { it.bedNo.toIntOrNull() }.maxOrNull() ?: 0
        bedNo = (maxBedNo + 1).toString()
    }

    val presetSizes = remember { mutableStateOf(prefs.getStringSet("sizes", setOf("S", "M", "L", "XL"))?.toMutableList() ?: mutableListOf()) }
    val presetColors = remember { mutableStateOf(prefs.getStringSet("colors", setOf("黑色", "白色", "杏色"))?.toMutableList() ?: mutableListOf()) }

    val selectedSizes = remember { mutableStateOf(listOf("S", "M", "L", "XL")) }
    val selectedColorsOrder = remember { mutableStateListOf<String>() }
    val rowList = remember { mutableStateListOf<RowInputData>() }

    var showSelectSizeDialog by remember { mutableStateOf(false) }
    var tempSelectedSizes by remember { mutableStateOf(listOf<String>()) }
    
    var showSelectColorDialog by remember { mutableStateOf(false) }
    var newColorInput by remember { mutableStateOf("") }
    var newSizeText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()).imePadding()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = styleNo, onValueChange = { styleNo = it }, label = { Text("款号") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = bedNo, onValueChange = { bedNo = it }, label = { Text("床号") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = customer, onValueChange = { customer = it }, label = { Text("客户") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("日期") }, modifier = Modifier.weight(1f))
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { continuousBundle = !continuousBundle }) {
            Checkbox(checked = continuousBundle, onCheckedChange = { continuousBundle = it })
            Text("连床编菲 (扎号接着上一床继续)", fontSize = 14.sp)
        }

        Spacer(Modifier.height(8.dp))
        
        Box(
            Modifier.fillMaxWidth().height(100.dp).border(1.dp, Color.Gray, MaterialTheme.shapes.medium).clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (imageBase64 == null) {
                Text("+ 点击上传裁床图片", color = Color.Gray, fontSize = 14.sp)
            } else {
                val bmp = decodeBase64ToBitmap(imageBase64!!)
                if (bmp != null) {
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "床单图片", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text("图片解析失败，点击重试", color = Color.Red, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("颜色 / 件数", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row {
                TextButton(onClick = { 
                    tempSelectedSizes = selectedSizes.value.toList()
                    showSelectSizeDialog = true 
                }) { Text("+ 选择码数") }
                TextButton(onClick = { showSelectColorDialog = true }) { Text("+ 选择颜色") }
            }
        }

        Spacer(Modifier.height(8.dp))
        
        if (selectedColorsOrder.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedColorsOrder.forEach { color ->
                    AssistChip(
                        onClick = { rowList.add(RowInputData(color = color)) },
                        label = { Text("+ $color", fontSize = 12.sp) }
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth().background(Color(0xFFF0F0F0)).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("颜色", modifier = Modifier.weight(1.5f).padding(start = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            selectedSizes.value.forEach { size ->
                Text(size, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(36.dp))
        }

        rowList.forEachIndexed { index, row ->
            key(index) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.weight(1.5f).height(50.dp).border(1.dp, Color.Gray, MaterialTheme.shapes.extraSmall).padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(row.color, fontSize = 14.sp)
                    }

                    selectedSizes.value.forEachIndexed { colIndex, size ->
                        val currentKey = "$size-$colIndex"
                        val qtyValue = row.quantities[currentKey] ?: ""
                        
                        OutlinedTextField(
                            value = qtyValue,
                            onValueChange = { newVal ->
                                if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                                    val newMap = row.quantities.toMutableMap()
                                    newMap[currentKey] = newVal
                                    
                                    if (newVal.isNotEmpty()) {
                                        for (i in colIndex + 1 until selectedSizes.value.size) {
                                            newMap["${selectedSizes.value[i]}-$i"] = newVal
                                        }
                                    }
                                    rowList[index] = row.copy(quantities = newMap)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.1f).padding(horizontal = 2.dp).height(50.dp),
                            singleLine = true
                        )
                    }

                    IconButton(onClick = { rowList.removeAt(index) }, modifier = Modifier.width(36.dp)) {
                        Text("X", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (styleNo.isBlank()) { Toast.makeText(context, "款号不能为空", Toast.LENGTH_SHORT).show(); return@Button }
                if (bedNo.isBlank()) { Toast.makeText(context, "床号不能为空", Toast.LENGTH_SHORT).show(); return@Button }
                
                val cutId = "C" + System.currentTimeMillis()
                val newTickets = mutableListOf<TicketData>()
                var totalQty = 0
                
                var globalBundleNo = 1
                if (continuousBundle) {
                    val currentMonthPrefix = dateStr.take(5)
                    val prevOrder = historyOrders
                        .filter { it.styleNo == styleNo && it.dateStr.startsWith(currentMonthPrefix) }
                        .maxByOrNull { it.time }
                    if (prevOrder != null) {
                        val maxBundleNoInPrev = prevOrder.tickets.maxOfOrNull { it.bundleNo } ?: 0
                        globalBundleNo = maxBundleNoInPrev + 1
                    }
                }

                val rowBundles = mutableMapOf<Int, MutableMap<Int, Int>>()

                selectedSizes.value.forEachIndexed { colIndex, size ->
                    rowList.forEachIndexed { rowIdx, row ->
                        val qty = row.quantities["$size-$colIndex"]?.toIntOrNull() ?: 0
                        if (row.color.isNotBlank() && qty > 0) {
                            rowBundles.getOrPut(rowIdx) { mutableMapOf() }[colIndex] = globalBundleNo

                            val no = "$styleNo-$size-" + String.format("%02d", globalBundleNo)
                            newTickets.add(TicketData(
                                ticketNo = no, cutId = cutId, customer = customer, styleNo = styleNo,
                                bedNo = bedNo, dateStr = dateStr, color = row.color, size = size,
                                quantity = qty, bundleNo = globalBundleNo
                            ))
                            totalQty += qty
                            globalBundleNo++
                        }
                    }
                }

                if (newTickets.isNotEmpty()) {
                    val totalBundles = newTickets.size 
                    val finalTickets = newTickets.map { it.copy(totalBundles = totalBundles, totalQuantity = totalQty) }
                    val finalRawRows = rowList.map { it.copy(quantities = it.quantities.toMutableMap()) }
                    
                    onGenerate(CutOrderData(
                        id = cutId, customer = customer, styleNo = styleNo, bedNo = bedNo, dateStr = dateStr,
                        time = System.currentTimeMillis(), totalQty = totalQty, tickets = finalTickets,
                        sizesOrder = selectedSizes.value, rawRows = finalRawRows, rowBundles = rowBundles,
                        imageBase64 = imageBase64
                    ))
                    Toast.makeText(context, "已保存：${finalTickets.size}张标签", Toast.LENGTH_SHORT).show()
                    
                    rowList.clear()
                    styleNo = ""
                    customer = ""
                    imageBase64 = null
                } else {
                    Toast.makeText(context, "没有有效件数", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) { Text("生成并保存裁床单", fontSize = 16.sp) }
        
        Spacer(Modifier.height(40.dp))
    }

    if (showSelectSizeDialog) {
        AlertDialog(
            onDismissRequest = { showSelectSizeDialog = false },
            title = { Text("选择填写的码数") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("已选(按顺序，可重复):", fontSize = 12.sp, color = Color.Gray)
                    Row(Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tempSelectedSizes.forEachIndexed { idx, s ->
                            Row(Modifier.background(Color.White, MaterialTheme.shapes.small).border(1.dp, Color.Gray, MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(s, fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("x", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                    tempSelectedSizes = tempSelectedSizes.toMutableList().also { it.removeAt(idx) }
                                })
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    Text("点击下方码数即可添加到上方：", fontSize = 12.sp, color = Color.Gray)
                    presetSizes.value.forEach { size ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                            tempSelectedSizes = tempSelectedSizes + size
                        }.padding(vertical = 8.dp)) {
                            Text("+ ", fontSize = 16.sp, color = Color(0xFF1565C0))
                            Text(size, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = newSizeText, onValueChange = { newSizeText = it.uppercase() }, modifier = Modifier.weight(1f), placeholder = { Text("新码数") }, singleLine = true)
                        Button(onClick = {
                            if (newSizeText.isNotBlank()) {
                                val s = newSizeText.trim()
                                if (!presetSizes.value.contains(s)) {
                                    presetSizes.value.add(s)
                                    prefs.edit().putStringSet("sizes", presetSizes.value.toSet()).apply()
                                }
                                tempSelectedSizes = tempSelectedSizes + s
                                newSizeText = ""
                            }
                        }, modifier = Modifier.padding(start = 8.dp)) { Text("添加") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedSizes.value = tempSelectedSizes; showSelectSizeDialog = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showSelectSizeDialog = false }) { Text("取消") } }
        )
    }

    if (showSelectColorDialog) {
        AlertDialog(
            onDismissRequest = { showSelectColorDialog = false },
            title = { Text("选择颜色") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("点击颜色即可添加一行：", fontSize = 12.sp, color = Color.Gray)
                    presetColors.value.forEach { color ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                            rowList.add(RowInputData(color = color))
                            if (!selectedColorsOrder.contains(color)) selectedColorsOrder.add(color)
                            showSelectColorDialog = false
                        }.padding(vertical = 12.dp)) {
                            Text("+ ", fontSize = 16.sp, color = Color(0xFF1565C0))
                            Text(color, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = newColorInput, onValueChange = { newColorInput = it }, modifier = Modifier.weight(1f), placeholder = { Text("新颜色(如藏青)") }, singleLine = true)
                        Button(onClick = {
                            if (newColorInput.isNotBlank()) {
                                val c = newColorInput.trim()
                                if (!presetColors.value.contains(c)) {
                                    presetColors.value.add(c)
                                    prefs.edit().putStringSet("colors", presetColors.value.toSet()).apply()
                                }
                                rowList.add(RowInputData(color = c))
                                if (!selectedColorsOrder.contains(c)) selectedColorsOrder.add(c)
                                newColorInput = ""
                                showSelectColorDialog = false
                            }
                        }, modifier = Modifier.padding(start = 8.dp)) { Text("添加并加入") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSelectColorDialog = false }) { Text("关闭") } }
        )
    }
}

// ================= 历史记录与裁床单详情 =================
@Composable
fun HistoryListScreen(historyOrders: List<CutOrderData>, onDelete: (Int) -> Unit) {
    if (historyOrders.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无历史数据", color = Color.Gray) }
        return
    }
    var selectedOrder by remember { mutableStateOf<CutOrderData?>(null) }
    var orderToDelete by remember { mutableStateOf<Pair<Int, CutOrderData>?>(null) }

    if (selectedOrder != null) {
        DetailScreen(order = selectedOrder!!) { selectedOrder = null }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(historyOrders.size) { index ->
                val order = historyOrders[index]
                Card(onClick = { selectedOrder = order }, modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("款号: ${order.styleNo}  床号: ${order.bedNo}", fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${order.totalQty} 件", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(12.dp))
                                Text("删除", color = Color.Red, fontSize = 13.sp, modifier = Modifier.clickable { orderToDelete = Pair(index, order) })
                            }
                        }
                        Text("客户: ${order.customer.ifBlank { "无" }}  |  日期: ${order.dateStr}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }

    if (orderToDelete != null) {
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text("删除确认") },
            text = { Text("确定删除裁床单【${orderToDelete!!.second.styleNo}】吗？") },
            confirmButton = { TextButton(onClick = { onDelete(orderToDelete!!.first); orderToDelete = null }) { Text("删除", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { orderToDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
fun DetailScreen(order: CutOrderData, onBack: () -> Unit) {
    var showPreview by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }

    if (showPreview) {
        TicketPreviewScreen(order.tickets, order) { showPreview = false }
        return
    }
    
    if (showImageDialog && order.imageBase64 != null) {
        ImageViewerDialog(order.imageBase64) { showImageDialog = false }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< 返回列表") }
            Text("裁床单明细", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(60.dp))
        }

        Spacer(Modifier.height(16.dp))

        Text("${order.dateStr}    款号: ${order.styleNo}    床号: ${order.bedNo}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        
        if (order.imageBase64 != null) {
            Button(
                onClick = { showImageDialog = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD), contentColor = Color(0xFF1565C0))
            ) { Text("📷 查看此单关联的图片") }
        }

        Text("💡 表格如果没显示全，可以左右滑动查看", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
        TableView(order = order)

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { showPreview = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) { Text("预览并打印标签", fontSize = 16.sp) }
        
        Spacer(Modifier.height(10.dp))
        Text("注：以上数据包含 ${order.tickets.size} 张标签，总计 ${order.totalQty} 件", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ImageViewerDialog(base64Str: String, onDismiss: () -> Unit) {
    val bitmap = decodeBase64ToBitmap(base64Str)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("裁床图片") },
        text = {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "大图",
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("
