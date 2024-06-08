package ir.instruction.matching

import ir.Value
import ir.instruction.*
import ir.types.PrimitiveType
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

inline fun binary(op: ArithmeticBinaryOp, crossinline a: ValueMatcher, crossinline b: ValueMatcher): InstructionMatcher = {
    it is ArithmeticBinary && it.op == op && a(it.first()) && b(it.second())
}

inline fun tupleDiv(crossinline a: ValueMatcher, crossinline b: ValueMatcher): InstructionMatcher = {
    it is TupleDiv && a(it.first()) && b(it.second())
}

inline fun ptr2int(crossinline origin: ValueMatcher): InstructionMatcher = {
    it is Pointer2Int && origin(it.value())
}

inline fun gep(crossinline src: ValueMatcher, crossinline idx: ValueMatcher): ValueMatcher = {
    it is GetElementPtr && src(it.source()) && idx(it.index())
}

inline fun gfp(crossinline src: ValueMatcher, crossinline idx: ValueMatcher): ValueMatcher = {
    it is GetFieldPtr && src(it.source()) && idx(it.index())
}

inline fun gfpOrGep(crossinline source: ValueMatcher, crossinline idx: ValueMatcher): ValueMatcher =
    gfp(source, idx) or gep(source, idx)

inline infix fun ValueMatcher.or(crossinline second: ValueMatcher): ValueMatcher = {
    this(it) || second(it)
}

inline infix fun ValueMatcher.and(crossinline second: ValueMatcher): ValueMatcher = {
    this(it) && second(it)
}

operator fun ValueMatcher.not(): ValueMatcher = { !this(it) }

inline fun generate(crossinline type: TypeMatcher): ValueMatcher = {
    it is Generate && type(it.type())
}

fun generate(): ValueMatcher = {
    it is Generate
}

inline fun alloc(crossinline type: TypeMatcher): ValueMatcher = {
    it is Alloc && type(it.allocatedType)
}

fun alloc(): ValueMatcher = {
    it is Alloc
}

fun nop(): ValueMatcher = { true }

inline fun value(crossinline type: TypeMatcher): ValueMatcher = { type(it.type()) }

////////////////////////////////////////////////////////////////////////////////////////
// Type matchers
////////////////////////////////////////////////////////////////////////////////////////
fun anytype(): TypeMatcher = { true }

fun primitive(): TypeMatcher = { it is PrimitiveType }

fun i8(): TypeMatcher = { it == Type.I8 }