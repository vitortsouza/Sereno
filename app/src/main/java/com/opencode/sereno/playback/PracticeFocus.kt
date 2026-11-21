package com.opencode.sereno.playback

object PracticeFocus {
    private val actions = LinkedHashMap<Any, () -> Unit>()

    fun register(owner: Any, onStop: () -> Unit) {
        actions[owner] = onStop
    }

    fun unregister(owner: Any) {
        actions.remove(owner)
    }

    fun stopActive() {
        actions.values.toList().forEach { it() }
    }
}
