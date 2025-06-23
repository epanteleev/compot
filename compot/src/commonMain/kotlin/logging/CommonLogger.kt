package logging

class CommonLogger(private val isEnabled: Boolean) {
    private fun out(message: Any?) {
        println(message)
    }

    private fun onEnabled(block: () -> Unit) {
        if (isEnabled) {
            block()
        }
    }

    fun debug(message: () -> String) = onEnabled {
        out("[DEBUG] ${message()}")
    }

    fun info(message: () -> String) = onEnabled {
        out("[INFO] ${message()}")
    }
}