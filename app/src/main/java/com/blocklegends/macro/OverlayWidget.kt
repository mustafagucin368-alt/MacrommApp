package com.blocklegends.macro

import android.content.Context
import android.graphics.PixelFormat
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat

class OverlayWidget(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isExpanded = false
    private var isPicking = false
    private var pickingSingle = false

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 50
        y = 300
    }

    // Full-screen transparent overlay for coordinate picking
    private var pickOverlayView: View? = null
    private val pickParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    fun show() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.widget_overlay, null)
        overlayView = view
        setupDrag(view)
        setupButtons(view)
        windowManager.addView(view, params)
    }

    fun dismiss() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        overlayView = null
        dismissPickOverlay()
    }

    private fun setupDrag(view: View) {
        var startX = 0f
        var startY = 0f
        var initialX = 0
        var initialY = 0
        var isDragging = false

        view.findViewById<View>(R.id.dragHandle).setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    val dy = event.rawY - startY
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) isDragging = true
                    if (isDragging) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        overlayView?.let { windowManager.updateViewLayout(it, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Tap — toggle expand
                        val expandPanel = view.findViewById<View>(R.id.expandPanel)
                        isExpanded = !isExpanded
                        expandPanel.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        overlayView?.let { windowManager.updateViewLayout(it, params) }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupButtons(view: View) {
        val btnToggle = view.findViewById<Button>(R.id.btnToggle)
        val btnAdd = view.findViewById<Button>(R.id.btnAdd)
        val btnAddSingle = view.findViewById<Button>(R.id.btnAddSingle)
        val btnHold = view.findViewById<ToggleButton>(R.id.btnHold)
        val btnClear = view.findViewById<Button>(R.id.btnClearSlots)

        btnToggle.setOnClickListener {
            val service = MacroService.instance ?: return@setOnClickListener
            if (MacroEngine.isRunning) {
                service.stopMacro()
                btnToggle.text = "BAŞLAT"
                btnToggle.backgroundTintList = ContextCompat.getColorStateList(context, R.color.btn_start)
            } else {
                service.startMacro()
                btnToggle.text = "DURDUR"
                btnToggle.backgroundTintList = ContextCompat.getColorStateList(context, R.color.btn_stop)
            }
        }

        btnAdd.setOnClickListener {
            isPicking = true
            pickingSingle = false
            showPickOverlay()
            Toast.makeText(context, "Tıklamak istediğin yere dokun", Toast.LENGTH_SHORT).show()
        }

        btnAddSingle.setOnClickListener {
            isPicking = true
            pickingSingle = true
            showPickOverlay()
            Toast.makeText(context, "Tek tıklama noktasına dokun", Toast.LENGTH_SHORT).show()
        }

        btnHold.setOnCheckedChangeListener { _, checked ->
            MacroEngine.holdMode = checked
        }

        btnClear.setOnClickListener {
            MacroEngine.clearSlots()
            Toast.makeText(context, "Tüm noktalar temizlendi.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPickOverlay() {
        val pickView = View(context).apply {
            setBackgroundColor(0x33000000)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN && isPicking) {
                    val x = event.rawX
                    val y = event.rawY
                    MacroEngine.addSlot(x, y, single = pickingSingle)
                    isPicking = false
                    dismissPickOverlay()
                    val type = if (pickingSingle) "Tek tıklama" else "Sürekli tıklama"
                    Toast.makeText(context, "$type noktası eklendi: (${x.toInt()}, ${y.toInt()})", Toast.LENGTH_SHORT).show()
                    true
                } else false
            }
        }
        pickOverlayView = pickView
        windowManager.addView(pickView, pickParams)
    }

    private fun dismissPickOverlay() {
        pickOverlayView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        pickOverlayView = null
        isPicking = false
    }
}
