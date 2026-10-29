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

    // --- 极端测试：纯英文，强制使用字体 "1" ---
    fun printTest() {
        if (outputStream == null) return
        try {
            val testCmd = "SIZE 40 mm, 80 mm\r\n" +
                    "GAP 2 mm, 0 mm\r\n" +
                    "DIRECTION 1\r\n" +
                    "CLS\r\n" +
                    // 字体 "1" 是大多数打印机的默认内置英文字体
                    "TEXT 10, 10, \"1\", 0, 1, 1, \"TEST SUCCESS\"\r\n" +
                    "TEXT 10, 60, \"1\", 0, 1, 1, \"Hello MHT!\"\r\n" +
                    "PRINT 1, 1\r\n"
            // 测试指令用 UTF-8 或 GBK 都行，纯英文通用
            outputStream?.write(testCmd.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 正式打印标签（修正版）---
    fun printTicket(
        customer: String, styleNo: String, ticketNo: String, color: String, size: String, qty: Int,
        bedNo: String, dateStr: String, bundleNo: Int, totalBundles: Int, totalQuantity: Int
    ) {
        if (outputStream == null) return

        // 1. 使用通用字体 "1" 和 "2"
        // 2. 顶部留白 20mm（Y=160）
        val command = "\r\n\r\n" +
                "SIZE 40 mm, 80 mm\r\n" +
                "GAP 2 mm, 0 mm\r\n" +
                "DIRECTION 1\r\n" +
                "CLS\r\n" +
                "TEXT 10, 160, \"2\", 0, 1, 1, \"款号: " + styleNo + "\"\r\n" +
                "TEXT 10, 210, \"2\", 0, 1, 1, \"床号: " + bedNo + "\"\r\n" +
                "TEXT 10, 260, \"2\", 0, 1, 1, \"扎号: " + String.format("%02d", bundleNo) + "\"\r\n" +
                "TEXT 10, 310, \"2\", 0, 1, 1, \"码数: " + size + "\"\r\n" +
                "TEXT 10, 360, \"2\", 0, 1, 1, \"数量: " + qty + "\"\r\n" +
                "TEXT 10, 410, \"2\", 0, 1, 1, \"颜色: " + color + "\"\r\n" +
                "TEXT 10, 460, \"1\", 0, 1, 1, \"----------------------\"\r\n" +
                "TEXT 10, 490, \"1\", 0, 1, 1, \"共 " + totalBundles + " 扎, " + totalQuantity + " 件\"\r\n" +
                "TEXT 10, 520, \"1\", 0, 1, 1, \"" + customer + " " + dateStr + "\"\r\n" +
                "PRINT 1, 1\r\n"

        try {
            // 关键：中文标签机强烈建议使用 GBK 编码
            outputStream?.write(command.toByteArray(charset("GBK")))
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
