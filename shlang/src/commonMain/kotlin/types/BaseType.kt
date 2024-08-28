package types


interface BaseType: TypeProperty {
    fun typename(): String
    fun size(): Int
}

enum class CPrimitive(val size: Int, val id: String): BaseType {
    VOID(-1, "void"),
    CHAR(1, "char"),
    SHORT(2, "short"),
    INT(4, "int"),
    LONG(8, "long"),
    FLOAT(4, "float"),
    DOUBLE(8, "double"),
    UCHAR(1, "unsigned char"),
    USHORT(2, "unsigned short"),
    UINT(4, "unsigned int"),
    ULONG(8, "unsigned long"),
    BOOL(1, "_Bool"),
    UNKNOWN(0, "<unknown>");

    override fun toString(): String = id

    override fun typename(): String = id

    override fun size(): Int = size
}

class TypeDef(val name: String, private val baseType: CType): BaseType {
    fun baseType(): CType = baseType
    override fun typename(): String = name
    override fun size(): Int = baseType.size()
    override fun toString(): String = baseType.toString()
}

abstract class AggregateBaseType: BaseType

abstract class AnyStructType(open val name: String): AggregateBaseType() {
    protected val fields = arrayListOf<Pair<String, CType>>()
    override fun typename(): String = name

    fun fieldIndex(name: String): Int {
        return fields.indexOfFirst { it.first == name }
    }

    fun fields(): List<Pair<String, CType>> {
        return fields
    }

    //TODO avoid???
    internal fun addField(name: String, type: CType) {
        fields.add(name to type)
    }
}


abstract class UncompletedType(name: String): AnyStructType(name) {
    override fun size(): Int = throw Exception("Uncompleted type")
}

data class StructBaseType(override val name: String): AnyStructType(name) { //TODO
    override fun size(): Int {
        return fields.sumOf { it.second.size() }
    }

    override fun toString(): String {
        return buildString {
            append("struct $name")
            append(" {")
            fields.forEach { (name, type) ->
                append("$type $name;")
            }
            append("}")

        }
    }
}

data class UnionBaseType(override val name: String): AnyStructType(name) {
    override fun size(): Int {
        if (fields.isEmpty()) {
            return 0
        }
        return fields.maxOf { it.second.size() }
    }

    override fun toString(): String {
        return buildString {
            append("union $name")
            append(" {")
            fields.forEach { (name, type) ->
                append("$type $name;")
            }
            append("}")
        }
    }
}

data class EnumBaseType(val name: String): BaseType {
    private val enumerators = mutableListOf<String>()
    override fun typename(): String = name

    override fun size(): Int {
        return CType.INT.size()
    }

    fun addEnumeration(name: String) {
        enumerators.add(name)
    }
}


data class CArrayBaseType(val type: CType, val dimension: Long) : AggregateBaseType() {
    override fun typename(): String {
        return toString()
    }

    override fun size(): Int {
        return type.size() * dimension.toInt() //TODO
    }

    override fun toString(): String {
        return buildString {
            append(type)
            append("[$dimension]")
        }
    }
}

data class UncompletedStructBaseType(override val name: String): UncompletedType(name) {
    override fun typename(): String = name

    override fun size(): Int = throw Exception("Uncompleted type '$name'")

    override fun toString(): String {
        return "struct $name"
    }
}

data class UncompletedUnionBaseType(override val name: String): UncompletedType(name) {
    override fun typename(): String = name

    override fun size(): Int = throw Exception("Uncompleted type")

    override fun toString(): String {
        return "union $name"
    }
}

data class UncompletedEnumType(override val name: String): UncompletedType(name) {
    override fun typename(): String = name

    override fun size(): Int = throw Exception("Uncompleted type")

    override fun toString(): String {
        return "enum $name"
    }
}