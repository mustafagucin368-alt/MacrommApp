package com.blocklegends.macro

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class MacroService : AccessibilityService() {

    companion object {
        var instance: MacroService? = null
    }

    private var macroJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var overlayWidget: OverlayWidget? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        overlayWidget?.dismiss()
        overlayWidget = null
        scope.cancel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { stopMacro() }

    fun showWidget() {
        if (overlayWidget == null) {
            overlayWidget = OverlayWidget(this)
            overlayWidget?.show()
        }
    }

    fun hideWidget() {
        overlayWidget?.dismiss()
        overlayWidget = null
    }

    fun startMacro() {
        if (MacroEngine.isRunning) return
        MacroEngine.isRunning = true

        macroJob = scope.launch {
            val activeSlots = MacroEngine.slots.filter { it.isActive }
            if (activeSlots.isEmpty()) {
                MacroEngine.isRunning = false
                return@launch
            }

            if (MacroEngine.holdMode) {
                while (isActive && MacroEngine.isRunning) {
                    activeSlots.forEach { slot ->
                        if (!MacroEngine.isRunning) return@forEach
                        performTap(slot.x, slot.y, holdDuration = slot.intervalMs)
                        delay(MacroEngine.getDelay(slot))
                    }
                }
            } else {
                val singleSlots = activeSlots.filter { it.isSingle }
                val repeatSlots = activeSlots.filter { !it.isSingle }

                singleSlots.forEach { slot ->
                    performTap(slot.x, slot.y)
                    delay(50L)
                }

                if (repeatSlots.isNotEmpty()) {
                    while (isActive && MacroEngine.isRunning) {
                        repeatSlots.forEach { slot ->
                            if (!MacroEngine.isRunning) return@forEach
                            performTap(slot.x, slot.y)
                            delay(MacroEngine.getDelay(slot))
                        }
                    }
                } else {
                    MacroEngine.isRunning = false
                }
            }
            MacroEngine.isRunning = false
        }
    }

    fun stopMacro() {
        MacroEngine.isRunning = false
        macroJob?.cancel()
        macroJob = null
    }

    fun performSingleTap(x: Float, y: Float) {
        performTap(x, y, holdDuration = 50L)
    }

    private fun performTap(x: Float, y: Float, holdDuration: Long = 50L) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, holdDuration.coerceAtLeast(1L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }
}
