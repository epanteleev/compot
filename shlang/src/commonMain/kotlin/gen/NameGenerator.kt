package gen


class NameGenerator(private var constantCounter: Int = 0) {
    fun createStringLiteralName(): String {
        return ".str${constantCounter++}"
    }

    fun createGlobalConstantName(): String {
        return ".v${constantCounter++}"
    }
}