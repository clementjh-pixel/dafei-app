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
        customer: String, styleNo: String, ticketNo: String, color: String, size: String, qty: Int,
        bedNo: String, dateStr: String, bundleNo: Int, totalBundles: Int, totalQuantity: Int
    ) {
        if (outputStream == null) return

        // 203dpi 环境下：
        // 40x80mm 大约为宽 320 点，高 640 点。
        // 顶部留白 20mm，大约相当于 160 点。
        // 字体加粗变大：TSS24.BF2(2倍大小) 或 TSS24.BF2 的 2倍宽高 (0,2,2)
        val command = "SIZE 40 mm, 80 mm\r\n" +
                "GAP 2 mm, 0 mm\r\n" +
                "DIRECTION 1\r\n" +
                "CLS\r\n" +
                // 从 Y=160 开始打印 (顶部留白 20mm)
                "TEXT 10, 160, \"TSS24.BF2\", 0, 1, 1, \"款号: " + styleNo + "\"\r\n" +
                "TEXT 10, 210, \"TSS24.BF2\", 0, 1, 1, \"床号: " + bedNo + "\"\r\n" +
                "TEXT 10, 260, \"TSS24.BF2\", 0, 1, 1, \"扎号: " + String.format("%02d", bundleNo) + "\"\r\n" +
                "TEXT 10, 310, \"TSS24.BF2\", 0, 1, 1, \"码数: " + size + "\"\r\n" +
                "TEXT 10, 360, \"TSS24.BF2\", 0, 1, 1, \"数量: " + qty + "\"\r\n" +
                "TEXT 10, 410, \"TSS24.BF2\", 0, 1, 1, \"颜色: " + color + "\"\r\n" +
                // 虚线
                "TEXT 10, 460, \"TSS16.BF2\", 0, 1, 1, \"----------------------\"\r\n" +
                // 底部小字信息
                "TEXT 10, 490, \"TSS16.BF2\", 0, 1, 1, \"共 " + totalBundles + " 扎, " + totalQuantity + " 件\"\r\n" +
                "TEXT 10, 520, \"TSS16.BF2\", 0, 1, 1, \"" + customer + " " + dateStr + "\"\r\n" +
                "PRINT 1, 1\r\n"

        try {
            outputStream?.write(command.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
