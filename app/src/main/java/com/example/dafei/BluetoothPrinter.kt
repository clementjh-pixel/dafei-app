import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.OutputStream
import java.util.UUID

object BluetoothPrinter {
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            outputStream = socket?.outputStream
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isConnected(): Boolean = socket?.isConnected == true

    // 发送 TSPL 指令打印 40x100mm 的标签
    fun printTicket(
        customer: String,
        styleNo: String,
        ticketNo: String,
        color: String,
        size: String,
        qty: Int
    ) {
        if (outputStream == null) return

        // TSPL 指令拼接 (参数：X坐标, Y坐标, 字体大小, 旋转, 粗细, 文字)
        val command = """
            SIZE 40 mm, 100 mm
            GAP 2 mm, 0 mm
            DIRECTION 1
            CLS
            
            TEXT 20, 20, "TSS24.BF2", 0, 1, 1, "客户: $customer"
            TEXT 20, 60, "TSS24.BF2", 0, 1, 1, "款号: $styleNo"
            TEXT 20, 100, "TSS24.BF2", 0, 1, 1, "颜色: $color"
            TEXT 20, 140, "TSS24.BF2", 0, 1, 1, "尺码: $size"
            
            TEXT 20, 200, "TSS24.BF2", 0, 2, 2, "菲号: $ticketNo"
            TEXT 20, 260, "TSS24.BF2", 0, 1, 1, "件数: ${qty}件"
            
            PRINT 1, 1
        """.trimIndent() + "\r\n"

        try {
            outputStream?.write(command.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}