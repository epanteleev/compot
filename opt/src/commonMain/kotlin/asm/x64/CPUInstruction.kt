package asm.x64

import common.assertion
import asm.x64.GPRegister.rdx
import asm.x64.GPRegister.rax
import ir.Definitions.QWORD_SIZE


sealed class CPUInstruction {
    companion object {
        fun prefix(size: Int): Char = when (size) {
            8 -> 'q'
            4 -> 'l'
            2 -> 'w'
            1 -> 'b'
            else -> throw RuntimeException("Unknown operand size: $size")
        }
    }
}

internal object Leave: CPUInstruction() {
    override fun toString(): String = "leave"
}

internal object Ret: CPUInstruction() {
    override fun toString(): String = "ret"
}

internal data class Push(val size: Int, val operand: Operand): CPUInstruction() {
    override fun toString(): String {
        return "push${prefix(size)} ${operand.toString(size)}"
    }
}

internal data class Pop(val size: Int, val register: GPRegister): CPUInstruction() {
    override fun toString(): String {
        return "pop${prefix(size)} ${register.toString(size)}"
    }
}

internal data class Mov(val size: Int, val src: Operand, val des: Operand): CPUInstruction() {
    override fun toString(): String {
        return if (size == QWORD_SIZE && src is Imm64) {
            "movabsq ${src.toString(size)}, ${des.toString(size)}"
        } else {
            "mov${prefix(size)} ${src.toString(size)}, ${des.toString(size)}"
        }
    }
}

internal data class Movsx(val fromSize: Int, val toSize: Int, val src: Operand, val des: Operand): CPUInstruction() {
    init {
        assertion(fromSize <= toSize && fromSize < 4) {
            "cannot be: fromSize=$fromSize, toSize=$toSize"
        }
    }

    override fun toString(): String {
        return "movsx${prefix(fromSize)} ${src.toString(fromSize)}, ${des.toString(toSize)}"
    }
}

internal data class Movsxd(val fromSize: Int, val toSize: Int, val src: Operand, val des: Operand): CPUInstruction() {
    init {
        assertion(fromSize <= toSize && (fromSize == 4 || fromSize == 8)) {
            "cannot be: fromSize=$fromSize, toSize=$toSize"
        }
    }

    override fun toString(): String {
        return "movsxd ${src.toString(fromSize)}, ${des.toString(toSize)}"
    }
}

internal data class Movzx(val fromSize: Int, val toSize: Int, val src: Operand, val des: Operand): CPUInstruction() {
    override fun toString(): String {
        return "movz${prefix(fromSize)}${prefix(toSize)} ${src.toString(fromSize)}, ${des.toString(toSize)}"
    }
}

internal data class Lea(val size: Int, val src: Operand, val des: Register): CPUInstruction() {
    init {
        assertion(ScaleFactor.isScaleFactor(size)) {
            "shoild be, but size=$size"
        }
    }

    override fun toString(): String {
        return "lea${prefix(size)} ${src.toString(size)}, ${des.toString(size)}"
    }
}

internal data class Add(val size: Int, val first: Operand, val second: Operand): CPUInstruction() {
    override fun toString(): String {
        return "add${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

internal data class Sub(val size: Int, val first: Operand, val second: Operand): CPUInstruction() {
    override fun toString(): String {
        return "sub${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

internal data class iMull(val size: Int, val third: Imm32?, val first: Operand, val second: Operand): CPUInstruction() {
    init {
        if (third != null) assertion(size != 1) { "size=$size" }
    }

    override fun toString(): String {
        val t = if (third != null) {
            " $third,"
        } else {
            ""
        }

        return "imul${prefix(size)}$t ${first.toString(size)}, ${second.toString(size)}"
    }
}

internal data class Xor(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "xor${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class And(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "and${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Shr(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    init {
        assertion(src == GPRegister.rcx || src is Imm) { "src=$src" }
    }

    override fun toString(): String {
        return "shr${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Sar(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    init {
        assertion(src == GPRegister.rcx || src is Imm) { "src=$src" }
    }

    override fun toString(): String {
        return "sar${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Shl(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    init {
        assertion(src == GPRegister.rcx || src is Imm) { "src=$src" }
    }

    override fun toString(): String {
        return "shl${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Sal(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    init {
        assertion(src == GPRegister.rcx || src is Imm) { "src=$src" }
    }

    override fun toString(): String {
        return "sal${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Or(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "or${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Div(val size: Int, val divider: Operand): CPUInstruction() {
    init {
        assertion(divider != rdx && divider != rax) {
            "Second operand cannot be $divider"
        }
    }

    override fun toString(): String {
        return "div${prefix(size)} ${divider.toString(size)}"
    }
}

internal data class Idiv(val size: Int, val divider: Operand): CPUInstruction() {
    init {
        assertion(divider != rdx && divider != rax) {
            "Second operand cannot be $divider"
        }
    }

    override fun toString(): String {
        return "idiv${prefix(size)} ${divider.toString(size)}"
    }
}

internal data class Convert(val toSize: Int): CPUInstruction() {
    override fun toString(): String = when (toSize) {
        2 -> "cwd"
        4 -> "cdq"
        8 -> "cqo"
        else -> throw RuntimeException("Unknown size: $toSize")
    }
}

data class Label(val id: String) {
    override fun toString(): String {
        return id
    }
}

internal data class Jcc(val jumpType: CondFlagType, val label: String): CPUInstruction() {
    private fun flagToJump(f: CondFlagType): String = when (f) {
        CondFlagType.EQ -> "je"
        CondFlagType.NE -> "jne"
        CondFlagType.G -> "jg"
        CondFlagType.GE -> "jge"
        CondFlagType.L -> "jl"
        CondFlagType.LE -> "jle"
        CondFlagType.A -> "ja"
        CondFlagType.AE -> "jae"
        CondFlagType.B -> "jb"
        CondFlagType.BE -> "jbe"
        CondFlagType.NA -> "jna"
        CondFlagType.NAE -> "jnae"
        CondFlagType.JNB -> "jnb"
        CondFlagType.P -> "jp"
        CondFlagType.S -> "js"
        CondFlagType.NS -> "jns"
        CondFlagType.Z -> "jz"
        CondFlagType.NZ -> "jnz"
        CondFlagType.NP -> "jnp"
    }

    override fun toString(): String {
        return "${flagToJump(jumpType)} $label"
    }
}

internal data class Jump(val label: String): CPUInstruction() {
    override fun toString(): String {
        return "jmp $label"
    }
}

internal data class Cmp(val size: Int, val first: Operand, val second: Operand): CPUInstruction() {
    override fun toString(): String {
        return "cmp${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

internal data class Call(private val data: String): CPUInstruction() {
    constructor(regOrMem: Operand): this("*${regOrMem.toString(8)}")
    constructor(funSymbol: FunSymbol): this(funSymbol.toString())

    override fun toString(): String {
        return "callq $data"
    }
}

internal data class Test(val size: Int, val first: Operand, val second: Operand): CPUInstruction() {
    override fun toString(): String {
        return "test${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

internal data class SetCc(val tp: CondFlagType, val reg: Operand): CPUInstruction() {
    private fun flagToSet(f: CondFlagType): String = when (f) {
        CondFlagType.EQ -> "sete"
        CondFlagType.NE -> "setne"
        CondFlagType.G -> "setg"
        CondFlagType.GE -> "setge"
        CondFlagType.L -> "setl"
        CondFlagType.LE -> "setle"
        CondFlagType.A -> "seta"
        CondFlagType.AE -> "setae"
        CondFlagType.B -> "setb"
        CondFlagType.BE -> "setbe"
        CondFlagType.NA -> "setna"
        CondFlagType.NAE -> "setnae"
        CondFlagType.JNB -> "setnb"
        CondFlagType.P -> "setp"
        CondFlagType.S -> "sets"
        CondFlagType.NS -> "setns"
        CondFlagType.Z -> "setz"
        CondFlagType.NZ -> "setnz"
        CondFlagType.NP -> "setnp"
    }

    override fun toString(): String {
        return "${flagToSet(tp)} ${reg.toString(1)}"
    }
}

internal data class Addss(val size: Int, val src: Operand, val des: Operand): CPUInstruction() {
    override fun toString(): String {
        return "addss ${src.toString(size)}, ${des.toString(size)}"
    }
}

internal data class Addsd(val size: Int, val src: Operand, val des: Operand): CPUInstruction() {
    override fun toString(): String {
        return "addsd ${src.toString(size)}, ${des.toString(size)}"
    }
}

internal data class Movss(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "movss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Movsd(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "movsd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Neg(val size: Int, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "neg${prefix(size)} ${dst.toString(size)}"
    }
}

internal data class Not(val size: Int, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "not${prefix(size)} ${dst.toString(size)}"
    }
}

internal data class Xorps(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "xorps ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Xorpd(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "xorpd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Pxor(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "pxor ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Subss(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "subss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Subsd(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "subsd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Mulss(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "mulss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Mulsd(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "mulsd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Divss(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "divss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Divsd(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "divsd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Ucomiss(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "ucomiss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Ucomisd(val size: Int, val src1: Operand, val src2: Operand): CPUInstruction() {
    override fun toString(): String {
        return "ucomisd ${src1.toString(size)}, ${src2.toString(size)}"
    }
}

internal data class Cvtsd2ss(val src1: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "cvtsd2ss ${src1.toString(16)}, ${dst.toString(16)}"
    }
}

internal data class Cvtss2sd(val src1: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "cvtss2sd ${src1.toString(16)}, ${dst.toString(16)}"
    }
}

internal data class Cvttsd2si(val toSize: Int, val src1: Operand, val src2: Operand): CPUInstruction() {
    override fun toString(): String {
        return "cvttsd2si ${src1.toString(16)}, ${src2.toString(toSize)}"
    }
}

internal data class Cvttss2si(val toSize: Int, val src1: Operand, val src2: Operand): CPUInstruction() {
    override fun toString(): String {
        return "cvttss2si ${src1.toString(16)}, ${src2.toString(toSize)}"
    }
}

internal data class Cvtsi2ss(val fromSize: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "cvtsi2ss ${src.toString(fromSize)}, ${dst.toString(16)}"
    }
}

internal data class Cvtsi2sd(val fromSize: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "cvtsi2sd ${src.toString(fromSize)}, ${dst.toString(16)}"
    }
}

internal data class CMOVcc(val size: Int, val flag: CondFlagType, val src: Operand, val dst: Operand): CPUInstruction() {
    private fun flagToCMov(f: CondFlagType): String = when (f) {
        CondFlagType.EQ -> "cmove"
        CondFlagType.NE -> "cmovne"
        CondFlagType.G -> "cmovg"
        CondFlagType.GE -> "cmovge"
        CondFlagType.L -> "cmovl"
        CondFlagType.LE -> "cmovle"
        CondFlagType.A -> "cmova"
        CondFlagType.AE -> "cmovae"
        CondFlagType.B -> "cmovb"
        CondFlagType.BE -> "cmovbe"
        CondFlagType.NA -> "cmovna"
        CondFlagType.NAE -> "cmovnae"
        CondFlagType.JNB -> "cmovnb"
        CondFlagType.P -> "cmovp"
        CondFlagType.S -> "cmovs"
        CondFlagType.NS -> "cmovns"
        CondFlagType.Z -> "cmovz"
        CondFlagType.NZ -> "cmovnz"
        CondFlagType.NP -> "cmovnp"
    }

    override fun toString(): String {
        return "${flagToCMov(flag)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Comment(val message: String): CPUInstruction() {
    override fun toString(): String = "# $message"
}