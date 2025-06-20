package com.example.yuukalistener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d("NOTIFY", "收到通知：[$pkg] $title - $text")

        val amount = extractAmountFromText(text)
        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        val threshold = prefs.getInt("min_amount", 50)

        if (amount != null && amount >= threshold) {
            Log.d("NOTIFY", "抓取金额：¥$amount，超过阈值，准备发送")

            val msg = "我刚通过 $pkg 支付了 ¥$amount 元"
            // 调用你的 sendToLLM 或发送 Intent 给 MainActivity
            PaymentQueue.enqueue(Message(msg, true))
        }
    }

    private fun extractAmountFromText(text: String): Int? {
        val regex = Regex("""(\d+(\.\d{1,2})?)元""")
        val match = regex.find(text)
        return match?.groups?.get(1)?.value?.toDoubleOrNull()?.toInt()
    }
}
