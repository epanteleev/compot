package ir.global

import common.assertion
import ir.attributes.GlobalValueAttribute
import ir.types.*
import ir.value.Constant


class GlobalValue private constructor(val name: String, private val type: NonTrivialType, private val init: Constant, private val attribute: GlobalValueAttribute): AnyGlobalValue {
    fun initializer(): Constant = init

    override fun name(): String = name

    override fun dump(): String {
        return "@$name = global $type ${init.data()} !$attribute"
    }

    fun contentType(): NonTrivialType = type

    override fun type(): PointerType = Type.Ptr

    override fun toString(): String {
        return "@$name"
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GlobalValue

        return name == other.name
    }

    fun data(): String {
        return init.data()
    }

    companion object {
        fun create(name: String, type: NonTrivialType, initializer: Constant, attributes: GlobalValueAttribute): GlobalValue {
            checkConstantType(type, initializer)
            return GlobalValue(name, type, initializer, attributes)
        }

        private fun checkConstantType(type: NonTrivialType, constant: Constant) = when (type) {
            is PointerType -> {
                assertion(constant.type() is PointerType || constant.type() is ArrayType) {
                    "GlobalValue: type mismatch: type=$type, init=${constant.type()}"
                }
            }
            is PrimitiveType -> {
                assertion(constant.type().sizeOf() == type.sizeOf()) {
                    "GlobalValue: type mismatch: type=$type, init=${constant}"
                }
            }

            is ArrayType -> {
                assertion(constant.type() is ArrayType) {
                    "GlobalValue: type mismatch: type=$type, init=${constant.type()}"
                }
                assertion((constant.type() as ArrayType).elementType().sizeOf() == type.elementType().sizeOf()) {
                    "GlobalValue: type mismatch: type=$type, init=${constant.type()}"
                }
            }

            is StructType -> assertion(constant.type() == type) { "GlobalValue: type mismatch" }
        }
    }
}