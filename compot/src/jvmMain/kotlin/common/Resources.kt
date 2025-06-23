package common

class Resources

actual fun getBuildInHeader(filename: String): String? {
    return Resources::class.java.getResource("/compot-includes/$filename")?.readText()
}