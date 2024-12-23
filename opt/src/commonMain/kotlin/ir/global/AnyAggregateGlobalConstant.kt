package ir.global

import ir.types.*
import common.quoted
import common.assertion
import ir.value.constant.*


sealed class AnyAggregateGlobalConstant(override val name: String): GlobalConstant(name) {
    abstract fun contentType(): NonTrivialType
    override fun type(): NonTrivialType = PtrType
    abstract fun data(): String
}

class StringLiteralGlobalConstant(override val name: String, val tp: ArrayType, val string: String): AnyAggregateGlobalConstant(name) {
    init {
        assertion(tp.length > 0) { "string length should be greater than 0" }
    }

    override fun dump(): String {
        return "@$name = constant ${contentType()} ${string.quoted()}"
    }

    override fun data(): String {
        return "\"$string\"" //TOdo unify with String.quoted??
    }

    override fun contentType(): NonTrivialType = ArrayType(I8Type, string.length)
    override fun constant(): Constant = StringLiteralConstant(tp, string)
}

sealed class AggregateGlobalConstant(override val name: String, val tp: NonTrivialType, protected val elements: InitializerListValue): AnyAggregateGlobalConstant(name) {
    fun elements(): InitializerListValue {
        return elements
    }

    override fun dump(): String {
        return "@$name = constant ${contentType()} ${data()}"
    }

    final override fun data(): String {
        return elements.toString()
    }

    final override fun constant(): Constant {
        return elements
    }
}

class ArrayGlobalConstant(name: String, elements: InitializerListValue): AggregateGlobalConstant(name, elements.type(), elements) {
    constructor(name: String, tp: ArrayType, elements: List<NonTrivialConstant>): this(name,
        InitializerListValue(tp, elements)
    )

    override fun contentType(): ArrayType = tp as ArrayType
}

class StructGlobalConstant(name: String, elements: InitializerListValue): AggregateGlobalConstant(name, elements.type(), elements) {
    constructor(name: String, tp: StructType, elements: List<NonTrivialConstant>): this(name,
        InitializerListValue(tp, elements)
    )

    override fun contentType(): StructType = tp as StructType
}