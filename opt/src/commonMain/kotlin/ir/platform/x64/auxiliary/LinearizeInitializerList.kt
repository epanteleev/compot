package ir.platform.x64.auxiliary

import ir.types.*
import ir.value.*


object LinearizeInitializerList {
    private fun fillIn(constants: MutableList<Constant>, structSize: Int, initializerListSizeOf: Int) {
        val diff = structSize - initializerListSizeOf
        for (i in 0 until diff) {
            constants.add(Constant.zero(Type.I8))
        }
    }

    fun linearize(initializerListValue: InitializerListValue, aggregateType: AggregateType): List<Constant> {
        val result = mutableListOf<Constant>()
        var initializerListSizeOf = 0
        for ((idx, element) in initializerListValue.elements.withIndex()) {
            val structSize = aggregateType.offset(idx) + aggregateType.field(idx).sizeOf()
            initializerListSizeOf += element.type().sizeOf()

            fillIn(result, structSize, initializerListSizeOf)
            when (element) {
                is InitializerListValue -> result.addAll(linearize(element, aggregateType.field(idx) as AggregateType))
                else -> result.add(element)
            }
        }
        fillIn(result, aggregateType.sizeOf(), initializerListSizeOf)
        return result
    }

    fun linearize(literal: StringLiteralConstant, aggregateType: AggregateType): List<Constant> {
        if (aggregateType !is ArrayType) {
            throw IllegalArgumentException("Expected ArrayType, got $aggregateType")
        }
        return literal.name.map { U8Value(it.code.toByte()) }
    }
}