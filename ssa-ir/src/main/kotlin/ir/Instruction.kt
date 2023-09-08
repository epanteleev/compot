package ir

import java.lang.RuntimeException

abstract class Instruction(protected val tp: Type, var usages: ArrayList<Value>) {
    internal open fun renameUsages(fn: (Value) -> Value) {
        for (i in 0 until usages.size) {
            usages[i] = fn(usages[i])
        }
    }

    fun usedInstructions(): List<Instruction> {
        return usages.filterIsInstanceTo<Instruction, MutableList<Instruction>>(arrayListOf())
    }

    abstract fun dump(): String
}

abstract class ValueInstruction(protected val define: Int, tp: Type, usages: ArrayList<Value>): Instruction(tp, usages), LocalValue {
    override fun defined(): Int {
        return define
    }

    override fun toString(): String {
        return "%$define"
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

class ArithmeticUnary(index: Int, tp: Type, val op: ArithmeticUnaryOp, value: Value): ValueInstruction(index, tp, arrayListOf(value)) {
    override fun dump(): String {
        return "%$define = $op $tp ${operand()}"
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
    Sar,
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
            Sar -> "sar"
            Shl -> "shl"
            And -> "and"
            Or  -> "or"
            Xor -> "xor"
        }
        return name
    }
}

class ArithmeticBinary(index: Int, tp: Type, a: Value, val op: ArithmeticBinaryOp, b: Value): ValueInstruction(index, tp, arrayListOf(a, b)) {
    override fun dump(): String {
        return "%$define = $op $tp ${first()}, ${second()}"
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

class IntCompare(index: Int, a: Value, val predicate: IntPredicate, b: Value) : ValueInstruction(index, Type.U1, arrayListOf(a, b)) {
    override fun dump(): String {
        return "%$define = icmp $predicate ${first()}, ${second()}"
    }

    fun first(): Value {
        return usages.first()
    }

    fun second(): Value {
        return usages.last()
    }
}

class Load(index: Int, ptr: Value): ValueInstruction(index, ptr.type().dereference(), arrayListOf(ptr)) {
    override fun dump(): String {
        return "%$define = load $tp ${operand()}"
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

class StackAlloc(index: Int, ty: Type, val size: Long): ValueInstruction(index, ty.ptr(), arrayListOf()) {
    override fun dump(): String {
        return "%$define = stackalloc $tp $size"
    }
}

class Call(index: Int, tp: Type, val func: AnyFunction, args: ArrayList<Value>): ValueInstruction(index, tp, args) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Call

        if (define != other.define) return false
        if (func != other.func) return false
        return usages === other.usages
    }

    override fun hashCode(): Int {
        var result = define.hashCode()
        result = 31 * result + func.hashCode()
        result = 31 * result + usages.hashCode()
        return result
    }

    fun arguments(): List<Value> {
        return usages
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%$define = call $tp ${func.name}(")
        usages.joinTo(builder) { "$it:${it.type()}"}
        builder.append(")")
        return builder.toString()
    }
}

class GetElementPtr(indexDes: Int, tp: Type, source: Value, index: Value): ValueInstruction(indexDes, tp, arrayListOf(source, index)) {
    override fun dump(): String {
        return "%$define = gep $tp ${source()}, ${index()}"
    }

    fun source(): Value {
        return usages[0]
    }

    fun index(): Value {
        return usages[1]
    }
}

class Cast(index: Int, ty: Type, val castType: CastType, value: Value): ValueInstruction(index, ty, arrayListOf(value)) {
    override fun dump(): String {
        return "%$define = $castType $tp ${value()}"
    }

    fun value(): Value {
        return usages[0]
    }
}

class Phi(index: Int, ty: Type, private val incoming: List<Label>, incomingValue: ArrayList<Value>): ValueInstruction(index, ty, incomingValue) {
    override fun dump(): String {
        val bulder = StringBuilder()
        bulder.append("%$define = phi $tp ")

        for ((v, l) in usages.zip(incoming)) {
            bulder.append("[$v, $l]")
        }
        return bulder.toString()
    }

    override fun renameUsages(fn: (Value) -> Value) {
        throw RuntimeException("Don't use it for phi node")
    }

    fun renameUsagesInPhi(fn: (Value, Label) -> Value) {
       for (i in 0 until usages.size) {
           usages[i] = fn(usages[i], incoming[i])
       }
    }

    fun incoming(): List<Label> {
         return incoming
    }

    companion object {
        fun create(index: Int, ty: Type): Phi {
            return Phi(index, ty, arrayListOf(), arrayListOf())
        }
    }
}

class Select(index: Int, ty: Type, cond: Value, onTrue: Value, onFalse: Value): ValueInstruction(index, ty, arrayListOf(cond, onTrue, onFalse)) {
    override fun dump(): String {
        return "%$define = select $tp ${condition()} ${onTrue()}, ${onFalse()}"
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

abstract class TerminateInstruction(usages: ArrayList<Value>, val targets: Array<BasicBlock>): Instruction(Type.UNDEF, usages) {
    fun targets(): Array<BasicBlock> {
        return targets
    }
}

class Branch(target: BasicBlock): TerminateInstruction(arrayListOf(), arrayOf(target)) {
    override fun dump(): String {
        return "br label ${target()}"
    }

    fun target(): BasicBlock {
        return targets[0]
    }
}

class BranchCond(value: Value, onTrue: BasicBlock, onFalse: BasicBlock): TerminateInstruction(arrayListOf(value), arrayOf(onTrue, onFalse)) {
    override fun dump(): String {
        return "br i1 ${condition()} label ${onTrue()}, label ${onFalse()} "
    }

    fun condition(): Value {
        return usages[0]
    }

    fun onTrue(): BasicBlock {
        return targets[0]
    }

    fun onFalse(): BasicBlock {
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