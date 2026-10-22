package com.example.dafei

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
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
    val matrix: Map<String, Map<String, Int>>
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
    val prefs = remember { context.getSharedPreferences("dafei_prefs", Context.MODE_PRIVATE) }

    var customer by remember { mutableStateOf("") }
    var styleNo by remember { mutableStateOf("") }
    var bedNo by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(SimpleDateFormat("yy-MM-dd", Locale.CHINA).format(Date())) }

    var currentSizes by remember { mutableStateOf(listOf("S", "M", "L", "XL")) }
    var showAddSizeDialog by remember { mutableStateOf(false) }
    var newSizeText by remember { mutableStateOf("") }

    // 这里修正了 val 的类型
    val presetColors = remember { mutableStateOf(prefs.getStringSet("colors", setOf("黑色", "白色", "杏色"))?.toMutableList() ?: mutableListOf()) }
    var showAddColorDialog by remember { mutableStateOf(false) }
    var newColorText by remember { mutableStateOf("") }

    var showSelectColorDialog by remember { mutableStateOf(false) }
    var colorToReplace by remember { mutableStateOf<String?>(null) }

    val rowData = remember { mutableStateMapOf<String, MutableMap<String, String>>() }

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
                TextButton(onClick = { showAddColorDialog = true }) { Text("+ 预设颜色") }
            }
        }
        
        Button(onClick = { 
            var emptyColorRow = ""
            var i = 1
            while(rowData.containsKey(emptyColorRow)) { emptyColorRow = "新颜色$i"; i++ }
            rowData[emptyColorRow] = mutableMapOf() 
        }, modifier = Modifier.fillMaxWidth().height(40.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)) {
            Text("+ 添加一行颜色")
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth().background(Color(0xFFF0F0F0)).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("颜色(点击选)", modifier = Modifier.weight(1f).padding(start = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            currentSizes.forEach { size ->
                Text(size, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(36.dp))
        }

        rowData.keys.toList().forEach { colorKey ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.weight(1f).height(50.dp).border(1.dp, Color.Gray, MaterialTheme.shapes.extraSmall)
                        .clickable {
                            colorToReplace = colorKey
                            showSelectColorDialog = true
                        }.padding(4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(if (colorKey.startsWith("新颜色")) "点击选择" else colorKey, fontSize = 14.sp, color = if (colorKey.startsWith("新颜色")) Color.Gray else Color.Black)
                }

                currentSizes.forEach { size ->
                    OutlinedTextField(
                        value = rowData[colorKey]?.get(size) ?: "",
                        onValueChange = { valMap -> rowData[colorKey]?.set(size, valMap) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp).height(50.dp),
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
                        val actualColor = if (color.startsWith("新颜色")) "未命名" else color
                        if (actualColor.isNotBlank() && qty > 0) {
                            val no = "$styleNo-$size-" + String.format("%02d", seq++)
                            newTickets.add(TicketData(no, cutId, customer, styleNo, bedNo, dateStr, actualColor, size, qty))
                            totalQty += qty
                            
                            val colorMatrix = matrix.getOrPut(actualColor) { mutableMapOf() }
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

    if (showAddSizeDialog) {
        AlertDialog(
            onDismissRequest = { showAddSizeDialog = false },
            title = { Text("添加码数") },
            text = { OutlinedTextField(value = newSizeText, onValueChange = { newSizeText = it.uppercase() }, label = { Text("码数 (如 2XL)") }) },
            confirmButton = {
                TextButton(onClick = {
                    if (newSizeText.isNotBlank() && !currentSizes.contains(newSizeText)) currentSizes = currentSizes + newSizeText
                    newSizeText = ""
                    showAddSizeDialog = false
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { showAddSizeDialog = false }) { Text("取消") } }
        )
    }

    if (showAddColorDialog) {
        AlertDialog(
            onDismissRequest = { showAddColorDialog = false },
            title = { Text("添加预设颜色") },
            text = { OutlinedTextField(value = newColorText, onValueChange = { newColorText = it }, label = { Text("如: 藏青") }) },
            confirmButton = {
                TextButton(onClick = {
                    if (newColorText.isNotBlank() && !presetColors.value.contains(newColorText)) {
                        presetColors.value.add(newColorText)
                        prefs.edit().putStringSet("colors", presetColors.value.toSet()).apply()
                    }
                    newColorText = ""
                    showAddColorDialog = false
                }) { Text("存入预设") }
            },
            dismissButton = { TextButton(onClick = { showAddColorDialog = false }) { Text("取消") } }
        )
    }

    if (showSelectColorDialog) {
        AlertDialog(
            onDismissRequest = { showSelectColorDialog = false },
            title = { Text("选择颜色") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    presetColors.value.forEach { color ->
                        Text(
                            text = color,
                            modifier = Modifier.fillMaxWidth().clickable {
                                val original = colorToReplace
                                if (original != null) {
                                    val oldData = rowData[original]
                                    if (rowData.containsKey(color)) {
                                        Toast.makeText(context, "该颜色已存在", Toast.LENGTH_SHORT).show()
                                    } else {
                                        rowData.remove(original)
                                        rowData[color] = oldData ?: mutableMapOf()
                                    }
                                }
                                showSelectColorDialog = false
                            }.padding(vertical = 12.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSelectColorDialog = false }) { Text("取消") } }
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
    var showPreview by remember { mutableStateOf(false) }

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

    val border = BorderStroke(1.dp, Color.Black)
    val cellModifier = Modifier.border(border).padding(8.dp)

    Column(Modifier.fillMaxWidth().border(border)) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).then(cellModifier)) { Text("", fontWeight = FontWeight.Bold) }
            allSizes.forEach { size ->
                Box(Modifier.weight(1f).then(cellModifier)) { Text(size, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) }
            }
            Box(Modifier.weight(1f).then(cellModifier)) { Text("合计", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) }
        }

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

// ================= 标签预览与打印界面 (40x80 专属) =================
@Composable
fun TicketPreviewScreen(tickets: List<TicketData>, order: CutOrderData, onBack: () -> Unit) {
    val context = LocalContext.current
    var isPrinting by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFFE0E0E0))) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< 返回") }
            Text("标签预览 (共${tickets.size}张)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Button(
                onClick = {
                    if (BluetoothPrinter.isConnected()) {
                        isPrinting = true
                        tickets.forEach { 
                            BluetoothPrinter.printTicket(it.customer, it.styleNo, it.ticketNo, it.color, it.size, it.quantity, it.bedNo, it.dateStr) 
                        }
                        Toast.makeText(context, "已发送打印指令", Toast.LENGTH_LONG).show()
                        isPrinting = false
                    } else {
                        Toast.makeText(context, "请先连接蓝牙打印机", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isPrinting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) { Text(if (isPrinting) "发送中..." else "确认打印") }
        }

        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tickets) { ticket ->
                Card(
                    modifier = Modifier.fillMaxWidth(0.7f).aspectRatio(0.5f).align(Alignment.CenterHorizontally),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(ticket.customer.ifBlank { "内部单" }, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(ticket.dateStr, fontSize = 10.sp, color = Color.Gray)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("款号: ${ticket.styleNo}", fontSize = 12.sp)
                            Text("床号: ${ticket.bedNo}", fontSize = 12.sp)
                        }
                        
                        Spacer(Modifier.weight(1f))
                        
                        Text(ticket.ticketNo, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                        
                        Spacer(Modifier.weight(1f))
                        
                        HorizontalDivider(modifier = Modifier.padding(bottom = 6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text("${ticket.color} / ${ticket.size}码", fontSize = 13.sp)
                            Text("${ticket.quantity}件", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
