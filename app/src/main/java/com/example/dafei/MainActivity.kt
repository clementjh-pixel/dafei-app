import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var connectedDeviceName by mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            Toast.makeText(this, "权限已获取", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermissions()
        
        setContent {
            MaterialTheme {
                MainScreen(
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
    private fun showDeviceListDialog() {
        val pairedDevices = bluetoothAdapter?.bondedDevices
        if (pairedDevices.isNullOrEmpty()) {
            Toast.makeText(this, "请在手机系统设置中先配对蓝牙打印机", Toast.LENGTH_LONG).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("选择打印机")
        val deviceNames = pairedDevices.map { it.name ?: "未知设备" }.toTypedArray()
        builder.setItems(deviceNames) { _, which ->
            val device: BluetoothDevice = pairedDevices.toList()[which]
            if (BluetoothPrinter.connect(device)) {
                connectedDeviceName = device.name
                Toast.makeText(this, "连接成功: ${device.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show()
            }
        }
        builder.show()
    }
}

data class TicketData(
    val ticketNo: String,
    val styleNo: String,
    val customer: String,
    val color: String,
    val size: String,
    val quantity: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    connectedDeviceName: String?,
    onConnectClick: () -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<TicketData>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentTab == 0) "打菲助手" else "历史记录") },
                actions = {
                    TextButton(onClick = onConnectClick) {
                        Text(
                            text = connectedDeviceName ?: "连接打印机",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    label = { Text("打菲") },
                    icon = { Text("1") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    label = { Text("记录") },
                    icon = { Text("2") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (currentTab == 0) {
                GenerateScreen { newTickets ->
                    history.addAll(newTickets)
                    currentTab = 1
                }
            } else {
                HistoryScreen(history)
            }
        }
    }
}

@Composable
fun GenerateScreen(onGenerate: (List<TicketData>) -> Unit) {
    var customer by remember { mutableStateOf("") }
    var styleNo by remember { mutableStateOf("A100") }
    var color by remember { mutableStateOf("黑色") }
    var s by remember { mutableStateOf("") }
    var m by remember { mutableStateOf("") }
    var l by remember { mutableStateOf("") }
    var xl by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = customer, onValueChange = { customer = it }, label = { Text("客户") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = styleNo, onValueChange = { styleNo = it }, label = { Text("款号") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("颜色") }, modifier = Modifier.fillMaxWidth())
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = s, onValueChange = { s = it }, label = { Text("S") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = m, onValueChange = { m = it }, label = { Text("M") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = l, onValueChange = { l = it }, label = { Text("L") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = xl, onValueChange = { xl = it }, label = { Text("XL") }, modifier = Modifier.weight(1f))
        }

        Button(
            onClick = {
                val list = mutableListOf<TicketData>()
                var seq = 1
                val sizes = mapOf("S" to s.toIntOrNull(), "M" to m.toIntOrNull(), "L" to l.toIntOrNull(), "XL" to xl.toIntOrNull())
                sizes.forEach { (size, qty) ->
                    if (qty != null && qty > 0) {
                        val no = "$styleNo-$size-" + String.format("%02d", seq++)
                        list.add(TicketData(no, styleNo, customer, color, size, qty))
                    }
                }
                if (list.isNotEmpty()) {
                    onGenerate(list)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("生成并去打印")
        }
    }
}

@Composable
fun HistoryScreen(history: List<TicketData>) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无菲票")
        }
        return
    }
    
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(history) { ticket ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("菲号: ${ticket.ticketNo}", style = MaterialTheme.typography.titleMedium)
                    Text("客户: ${ticket.customer} | 款号: ${ticket.styleNo}")
                    Text("颜色: ${ticket.color} | 尺码: ${ticket.size} | 件数: ${ticket.quantity}")
                    
                    Button(
                        onClick = {
                            if (BluetoothPrinter.isConnected()) {
                                BluetoothPrinter.printTicket(
                                    ticket.customer, ticket.styleNo, ticket.ticketNo,
                                    ticket.color, ticket.size, ticket.quantity
                                )
                            } else {
                                Toast.makeText(null, "请先连接打印机", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("打印这张")
                    }
                }
            }
        }
    }
}