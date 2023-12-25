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

// X86 and amd64 instruction reference
// https://www.felixcloutier.com/x86/
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
    fun add(size: Int, first: ImmInt, destination: Register) = add(Add(size, first, destination))
    fun add(size: Int, first: Register, destination: Address) = add(Add(size, first, destination))
    fun add(size: Int, first: Address, destination: Register) = add(Add(size, first, destination))
    fun add(size: Int, first: ImmInt, destination: Address) = add(Add(size, first, destination))

    fun sub(size: Int, first: Register, destination: Register) = add(Sub(size, first, destination))
    fun sub(size: Int, first: ImmInt, destination: Register) = add(Sub(size, first, destination))
    fun sub(size: Int, first: Register, destination: Address) = add(Sub(size, first, destination))
    fun sub(size: Int, first: Address, destination: Register) = add(Sub(size, first, destination))
    fun sub(size: Int, first: ImmInt, destination: Address) = add(Sub(size, first, destination))

    fun mul(size: Int, src: Register, dst: Register) = add(iMull(size, src, dst))
    fun mul(size: Int, src: ImmInt, dst: Register) = add(iMull(size, src, dst))
    fun mul(size: Int, src: Register, dst: Address) = add(iMull(size, src, dst))
    fun mul(size: Int, src: Address, dst: Register) = add(iMull(size, src, dst))
    fun mul(size: Int, src: ImmInt, dst: Address) = add(iMull(size, src, dst))


    fun movd(size: Int, src: GPRegister, dst: XmmRegister) = add(Movd(size, src, dst))
    fun movd(size: Int, src: XmmRegister, dst: Register) = add(Movd(size, src, dst))
    fun movd(size: Int, src: Address, dst: XmmRegister) = add(Movd(size, src, dst))
    fun movd(size: Int, src: XmmRegister, dst: Address) = add(Movd(size, src, dst))


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

    fun push(size: Int, imm: ImmInt) = add(Push(size, imm))

    fun pop(size: Int, toReg: GPRegister) {
        add(Pop(size, toReg))
    }

    fun <T: Operand> movOld(size: Int, src: AnyOperand, des: T): T {
        add(Mov(size, src, des))
        return des
    }

    fun mov(size: Int, src: GPRegister, dst: GPRegister) = add(Mov(size, src, dst))
    fun mov(size: Int, src: Address, dst: GPRegister) = add(Mov(size, src, dst))
    fun mov(size: Int, src: GPRegister, dst: Address) = add(Mov(size, src, dst))
    fun mov(size: Int, imm32: ImmInt, dst: Address) = add(Mov(size, imm32, dst))
    fun mov(size: Int, imm32: ImmInt, dst: GPRegister) = add(Mov(size, imm32, dst))

    fun movsx(fromSize: Int, toSize: Int, src: GPRegister, des: AnyOperand) = add(Movsx(fromSize, toSize, src, des))

    fun call(name: String) {
        add(Call(name))
    }

    fun cmp(size: Int, first: Register, second: AnyOperand) = add(Cmp(size, first, second))

    fun jump(jmpType: JmpType, label: String) = add(Jump(jmpType, label))

    fun ret() = add(Ret)
    fun leave() = add(Leave)

    // Add Scalar Double-Precision Floating-Point Values
    fun addsd(src: Address, dst: XmmRegister) = add(Addsd(16, src, dst))
    fun addsd(src: XmmRegister, dst: XmmRegister) = add(Addsd(16, src, dst))

    // Add Scalar Single-Precision Floating-Point Values
    fun addss(src: Address, dst: XmmRegister) = add(Addss(16, src, dst))
    fun addss(src: XmmRegister, dst: XmmRegister) = add(Addss(16, src, dst))

    fun addf(size: Int, src: Address, dst: XmmRegister) = when (size) {
        4 -> addss(src, dst)
        8 -> addsd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
    }
    fun addf(size: Int, src: XmmRegister, dst: XmmRegister) = when (size) {
        4 -> addss(src, dst)
        8 -> addsd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
    }

    // Move Scalar Single-Precision Floating-Point Values
    fun movss(src: Address, dst: XmmRegister) = add(Movss(16, src, dst))
    fun movss(src: XmmRegister, dst: XmmRegister) = add(Movss(16, src, dst))
    fun movss(src: XmmRegister, dst: Address) = add(Movss(16, src, dst))

    // Move Scalar Double-Precision Floating-Point Values
    fun movsd(src: Address, dst: XmmRegister) = add(Movsd(16, src, dst))
    fun movsd(src: XmmRegister, dst: XmmRegister) = add(Movsd(16, src, dst))
    fun movsd(src: XmmRegister, dst: Address) = add(Movsd(16, src, dst))

    fun movf(size: Int, src: XmmRegister, dst: XmmRegister) = when (size) {
        4 -> movss(src, dst)
        8 -> movsd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
    }
    fun movf(size: Int, src: Address, dst: XmmRegister) = when (size) {
        4 -> movss(src, dst)
        8 -> movsd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
    }
    fun movf(size: Int, src: XmmRegister, dst: Address) = when (size) {
        4 -> movss(src, dst)
        8 -> movsd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
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