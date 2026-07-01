package com.blocklegends.macro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blocklegends.macro.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnOverlayPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay izni zaten verilmiş.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLaunchWidget.setOnClickListener {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "Önce erişilebilirlik servisini açın.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Önce overlay iznini verin.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val service = MacroService.instance
            if (service == null) {
                Toast.makeText(this, "Servis bağlı değil. Erişilebilirliği kontrol edin.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            service.showWidget()
            Toast.makeText(this, "Widget açıldı. Oyuna geçebilirsiniz.", Toast.LENGTH_SHORT).show()
            moveTaskToBack(true)
        }

        binding.btnGoAdmin.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatusUI()
    }

    private fun updateStatusUI() {
        val accessEnabled = isAccessibilityEnabled()
        val overlayEnabled = Settings.canDrawOverlays(this)

        binding.tvAccessibilityStatus.text = if (accessEnabled) "✅ Açık" else "❌ Kapalı - Makro çalışmaz"
        binding.tvAccessibilityStatus.setTextColor(
            if (accessEnabled) getColor(android.R.color.holo_green_dark)
            else getColor(android.R.color.holo_red_dark)
        )

        binding.tvOverlayStatus.text = if (overlayEnabled) "✅ Açık" else "❌ Kapalı - Widget gösterilmez"
        binding.tvOverlayStatus.setTextColor(
            if (overlayEnabled) getColor(android.R.color.holo_green_dark)
            else getColor(android.R.color.holo_red_dark)
        )

        val keyExpiry = KeyManager.getExpiry(this)
        val keyStr = KeyManager.getActiveKey(this) ?: "—"
        binding.tvStatus.text = "● Key: $keyStr | Bitiş: ${KeyManager.formatExpiry(keyExpiry)}"
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(packageName)
    }
}
