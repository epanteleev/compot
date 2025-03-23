package preprocess

class Hideset {
    private val hidden = arrayListOf<String>()

    fun unionWith(other: Hideset) {
        hidden.addAll(other.hidden)
    }

    fun contains(name: String): Boolean {
        return hidden.contains(name)
    }
}