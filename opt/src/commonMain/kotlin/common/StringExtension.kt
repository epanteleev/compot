package common

fun String.padTo(count: Int, pad: String): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append(this)
    for (i in 0 until count - length) {
        stringBuilder.append(pad)
    }
    return stringBuilder.toString()
}