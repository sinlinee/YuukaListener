package com.example.yuukalistener

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PaymentMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val intervalMillis = 8000L // 每 8 秒检查一次
    private val apiKey = "Bearer sk-xxx" // 替换你的 key

    private val messages = mutableListOf<Message>() // 暂存消息（可替换成数据库）

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Service", "后台监听服务已启动")
        handler.post(checkTask)
        return START_STICKY
    }

    private val checkTask = object : Runnable {
        override fun run() {
            val fakeAmount = listOf(20, 50, 80, 120).random() // 模拟抓取金额
            val prefs = getSharedPreferences("config", Context.MODE_PRIVATE)
            val threshold = prefs.getInt("min_amount", 50)

            if (fakeAmount >= threshold) {
                val text = "我刚支付了 ¥$fakeAmount 元"
                messages.add(Message(text, true))
                sendToLLM(text)
            } else {
                Log.d("Service", "跳过金额 $fakeAmount（小于阈值 $threshold）")
            }

            handler.postDelayed(this, intervalMillis)
        }
    }

    private fun sendToLLM(userMessage: String) {
        val request = ChatRequest(
            model = "deepseek-chat",
            content = userMessage,
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = "你是早濑优香...（可填写完整提示）"
                ),
                ChatMessage(role = "user", content = userMessage)
            )
        )

        ApiClient.instance.chat(request, apiKey).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: "(无回复)"
                messages.add(Message(reply, false))
                Log.d("Service", "AI 回复：$reply")
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                Log.e("Service", "请求失败: ${t.message}")
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
