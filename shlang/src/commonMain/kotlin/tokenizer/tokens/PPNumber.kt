package tokenizer.tokens

import tokenizer.Position

// 6.4.8 Preprocessing numbers
class PPNumber(private val data: String, private val radix: Int, position: Position): CToken(position) {
    private var cachedNumber: Any? = null

    override fun str(): String = data

    override fun hashCode(): Int {
        return data.hashCode()
    }

    fun toNumberOrNull(): Any? {
        if (cachedNumber != null) {
            return cachedNumber
        }
        cachedNumber = when {
            data.endsWith("ULL") -> data.substring(0, data.length - 3).toULongOrNull(radix)
            data.endsWith("ull") -> data.substring(0, data.length - 3).toULongOrNull(radix)
            data.endsWith("LL")  -> data.substring(0, data.length - 2).toLongOrNull(radix)
            data.endsWith("ll")  -> data.substring(0, data.length - 2).toLongOrNull(radix)
            data.endsWith("LL")  -> data.substring(0, data.length - 2).toLongOrNull(radix)
            data.endsWith("ll")  -> data.substring(0, data.length - 2).toLongOrNull(radix)
            data.endsWith("UL")  -> data.substring(0, data.length - 2).toULongOrNull(radix)
            data.endsWith("ul")  -> data.substring(0, data.length - 2).toULongOrNull(radix)
            data.endsWith("L")   -> data.substring(0, data.length - 1).toLongOrNull(radix)
            data.endsWith("l")   -> data.substring(0, data.length - 1).toLongOrNull(radix)
            data.endsWith("U")   -> data.substring(0, data.length - 1).toULongOrNull(radix)
            data.endsWith("u")   -> data.substring(0, data.length - 1).toULongOrNull(radix)
            else -> data.toByteOrNull(radix) ?: data.toIntOrNull(radix) ?: data.toLongOrNull(radix) ?: data.toULongOrNull(radix) ?: data.toDoubleOrNull()
        }
        if (cachedNumber != null) {
            return cachedNumber
        }
        cachedNumber = when {
            data.endsWith("F") -> data.substring(0, data.length - 1).toFloatOrNull()
            data.endsWith("f") -> data.substring(0, data.length - 1).toFloatOrNull()
            data.endsWith("D") -> data.substring(0, data.length - 1).toDoubleOrNull()
            data.endsWith("d") -> data.substring(0, data.length - 1).toDoubleOrNull()
            else -> null
        }
        return cachedNumber
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PPNumber

        return data == other.data
    }

    override fun cloneWith(pos: Position): CToken {
        return PPNumber(data, radix, pos)
    }
}