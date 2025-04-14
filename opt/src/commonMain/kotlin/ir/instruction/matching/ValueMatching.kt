package ir.instruction.matching

import ir.types.*
import ir.value.Value
import ir.instruction.*
import ir.attributes.ByValue
import ir.global.ExternValue
import ir.global.GlobalConstant
import ir.global.GlobalValue
import ir.instruction.lir.Generate
import ir.value.ArgumentValue
import ir.value.LocalValue
import ir.value.constant.Constant
import ir.value.constant.F32Value
import ir.value.constant.F64Value


typealias ValueMatcher = (Value) -> Boolean
typealias InstructionMatcher = (Instruction) -> Boolean
typealias TypeMatcher = (Type) -> Boolean

inline fun ret(crossinline value: ValueMatcher): InstructionMatcher = {
    it is ReturnValue && value(it.returnValue(0))
}

inline fun store(crossinline pointer: ValueMatcher, crossinline value: ValueMatcher): InstructionMatcher = {
    it is Store && pointer(it.pointer()) && value(it.value())
}

inline fun load(crossinline pointer: ValueMatcher): InstructionMatcher = {
    it is Load && pointer(it.operand())
}

inline fun load(crossinline type: TypeMatcher, crossinline pointer: ValueMatcher): InstructionMatcher = {
    it is Load && type(it.type()) && pointer(it.operand())
}

inline fun lea(crossinline pointer: ValueMatcher): ValueMatcher = {
    it is ir.instruction.lir.Lea && pointer(it.operand())
}

inline fun copy(crossinline origin: ValueMatcher): InstructionMatcher = {
    it is Copy && origin(it.operand())
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
    it is TupleDiv && a(it.lhs()) && b(it.rhs())
}

inline fun proj(crossinline type: TypeMatcher, crossinline origin: ValueMatcher, idx: Int): InstructionMatcher = {
    it is Projection && it.index() == idx && type(it.type()) && origin(it.tuple())
}

fun tupleCall(): ValueMatcher = { it is TupleCall }

inline fun uint2float(crossinline origin: ValueMatcher): InstructionMatcher = {
    it is Unsigned2Float && origin(it.operand())
}

inline fun tupleDiv(crossinline a: ValueMatcher, crossinline b: ValueMatcher, crossinline type: TypeMatcher): InstructionMatcher = {
    it is TupleDiv && a(it.lhs()) && b(it.rhs()) && type(it.type())
}

inline fun ptr2int(crossinline origin: ValueMatcher): InstructionMatcher = {
    it is Pointer2Int && origin(it.operand())
}

inline fun memcpy(crossinline dst: ValueMatcher, crossinline src: ValueMatcher, crossinline length: ValueMatcher): InstructionMatcher = {
    it is Memcpy && dst(it.destination()) && src(it.source()) && length(it.length())
}

inline fun icmp(crossinline lhs: ValueMatcher, crossinline typeMatcher: TypeMatcher, crossinline rhs: ValueMatcher): InstructionMatcher = {
    it is IntCompare && lhs(it.lhs()) && typeMatcher(it.operandsType()) && rhs(it.rhs())
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

inline fun neg(crossinline type: TypeMatcher, crossinline value: ValueMatcher): InstructionMatcher = {
    it is Neg && type(it.type()) && value(it.operand())
}

inline fun generate(crossinline type: TypeMatcher): ValueMatcher = {
    it is Generate && type(it.type())
}

fun generate(): ValueMatcher = {
    it is Generate
}

fun iCall(fnPointer: ValueMatcher): ValueMatcher = {
    it is IndirectionCall && fnPointer(it.pointer())
}

fun ivCall(fnPointer: ValueMatcher): InstructionMatcher = {
    it is IndirectionVoidCall && fnPointer(it.pointer())
}

fun itCall(fnPointer: ValueMatcher): InstructionMatcher = {
    it is IndirectionTupleCall && fnPointer(it.pointer())
}

fun stackAlloc(): ValueMatcher = generate() or argumentByValue()

fun local(): ValueMatcher = { it is LocalValue }

inline fun alloc(crossinline type: TypeMatcher): ValueMatcher = {
    it is Alloc && type(it.allocatedType)
}

fun alloc(): ValueMatcher = { it is Alloc }

fun any(): ValueMatcher = { true }

fun constant(): ValueMatcher = { it is Constant }

fun externValue(): ValueMatcher = { it is ExternValue }

inline fun value(crossinline type: TypeMatcher): ValueMatcher = { type(it.type()) }

inline fun gValue(crossinline type: TypeMatcher): ValueMatcher = {
    it is GlobalValue && type(it.contentType())
}

inline fun gConstant(crossinline type: TypeMatcher): ValueMatcher = {
    it is GlobalConstant && type(it.type())
}

fun argumentByValue(): ValueMatcher = { it is ArgumentValue && it.attributes.any { it is ByValue } }

fun f32v(): ValueMatcher = { it is F32Value }

fun f64v(): ValueMatcher = { it is F64Value }

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

fun i8(): TypeMatcher = { it == I8Type }

fun u8(): TypeMatcher = { it == U8Type }

fun u64(): TypeMatcher = { it == U64Type }

fun fp(): TypeMatcher = { it is FloatingPointType }

fun f32(): TypeMatcher = { it == F32Type }
fun f64(): TypeMatcher = { it == F64Type }
////////////////////////////////////////////////////////////////////////////////////////
// Operations
////////////////////////////////////////////////////////////////////////////////////////
inline infix fun<reified Type> ((Type) -> Boolean).or(crossinline second: (Type) -> Boolean): (Type) -> Boolean = {
    this(it) || second(it)
}

inline infix fun<reified Type> ((Type) -> Boolean).and(crossinline second: (Type) -> Boolean): (Type) -> Boolean = {
    this(it) && second(it)
}

inline operator fun<reified Type> ((Type) -> Boolean).not(): (Type) -> Boolean = { !this(it) }
