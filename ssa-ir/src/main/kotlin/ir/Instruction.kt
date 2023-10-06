package ir

import ir.block.Block
import java.lang.RuntimeException

abstract class Instruction(protected val tp: Type, protected var usages: ArrayList<Value>) {
    internal open fun rewriteUsages(fn: (Value) -> Value) {
        for (i in 0 until usages.size) {
            usages[i] = fn(usages[i])
        }
    }

    fun usedInstructions(): List<ValueInstruction> {
        return usages.filterIsInstanceTo<ValueInstruction, MutableList<ValueInstruction>>(arrayListOf())
    }

    fun usages(): List<Value> {
        return usages
    }

    abstract fun dump(): String
}

abstract class ValueInstruction(protected val identifier: String, tp: Type, usages: ArrayList<Value>): Instruction(tp, usages), LocalValue {
    override fun name(): String {
        return identifier
    }

    override fun toString(): String {
        return "%$identifier"
    }

    override fun type(): Type {
        return tp
    }
}

enum class IntPredicate {
    Eq,
    Ne,
    Ugt,
    Uge,
    Ult,
    Ule,
    Sgt,
    Sge,
    Slt,
    Sle;

    override fun toString(): String {
        val name = when (this) {
            Eq  -> "eq"
            Ne  -> "ne"
            Uge -> "uge"
            Ugt -> "ugt"
            Ult -> "ult"
            Ule -> "ule"
            Sgt -> "sgt"
            Sge -> "sge"
            Slt -> "slt"
            Sle -> "sle"
        }
        return name
    }

    fun invert(): IntPredicate {
        return when (this) {
            Eq  -> Ne
            Ne  -> Eq
            Uge -> Ult
            Ugt -> Ule
            Ult -> Uge
            Ule -> Ugt
            Sgt -> Sle
            Sge -> Slt
            Slt -> Sge
            Sle -> Sgt
        }
    }
}

enum class ArithmeticUnaryOp {
    Neg,
    Not;

    override fun toString(): String {
        val name = when (this) {
            Neg -> "neq"
            Not -> "not"
        }
        return name
    }
}

class ArithmeticUnary(name: String, tp: Type, val op: ArithmeticUnaryOp, value: Value): ValueInstruction(name, tp, arrayListOf(value)) {
    override fun dump(): String {
        return "%$identifier = $op $tp ${operand()}"
    }

    fun operand(): Value {
        return usages[0]
    }
}

enum class ArithmeticBinaryOp {
    Add,
    Sub,
    Mul,
    Mod,
    Div,
    Shr,
    Shl,
    And,
    Or,
    Xor;

    override fun toString(): String {
        val name = when (this) {
            Add -> "add"
            Sub -> "sub"
            Mul -> "mul"
            Mod -> "mod"
            Div -> "div"
            Shr -> "shr"
            Shl -> "shl"
            And -> "and"
            Or  -> "or"
            Xor -> "xor"
        }
        return name
    }
}

class ArithmeticBinary(name: String, tp: Type, a: Value, val op: ArithmeticBinaryOp, b: Value): ValueInstruction(name, tp, arrayListOf(a, b)) {
    override fun dump(): String {
        return "%$identifier = $op $tp ${first()}, ${second()}"
    }

    fun first(): Value {
        return usages[0]
    }

    fun second(): Value {
        return usages[1]
    }
}

enum class CastType {
    ZeroExtend,
    SignExtend,
    Truncate,
    Bitcast;

    override fun toString(): String {
        val name = when (this) {
            ZeroExtend -> "zext"
            SignExtend -> "sext"
            Truncate   -> "trunc"
            Bitcast    -> "bitcast"
        }
        return name
    }
}

class IntCompare(name: String, a: Value, private val predicate: IntPredicate, b: Value) : ValueInstruction(name, Type.U1, arrayListOf(a, b)) {
    override fun dump(): String {
        return "%$identifier = icmp $predicate ${first().type()} ${first()}, ${second()}"
    }

    fun predicate(): IntPredicate {
        return predicate
    }

    fun first(): Value {
        return usages.first()
    }

    fun second(): Value {
        return usages.last()
    }
}

class Load(name: String, ptr: Value): ValueInstruction(name, ptr.type().dereference(), arrayListOf(ptr)) {
    override fun dump(): String {
        return "%$identifier = load $tp ${operand()}"
    }

    fun operand(): Value {
        return usages[0]
    }
}

class Store(pointer: Value, value: Value): Instruction(pointer.type(), arrayListOf(pointer, value)) {
    override fun dump(): String {
        return "store $tp ${pointer()}, ${value()}"
    }

    fun pointer(): Value {
        return usages[0]
    }

    fun value(): Value {
        return usages[1]
    }
}

class StackAlloc(name: String, ty: Type, val size: Long): ValueInstruction(name, ty.ptr(), arrayListOf()) {
    override fun dump(): String {
        return "%$identifier = stackalloc $tp $size"
    }
}

interface Callable: Value {
    fun arguments(): List<Value>

    fun prototype(): AnyFunctionPrototype
}

class Call(name: String, tp: Type, private val func: AnyFunctionPrototype, args: ArrayList<Value>):
    ValueInstruction(name, tp, args), Callable {
    init {
        require(func.type() != Type.Void) { "Must be non void" }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Call

        if (identifier != other.identifier) return false
        if (func != other.func) return false
        return usages === other.usages
    }

    override fun hashCode(): Int {
        var result = identifier.hashCode()
        result = 31 * result + func.hashCode()
        result = 31 * result + usages.hashCode()
        return result
    }

    override fun arguments(): List<Value> {
        return usages
    }

    override fun prototype(): AnyFunctionPrototype {
        return func
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%$identifier = call $tp ${func.name}(")
        usages.joinTo(builder) { "$it:${it.type()}"}
        builder.append(")")
        return builder.toString()
    }
}

class VoidCall(private val func: AnyFunctionPrototype, args: ArrayList<Value>): Instruction(Type.Void, args), Callable {
    init {
        require(func.type() == Type.Void) { "Must be void" }
    }

    override fun arguments(): List<Value> {
        return usages
    }

    override fun prototype(): AnyFunctionPrototype {
        return func
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("call $tp ${func.name}(")
        usages.joinTo(builder) { "$it:${it.type()}"}
        builder.append(")")
        return builder.toString()
    }

    override fun type(): Type {
        return Type.Void
    }
}

class GetElementPtr(name: String, tp: Type, source: Value, index: Value): ValueInstruction(name, tp, arrayListOf(source, index)) {
    override fun dump(): String {
        return "%$identifier = gep $tp ${source()}, ${index()}"
    }

    fun source(): Value {
        return usages[0]
    }

    fun index(): Value {
        return usages[1]
    }
}

class Cast(name: String, ty: Type, val castType: CastType, value: Value): ValueInstruction(name, ty, arrayListOf(value)) {
    override fun dump(): String {
        return "%$identifier = $castType $tp ${value()}"
    }

    fun value(): Value {
        return usages[0]
    }
}

class Phi(name: String, ty: Type, private val incoming: MutableList<Block>, incomingValue: ArrayList<Value>): ValueInstruction(name, ty, incomingValue) {
    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%$identifier = phi $tp [")
        usages.zip(incoming).joinTo(builder) {
            "${it.first}: ${it.second}"
        }
        builder.append(']')
        return builder.toString()
    }

    override fun rewriteUsages(fn: (Value) -> Value) {
        throw RuntimeException("Don't use it for phi node")
    }

    fun updateUsagesInPhi(fn: (Value, Block) -> Value) {
       for (i in 0 until usages.size) {
           usages[i] = fn(usages[i], incoming[i])
       }
    }

    fun updateIncoming(old: Block, new: Block) {
        val index = incoming.indexOf(old)
        assert(index != -1) {
            "cannot find old=$old"
        }

        incoming[index] = new
    }

    fun incoming(): List<Block> {
         return incoming
    }

    fun zip(): Iterator<Pair<Block, Value>> {
        return (incoming() zip usages()).iterator()
    }
    companion object {
        fun create(index: String, ty: Type): Phi {
            return Phi(index, ty, arrayListOf(), arrayListOf())
        }
    }
}

class Select(name: String, ty: Type, cond: Value, onTrue: Value, onFalse: Value): ValueInstruction(name, ty, arrayListOf(cond, onTrue, onFalse)) {
    override fun dump(): String {
        return "%$identifier = select $tp ${condition()} ${onTrue()}, ${onFalse()}"
    }

    fun condition(): Value {
        return usages[0]
    }

    fun onTrue(): Value {
        return usages[1]
    }

    fun onFalse(): Value {
        return usages[2]
    }
}

class Copy(name: String, origin: Value): ValueInstruction(name, origin.type(), arrayListOf(origin)) {
    override fun dump(): String {
        return "%$identifier = copy $tp ${origin()}"
    }

    fun origin(): Value {
        return usages[0]
    }
}

abstract class TerminateInstruction(usages: ArrayList<Value>, val targets: Array<Block>): Instruction(Type.UNDEF, usages) {
    fun targets(): Array<Block> {
        return targets
    }
}

class Branch(target: Block): TerminateInstruction(arrayListOf(), arrayOf(target)) {
    override fun dump(): String {
        return "br label ${target()}"
    }

    fun target(): Block {
        return targets[0]
    }
}

class BranchCond(value: Value, onTrue: Block, onFalse: Block): TerminateInstruction(arrayListOf(value), arrayOf(onTrue, onFalse)) {
    override fun dump(): String {
        return "br u1 ${condition()} label ${onTrue()}, label ${onFalse()} "
    }

    fun condition(): Value {
        return usages[0]
    }

    fun onTrue(): Block {
        return targets[0]
    }

    fun onFalse(): Block {
        return targets[1]
    }
}

class Return(value: Value): TerminateInstruction(arrayListOf(value), arrayOf()) {
    override fun dump(): String {
        return "ret ${value().type()} ${value()}"
    }

    fun value(): Value {
        return usages[0]
    }
}