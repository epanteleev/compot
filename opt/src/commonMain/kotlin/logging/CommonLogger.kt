package logging

class CommonLogger(val byTags: Array<LogTag>) {
    private val isAll = byTags.any { it == All }

    private fun out(message: Any?) {
        println(message)
    }

    private fun onTagged(vararg tags: LogTag, eval: () -> Unit) {
        if (isAll || tags.any { it in byTags }) {
            eval()
        }
    }

    fun debug(vararg tag: LogTag, message: () -> String) = onTagged(*tag) {
        out("[DEBUG: ")
        for ((i, t) in tag.withIndex()) {
            out(t.tag)
            if (i != tag.size - 1) {
                out(", ")
            }
        }
        out("] ${message()}")
    }

    fun info(vararg tag: LogTag, message: () -> String) = onTagged(*tag) {
        out("[INFO: ")
        for ((i, t) in tag.withIndex()) {
            out(t.tag)
            if (i != tag.size - 1) {
                out(", ")
            }
        }
        out("] ${message()}")
    }
}