package ir.instruction.matching

import ir.types.*
import ir.value.Value
import ir.instruction.*
import ir.attributes.ByValue
import ir.global.ExternValue
import ir.global.GlobalValue
import ir.value.ArgumentValue
import ir.value.constant.Constant


typealias ValueMatcher = (Value) -> Boolean
typealias InstructionMatcher = (Instruction) -> Boolean
typealias TypeMatcher = (Type) -> Boolean

inline fun store(crossinline pointer: ValueMatcher, crossinline value: ValueMatcher): InstructionMatcher = {
    it is Store && pointer(it.pointer()) && value(it.value())
}

inline fun load(crossinline pointer: ValueMatcher): InstructionMatcher = {
    it is Load && pointer(it.operand())
}

inline fun load(crossinline type: TypeMatcher, crossinline pointer: ValueMatcher): InstructionMatcher = {
    it is Load && type(it.type()) && pointer(it.operand())
}

inline fun lea(crossinline pointer: ValueMatcher): InstructionMatcher = {
    it is ir.instruction.lir.Lea && pointer(it.operand())
}

inline fun copy(crossinline origin: ValueMatcher): InstructionMatcher = {
    it is Copy && origin(it.origin())
}

inline fun div(crossinline a: ValueMatcher, crossinline b: ValueMatcher): InstructionMatcher = {
    it is Div && a(it.lhs()) && b(it.rhs())
}

inline fun div(crossinline a: ValueMatcher, crossinline b: ValueMatcher, crossinline type: TypeMatcher): InstructionMatcher = {
    it is Div && a(it.lhs()) && b(it.rhs()) && type(it.type())
}

inline fun shl(crossinline a: ValueMatcher, crossinline b: ValueMatcher): InstructionMatcher = {
    it is Shl && a(it.lhs()) && b(it.rhs())
}

inline fun shr(crossinline a: ValueMatcher, crossinline b: ValueMatcher): InstructionMatcher = {
    it is Shr && a(it.lhs()) && b(it.rhs())
}

inline fun<reified T: Instruction> select(crossinline cond: ValueMatcher, crossinline onTrue: ValueMatcher, crossinline onFalse: ValueMatcher): (T) -> Boolean = {
    it is Select && cond(it.condition()) && onTrue(it.onTrue()) && onFalse(it.onFalse())
}

inline fun tupleDiv(crossinline a: ValueMatcher, crossinline b: ValueMatcher): InstructionMatcher = {
    it is TupleDiv && a(it.first()) && b(it.second())
}

inline fun uint2float(crossinline origin: ValueMatcher): InstructionMatcher = {
    it is Unsigned2Float && origin(it.value())
}

inline fun tupleDiv(crossinline a: ValueMatcher, crossinline b: ValueMatcher, crossinline type: TypeMatcher): InstructionMatcher = {
    it is TupleDiv && a(it.first()) && b(it.second()) && type(it.type())
}

inline fun ptr2int(crossinline origin: ValueMatcher): InstructionMatcher = {
    it is Pointer2Int && origin(it.value())
}

inline fun memcpy(crossinline dst: ValueMatcher, crossinline src: ValueMatcher, crossinline length: ValueMatcher): InstructionMatcher = {
    it is Memcpy && dst(it.destination()) && src(it.source()) && length(it.length())
}

inline fun icmp(crossinline lhs: ValueMatcher, crossinline typeMatcher: TypeMatcher, crossinline rhs: ValueMatcher): InstructionMatcher = {
    it is IntCompare && lhs(it.first()) && typeMatcher(it.operandsType()) && rhs(it.second())
}

inline fun gep(crossinline src: ValueMatcher, crossinline idx: ValueMatcher): ValueMatcher = {
    it is GetElementPtr && src(it.source()) && idx(it.index())
}

inline fun gep(crossinline type: TypeMatcher, crossinline src: ValueMatcher, crossinline idx: ValueMatcher): ValueMatcher = {
    it is GetElementPtr && src(it.source()) && idx(it.index()) && type(it.basicType)
}

inline fun gfp(crossinline src: ValueMatcher): ValueMatcher = {
    it is GetFieldPtr && src(it.source())
}

inline fun gfp(crossinline type: TypeMatcher, crossinline src: ValueMatcher): ValueMatcher = {
    it is GetFieldPtr && src(it.source()) && type(it.basicType)
}

inline fun gfpOrGep(crossinline source: ValueMatcher, crossinline idx: ValueMatcher): ValueMatcher =
    gfp(source) or gep(source, idx)

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

fun alloc(): ValueMatcher = { it is Alloc }

fun nop(): ValueMatcher = { true }

fun constant(): ValueMatcher = { it is Constant }

fun extern(): ValueMatcher = { it is ExternValue }

inline fun value(crossinline type: TypeMatcher): ValueMatcher = { type(it.type()) }

inline fun gValue(crossinline type: TypeMatcher): ValueMatcher = {
    it is GlobalValue && type(it.contentType())
}

fun argumentByValue(): ValueMatcher = { it is ArgumentValue && it.attributes.any { it is ByValue } }

////////////////////////////////////////////////////////////////////////////////////////
// Type matchers
////////////////////////////////////////////////////////////////////////////////////////
fun anytype(): TypeMatcher = { true }

fun primitive(): TypeMatcher = { it is PrimitiveType }

fun int(): TypeMatcher = { it is IntegerType }

fun ptr(): TypeMatcher = { it is PtrType }

fun aggregate(): TypeMatcher = { it is AggregateType }

fun array(elementType: TypeMatcher): TypeMatcher = { it is ArrayType && elementType(it.elementType()) }

fun struct(): TypeMatcher = { it is StructType }

fun i8(): TypeMatcher = { it == Type.I8 }

fun u8(): TypeMatcher = { it == Type.U8 }

fun u64(): TypeMatcher = { it == Type.U64 }

fun it_is(value: Value): ValueMatcher = { it == value }