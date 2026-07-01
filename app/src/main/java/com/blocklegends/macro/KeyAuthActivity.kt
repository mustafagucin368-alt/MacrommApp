package com.blocklegends.macro

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blocklegends.macro.databinding.ActivityKeyAuthBinding

class KeyAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeyAuthBinding
    private var failCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeyAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnActivate.setOnClickListener {
            val input = binding.etKey.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(this, "Key girin.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (KeyManager.validateKey(this, input)) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                failCount++
                when {
                    failCount >= 3 -> {
                        Toast.makeText(this, "Geçersiz key. Uygulama kapatılıyor.", Toast.LENGTH_LONG).show()
                        finishAffinity()
                    }
                    else -> {
                        val remaining = 3 - failCount
                        Toast.makeText(this, "Geçersiz veya süresi dolmuş key. ($remaining hak kaldı)", Toast.LENGTH_SHORT).show()
                        binding.etKey.text?.clear()
                    }
                }
            }
        }

        binding.tvAdminLogin.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
    }
}
