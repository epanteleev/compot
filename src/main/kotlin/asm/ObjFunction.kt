package asm

import ir.Call

data class ObjFunctionCreationException(override val message: String): Exception(message)

private data class BuilderContext(var label: Label, var instructions: MutableList<CPUInstruction>)

class ObjFunction(val name: String) {
    private val instructions: LinkedHashMap<Label, MutableList<CPUInstruction>>
    private val activeContext: BuilderContext

    init {
        val label = Label(name)
        var insts = arrayListOf<CPUInstruction>()

        instructions = linkedMapOf(Pair(label, insts))
        activeContext = BuilderContext(label, insts)
    }

    private fun ctx(): BuilderContext {
        return activeContext
    }

    private fun makeArithmetic(op: ArithmeticOp, first: Operand, second: Register): Operand {
        ctx().instructions.add(Arithmetic(op, first, second))
        return second
    }

    fun label(name: String) {
        val newLabel = Label(name)
        if (instructions[newLabel] == null) {
            throw ObjFunctionCreationException("Label with name '$name' already exist")
        }

        val newInstructions = arrayListOf<CPUInstruction>()
        instructions[newLabel] = newInstructions
        activeContext.label = newLabel
        activeContext.instructions = newInstructions
    }

    fun add(first: Operand, second: Register): Operand {
        return makeArithmetic(ArithmeticOp.ADD, first, second)
    }

    fun sub(first: Operand, second: Register) {
        makeArithmetic(ArithmeticOp.SUB, first, second)
    }

    fun mul(first: Operand, second: Register) {
        makeArithmetic(ArithmeticOp.MUL, first, second)
    }

    fun div(first: Operand, second: Register) {
        makeArithmetic(ArithmeticOp.DIV, first, second)
    }

    fun xor(first: Operand, second: Register) {
        makeArithmetic(ArithmeticOp.XOR, first, second)
    }

    fun test(first: Register, second: Operand) {
        ctx().instructions.add(Test(first, second))
    }

    fun setcc(tp: SetCCType, reg: GPRegister) {
        ctx().instructions.add(SetCc(tp, reg))
    }

    fun push(reg: GPRegister) {
        ctx().instructions.add(Push(reg))
    }

    fun pop(toReg: Register) {
        ctx().instructions.add(Pop(toReg))
    }

    fun mov(src: Operand, des: Operand): Operand {
        ctx().instructions.add(Mov(src, des))
        return des
    }

    fun call(name: String) {
        ctx().instructions.add(Call(name))
    }

    fun cmp(first: Register, second: Operand) {
        ctx().instructions.add(Cmp(first, second))
    }

    fun ret() {
        ctx().instructions.add(Ret)
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for ((label, instructions) in instructions) {
            builder.append("$label:\n")
            instructions.joinTo(builder, prefix = "    ", separator = "\n    ")
        }

        return builder.toString()
    }

    companion object {
        val Ret = object : CPUInstruction {
            override fun toString(): String {
                return "ret"
            }
        }
    }
}