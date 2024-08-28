package common

class Resources

actual fun getInclude(filename: String): String? {
    return Resources::class.java.getResource("/shlang-includes/$filename")?.readText()
}