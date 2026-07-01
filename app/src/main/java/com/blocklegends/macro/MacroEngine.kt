package com.blocklegends.macro

data class MacroSlot(
    val id: Int,
    var x: Float,
    var y: Float,
    var intervalMs: Long = 100L,
    var isSingle: Boolean = false,
    var isActive: Boolean = true
)

object MacroEngine {
    val slots = mutableListOf<MacroSlot>()
    var isRunning = false
    var holdMode = false
    var randomizeDelay = true
    var randomRangeMs = 15L
    private var nextId = 0

    fun addSlot(x: Float, y: Float, single: Boolean = false): MacroSlot {
        val slot = MacroSlot(
            id = nextId++,
            x = x,
            y = y,
            isSingle = single
        )
        slots.add(slot)
        return slot
    }

    fun removeSlot(id: Int) {
        slots.removeAll { it.id == id }
    }

    fun clearSlots() {
        slots.clear()
        nextId = 0
    }

    fun getDelay(slot: MacroSlot): Long {
        if (!randomizeDelay) return slot.intervalMs
        val variance = (-randomRangeMs..randomRangeMs).random()
        return (slot.intervalMs + variance).coerceAtLeast(16L)
    }
}
