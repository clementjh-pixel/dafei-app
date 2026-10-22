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

    fun printTicket(
        customer: String,
        styleNo: String,
        ticketNo: String,
        color: String,
        size: String,
        qty: Int,
        bedNo: String,
        dateStr: String,
        bundleNo: Int,         // 全局扎号
        totalBundles: Int,     // 总扎数
        totalQuantity: Int     // 总件数
    ) {
        if (outputStream == null) return

        // 40x80mm TSPL 指令
        val command = "SIZE 40 mm, 80 mm\r\n" +
                "GAP 2 mm, 0 mm\r\n" +
                "DIRECTION 1\r\n" +
                "CLS\r\n" +
                // 第一行：款号
                "TEXT 10, 15, \"TSS24.BF2\", 0, 1, 1, \"款号: " + styleNo + "\"\r\n" +
                // 第二行：扎号
                "TEXT 10, 55, \"TSS24.BF2\", 0, 1, 1, \"扎号: " + String.format("%02d", bundleNo) + "\"\r\n" +
                // 第三行：码数
                "TEXT 10, 95, \"TSS24.BF2\", 0, 1, 1, \"码数: " + size + "\"\r\n" +
                // 第四行：数量
                "TEXT 10, 135, \"TSS24.BF2\", 0, 1, 1, \"数量: " + qty + "\"\r\n" +
                // 虚线分割
                "TEXT 10, 175, \"TSS16.BF2\", 0, 1, 1, \"----------------------\"\r\n" +
                // 第五行：总共扎数、总件数（小字体）
                "TEXT 10, 215, \"TSS16.BF2\", 0, 1, 1, \"共 " + totalBundles + " 扎, 总 " + totalQuantity + " 件\"\r\n" +
                // 第六行：客户、日期（小字体）
                "TEXT 10, 250, \"TSS16.BF2\", 0, 1, 1, \"" + customer + "  " + dateStr + "\"\r\n" +
                "PRINT 1, 1\r\n"

        try {
            outputStream?.write(command.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
