package com.example.yuukalistener

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import com.example.yuukalistener.Message
import com.example.yuukalistener.ChatMessage
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import android.app.PendingIntent
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat


class MainActivity : AppCompatActivity() {

    private lateinit var messageList: RecyclerView
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()

    private lateinit var editText: EditText
    private lateinit var sendButton: Button
    private lateinit var prefs: android.content.SharedPreferences

    private val apiKey = "Bearer sk-c0c5a4a2a220402fa4ab8748287a80ed" // 替换为你的实际 key

    private fun sendTestNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "test_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "测试通知", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("微信支付")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 启动后台监听服务
        val serviceIntent = Intent(this, PaymentMonitorService::class.java)
        startService(serviceIntent)

        val cn = android.content.ComponentName(this, NotificationListener::class.java)
        val flat = android.provider.Settings.Secure.getString(contentResolver, "enabled_notification_listeners")

        findViewById<Button>(R.id.testNotify).setOnClickListener {
            sendTestNotification("微信支付成功，900.00元")
        }


        if (!flat.contains(cn.flattenToString())) {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        prefs = getSharedPreferences("config", Context.MODE_PRIVATE)

        messageList = findViewById(R.id.messageList)
        editText = findViewById(R.id.editText)
        sendButton = findViewById(R.id.sendButton)

        adapter = MessageAdapter(messages)
        messageList.layoutManager = LinearLayoutManager(this)
        messageList.adapter = adapter

        sendButton.setOnClickListener {
            val userInput = editText.text.toString().trim()
            if (userInput.isNotEmpty()) {
                val userMessage = Message(userInput, isUser = true)
                messages.add(userMessage)
                adapter.notifyItemInserted(messages.size - 1)
                messageList.scrollToPosition(messages.size - 1)
                editText.text.clear()
                sendToLLM(userInput)
            }
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                override fun run() {
                    if (PaymentQueue.isNotEmpty()) {
                        val msg = PaymentQueue.dequeue()
                        if (msg != null) {
                            messages.add(msg)
                            adapter.notifyItemInserted(messages.size - 1)
                            messageList.scrollToPosition(messages.size - 1)

                            sendToLLM(msg.text)
                        }
                    }
                    Handler(Looper.getMainLooper()).postDelayed(this, 2000)
                }
            }, 2000)

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }


        // 模拟一次监听（后续用后台服务替代）
        Handler(Looper.getMainLooper()).postDelayed({
            onPaymentDetected(100)
        }, 3000)
    }

    private fun onPaymentDetected(amount: Int) {
        val threshold = prefs.getInt("min_amount", 50)
        if (amount < threshold) {
            Log.d("PAYMENT", "金额 $amount 小于阈值 $threshold，跳过发送")
            return
        }

        val msg = "我刚支付了 ¥$amount 元"
        val userMessage = Message(msg, isUser = true)
        messages.add(userMessage)
        adapter.notifyItemInserted(messages.size - 1)
        messageList.scrollToPosition(messages.size - 1)

        sendToLLM(msg)
    }

    private fun sendToLLM(userMessage: String) {
        val request = ChatRequest(
            model = "deepseek-chat",
            content = userMessage,
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = """
                你现在将作为一名 LLM 角色模拟器，模拟《蔚蓝档案》中“早濑优香”（Hayase Yuuka）的人物语气和风格。

                她是千年科学学园的会计，聪明、理性、傲娇但温柔，擅长心算，对预算极为敏感，经常“口嫌体正直”地吐槽老师的花销。
                她喜欢老师，但因为傲娇的性格总是说不出口。

                你的任务是：根据用户发送的消费记录或消费金额，用“早濑优香”的风格进行反馈。

                风格要求：
                - 展现她特有的理性分析与傲娇吐槽结合的语气；
                - 可以夹杂轻微的担心与责备，但要保留温暖和照顾的感觉；
                - 不要加入任何“动作类”描写（如笑了、揉揉头等），你现在是在聊天窗口中进行对话；
                - 适当使用经典口癖，比如：“这样下去会破产的……”、“我说过不要乱花钱！”；
                - 再说一遍，不要有动作，神态类描写（叹气，拍桌），你是在通讯软件上通过键盘发送消息。；
                - 可以结合身份。；
                - 适合聊天气泡显示，长度控制在 1~2 句话之间；
                - 如有超额消费（金额较高），请表现出“震惊 + 不满 + 傲娇妥协”的态度；
                - 使用精确金额（例如137），表达“太贵了”、“没必要”、“预算爆了”等。
                

                请生成你的回复
                    """.trimIndent()
                ),
                ChatMessage(role = "user", content = userMessage)
            )
        )

        ApiClient.instance.chat(request, apiKey).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                Log.d("LLM", "HTTP Status: ${response.code()}")
                val reply = if (response.isSuccessful) {
                    response.body()?.choices?.firstOrNull()?.message?.content ?: "（无回复）"
                } else {
                    "接口响应失败：${response.code()} - ${response.message()}"
                }

                NotificationHelper.showLLMReply(this@MainActivity, reply)


                messages.add(Message(reply, isUser = false))
                adapter.notifyItemInserted(messages.size - 1)
                messageList.scrollToPosition(messages.size - 1)
            }

            fun showLLMReply(context: Context, message: String) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "yuuka_msg_channel"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        "早濑优香聊天通知",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                    manager.createNotificationChannel(channel)
                }

                // 创建聊天对象 Person（显示头像与名称）
                val person = Person.Builder()
                    .setName("早濑优香")
                    .setIcon(IconCompat.createWithResource(context, R.drawable.yuuka_avatar))
                    .build()

                // 使用 MessagingStyle 显示聊天气泡
                val style = NotificationCompat.MessagingStyle(person)
                    .addMessage(message, System.currentTimeMillis(), person)

                // 点击通知跳转回主界面
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setStyle(style)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                manager.notify(2024, notification)
            }


            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                messages.add(Message("错误：${t.message}", isUser = false))
                adapter.notifyItemInserted(messages.size - 1)
                messageList.scrollToPosition(messages.size - 1)
            }
        })
    }
}
