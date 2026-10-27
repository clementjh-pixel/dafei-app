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

    // --- 新增：测试打印 ---
    fun printTest() {
        if (outputStream == null) return
        try {
            // 简单的 TSPL 测试指令
            val testCmd = "SIZE 40 mm, 80 mm\r\n" +
                    "GAP 2 mm, 0 mm\r\n" +
                    "DIRECTION 1\r\n" +
                    "CLS\r\n" +
                    "TEXT 20, 50, \"3\", 0, 1, 1, \"Hello Printer!\"\r\n" +
                    "PRINT 1, 1\r\n"
            outputStream?.write(testCmd.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 正式打印标签 ---
    fun printTicket(
        customer: String, styleNo: String, ticketNo: String, color: String, size: String, qty: Int,
        bedNo: String, dateStr: String, bundleNo: Int, totalBundles: Int, totalQuantity: Int
    ) {
        if (outputStream == null) return

        // 强制留白 20mm（203dpi 下 20mm 约等于 160 点）
        // 打印机中文支持：TSS24.BF2 (大)，TSS16.BF2 (小)
        val command = "\r\n\r\n" + // 先发两个回车唤醒打印机缓冲区
                "SIZE 40 mm, 80 mm\r\n" +
                "GAP 2 mm, 0 mm\r\n" +
                "DIRECTION 1\r\n" +
                "CLS\r\n" +
                "TEXT 10, 160, \"TSS24.BF2\", 0, 1, 1, \"款号: " + styleNo + "\"\r\n" +
                "TEXT 10, 210, \"TSS24.BF2\", 0, 1, 1, \"床号: " + bedNo + "\"\r\n" +
                "TEXT 10, 260, \"TSS24.BF2\", 0, 1, 1, \"扎号: " + String.format("%02d", bundleNo) + "\"\r\n" +
                "TEXT 10, 310, \"TSS24.BF2\", 0, 1, 1, \"码数: " + size + "\"\r\n" +
                "TEXT 10, 360, \"TSS24.BF2\", 0, 1, 1, \"数量: " + qty + "\"\r\n" +
                "TEXT 10, 410, \"TSS24.BF2\", 0, 1, 1, \"颜色: " + color + "\"\r\n" +
                "TEXT 10, 460, \"TSS16.BF2\", 0, 1, 1, \"----------------------\"\r\n" +
                "TEXT 10, 490, \"TSS16.BF2\", 0, 1, 1, \"共 " + totalBundles + " 扎, " + totalQuantity + " 件\"\r\n" +
                "TEXT 10, 520, \"TSS16.BF2\", 0, 1, 1, \"" + customer + " " + dateStr + "\"\r\n" +
                "PRINT 1, 1\r\n"

        try {
            // 尝试使用 GBK 编码发送，部分打印机中文需要 GBK
            outputStream?.write(command.toByteArray(charset("GBK")))
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
