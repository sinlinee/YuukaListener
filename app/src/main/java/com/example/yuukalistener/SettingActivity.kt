
package com.example.yuukalistener

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        val input = findViewById<EditText>(R.id.editThreshold)
        val save = findViewById<Button>(R.id.btnSave)

        val prefs = getSharedPreferences("config", Context.MODE_PRIVATE)
        input.setText(prefs.getInt("min_amount", 50).toString())

        save.setOnClickListener {
            val value = input.text.toString().toIntOrNull()
            if (value != null) {
                prefs.edit().putInt("min_amount", value).apply()
                Toast.makeText(this, "阈值已保存", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
