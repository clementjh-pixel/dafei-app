import android.annotation.SuppressLint
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

    fun isConnected(): Boolean = socket?.isConnected == true

    // 发送 TSPL 指令打印 40x80mm 的标签
    fun printTicket(
        customer: String,
        styleNo: String,
        ticketNo: String,
        color: String,
        size: String,
        qty: Int,
        bedNo: String,  // 新增床号
        dateStr: String // 新增日期
    ) {
        if (outputStream == null) return

        val command = "SIZE 40 mm, 80 mm\r\n" +
                "GAP 2 mm, 0 mm\r\n" +
                "DIRECTION 1\r\n" +
                "CLS\r\n" +
                // 第一行：客户与日期
                "TEXT 10, 10, \"TSS24.BF2\", 0, 1, 1, \"客: " + customer + "\"\r\n" +
                "TEXT 230, 10, \"TSS24.BF2\", 0, 1, 1, \"" + dateStr + "\"\r\n" +
                // 第二行：款号与床号
                "TEXT 10, 45, \"TSS24.BF2\", 0, 1, 1, \"款: " + styleNo + "\"\r\n" +
                "TEXT 230, 45, \"TSS24.BF2\", 0, 1, 1, \"床: " + bedNo + "\"\r\n" +
                // 第三行：大号菲号
                "TEXT 10, 90, \"TSS24.BF2\", 0, 2, 2, \"菲号: " + ticketNo + "\"\r\n" +
                // 第四行：颜色与码数
                "TEXT 10, 150, \"TSS24.BF2\", 0, 1, 1, \"颜色: " + color + "\"\r\n" +
                "TEXT 230, 150, \"TSS24.BF2\", 0, 1, 1, \"码: " + size + "\"\r\n" +
                // 第五行：件数（加粗）
                "TEXT 10, 190, \"TSS24.BF2\", 0, 1, 1, \"件数: " + qty + "件\"\r\n" +
                "PRINT 1, 1\r\n"

        try {
            outputStream?.write(command.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
