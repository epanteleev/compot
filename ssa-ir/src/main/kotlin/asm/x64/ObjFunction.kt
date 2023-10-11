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

    private fun addInstruction(inst: CPUInstruction) {
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
        addInstruction(Add(first, destination))
        return destination
    }

    fun sub(first: AnyOperand, destination: Register): Operand {
        addInstruction(Sub(first, destination))
        return destination
    }

    fun mul(first: AnyOperand, destination: Register): Operand {
        addInstruction(iMull(first, destination))
        return destination
    }

    fun div(first: AnyOperand, destination: Register): Operand {
        addInstruction(Div(first, destination))
        return destination
    }

    fun xor(first: AnyOperand, destination: Register): Operand {
        addInstruction(Xor(first, destination))
        return destination
    }

    fun test(first: Register, second: Operand) {
        addInstruction(Test(first, second))
    }

    fun setcc(tp: SetCCType, reg: GPRegister) {
        addInstruction(SetCc(tp, reg))
    }

    fun push(reg: GPRegister) {
        addInstruction(Push(reg))
    }

    fun push(imm: Imm) {
        addInstruction(Push(imm))
    }

    fun pop(toReg: GPRegister) {
        addInstruction(Pop(toReg))
    }

    fun <T: Operand> mov(src: AnyOperand, des: T): T {
        addInstruction(Mov(src, des))
        return des
    }

    fun call(name: String) {
        addInstruction(Call(name))
    }

    fun cmp(first: Register, second: AnyOperand) {
        addInstruction(Cmp(first, second))
    }

    fun jump(jmpType: JmpType, label: String) {
        addInstruction(Jump(jmpType, label))
    }

    fun ret() {
        addInstruction(Ret)
    }

    fun leave() {
        addInstruction(Leave)
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