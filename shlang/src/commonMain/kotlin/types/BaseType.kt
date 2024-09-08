package types


interface BaseType: TypeProperty {
    fun typename(): String
    fun size(): Int
}

sealed class CPrimitive: BaseType {
    override fun toString(): String = typename()
}

object VOID: CPrimitive() {
    override fun typename(): String = "void"
    override fun size(): Int = -1
}

object CHAR: CPrimitive() {
    override fun typename(): String = "char"
    override fun size(): Int = 1
}

object SHORT: CPrimitive() {
    override fun typename(): String = "short"
    override fun size(): Int = 2
}

object INT: CPrimitive() {
    override fun typename(): String = "int"
    override fun size(): Int = 4
}

object LONG: CPrimitive() {
    override fun typename(): String = "long"
    override fun size(): Int = 8
}

object FLOAT: CPrimitive() {
    override fun typename(): String = "float"
    override fun size(): Int = 4
}

object DOUBLE: CPrimitive() {
    override fun typename(): String = "double"
    override fun size(): Int = 8
}

object UCHAR: CPrimitive() {
    override fun typename(): String = "unsigned char"
    override fun size(): Int = 1
}

object USHORT: CPrimitive() {
    override fun typename(): String = "unsigned short"
    override fun size(): Int = 2
}

object UINT: CPrimitive() {
    override fun typename(): String = "unsigned int"
    override fun size(): Int = 4
}

object ULONG: CPrimitive() {
    override fun typename(): String = "unsigned long"
    override fun size(): Int = 8
}

object BOOL: CPrimitive() {
    override fun typename(): String = "_Bool"
    override fun size(): Int = 1
}

object UNKNOWN: CPrimitive() {
    override fun typename(): String = "<unknown>"
    override fun size(): Int = 0
}

class TypeDef(val name: String, private val baseType: TypeDesc): BaseType {
    fun baseType(): TypeDesc = baseType
    override fun typename(): String = name
    override fun size(): Int = baseType.size()
    override fun toString(): String = baseType.toString()
}

sealed class AggregateBaseType: BaseType

sealed class AnyStructType(open val name: String): AggregateBaseType() {
    protected val fields = arrayListOf<Pair<String, TypeDesc>>()
    override fun typename(): String = name

    fun fieldIndex(name: String): Int {
        return fields.indexOfFirst { it.first == name }
    }

    fun fieldIndex(index: Int): TypeDesc {
        return fields[index].second
    }

    fun fields(): List<Pair<String, TypeDesc>> {
        return fields
    }

    //TODO avoid???
    internal fun addField(name: String, type: TypeDesc) {
        fields.add(name to type)
    }
}


sealed class UncompletedType(name: String): AnyStructType(name) {
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
        return TypeDesc.CINT.size()
    }

    fun addEnumeration(name: String) {
        enumerators.add(name)
    }
}


data class CArrayBaseType(val type: TypeDesc, val dimension: Long) : AggregateBaseType() {
    override fun typename(): String {
        return toString()
    }

    override fun size(): Int {
        return type.size() * dimension.toInt() //TODO
    }

    override fun toString(): String {
        return buildString {
            append("[$dimension]")
            append(type)
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