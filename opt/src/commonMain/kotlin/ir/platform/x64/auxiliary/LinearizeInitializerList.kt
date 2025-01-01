package ir.platform.x64.auxiliary

import ir.types.*
import ir.value.constant.*


object LinearizeInitializerList {
    private fun fillIn(constants: MutableList<NonTrivialConstant>, structSize: Int, initializerListSizeOf: Int) {
        val diff = structSize - initializerListSizeOf
        for (i in 0 until diff) {
            constants.add(I8Value.of(0))
        }
    }

    fun linearize(initializerListValue: InitializerListValue, aggregateType: AggregateType): List<NonTrivialConstant> {
        val result = mutableListOf<NonTrivialConstant>()
        var initializerListSizeOf = 0
        for ((idx, element) in initializerListValue.elements.withIndex()) {
            val structSize = aggregateType.offset(idx) + aggregateType.field(idx).sizeOf()
            initializerListSizeOf += element.type().sizeOf()


            when (element) {
                is InitializerListValue -> result.addAll(linearize(element, aggregateType.field(idx) as AggregateType))
                else -> result.add(element)
            }
            fillIn(result, structSize, initializerListSizeOf)
        }
        fillIn(result, aggregateType.sizeOf(), initializerListSizeOf)
        return result
    }
}