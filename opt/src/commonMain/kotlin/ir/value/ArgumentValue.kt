package ir.value

import ir.types.*
import ir.instruction.Instruction


data class ArgumentValue(private val index: Int, private val tp: NonTrivialType): LocalValue {
    override var usedIn: MutableList<Instruction> = arrayListOf()
    override fun name(): String {
        return "arg$index"
    }

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    fun position(): Int {
        return index
    }

    override fun toString(): String {
        return "%$index"
    }
}