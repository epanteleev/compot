package preprocess

class Hideset {
    private val hidden = arrayListOf<String>()

    fun add(name: String) {
        hidden.add(name)
    }

    fun contains(name: String): Boolean {
        return hidden.contains(name)
    }

    fun copy(): Hideset {
        val copy = Hideset()
        copy.hidden.addAll(hidden)
        return copy
    }
}