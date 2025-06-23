package codegen


internal class NameGenerator(private var constantCounter: Int = 0) {
    fun createStringLiteralName(): String {
        return ".str${constantCounter++}"
    }

    fun createGlobalConstantName(): String {
        return ".v${constantCounter++}"
    }

    fun createStaticVariableName(prefix: String): String {
        return ".$prefix.static${constantCounter++}"
    }
}