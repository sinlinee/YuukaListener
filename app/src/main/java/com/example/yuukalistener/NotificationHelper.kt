package com.example.yuukalistener

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat


object NotificationHelper {
    private const val CHANNEL_ID = "yuuka_msg_channel"
    private const val NOTIFY_ID = 2024

    // 全局缓存的聊天风格对象
    private var cachedStyle: NotificationCompat.MessagingStyle? = null

    // 构造 Person（发消息的角色）
    private fun buildPerson(context: Context): Person {
        return Person.Builder()
            .setName("早濑优香")
            .setIcon(IconCompat.createWithResource(context, R.drawable.yuuka_avatar))
            .build()
    }

    fun showLLMReply(context: Context, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "优香聊天通知",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val person = buildPerson(context)

        // 初始化或复用聊天风格
        val style = cachedStyle ?: NotificationCompat.MessagingStyle(person).also {
            cachedStyle = it
        }

        // 添加新消息（时间戳为当前）
        style.addMessage(message, System.currentTimeMillis(), person)

        // 点击通知跳回 App
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(style)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // 同一个 ID，系统会更新而不是弹多个
        manager.notify(NOTIFY_ID, notification)
    }
}
