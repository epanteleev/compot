package ir.instruction.matching

import ir.Value
import ir.instruction.*
import ir.types.Type

typealias ValueMatcher = (Value) -> Boolean
typealias InstructionMatcher = (Instruction) -> Boolean
typealias TypeMatcher = (Type) -> Boolean


inline fun store(crossinline pointer: ValueMatcher, crossinline value: ValueMatcher): InstructionMatcher = {
    it is Store && pointer(it.pointer()) && value(it.value())
}

inline fun load(crossinline pointer: ValueMatcher): InstructionMatcher = {
    it is Load && pointer(it.operand())
}

inline fun copy(crossinline origin: ValueMatcher): InstructionMatcher = {
    it is Copy && origin(it.origin())
}

inline fun ptr2int(crossinline origin: ValueMatcher): InstructionMatcher = {
    it is Pointer2Int && origin(it.value())
}

inline fun gep(crossinline src: ValueMatcher, crossinline idx: ValueMatcher): ValueMatcher = {
    it is GetElementPtr && src(it.source()) && idx(it.index())
}

inline infix fun ValueMatcher.or(crossinline second: ValueMatcher): ValueMatcher = {
    this(it) || second(it)
}

operator fun ValueMatcher.not(): ValueMatcher = { !this(it) }

inline fun generate(crossinline type: TypeMatcher): ValueMatcher = {
    it is Generate && type(it.type())
}

inline fun generate(): ValueMatcher = {
    it is Generate
}

inline fun alloc(crossinline type: TypeMatcher): ValueMatcher = {
    it is Alloc && type(it.allocatedType)
}

inline fun alloc(): ValueMatcher = {
    it is Alloc
}

fun nop(): ValueMatcher = { true }
fun anytype(): TypeMatcher = { true }