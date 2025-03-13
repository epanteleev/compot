package ir.platform.x64.auxiliary

import ir.types.*
import ir.value.constant.*


class LinearizeInitializerList private constructor(private val aggregateType: AggregateType) {
    private val result = mutableListOf<NonTrivialConstant>()

    private fun addPadding(fieldOffset: Int, listCursor: Int) {
        val diff = fieldOffset - listCursor
        for (i in 0 until diff) {
            result.add(I8Value.of(0))
        }
    }

    private fun linearize(initializerListValue: InitializerListValue): List<NonTrivialConstant> {
        var listCursor = 0
        for ((idx, element) in initializerListValue.elements.withIndex()) {
            val offset = aggregateType.offset(idx)
            addPadding(offset, listCursor)

            val field = aggregateType.field(idx)
            when (element) {
                is InitializerListValue -> result.addAll(linearize(element, field.asType()))
                is PrimitiveConstant -> result.add(element.convertTo(field.asType()))
                is StringLiteralConstant -> result.add(element)
            }

            listCursor = offset + field.sizeOf()
        }

        addPadding(aggregateType.sizeOf(), listCursor)
        return result
    }

    companion object {
        fun linearize(initializerListValue: InitializerListValue, aggregateType: AggregateType): List<NonTrivialConstant> {
            return LinearizeInitializerList(aggregateType).linearize(initializerListValue)
        }
    }
}