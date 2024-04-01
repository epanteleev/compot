package types

interface TypeProperty

enum class PointerQualifier: TypeProperty {
    CONST {
        override fun toString() = "const"
    },
    VOLATILE {
        override fun toString() = "volatile"
    },
    RESTRICT {
        override fun toString() = "restrict"
    }
}

enum class StorageClass: TypeProperty {
    TYPEDEF {
        override fun toString(): String = "typedef"
    },
    EXTERN {
        override fun toString(): String = "extern"
    },
    STATIC {
        override fun toString(): String = "static"
    },
    REGISTER {
        override fun toString(): String = "register"
    },
    AUTO {
        override fun toString(): String = "auto"
    }
}

data class SpecifiedType(val basicType: CType, val properties: List<TypeProperty>) {

    companion object {
        val VOID = SpecifiedType(CType.VOID, emptyList())
    }
}

class SpecifiedTypeBuilder {
    var basicType: CType? = null
    private val properties = mutableListOf<TypeProperty>()

    fun basicType(type: CType) {
        basicType = type
    }

    fun add(property: TypeProperty) {
        properties.add(property)
    }

    fun addAll(properties: List<TypeProperty>) {
        this.properties.addAll(properties)
    }

    fun build(): SpecifiedType {
        return SpecifiedType(basicType as CType, properties)
    }
}