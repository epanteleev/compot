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

    fun lea(size: Int, first: Address, destination: GPRegister) = add(Lea(size, first, destination))
    fun lea(size: Int, first: GPRegister, destination: GPRegister) = add(Lea(size, first, destination))

    fun add(size: Int, first: GPRegister, destination: GPRegister) = add(Add(size, first, destination))
    fun add(size: Int, first: Imm32, destination: GPRegister) = add(Add(size, first, destination))
    fun add(size: Int, first: GPRegister, destination: Address) = add(Add(size, first, destination))
    fun add(size: Int, first: Address, destination: GPRegister) = add(Add(size, first, destination))
    fun add(size: Int, first: Imm32, destination: Address) = add(Add(size, first, destination))

    fun sub(size: Int, first: GPRegister, destination: GPRegister) = add(Sub(size, first, destination))
    fun sub(size: Int, first: Imm32, destination: GPRegister) = add(Sub(size, first, destination))
    fun sub(size: Int, first: GPRegister, destination: Address) = add(Sub(size, first, destination))
    fun sub(size: Int, first: Address, destination: GPRegister) = add(Sub(size, first, destination))
    fun sub(size: Int, first: Imm32, destination: Address) = add(Sub(size, first, destination))

    fun mul(size: Int, src: GPRegister, dst: GPRegister) = add(iMull(size, src, dst))
    fun mul(size: Int, src: Imm32, dst: GPRegister) = add(iMull(size, src, dst))
    fun mul(size: Int, src: GPRegister, dst: Address) = add(iMull(size, src, dst))
    fun mul(size: Int, src: Address, dst: GPRegister) = add(iMull(size, src, dst))
    fun mul(size: Int, src: Imm32, dst: Address) = add(iMull(size, src, dst))


    fun xor(size: Int, src: Address, dst: GPRegister) = add(Xor(size, src, dst))
    fun xor(size: Int, imm32: Imm32, dst: Address) = add(Xor(size, imm32, dst))
    fun xor(size: Int, src: GPRegister, dst: GPRegister) = add(Xor(size, src, dst))
    fun xor(size: Int, imm32: Imm32, dst: GPRegister) = add(Xor(size, imm32, dst))
    fun xor(size: Int, src: GPRegister, dst: Address) = add(Xor(size, src, dst))


    fun movd(size: Int, src: GPRegister, dst: XmmRegister) = add(Movd(size, src, dst))
    fun movd(size: Int, src: XmmRegister, dst: GPRegister) = add(Movd(size, src, dst))
    fun movd(size: Int, src: Address, dst: XmmRegister) = add(Movd(size, src, dst))
    fun movd(size: Int, src: XmmRegister, dst: Address) = add(Movd(size, src, dst))

    fun neg(size: Int, dst: GPRegister) = add(Neg(size, dst))
    fun neg(size: Int, dst: Address) = add(Neg(size, dst))

    fun not(size: Int, dst: GPRegister) = add(Not(size, dst))
    fun not(size: Int, dst: Address) = add(Not(size, dst))

    fun div(size: Int, first: AnyOperand, destination: GPRegister): Operand {
        add(Div(size, first, destination))
        return destination
    }

    fun xor(size: Int, first: AnyOperand, destination: GPRegister): Operand {
        add(Xor(size, first, destination))
        return destination
    }

    fun test(size: Int, first: GPRegister, second: Operand) {
        add(Test(size, first, second))
    }

    fun setcc(size: Int, tp: SetCCType, reg: GPRegister) {
        add(SetCc(size, tp, reg))
    }

    fun push(size: Int, reg: GPRegister) = add(Push(size, reg))

    fun push(size: Int, imm: Imm32) = add(Push(size, imm))

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
    fun mov(size: Int, imm32: Imm32, dst: Address) {
        add(Mov(size, imm32, dst))
    }

    fun mov(size: Int, imm32: Imm32, dst: GPRegister) = add(Mov(size, imm32, dst))

    fun movsx(fromSize: Int, toSize: Int, src: GPRegister, des: AnyOperand) = add(Movsx(fromSize, toSize, src, des))

    fun call(name: String) {
        add(Call(name))
    }

    fun cmp(size: Int, first: AnyOperand, second: AnyOperand) = add(Cmp(size, first, second))

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

    // Subtract Scalar Single-Precision Floating-Point Values
    fun subss(src: Address, dst: XmmRegister) = add(Subss(16, src, dst))
    fun subss(src: XmmRegister, dst: XmmRegister) = add(Subss(16, src, dst))

    // Subtract Scalar Double-Precision Floating-Point Values
    fun subsd(src: Address, dst: XmmRegister) = add(Subsd(16, src, dst))
    fun subsd(src: XmmRegister, dst: XmmRegister) = add(Subsd(16, src, dst))

    fun subf(size: Int, src: Address, dst: XmmRegister) = when (size) {
        4 -> subss(src, dst)
        8 -> subsd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
    }
    fun subf(size: Int, src: XmmRegister, dst: XmmRegister) = when (size) {
        4 -> subss(src, dst)
        8 -> subsd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
    }

    // Multiply Scalar Single-Precision Floating-Point Values
    fun mulss(src: Address, dst: XmmRegister) = add(Mulss(16, src, dst))
    fun mulss(src: XmmRegister, dst: XmmRegister) = add(Mulss(16, src, dst))

    // Multiply Scalar Double-Precision Floating-Point Values
    fun mulsd(src: Address, dst: XmmRegister) = add(Mulsd(16, src, dst))
    fun mulsd(src: XmmRegister, dst: XmmRegister) = add(Mulsd(16, src, dst))

    fun mulf(size: Int, src: Address, dst: XmmRegister) = when (size) {
        4 -> mulss(src, dst)
        8 -> mulsd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
    }
    fun mulf(size: Int, src: XmmRegister, dst: XmmRegister) = when (size) {
        4 -> mulss(src, dst)
        8 -> mulsd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
    }

    // Divide Scalar Single-Precision Floating-Point Values
    fun divss(src: Address, dst: XmmRegister) = add(Divss(16, src, dst))
    fun divss(src: XmmRegister, dst: XmmRegister) = add(Divss(16, src, dst))

    // Divide Scalar Double-Precision Floating-Point Values
    fun divsd(src: Address, dst: XmmRegister) = add(Divsd(16, src, dst))
    fun divsd(src: XmmRegister, dst: XmmRegister) = add(Divsd(16, src, dst))

    fun divf(size: Int, src: Address, dst: XmmRegister) = when (size) {
        4 -> divss(src, dst)
        8 -> divsd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
    }
    fun divf(size: Int, src: XmmRegister, dst: XmmRegister) = when (size) {
        4 -> divss(src, dst)
        8 -> divsd(src, dst)
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

    // Bitwise Logical XOR of Packed Floating-Point Values
    fun xorpd(src: XmmRegister, dst: XmmRegister) = add(Xorpd(16, src, dst))
    fun xorpd(src: Address, dst: XmmRegister) = add(Xorpd(16, src, dst))
    fun xorps(src: XmmRegister, dst: XmmRegister) = add(Xorps(16, src, dst))
    fun xorps(src: Address, dst: XmmRegister) = add(Xorps(16, src, dst))
    fun xorpf(size: Int, src: Address, dst: XmmRegister) = when (size) {
        4 -> xorps(src, dst)
        8 -> xorpd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
    }
    fun xorpf(size: Int, src: XmmRegister, dst: XmmRegister) = when (size) {
        4 -> xorps(src, dst)
        8 -> xorpd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
    }

    // Unordered Compare Scalar Single-Precision Floating-Point Values and set EFLAGS
    fun ucomiss(src: Address, dst: XmmRegister) = add(Ucomiss(16, src, dst))
    fun ucomiss(src: XmmRegister, dst: XmmRegister) = add(Ucomiss(16, src, dst))

    // Unordered Compare Scalar Double-Precision Floating-Point Values and set EFLAGS
    fun ucomisd(src: Address, dst: XmmRegister) = add(Ucomisd(16, src, dst))
    fun ucomisd(src: XmmRegister, dst: XmmRegister) = add(Ucomisd(16, src, dst))

    fun cmpf(size: Int, src: Address, dst: XmmRegister) = when (size) {
        4 -> ucomiss(src, dst)
        8 -> ucomisd(src, dst)
        else -> throw IllegalArgumentException("size=$size, src=$src, dst=$dst")
    }

    fun cmpf(size: Int, src: XmmRegister, dst: XmmRegister) = when (size) {
        4 -> ucomiss(src, dst)
        8 -> ucomisd(src, dst)
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
}