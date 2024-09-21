package ir.instruction.matching

import ir.global.ExternValue
import ir.global.GlobalValue
import ir.value.Constant
import ir.value.Value
import ir.instruction.*
import ir.types.AggregateType
import ir.types.ArrayType
import ir.types.IntegerType
import ir.types.PrimitiveType
import ir.types.StructType
import ir.types.Type

typealias ValueMatcher = (Value) -> Boolean
typealias InstructionMatcher = (Instruction) -> Boolean
typealias TypeMatcher = (Type) -> Boolean

class Matcher(val inst: Instruction) {
    var isEnd = false
    var result: Any? = null

    inline fun<reified U, reified T: Instruction, reified R> rule(matcher: (U) -> Boolean, block: (T) -> R?): R? {
        if (isEnd) {
            return result as R?
        }
        if (inst !is T) {
            return result as R?
        }
        if (matcher(inst as U)) {
            isEnd = true
            result = block(inst as T)
            return result as R?
        } else {
            return null
        }
    }

    fun store(pointer: ValueMatcher, value: ValueMatcher, block: (Store) -> Instruction?): Instruction? {
        return rule(store(pointer, value), block)
    }

    fun load(pointer: ValueMatcher, block: (Load) -> Instruction?): Instruction? {
        return rule(load(pointer), block)
    }

    fun load(type: TypeMatcher, pointer: ValueMatcher, block: (Load) -> Instruction?): Instruction? {
        return rule(load(type, pointer), block)
    }

    fun gep(source: ValueMatcher, idx: ValueMatcher, block: (GetElementPtr) -> Instruction?): Instruction? {
        return rule(gep(source, idx), block)
    }

    fun gep(baseType: TypeMatcher, source: ValueMatcher, idx: ValueMatcher, block: (GetElementPtr) -> Instruction?): Instruction? {
        return rule(gep(baseType, source, idx), block)
    }

    fun gfp(source: ValueMatcher, block: (GetFieldPtr) -> Instruction?): Instruction? {
        return rule(gfp(source), block)
    }

    fun gfp(basicType: TypeMatcher, source: ValueMatcher, block: (GetFieldPtr) -> Instruction?): Instruction? {
        return rule(gfp(basicType, source), block)
    }

    inline fun<reified R> shl(crossinline a: ValueMatcher, crossinline b: ValueMatcher, block: (Shl) -> R?): R? {
        return rule(shl(a, b), block)
    }

    inline fun<reified R> shr(crossinline a: ValueMatcher, crossinline b: ValueMatcher, block: (Shr) -> R?): R? {
        return rule(shr(a, b), block)
    }

    inline fun<reified R> tupleDiv(crossinline a: ValueMatcher, crossinline b: ValueMatcher, block: (TupleDiv) -> R?): R? {
        return rule(tupleDiv(a, b), block)
    }

    fun default(): Instruction? {
        return rule<Instruction, Instruction, Instruction> ({ true }, { it })
    }
}

inline fun<reified R> match(instruction: Instruction, block: Matcher.() -> R?): R? {
    return Matcher(instruction).block()
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

inline fun select(crossinline cond: ValueMatcher, crossinline onTrue: ValueMatcher, crossinline onFalse: ValueMatcher): InstructionMatcher = {
    it is Select && cond(it.condition()) && onTrue(it.onTrue()) && onFalse(it.onFalse())
}

inline fun tupleDiv(crossinline a: ValueMatcher, crossinline b: ValueMatcher): InstructionMatcher = {
    it is TupleDiv && a(it.first()) && b(it.second())
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

inline fun alloc(crossinline type: TypeMatcher): InstructionMatcher = {
    it is Alloc && type(it.allocatedType)
}

fun alloc(): InstructionMatcher = {
    it is Alloc
}

inline fun nop(): ValueMatcher = { true }

inline fun constant(): ValueMatcher = { it is Constant }

inline fun extern(): ValueMatcher = { it is ExternValue }

inline fun value(crossinline type: TypeMatcher): ValueMatcher = { type(it.type()) }

inline fun gValue(crossinline type: TypeMatcher): ValueMatcher = {
    it is GlobalValue && type(it.type()) //TODO bug in type() method
}

////////////////////////////////////////////////////////////////////////////////////////
// Type matchers
////////////////////////////////////////////////////////////////////////////////////////
fun anytype(): TypeMatcher = { true }

fun primitive(): TypeMatcher = { it is PrimitiveType }

fun int(): TypeMatcher = { it is IntegerType }

fun aggregate(): TypeMatcher = { it is AggregateType }

fun array(elementType: TypeMatcher): TypeMatcher = { it is ArrayType && elementType(it.elementType()) }

fun struct(): TypeMatcher = { it is StructType }

fun i8(): TypeMatcher = { it == Type.I8 }

fun u8(): TypeMatcher = { it == Type.U8 }

fun it_is(value: Value): ValueMatcher = { it == value }