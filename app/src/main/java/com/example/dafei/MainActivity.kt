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
    val rawRows: List<RowInputData>, // 保存原始输入行，保证颜色不合并
    val rowBundles: Map<Int, List<Int>> // 每一行对应的扎号
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

    val presetSizes = remember { mutableStateOf(prefs.getStringSet("sizes", setOf("S", "M", "L", "XL"))?.toMutableList() ?: mutableListOf()) }
    val presetColors = remember { mutableStateOf(prefs.getStringSet("colors", setOf("黑色", "白色", "杏色"))?.toMutableList() ?: mutableListOf()) }

    val selectedSizes = remember { mutableStateOf(listOf("S", "M", "L", "XL")) }
    val rowList = remember { mutableStateListOf<RowInputData>() }

    var showAddSizeDialog by remember { mutableStateOf(false) }
    var newSizeText by remember { mutableStateOf("") }
    
    var showSelectSizeDialog by remember { mutableStateOf(false) }
    var tempSelectedSizes by remember { mutableStateOf(listOf<String>()) }
    
    var showSelectColorDialog by remember { mutableStateOf(false) }
    var rowIndexToReplaceColor by remember { mutableStateOf<Int?>(null) }
    var newColorInput by remember { mutableStateOf("") }

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
            TextButton(onClick = { 
                tempSelectedSizes = selectedSizes.value.toList()
                showSelectSizeDialog = true 
            }) { Text("+ 选择码数") }
        }

        Spacer(Modifier.height(8.dp))

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
                        modifier = Modifier.weight(1.5f).height(50.dp).border(1.dp, Color.Gray, MaterialTheme.shapes.extraSmall)
                            .clickable {
                                rowIndexToReplaceColor = index
                                newColorInput = ""
                                showSelectColorDialog = true
                            }.padding(4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(if (row.color.isEmpty()) "选择" else row.color, fontSize = 14.sp, color = if (row.color.isEmpty()) Color.Gray else Color.Black)
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

        Button(
            onClick = { 
                rowList.add(RowInputData())
                rowIndexToReplaceColor = rowList.size - 1
                newColorInput = ""
                showSelectColorDialog = true 
            }, 
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(40.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)
        ) {
            Text("+ 添加一行颜色")
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (styleNo.isBlank()) { Toast.makeText(context, "款号不能为空", Toast.LENGTH_SHORT).show(); return@Button }
                
                val cutId = "C" + System.currentTimeMillis()
                val newTickets = mutableListOf<TicketData>()
                var totalQty = 0
                
                // 全局扎号，从1开始，遇到空/0跳过
                var globalBundleNo = 1
                val rowBundles = mutableMapOf<Int, MutableList<Int>>()

                selectedSizes.value.forEachIndexed { colIndex, size ->
                    rowList.forEachIndexed { rowIdx, row ->
                        val qty = row.quantities["$size-$colIndex"]?.toIntOrNull() ?: 0
                        if (row.color.isNotBlank() && qty > 0) {
                            // 记录行对应的扎号
                            rowBundles.getOrPut(rowIdx) { mutableListOf() }.add(globalBundleNo)

                            val no = "$styleNo-$size-" + String.format("%02d", globalBundleNo)
                            newTickets.add(TicketData(
                                ticketNo = no, cutId = cutId, customer = customer, styleNo = styleNo,
                                bedNo = bedNo, dateStr = dateStr, color = row.color, size = size,
                                quantity = qty, bundleNo = globalBundleNo
                            ))
                            totalQty += qty
                            
                            globalBundleNo++ // 只有成功生成扎才顺移
                        }
                    }
                }

                if (newTickets.isNotEmpty()) {
                    val totalBundles = globalBundleNo - 1
                    val finalTickets = newTickets.map { it.copy(totalBundles = totalBundles, totalQuantity = totalQty) }
                    // 保存原始数据，防止后续修改影响历史单
                    val finalRawRows = rowList.map { it.copy(quantities = it.quantities.toMutableMap()) }
                    
                    onGenerate(CutOrderData(
                        id = cutId, customer = customer, styleNo = styleNo, bedNo = bedNo, dateStr = dateStr,
                        time = System.currentTimeMillis(), totalQty = totalQty, tickets = finalTickets,
                        sizesOrder = selectedSizes.value, rawRows = finalRawRows, rowBundles = rowBundles
                    ))
                    Toast.makeText(context, "已保存：${finalTickets.size}张标签", Toast.LENGTH_SHORT).show()
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
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(8.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tempSelectedSizes.forEachIndexed { idx, s ->
                            Row(
                                Modifier.background(Color.White, MaterialTheme.shapes.small).border(1.dp, Color.Gray, MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier.fillMaxWidth().clickable {
                                tempSelectedSizes = tempSelectedSizes + size
                            }.padding(vertical = 8.dp)
                        ) {
                            Text("+ ", fontSize = 16.sp, color = Color(0xFF1565C0))
                            Text(size, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("添加新码数到列表：", fontSize = 12.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSizeText, 
                            onValueChange = { newSizeText = it.uppercase() }, 
                            modifier = Modifier.weight(1f), 
                            placeholder = { Text("如 XXL") },
                            singleLine = true
                        )
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
            confirmButton = {
                TextButton(onClick = {
                    selectedSizes.value = tempSelectedSizes
                    showSelectSizeDialog = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showSelectSizeDialog = false }) { Text("取消") } }
        )
    }

    if (showSelectColorDialog) {
        AlertDialog(
            onDismissRequest = { 
                val index = rowIndexToReplaceColor
                if (index != null && index in rowList.indices && rowList[index].color.isEmpty()) {
                    rowList.removeAt(index)
                }
                showSelectColorDialog = false 
            },
            title = { Text("选择颜色") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    presetColors.value.forEach { color ->
                        Text(
                            text = color,
                            modifier = Modifier.fillMaxWidth().clickable {
                                val index = rowIndexToReplaceColor
                                if (index != null && index in rowList.indices) {
                                    rowList[index] = rowList[index].copy(color = color)
                                }
                                showSelectColorDialog = false
                            }.padding(vertical = 12.dp),
                            fontSize = 16.sp
                        )
                    }
                    
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("添加新预设颜色：", fontSize = 12.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newColorInput, 
                            onValueChange = { newColorInput = it }, 
                            modifier = Modifier.weight(1f), 
                            placeholder = { Text("如 藏青") },
                            singleLine = true
                        )
                        Button(onClick = {
                            if (newColorInput.isNotBlank()) {
                                val c = newColorInput.trim()
                                if (!presetColors.value.contains(c)) {
                                    presetColors.value.add(c)
                                    prefs.edit().putStringSet("colors", presetColors.value.toSet()).apply()
                                }
                                val index = rowIndexToReplaceColor
                                if (index != null && index in rowList.indices) {
                                    rowList[index] = rowList[index].copy(color = c)
                                }
                                newColorInput = ""
                                showSelectColorDialog = false
                            }
                        }, modifier = Modifier.padding(start = 8.dp)) { Text("添加并填入") }
                    }
                }
            },
            confirmButton = { 
                TextButton(onClick = { 
                    val index = rowIndexToReplaceColor
                    if (index != null && index in rowList.indices && rowList[index].color.isEmpty()) {
                        rowList.removeAt(index)
                    }
                    showSelectColorDialog = false 
                }) { Text("取消") } 
            }
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
fun TableView(order: CutOrderData) {
    val sizeTotals = mutableMapOf<Int, Int>()
    val border = BorderStroke(1.dp, Color.Black)
    val cellModifier = Modifier.border(border).padding(8.dp)

    Column(Modifier.fillMaxWidth().border(border)) {
        // 表头
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1.2f).then(cellModifier)) { Text("颜色", fontWeight = FontWeight.Bold) }
            Box(Modifier.weight(1.5f).then(cellModifier)) { Text("扎号", fontWeight = FontWeight.Bold) }
            order.sizesOrder.forEachIndexed { colIndex, size ->
                Box(Modifier.weight(1f).then(cellModifier)) { Text(size, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) }
            }
            Box(Modifier.weight(1f).then(cellModifier)) { Text("合计", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) }
        }

        // 数据行（按输入行展示，不合并颜色）
        order.rawRows.forEachIndexed { rowIdx, row ->
            var rowTotal = 0
            val bundles = order.rowBundles[rowIdx] ?: emptyList()
            val bundleStr = bundles.joinToString(",") { String.format("%02d", it) }

            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1.2f).then(cellModifier)) { Text(row.color) }
                Box(Modifier.weight(1.5f).then(cellModifier)) { Text(bundleStr, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold) }
                order.sizesOrder.forEachIndexed { colIndex, _ ->
                    val qty = row.quantities["${order.sizesOrder[colIndex]}-$colIndex"]?.toIntOrNull() ?: 0
                    rowTotal += qty
                    sizeTotals[colIndex] = (sizeTotals[colIndex] ?: 0) + qty
                    Box(Modifier.weight(1f).then(cellModifier)) { Text(if (qty > 0) qty.toString() else "", modifier = Modifier.align(Alignment.Center)) }
                }
                Box(Modifier.weight(1f).then(cellModifier)) { Text("${rowTotal}件", modifier = Modifier.align(Alignment.Center)) }
            }
        }

        // 合计行
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1.2f).then(cellModifier)) { Text("合计", fontWeight = FontWeight.Bold) }
            Box(Modifier.weight(1.5f).then(cellModifier)) { Text("", fontWeight = FontWeight.Bold) }
            var grandTotal = 0
            order.sizesOrder.forEachIndexed { colIndex, _ ->
                val st = sizeTotals[colIndex] ?: 0
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
        Row(Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< 返回") }
            Text("标签预览 (共${tickets.size}张)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Button(
                onClick = {
                    if (BluetoothPrinter.isConnected()) {
                        isPrinting = true
                        tickets.forEach { 
                            BluetoothPrinter.printTicket(
                                it.customer, it.styleNo, it.ticketNo, it.color, it.size, 
                                it.quantity, it.bedNo, it.dateStr, it.bundleNo, it.totalBundles, it.totalQuantity
                            ) 
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
                        // 1. 款号
                        Text("款号: ${ticket.styleNo}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        // 2. 扎号
                        Text("扎号: ${String.format("%02d", ticket.bundleNo)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=4.dp))
                        // 3. 码数
                        Text("码数: ${ticket.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=4.dp))
                        // 4. 数量
                        Text("数量: ${ticket.quantity}", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=4.dp))
                        
                        // 虚线分割
                        Text("------------------------", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 6.dp))
                        
                        // 5. 总扎/总件
                        Text("共 ${ticket.totalBundles} 扎, 总 ${ticket.totalQuantity} 件", fontSize = 11.sp, color = Color.DarkGray)
                        // 6. 客户/日期
                        Text("${ticket.customer}  ${ticket.dateStr}", fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}
