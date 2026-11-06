package com.example.dafei

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SyncManager {
    // 传入你本地当前的记录列表，返回合并后的最新列表
    suspend fun syncData(context: Context, localOrders: List<CutOrderData>): List<CutOrderData> {
        val gson = Gson()

        // 1. 把本地数据打包准备推送到云端
        val pushList = localOrders.map {
            SyncPushReq(it.id, gson.toJson(it), it.time)
        }
        try {
            NetworkClient.api.pushData(pushList)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. 从云端拉取最新数据
        val remoteData = NetworkClient.api.pullData()
        val remoteMap = remoteData.associateBy { it.cutId }

        // 3. 合并：以云端为主，如果有新变化就转成 CutOrderData
        val mergedList = localOrders.toMutableList()
        val localIds = localOrders.map { it.id }.toSet()

        remoteData.forEach { remote ->
            val local = localOrders.find { it.id == remote.cutId }
            val remoteOrder = gson.fromJson(remote.jsonData, CutOrderData::class.java)

            if (local == null) {
                // 本地没有，直接加入
                mergedList.add(remoteOrder)
            } else if (remote.time > local.time) {
                // 云端比本地新，替换本地
                val index = mergedList.indexOf(local)
                mergedList[index] = remoteOrder
            }
        }

        // 按时间倒序排列
        return mergedList.sortedByDescending { it.time }
    }
}