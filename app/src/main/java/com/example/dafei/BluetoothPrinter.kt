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

    // --- 测试打印：验证中文字符集是否激活 ---
    fun printTest() {
        if (outputStream == null) return
        try {
            val testCmd = "SIZE 40 mm, 80 mm\r\n" +
                    "GAP 2 mm, 0 mm\r\n" +
                    "DIRECTION 1\r\n" +
                    "CLS\r\n" +
                    "CODEPAGE GB18030\r\n" +  // 激活 GB18030 字符集
                    "CHARSET CHINESE\r\n" +   // 启用中文
                    "TEXT 10, 10, \"TSS24.BF2\", 0, 1, 1, \"测试成功\"\r\n" +
                    "PRINT 1, 1\r\n"
            // 关键：发送 GB18030 编码字节
            outputStream?.write(testCmd.toByteArray(charset("GB18030")))
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 正式打印标签（含中文）---
    fun printTicket(
        customer: String, styleNo: String, ticketNo: String, color: String, size: String, qty: Int,
        bedNo: String, dateStr: String, bundleNo: Int, totalBundles: Int, totalQuantity: Int
    ) {
        if (outputStream == null) return

        // 使用 TSS24.BF2 简体中文字体，顶部留白 20mm（Y=160）
        val command = "SIZE 40 mm, 80 mm\r\n" +
                "GAP 2 mm, 0 mm\r\n" +
                "DIRECTION 1\r\n" +
                "CLS\r\n" +
                "CODEPAGE GB18030\r\n" +  // 必须放在 TEXT 之前
                "CHARSET CHINESE\r\n" +
                "TEXT 10, 160, \"TSS24.BF2\", 0, 1, 1, \"款号: " + styleNo + "\"\r\n" +
                "TEXT 10, 210, \"TSS24.BF2\", 0, 1, 1, \"床号: " + bedNo + "\"\r\n" +
                "TEXT 10, 260, \"TSS24.BF2\", 0, 1, 1, \"扎号: " + String.format("%02d", bundleNo) + "\"\r\n" +
                "TEXT 10, 310, \"TSS24.BF2\", 0, 1, 1, \"码数: " + size + "\"\r\n" +
                "TEXT 10, 360, \"TSS24.BF2\", 0, 1, 1, \"数量: " + qty + "\"\r\n" +
                "TEXT 10, 410, \"TSS24.BF2\", 0, 1, 1, \"颜色: " + color + "\"\r\n" +
                "TEXT 10, 460, \"TSS24.BF2\", 0, 1, 1, \"----------------------\"\r\n" +
                "TEXT 10, 490, \"TSS24.BF2\", 0, 1, 1, \"共 " + totalBundles + " 扎, " + totalQuantity + " 件\"\r\n" +
                "TEXT 10, 520, \"TSS24.BF2\", 0, 1, 1, \"" + customer + " " + dateStr + "\"\r\n" +
                "PRINT 1, 1\r\n"

        try {
            outputStream?.write(command.toByteArray(charset("GB18030")))
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
