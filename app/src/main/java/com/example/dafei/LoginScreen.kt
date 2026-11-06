package com.example.dafei

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("123456") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("打菲助手 - 云端登录", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("账号") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            // ==================== 登录按钮 ====================
            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "请输入账号和密码", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            val req = LoginReq(username, password, getDeviceId(context), android.os.Build.MODEL)
                            val res = NetworkClient.api.login(req)
                            if (res.success && res.token != null) {
                                NetworkClient.token = res.token
                                Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(username)
                            } else {
                                Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: retrofit2.HttpException) {
                            // 👇 专门拦截 401 等 HTTP 错误，必须放在 Exception 前面
                            if (e.code() == 401) {
                                Toast.makeText(context, "账号或密码错误", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "请求失败: ${e.code()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("登 录", fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))

            // ==================== 注册按钮 ====================
            TextButton(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "请输入账号和密码", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            val req = LoginReq(username, password, getDeviceId(context), android.os.Build.MODEL)
                            // 👇 这里必须是 register！
                            val res = NetworkClient.api.register(req)
                            // 注册成功返回的是 Map<String,String>，取 msg 字段
                            Toast.makeText(context, res["msg"] ?: "注册成功", Toast.LENGTH_SHORT).show()
                        } catch (e: retrofit2.HttpException) {
                            if (e.code() == 400) {
                                Toast.makeText(context, "用户已存在", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "注册失败: ${e.code()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            ) {
                Text("没有账号？点击注册")
            }
        }
    }
}