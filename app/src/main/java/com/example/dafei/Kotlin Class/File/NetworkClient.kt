package com.example.dafei

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.UUID
import java.util.concurrent.TimeUnit

// --- 数据结构 ---
data class LoginReq(
    val username: String,
    val password: String,
    val deviceId: String,
    val deviceName: String
)

data class LoginRes(
    val success: Boolean,
    val message: String,
    val token: String?
)

data class SyncPushReq(val cutId: String, val jsonData: String, val time: Long)
data class SyncPullRes(val cutId: String, val jsonData: String, val time: Long)
// --- 接口定义 ---
interface ApiService {
    @POST("register")
    suspend fun register(@Body req: LoginReq): Map<String, String>

    @POST("login")
    suspend fun login(@Body req: LoginReq): LoginRes

    @POST("sync/push")
    suspend fun pushData(@Body data: List<SyncPushReq>): Map<String, String>

    @GET("sync/pull")
    suspend fun pullData(): List<SyncPullRes>

    @GET("generate/bed-no")
    suspend fun getNextBedNo(@Query("monthPrefix") monthPrefix: String): Map<String, String>
}

// --- 全局网络对象 ---
object NetworkClient {
    private const val BASE_URL = "https://citizens-tough-render-starter.trycloudflare.com/" // 别忘带斜杠

    // 用 SharedPreferences 持久化保存 Token
    private lateinit var prefs: android.content.SharedPreferences

    var token: String?
        get() = prefs.getString("auth_token", null)
        set(value) {
            prefs.edit().putString("auth_token", value).apply()
        }

    fun init(context: Context) {
        prefs = context.getSharedPreferences("dafei_prefs", Context.MODE_PRIVATE)
    }

    val api: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                // 自动把 Token 塞进请求头
                val original = chain.request()
                val builder = original.newBuilder()
                token?.let {
                    builder.header("Authorization", "Bearer $it")
                }
                chain.proceed(builder.build())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// --- 获取设备唯一 ID ---
fun getDeviceId(context: Context): String {
    val prefs = context.getSharedPreferences("dafei_prefs", Context.MODE_PRIVATE)
    var id = prefs.getString("device_id", null)
    if (id == null) {
        id = UUID.randomUUID().toString()
        prefs.edit().putString("device_id", id).apply()
    }
    return id
}