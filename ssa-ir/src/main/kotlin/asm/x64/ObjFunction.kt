package asm.x64

data class ObjFunctionCreationException(override val message: String): Exception(message)

private data class BuilderContext(var label: Label, var instructions: InstructionList)


private class InstructionList {
    private val list = arrayListOf<CPUInstruction>()

    fun add(inst: CPUInstruction) {
        if (list.isNotEmpty() && list.last() is Jump) {
            list.add(list.size - 1, inst)
        } else {
            list.add(inst)
        }
    }

    override fun toString(): String {
        return list.toString()
    }

    fun joinTo(builder: StringBuilder, prefix: String, separator: String) {
        list.joinTo(builder, separator, prefix)
    }
}

class ObjFunction(private val name: String) {
    private val codeBlocks: LinkedHashMap<Label, InstructionList>
    private val activeContext: BuilderContext

    init {
        val label = Label(name)
        val instructions = InstructionList()

        this.codeBlocks = linkedMapOf(Pair(label, instructions))
        activeContext = BuilderContext(label, instructions)
    }

    fun name(): String {
        return name
    }

    private fun ctx(): BuilderContext {
        return activeContext
    }

    private fun makeArithmetic(op: ArithmeticOp, first: AnyOperand, second: Register): Operand {
        ctx().instructions.add(Arithmetic(op, first, second))
        return second
    }

    fun label(name: String) {
        val newLabel = Label(name)
        if (codeBlocks[newLabel] != null) {
            throw ObjFunctionCreationException("Label with name '$name' already exist")
        }

        val newInstructions = InstructionList()
        codeBlocks[newLabel] = newInstructions
        activeContext.label = newLabel
        activeContext.instructions = newInstructions
    }

    fun switchLabel(name: String) {
        val newLabel = Label(name)
        val instructions = codeBlocks[newLabel]
            ?: throw ObjFunctionCreationException("Label with name '$name' doesn't exist")

        activeContext.label = newLabel
        activeContext.instructions = instructions
    }

    fun currentLabel(): String {
        return activeContext.label.id
    }

    fun add(first: AnyOperand, destination: Register): Operand {
        return makeArithmetic(ArithmeticOp.ADD, first, destination)
    }

    fun sub(first: AnyOperand, desttination: Register) {
        makeArithmetic(ArithmeticOp.SUB, first, desttination)
    }

    fun mul(first: AnyOperand, destination: Register) {
        makeArithmetic(ArithmeticOp.MUL, first, destination)
    }

    fun div(first: AnyOperand, destination: Register) {
        makeArithmetic(ArithmeticOp.DIV, first, destination)
    }

    fun xor(first: AnyOperand, destination: Register) {
        makeArithmetic(ArithmeticOp.XOR, first, destination)
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

    fun push(imm: Imm) {
        ctx().instructions.add(Push(imm))
    }

    fun pop(toReg: GPRegister) {
        ctx().instructions.add(Pop(toReg))
    }

    fun <T: Operand> mov(src: AnyOperand, des: T): T {
        ctx().instructions.add(Mov(src, des))
        return des
    }

    fun call(name: String) {
        ctx().instructions.add(Call(name))
    }

    fun cmp(first: Register, second: AnyOperand) {
        ctx().instructions.add(Cmp(first, second))
    }

    fun jump(jmpType: JmpType, label: String) {
        ctx().instructions.add(Jump(jmpType, label))
    }

    fun ret() {
        ctx().instructions.add(Ret)
    }

    fun leave() {
        ctx().instructions.add(Leave)
    }

    override fun toString(): String {
        val builder = StringBuilder()
        var count = 0
        for ((label, instructions) in codeBlocks) {
            builder.append("$label:\n")
            instructions.joinTo(builder, "    ", "\n    ")

            if (count < codeBlocks.size - 1) {
                builder.append('\n')
            }
            count += 1
        }

        return builder.toString()
    }

    companion object {
        val Ret = object : CPUInstruction {
            override fun toString(): String {
                return "ret"
            }
        }

        val Leave = object : CPUInstruction {
            override fun toString(): String {
                return "leave"
            }
        }
    }
}