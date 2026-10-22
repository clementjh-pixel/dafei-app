package com.example.dafei

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

// 数据模型
data class ColorRowData(val color: String, val s: String, val m: String, val l: String, val xl: String)
data class TicketData(
    val ticketNo: String,
    val cutId: String,
    val customer: String,
    val styleNo: String,
    val color: String,
    val size: String,
    val quantity: Int
)
data class CutOrderData(
    val id: String,
    val customer: String,
    val styleNo: String,
    val time: Long,
    val totalQty: Int,
    val tickets: List<TicketData>
)

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
fun MainAppScreen(
    connectedDeviceName: String?,
    onConnectClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as MainActivity
    
    var currentTab by remember { mutableIntStateOf(0) } // 0:打菲 1:历史
    val historyOrders = remember { mutableStateListOf<CutOrderData>() }
    
    var showBluetoothDialog by remember { mutableStateOf(false) }
    val pairedDevices = remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentTab == 0) "打菲助手" else "裁床单记录") },
                actions = {
                    TextButton(onClick = {
                        if (activity.getPairedDevices().isEmpty()) {
                            Toast.makeText(context, "请先在手机设置里配对蓝牙打印机", Toast.LENGTH_LONG).show()
                        } else {
                            pairedDevices.value = activity.getPairedDevices()
                            showBluetoothDialog = true
                        }
                    }) {
                        Text(text = connectedDeviceName ?: "未连接打印机", color = Color.White)
                    }
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
                HistoryScreen(historyOrders) { tickets ->
                    if (BluetoothPrinter.isConnected()) {
                        tickets.forEach { BluetoothPrinter.printTicket(it.customer, it.styleNo, it.ticketNo, it.color, it.size, it.quantity) }
                        Toast.makeText(context, "正在打印 ${tickets.size} 张菲票...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "请先连接打印机", Toast.LENGTH_SHORT).show()
                    }
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

@Composable
fun GenerateScreen(onGenerate: (CutOrderData) -> Unit) {
    val context = LocalContext.current
    var customer by remember { mutableStateOf("") }
    var styleNo by remember { mutableStateOf("A100") }
    
    // 颜色列表状态
    val colorRows = remember { mutableStateListOf(ColorRowData("黑色", "", "", "", "")) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = customer, onValueChange = { customer = it }, label = { Text("客户名称") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = styleNo, onValueChange = { styleNo = it }, label = { Text("款号") }, modifier = Modifier.fillMaxWidth())
        
        Spacer(Modifier.height(16.dp))
        Text("颜色 / 件数", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        
        colorRows.forEachIndexed { index, row ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))) {
                Column(Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = row.color, 
                            onValueChange = { colorRows[index] = row.copy(color = it) }, 
                            label = { Text("颜色") }, 
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { colorRows.removeAt(index) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))) {
                            Text("删")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("S", "M", "L", "XL").forEach { size ->
                            OutlinedTextField(
                                value = when(size) { "S" -> row.s; "M" -> row.m; "L" -> row.l; else -> row.xl },
                                onValueChange = { newVal ->
                                    // 右侧自动填充逻辑
                                    val updated = when(size) {
                                        "S" -> row.copy(s = newVal, m = newVal, l = newVal, xl = newVal)
                                        "M" -> row.copy(m = newVal, l = newVal, xl = newVal)
                                        "L" -> row.copy(l = newVal, xl = newVal)
                                        else -> row.copy(xl = newVal)
                                    }
                                    colorRows[index] = updated
                                },
                                label = { Text(size) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        
        Button(onClick = { colorRows.add(ColorRowData("", "", "", "", "")) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)) {
            Text("+ 添加颜色")
        }
        
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (styleNo.isBlank()) { Toast.makeText(context, "款号不能为空", Toast.LENGTH_SHORT).show(); return@Button }
                val validRows = colorRows.filter { it.color.isNotBlank() && (it.s.isNotBlank() || it.m.isNotBlank() || it.l.isNotBlank() || it.xl.isNotBlank()) }
                if (validRows.isEmpty()) { Toast.makeText(context, "请填写有效件数", Toast.LENGTH_SHORT).show(); return@Button }
                
                val cutId = "C" + System.currentTimeMillis()
                val newTickets = mutableListOf<TicketData>()
                var seq = 1
                
                // 按尺码顺序 S->M->L->XL 生成菲号
                listOf("S", "M", "L", "XL").forEach { size ->
                    validRows.forEach { row ->
                        val qtyStr = when(size) { "S" -> row.s; "M" -> row.m; "L" -> row.l; else -> row.xl }
                        val qty = qtyStr.toIntOrNull() ?: 0
                        if (qty > 0) {
                            val no = "$styleNo-$size-" + String.format("%02d", seq++)
                            newTickets.add(TicketData(no, cutId, customer, styleNo, row.color, size, qty))
                        }
                    }
                }
                
                if (newTickets.isNotEmpty()) {
                    val totalQty = newTickets.sumOf { it.quantity }
                    onGenerate(CutOrderData(cutId, customer, styleNo, System.currentTimeMillis(), totalQty, newTickets))
                    Toast.makeText(context, "生成成功，共 ${newTickets.size} 张菲票", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) {
            Text("生成菲票并保存裁床单", fontSize = 16.sp)
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun HistoryScreen(historyOrders: List<CutOrderData>, onPrintClick: (List<TicketData>) -> Unit) {
    if (historyOrders.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无裁床单记录", color = Color.Gray) }
        return
    }
    
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(historyOrders) { order ->
            var expanded by remember { mutableStateOf(false) }
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(order.time))
            
            Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(order.styleNo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${order.tickets.size} 张", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                    }
                    Text("客户：${order.customer.ifBlank { "未填" }}", fontSize = 14.sp)
                    Text("时间：$dateStr · 共 ${order.totalQty} 件", fontSize = 12.sp, color = Color.Gray)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { expanded = !expanded }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)) {
                            Text(if (expanded) "收起明细" else "查看明细")
                        }
                        Button(onClick = { onPrintClick(order.tickets) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                            Text("打印本单")
                        }
                    }
                    
                    if (expanded) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        order.tickets.forEach { t ->
                            Text("${t.ticketNo}  |  ${t.color}  |  ${t.size}码  |  ${t.quantity}件", modifier = Modifier.padding(vertical = 4.dp), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
