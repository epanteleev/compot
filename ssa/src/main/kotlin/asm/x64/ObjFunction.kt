package asm.x64

data class ObjFunctionCreationException(override val message: String): Exception(message)

private data class BuilderContext(var label: Label, var instructions: InstructionList)

private class InstructionList {
    private val list = arrayListOf<CPUInstruction>()

    fun add(inst: CPUInstruction) {
        list.add(inst)
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

    private fun add(inst: CPUInstruction) {
        activeContext.instructions.add(inst)
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

    fun lea(size: Int, first: Address, destination: Register) = add(Lea(size, first, destination))
    fun lea(size: Int, first: Register, destination: Register) = add(Lea(size, first, destination))

    fun add(size: Int, first: Register, destination: Register) = add(Add(size, first, destination))
    fun add(size: Int, first: Imm, destination: Register) = add(Add(size, first, destination))
    fun add(size: Int, first: Register, destination: Address) = add(Add(size, first, destination))
    fun add(size: Int, first: Address, destination: Register) = add(Add(size, first, destination))
    fun add(size: Int, first: Imm, destination: Address) = add(Add(size, first, destination))

    fun sub(size: Int, first: AnyOperand, destination: Register): Operand {
        add(Sub(size, first, destination))
        return destination
    }

    fun mul(size: Int, first: AnyOperand, destination: Register): Operand {
        add(iMull(size, first, destination))
        return destination
    }

    fun div(size: Int, first: AnyOperand, destination: Register): Operand {
        add(Div(size, first, destination))
        return destination
    }

    fun xor(size: Int, first: AnyOperand, destination: Register): Operand {
        add(Xor(size, first, destination))
        return destination
    }

    fun test(size: Int, first: Register, second: Operand) {
        add(Test(size, first, second))
    }

    fun setcc(size: Int, tp: SetCCType, reg: GPRegister) {
        add(SetCc(size, tp, reg))
    }

    fun push(size: Int, reg: GPRegister) = add(Push(size, reg))

    fun push(size: Int, imm: Imm) = add(Push(size, imm))

    fun pop(size: Int, toReg: GPRegister) {
        add(Pop(size, toReg))
    }

    fun <T: Operand> mov(size: Int, src: AnyOperand, des: T): T {
        add(Mov(size, src, des))
        return des
    }

    fun movsx(fromSize: Int, toSize: Int, src: GPRegister, des: AnyOperand) = add(Movsx(fromSize, toSize, src, des))

    fun call(name: String) {
        add(Call(name))
    }

    fun cmp(size: Int, first: Register, second: AnyOperand) {
        add(Cmp(size, first, second))
    }

    fun jump(jmpType: JmpType, label: String) {
        add(Jump(jmpType, label))
    }

    fun ret() {
        add(Ret)
    }

    fun leave() {
        add(Leave)
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