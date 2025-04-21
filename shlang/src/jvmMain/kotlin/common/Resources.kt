package common

class Resources

actual fun getBuildInHeader(filename: String): String? {
    return Resources::class.java.getResource("/shlang-includes/$filename")?.readText()
}