package com.example.dafei

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.mht.print.sdk.PrinterConstants
import com.mht.print.sdk.PrinterInstance
import com.mht.print.sdk.TsplCommand
import kotlinx.coroutines.yield

object BluetoothPrinter {
    private var mPrinter: PrinterInstance? = null
    private var isConnecting = false

    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun dispatchMessage(msg: Message) {
            super.dispatchMessage(msg)
            when (msg.what) {
                PrinterConstants.Connect.SUCCESS -> isConnecting = false
                PrinterConstants.Connect.FAILED -> {
                    isConnecting = false
                    mPrinter = null
                }
                PrinterConstants.Connect.CLOSED -> {
                    isConnecting = false
                    mPrinter = null
                }
            }
        }
    }

    fun connect(context: Context, device: BluetoothDevice): Boolean {
        return try {
            isConnecting = true
            mPrinter = PrinterInstance(context, device, mHandler)
            mPrinter?.openConnection()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isConnected(): Boolean {
        return mPrinter != null && mPrinter!!.isConnected
    }

    fun disconnect() {
        mPrinter?.closeConnection()
        mPrinter = null
    }

    // 4. 发送测试指令
    fun printTest() {
        if (!isConnected()) return
        val tsplCommand = TsplCommand()
        tsplCommand.clearCanvas()
        tsplCommand.initCanvas(40, 80)
        tsplCommand.setDirection(1)

        tsplCommand.setText(20, 80, 1, 1, "测试打印成功")
        tsplCommand.setText(20, 120, 1, 1, "美恒通打印机")
        tsplCommand.setPrintNumber(1)

        mPrinter?.sendByteData(tsplCommand.command)
    }

    fun printTicket(
        customer: String, styleNo: String, ticketNo: String, color: String,
        size: String, qty: Int, bedNo: String, dateStr: String, bundleNo: Int,
        totalBundles: Int, totalQuantity: Int, isRotated: Boolean = false
    ) {
        if (!isConnected()) return

        val tsplCommand = TsplCommand()
        tsplCommand.clearCanvas()
        tsplCommand.initCanvas(40, 80) // 40x80mm 画布

        // 方向控制
        tsplCommand.setDirection(if (isRotated) 0 else 1)

        // --- 坐标计算 ---
        // 正向：左边距 20 点，顶部留白 80 点（10mm）
        // 倒置：顶部留白 2mm (16点)，向左偏移 20mm (160点)
        // 注意：如果倒置打出来偏右了，把 160 改成 -160 或 0 试试
        val baseX = if (isRotated) 50 else 20
        val baseY = if (isRotated) 60 else 150

        val step = 50 // 行距统一为 50 点，顺序绝对不乱

        // 虚线上方的文字（等比例放大 2 倍，全部统一 + 递增）
        tsplCommand.setText(baseX, baseY, 1, 1, "款号: $styleNo")
        tsplCommand.setText(baseX, baseY + step, 1, 1, "床号: $bedNo")
        tsplCommand.setText(baseX, baseY + step * 2, 1, 1, "扎号: ${String.format("%02d", bundleNo)}")
        tsplCommand.setText(baseX, baseY + step * 3, 1, 1, "码数: $size")
        tsplCommand.setText(baseX, baseY + step * 4, 2, 2, "数量: $qty")
        tsplCommand.setText(baseX, baseY + step * 5+20, 1, 1, "颜色: $color")

        // 虚线及底部（普通字号 1 倍）
        tsplCommand.setText(baseX, baseY + step * 6 + 10, 1, 1, "--------------------")
        tsplCommand.setText(baseX, baseY + step * 7, 1, 1, "共 $totalBundles 扎, 总 $totalQuantity 件")
        tsplCommand.setText(baseX, baseY + step * 8, 1, 1, "$customer  $dateStr")

        tsplCommand.setPrintNumber(1)
        mPrinter?.sendByteData(tsplCommand.command)
    }

    // 纯文本全拼包：把所有不同内容的标签拼成一个数据流，一次性发送
    fun printBatchTickets(tickets: List<TicketData>, isRotated: Boolean, density: Int = 8) {
        if (!isConnected() || tickets.isEmpty()) return

        val outputStream = java.io.ByteArrayOutputStream()

        // 👇 新增：写入浓度指令 (DENSITY 范围通常是 0-15)
        outputStream.write("DENSITY $density\r\n".toByteArray(charset("GBK")))

        // 采用你实测完美的坐标与排版
        val baseX = if (isRotated) 50 else 20
        val baseY = if (isRotated) 60 else 150
        val step = 50

        tickets.forEach { ticket ->
            val tsplCommand = TsplCommand()
            tsplCommand.clearCanvas()
            tsplCommand.initCanvas(40, 80)
            tsplCommand.setDirection(if (isRotated) 0 else 1)

            tsplCommand.setText(baseX, baseY, 1, 1, "款号: ${ticket.styleNo}")
            tsplCommand.setText(baseX, baseY + step, 1, 1, "床号: ${ticket.bedNo}")
            tsplCommand.setText(baseX, baseY + step * 2, 1, 1, "扎号: ${String.format("%02d", ticket.bundleNo)}")
            tsplCommand.setText(baseX, baseY + step * 3, 1, 1, "码数: ${ticket.size}")
            tsplCommand.setText(baseX, baseY + step * 4, 2, 2, "数量: ${ticket.quantity}")
            tsplCommand.setText(baseX, baseY + step * 5 + 20, 1, 1, "颜色: ${ticket.color}")

            tsplCommand.setText(baseX, baseY + step * 6 + 10, 1, 1, "--------------------")
            tsplCommand.setText(baseX, baseY + step * 7, 1, 1, "共 ${ticket.totalBundles} 扎, 总 ${ticket.totalQuantity} 件")
            tsplCommand.setText(baseX, baseY + step * 8, 1, 1, "${ticket.customer}  ${ticket.dateStr}")

            tsplCommand.setPrintNumber(1)

            val commandBytes = tsplCommand.command
            if (commandBytes != null) {
                outputStream.write(commandBytes)
            }
        }

        mPrinter?.sendByteData(outputStream.toByteArray())
    }
//    fun printBatchTickets(tickets: List<TicketData>, isRotated: Boolean) {
//        if (!isConnected() || tickets.isEmpty()) return
//
//        val outputStream = java.io.ByteArrayOutputStream()
//
//        // 采用你实测完美的坐标与排版
//        val baseX = if (isRotated) 50 else 20
//        val baseY = if (isRotated) 60 else 150
//        val step = 50
//
//        tickets.forEach { ticket ->
//            val tsplCommand = TsplCommand()
//            tsplCommand.clearCanvas()
//            tsplCommand.initCanvas(40, 80)
//            tsplCommand.setDirection(if (isRotated) 0 else 1)
//
//            tsplCommand.setText(baseX, baseY, 1, 1, "款号: ${ticket.styleNo}")
//            tsplCommand.setText(baseX, baseY + step, 1, 1, "床号: ${ticket.bedNo}")
//            tsplCommand.setText(baseX, baseY + step * 2, 1, 1, "扎号: ${String.format("%02d", ticket.bundleNo)}")
//            tsplCommand.setText(baseX, baseY + step * 3, 1, 1, "码数: ${ticket.size}")
//            tsplCommand.setText(baseX, baseY + step * 4, 2, 2, "数量: ${ticket.quantity}")
//            tsplCommand.setText(baseX, baseY + step * 5 + 20, 1, 1, "颜色: ${ticket.color}")
//
//            tsplCommand.setText(baseX, baseY + step * 6 + 10, 1, 1, "--------------------")
//            tsplCommand.setText(baseX, baseY + step * 7, 1, 1, "共 ${ticket.totalBundles} 扎, 总 ${ticket.totalQuantity} 件")
//            tsplCommand.setText(baseX, baseY + step * 8, 1, 1, "${ticket.customer}  ${ticket.dateStr}")
//
//            tsplCommand.setPrintNumber(1)
//
//            // 👇 把每一张的指令追加到同一个输出流中，中间绝对不暂停
//            val commandBytes = tsplCommand.command
//            if (commandBytes != null) {
//                outputStream.write(commandBytes)
//            }
//        }
//
//        // 👇 一次性把几十张的数据全部推给蓝牙，让打印机像吃豆人一样一口气吃完
//        mPrinter?.sendByteData(outputStream.toByteArray())
//    }

    // 新增：批量打印，把所有标签拼成一包，一次性发给打印机，实现连续输出
//    fun printBatchTickets(
//        tickets: List<com.example.dafei.TicketData>, // 这里直接传你的数据模型
//        isRotated: Boolean
//    ) {
//        if (!isConnected()) return
//
//        val outputStream = java.io.ByteArrayOutputStream()
//
//        tickets.forEach { ticket ->
//            val tsplCommand = TsplCommand()
//            tsplCommand.clearCanvas()
//            tsplCommand.initCanvas(40, 80)
//
//            // 方向控制
//            tsplCommand.setDirection(if (isRotated) 0 else 1)
//
//            val baseX = if (isRotated) 50 else 20
//            val baseY = if (isRotated) 60 else 150 // 依据你最后测试的基准版参数
//            val step = 50
//
//            // 拼装单张内容 (与你最后确定的基准版参数完全一致)
//            tsplCommand.setText(baseX, baseY, 1, 1, "款号: ${ticket.styleNo}")
//            tsplCommand.setText(baseX, baseY + step, 1, 1, "床号: ${ticket.bedNo}")
//            tsplCommand.setText(baseX, baseY + step * 2, 1, 1, "扎号: ${String.format("%02d", ticket.bundleNo)}")
//            tsplCommand.setText(baseX, baseY + step * 3, 1, 1, "码数: ${ticket.size}")
//            tsplCommand.setText(baseX, baseY + step * 4, 2, 2, "数量: ${ticket.quantity}")
//            tsplCommand.setText(baseX, baseY + step * 5 + 20, 1, 1, "颜色: ${ticket.color}")
//
//            tsplCommand.setText(baseX, baseY + step * 6 + 10, 1, 1, "--------------------")
//            tsplCommand.setText(baseX, baseY + step * 7, 1, 1, "共 ${ticket.totalBundles} 扎, 总 ${ticket.totalQuantity} 件")
//            tsplCommand.setText(baseX, baseY + step * 8, 1, 1, "${ticket.customer}  ${ticket.dateStr}")
//
//            tsplCommand.setPrintNumber(1)
//
//            // 关键：把这一张的指令流，追加到总的大字节数组里
//            val commandBytes = tsplCommand.command
//            if (commandBytes != null) {
//                outputStream.write(commandBytes)
//            }
//        }
//
//        // 一次性把 N 张标签的所有数据发送出去！
//        val fullBatchBytes = outputStream.toByteArray()
//        mPrinter?.sendByteData(fullBatchBytes)
//    }
    // 坐标单位修正为 dots，解决文字全堆叠的问题！
//    fun printTicket(
//        customer: String,
//        styleNo: String,
//        ticketNo: String,
//        color: String,
//        size: String,
//        qty: Int,
//        bedNo: String,
//        dateStr: String,
//        bundleNo: Int,
//        totalBundles: Int,
//        totalQuantity: Int,
//        isRotated: Boolean = false // 👈 有这个参数
//    ) {
//        if (!isConnected()) return
//
//        val tsplCommand = TsplCommand()
//        tsplCommand.clearCanvas()
//        tsplCommand.initCanvas(40, 80)
//        tsplCommand.setDirection(1)
//
//        // 处理 180° 旋转 (1是正向，如果选了旋转，就传 0 或 2)
//        tsplCommand.setDirection(if (isRotated) 0 else 1)
//
//        val x = 20 // 左边距
//
//        // 普通字体：必须使用 1, 1 比例（防止被拉高），行距改为 35（紧凑饱满）
//        tsplCommand.setText(x, 120, 1, 1, "款号: $styleNo")
//        tsplCommand.setText(x, 155, 1, 1, "床号: $bedNo")
//        tsplCommand.setText(x, 190, 1, 1, "扎号: ${String.format("%02d", bundleNo)}")
//        tsplCommand.setText(x, 225, 1, 1, "码数: $size")
//
//        // 数量：等比例放大 2 倍（横竖都放大），字大且不拉伸
//        tsplCommand.setText(x, 265, 2, 2, "数量: $qty")
//
//        // 颜色：因为数量高了，这里往下移一点
//        tsplCommand.setText(x, 330, 1, 1, "颜色: $color")
//
//        // 虚线以下
//        tsplCommand.setText(x, 370, 1, 1, "--------------------")
//        tsplCommand.setText(x, 405, 1, 1, "共 $totalBundles 扎, 总 $totalQuantity 件")
//        tsplCommand.setText(x, 440, 1, 1, "$customer  $dateStr")
//
//        tsplCommand.setPrintNumber(1)
//        mPrinter?.sendByteData(tsplCommand.command)
//    }
}