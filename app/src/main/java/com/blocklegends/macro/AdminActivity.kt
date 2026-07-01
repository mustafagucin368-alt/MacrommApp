package com.blocklegends.macro

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blocklegends.macro.databinding.ActivityAdminBinding

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val generatedKeys = mutableListOf<Pair<String, Long>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAdminLogin.setOnClickListener {
            val pass = binding.etAdminPass.text.toString()
            if (KeyManager.validateAdmin(this, pass)) {
                binding.loginPanel.visibility = View.GONE
                binding.adminPanel.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Yanlış şifre.", Toast.LENGTH_SHORT).show()
                binding.etAdminPass.text?.clear()
            }
        }

        binding.btnGenerate.setOnClickListener {
            val days = when (binding.rgDuration.checkedRadioButtonId) {
                R.id.rb1day -> 1
                R.id.rb7days -> 7
                R.id.rb30days -> 30
                R.id.rb90days -> 90
                R.id.rbCustom -> {
                    val custom = binding.etCustomDays.text.toString().toIntOrNull()
                    if (custom == null || custom <= 0) {
                        Toast.makeText(this, "Geçerli gün sayısı girin.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    custom
                }
                else -> 7
            }
            val key = KeyManager.generateKey()
            val expiry = KeyManager.getExpiryMillis(days)
            KeyManager.saveKey(this, key, expiry)
            generatedKeys.add(Pair(key, expiry))
            refreshKeyList()
            binding.tvGeneratedKey.text = "✓ $key"
            Toast.makeText(this, "Key oluşturuldu: $key", Toast.LENGTH_LONG).show()
        }

        binding.btnRevokeAll.setOnClickListener {
            KeyManager.revokeKey(this)
            generatedKeys.clear()
            binding.tvKeyList.text = "Tüm keyler iptal edildi."
            binding.tvGeneratedKey.text = ""
            Toast.makeText(this, "Tüm keyler iptal edildi.", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            binding.adminPanel.visibility = View.GONE
            binding.loginPanel.visibility = View.VISIBLE
            binding.etAdminPass.text?.clear()
        }

        binding.rbCustom.setOnCheckedChangeListener { _, checked ->
            val customLayout = binding.etCustomDays.parent.parent as? View
            customLayout?.visibility = if (checked) View.VISIBLE else View.GONE
        }
    }

    private fun refreshKeyList() {
        if (generatedKeys.isEmpty()) {
            binding.tvKeyList.text = "Henüz key oluşturulmadı."
            return
        }
        binding.tvKeyList.text = buildString {
            generatedKeys.forEachIndexed { i, (k, e) ->
                val expired = System.currentTimeMillis() > e
                val status = if (expired) "❌ Süresi Doldu" else "✅ Aktif"
                append("${i + 1}. $k\n")
                append("   Durum: $status\n")
                append("   Bitiş: ${KeyManager.formatExpiry(e)}\n\n")
            }
        }
    }
}
