package com.example.dafei

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val quantity: Int
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
    val matrix: Map<String, Map<String, Int>> // 颜色 -> 码数 -> 件数
)

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
                MainAppScreen(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(connectedDeviceName: String?, onConnectClick: () -> Unit) {
    val context = LocalContext.current
    val activity = context as MainActivity
    
    var currentTab by remember { mutableIntStateOf(0) }
    val historyOrders = remember { mutableStateListOf<CutOrderData>() }
    
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
                GenerateScreen { newOrder ->
                    historyOrders.add(0, newOrder)
                    currentTab = 1
                }
            } else {
                HistoryListScreen(historyOrders)
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
fun GenerateScreen(onGenerate: (CutOrderData) -> Unit) {
    val context = LocalContext.current
    var customer by remember { mutableStateOf("") }
    var styleNo by remember { mutableStateOf("") }
    var bedNo by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(SimpleDateFormat("yy 年 MM 月 dd 日", Locale.CHINA).format(Date())) }

    // 动态码数
    var currentSizes by remember { mutableStateOf(listOf("S", "M", "L", "XL")) }
    var showAddSizeDialog by remember { mutableStateOf(false) }
    var newSizeText by remember { mutableStateOf("") }

    // 动态颜色和件数数据：颜色名 -> (码数 -> 文本)
    val rowData = remember { mutableStateMapOf<String, MutableMap<String, String>>() }
    var newColorText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = styleNo, onValueChange = { styleNo = it }, label = { Text("款号") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = bedNo, onValueChange = { bedNo = it }, label = { Text("床号") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = customer, onValueChange = { customer = it }, label = { Text("客户") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("日期") }, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("颜色 / 件数", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row {
                TextButton(onClick = { showAddSizeDialog = true }) { Text("+ 预设码数") }
                TextButton(onClick = { rowData[""] = mutableMapOf() }) { Text("+ 添加颜色") }
            }
        }

        // 表头
        Row(Modifier.fillMaxWidth().background(Color(0xFFF0F0F0)).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("颜色", modifier = Modifier.weight(1f).padding(start = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            currentSizes.forEach { size ->
                Text(size, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(36.dp)) // 对齐删除按钮
        }

        // 数据行
        rowData.keys.toList().forEach { colorKey ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = colorKey,
                    onValueChange = { newColor ->
                        val map = rowData.remove(colorKey)
                        if (map != null) rowData[newColor] = map
                    },
                    placeholder = { Text("颜色") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                currentSizes.forEach { size ->
                    OutlinedTextField(
                        value = rowData[colorKey]?.get(size) ?: "",
                        onValueChange = { valMap -> rowData[colorKey]?.set(size, valMap) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        singleLine = true
                    )
                }
                IconButton(onClick = { rowData.remove(colorKey) }, modifier = Modifier.width(36.dp)) {
                    Text("X", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (styleNo.isBlank()) { Toast.makeText(context, "款号不能为空", Toast.LENGTH_SHORT).show(); return@Button }
                
                val cutId = "C" + System.currentTimeMillis()
                val newTickets = mutableListOf<TicketData>()
                val matrix = mutableMapOf<String, Map<String, Int>>()
                var seq = 1
                var totalQty = 0

                currentSizes.forEach { size ->
                    rowData.forEach { (color, qtyMap) ->
                        val qty = qtyMap[size]?.toIntOrNull() ?: 0
                        if (color.isNotBlank() && qty > 0) {
                            val no = "$styleNo-$size-" + String.format("%02d", seq++)
                            newTickets.add(TicketData(no, cutId, customer, styleNo, bedNo, dateStr, color, size, qty))
                            totalQty += qty
                            
                            val colorMatrix = matrix.getOrPut(color) { mutableMapOf() }
                            (colorMatrix as MutableMap)[size] = qty
                        }
                    }
                }

                if (newTickets.isNotEmpty()) {
                    onGenerate(CutOrderData(cutId, customer, styleNo, bedNo, dateStr, System.currentTimeMillis(), totalQty, newTickets, matrix))
                    Toast.makeText(context, "已保存：${newTickets.size}张标签", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "没有有效件数", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) { Text("生成并保存裁床单", fontSize = 16.sp) }
        
        Spacer(Modifier.height(40.dp))
    }

    // 自定义码数弹窗
    if (showAddSizeDialog) {
        AlertDialog(
            onDismissRequest = { showAddSizeDialog = false },
            title = { Text("添加自定义码数") },
            text = { OutlinedTextField(value = newSizeText, onValueChange = { newSizeText = it.uppercase() }, label = { Text("码数 (如 2XL)") }) },
            confirmButton = {
                TextButton(onClick = {
                    if (newSizeText.isNotBlank() && !currentSizes.contains(newSizeText)) {
                        currentSizes = currentSizes + newSizeText
                    }
                    newSizeText = ""
                    showAddSizeDialog = false
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { showAddSizeDialog = false }) { Text("取消") } }
        )
    }
}

// ================= 历史记录与裁床单详情 =================
@Composable
fun HistoryListScreen(historyOrders: List<CutOrderData>) {
    if (historyOrders.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无历史数据", color = Color.Gray) }
        return
    }
    var selectedOrder by remember { mutableStateOf<CutOrderData?>(null) }

    if (selectedOrder != null) {
        DetailScreen(order = selectedOrder!!) { selectedOrder = null }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(historyOrders) { order ->
                Card(onClick = { selectedOrder = order }, modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("款号: ${order.styleNo} / 床号: ${order.bedNo}", fontWeight = FontWeight.Bold)
                            Text("${order.totalQty} 件", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                        }
                        Text("客户: ${order.customer.ifBlank { "无" }}  |  日期: ${order.dateStr}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(order: CutOrderData, onBack: () -> Unit) {
    val context = LocalContext.current
    var showPreview by remember { mutableStateOf(false) }

    // 预览标签界面
    if (showPreview) {
        TicketPreviewScreen(order.tickets, order) { showPreview = false }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< 返回列表") }
            Text("裁床单明细", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(60.dp))
        }

        Spacer(Modifier.height(16.dp))

        // ============ 渲染你要求的表格 ============
        Text("${order.dateStr}    款号: ${order.styleNo}    床号: ${order.bedNo}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        
        TableView(matrix = order.matrix)

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
fun TableView(matrix: Map<String, Map<String, Int>>) {
    val allSizes = matrix.values.flatMap { it.keys }.distinct().sorted()
    val colorTotals = mutableMapOf<String, Int>()
    val sizeTotals = mutableMapOf<String, Int>()

    // 边框粗细和颜色
    val border = BorderStroke(1.dp, Color.Black)
    val cellModifier = Modifier.border(border).padding(8.dp)

    Column(Modifier.fillMaxWidth().border(border)) {
        // 表头行
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).then(cellModifier)) { Text("", fontWeight = FontWeight.Bold) }
            allSizes.forEach { size ->
                Box(Modifier.weight(1f).then(cellModifier)) { Text(size, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) }
            }
            Box(Modifier.weight(1f).then(cellModifier)) { Text("合计", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) }
        }

        // 数据行
        matrix.forEach { (color, qtyMap) ->
            var rowTotal = 0
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f).then(cellModifier)) { Text(color) }
                allSizes.forEach { size ->
                    val qty = qtyMap[size] ?: 0
                    rowTotal += qty
                    sizeTotals[size] = (sizeTotals[size] ?: 0) + qty
                    Box(Modifier.weight(1f).then(cellModifier)) { Text(if (qty > 0) qty.toString() else "", modifier = Modifier.align(Alignment.Center)) }
                }
                colorTotals[color] = rowTotal
                Box(Modifier.weight(1f).then(cellModifier)) { Text("${rowTotal}件", modifier = Modifier.align(Alignment.Center)) }
            }
        }

        // 底部合计行
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).then(cellModifier)) { Text("合计", fontWeight = FontWeight.Bold) }
            var grandTotal = 0
            allSizes.forEach { size ->
                val st = sizeTotals[size] ?: 0
                grandTotal += st
                Box(Modifier.weight(1f).then(cellModifier)) { Text("${st}件", modifier = Modifier.align(Alignment.Center)) }
            }
            Box(Modifier.weight(1f).then(cellModifier)) { Text("${grandTotal}件", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) }
        }
    }
}

// ================= 标签预览与打印界面 =================
@Composable
fun TicketPreviewScreen(tickets: List<TicketData>, order: CutOrderData, onBack: () -> Unit) {
    val context = LocalContext.current
    var isPrinting by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFFE0E0E0))) {
        // 顶部操作栏
        Row(Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< 返回") }
            Text("标签打印预览 (共${tickets.size}张)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Button(
                onClick = {
                    if (BluetoothPrinter.isConnected()) {
                        isPrinting = true
                        tickets.forEach { BluetoothPrinter.printTicket(it.customer, it.styleNo, it.ticketNo, it.color, it.size, it.quantity) }
                        Toast.makeText(context, "正在打印，请观察打印机...", Toast.LENGTH_LONG).show()
                        isPrinting = false
                    } else {
                        Toast.makeText(context, "请先连接蓝牙打印机", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isPrinting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) { Text(if (isPrinting) "发送中..." else "确认打印") }
        }

        // 模拟浏览器的打印预览（A4纸效果）
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 每 5 张卡片一页
            val pages = tickets.chunked(5)
            items(pages) { pageTickets ->
                Card(
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.707f), // A4 纸比例 1:1.414
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        pageTickets.forEach { ticket ->
                            // 标签卡片 40x100mm 比例（宽长比 1:2.5）
                            Box(
                                Modifier.fillMaxWidth().weight(1f).border(1.dp, Color.Black).padding(8.dp)
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(ticket.customer.ifBlank { "内部单" }, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("款: ${ticket.styleNo}", fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text("${ticket.ticketNo}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${ticket.color} / ${ticket.size}", fontSize = 14.sp)
                                        Text("${ticket.quantity}件", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Text("床号: ${ticket.bedNo} | 日期: ${ticket.dateStr}", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End).padding(top = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
