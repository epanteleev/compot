package asm.x64

import asm.Operand
import asm.Register
import common.assertion
import asm.x64.GPRegister.rdx
import asm.x64.GPRegister.rax


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
        return "mov${prefix(size)} ${src.toString(size)}, ${des.toString(size)}"
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
        assertion(src == GPRegister.rcx || src is ImmInt) { "src=$src" }
    }

    override fun toString(): String {
        return "shr${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Sar(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    init {
        assertion(src == GPRegister.rcx || src is ImmInt) { "src=$src" }
    }

    override fun toString(): String {
        return "sar${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Shl(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    init {
        assertion(src == GPRegister.rcx || src is ImmInt) { "src=$src" }
    }

    override fun toString(): String {
        return "shl${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Sal(val size: Int, val src: Operand, val dst: Operand): CPUInstruction() {
    init {
        assertion(src == GPRegister.rcx || src is ImmInt) { "src=$src" }
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

enum class CondType { // TODO: unify with SetCCType
    JE { // Jump if equal (ZF=1).
        override fun toString(): String = "je"
    },
    JNE { // Jump if not equal (ZF=0).
        override fun toString(): String = "jne"
    },
    JG { // Jump if greater (ZF=0 and SF=OF).
        override fun toString(): String = "jg"
    },
    JGE  { // Jump if greater or equal (SF=OF).
        override fun toString(): String = "jge"
    },
    JL { // Jump if less (SF≠ OF).
        override fun toString(): String = "jl"
    },
    JLE { // Jump if less or equal (ZF=1 or SF≠ OF).
        override fun toString(): String = "jle"
    },
    JA { // Jump if below (CF=1).
        override fun toString(): String = "ja"
    },
    JAE { // Jump if above or equal (CF=0).
        override fun toString(): String = "jae"
    },
    JB { // Jump if below (CF=1).
        override fun toString(): String = "jb"
    },
    JBE { // Jump if below or equal (CF=1 or ZF=1).
        override fun toString(): String = "jbe"
    },
    JNA { // Jump if not above (CF=1 or ZF=1).
        override fun toString(): String = "jna"
    },
    JNAE { // Jump if not above or equal (CF=1).
        override fun toString(): String = "jnae"
    },
    JNB { // Jump if not below (CF=0).
        override fun toString(): String = "jnb"
    },
    JP { // Jump if parity (PF=1).
        override fun toString(): String = "jp"
    },
    JS { // Jump if sign (SF=1).
        override fun toString(): String = "js"
    },
    JZ { // Jump short if zero (ZF = 1).
        override fun toString(): String = "jz"
    },
    JNP {
        override fun toString(): String = "jnp"
    }
}

internal data class Jcc(val jumpType: CondType, val label: String): CPUInstruction() {
    override fun toString(): String {
        return "$jumpType $label"
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

enum class SetCCType {
    SETL {
        override fun toString(): String = "setl"
    },
    SETE {
        override fun toString(): String = "sete"
    },
    SETG {
        override fun toString(): String = "setg"
    },
    SETGE {
        override fun toString(): String = "setge"
    },
    SETLE  {
        override fun toString(): String = "setle"
    },
    SETNE {
        override fun toString(): String = "setne"
    },
    SETA {
        override fun toString(): String = "seta"
    },
    SETAE {
        override fun toString(): String = "setae"
    },
    SETB {
        override fun toString(): String = "setb"
    },
    SETBE {
        override fun toString(): String = "setbe"
    },
    SETNA {
        override fun toString(): String = "setna"
    },
    SETNAE {
        override fun toString(): String = "setnae"
    },
    SETNB {
        override fun toString(): String = "setnb"
    },
    SETP {
        override fun toString(): String = "setp"
    },
    SETNP {
        override fun toString(): String = "setnp"
    },
}

internal data class SetCc(val tp: SetCCType, val reg: Operand): CPUInstruction() {
    override fun toString(): String {
        return "$tp ${reg.toString(1)}"
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

enum class CMoveFlag { //TODO: unify with CondType
    CMOVA { // Move if above (CF=0 and ZF=0).
        override fun toString(): String = "cmova"
        override fun invert(): CMoveFlag = CMOVNA
    },
    CMOVAE { // Move if above or equal (CF=0).
        override fun toString(): String = "cmovae"
        override fun invert(): CMoveFlag = CMOVNAE
    },
    CMOVB { // Move if below (CF=1).
        override fun toString(): String = "cmovb"
        override fun invert(): CMoveFlag = CMOVNB
    },
    CMOVBE { // Move if below or equal (CF=1 or ZF=1).
        override fun toString(): String = "cmovbe"
        override fun invert(): CMoveFlag = CMOVNBE
    },
    CMOVC { // Move if carry (CF=1).
        override fun toString(): String = "cmovc"
        override fun invert(): CMoveFlag = CMOVNC
    },
    CMOVE { // Move if equal (ZF=1).
        override fun toString(): String = "cmove"
        override fun invert(): CMoveFlag = CMOVNE
    },
    CMOVG { // Move if greater (ZF=0 and SF=OF).
        override fun toString(): String = "cmovg"
        override fun invert(): CMoveFlag = CMOVNG
    },
    CMOVGE { // Move if greater or equal (SF=OF).
        override fun toString(): String = "cmovge"
        override fun invert(): CMoveFlag = CMOVNGE
    },
    CMOVL { // Move if less (SF≠ OF).
        override fun toString(): String = "cmovl"
        override fun invert(): CMoveFlag = CMOVNL
    },
    CMOVLE { // Move if less or equal (ZF=1 or SF≠ OF).
        override fun toString(): String = "cmovle"
        override fun invert(): CMoveFlag = CMOVNLE
    },
    CMOVNA { // Move if not above (CF=1 or ZF=1).
        override fun toString(): String = "cmovna"
        override fun invert(): CMoveFlag = CMOVA
    },
    CMOVNAE { // Move if not above or equal (CF=1).
        override fun toString(): String = "cmovnae"
        override fun invert(): CMoveFlag = CMOVAE
    },
    CMOVNB { // Move if not below (CF=0).
        override fun toString(): String = "cmovnb"
        override fun invert(): CMoveFlag = CMOVB
    },
    CMOVNBE { // Move if not below or equal (CF=0 and ZF=0).
        override fun toString(): String = "cmovnbe"
        override fun invert(): CMoveFlag = CMOVBE
    },
    CMOVNC { // Move if not carry (CF=0).
        override fun toString(): String = "cmovnc"
        override fun invert(): CMoveFlag = CMOVC
    },
    CMOVNE { // Move if not equal (ZF=0).
        override fun toString(): String = "cmovne"
        override fun invert(): CMoveFlag = CMOVE
    },
    CMOVNG { // Move if not greater (ZF=1 or SF≠ OF).
        override fun toString(): String = "cmovng"
        override fun invert(): CMoveFlag = CMOVG
    },
    CMOVNGE { // Move if not greater or equal (SF≠ OF).
        override fun toString(): String = "cmovnge"
        override fun invert(): CMoveFlag = CMOVGE
    },
    CMOVNL { // Move if not less (SF=OF).
        override fun toString(): String = "cmovnl"
        override fun invert(): CMoveFlag = CMOVL
    },
    CMOVNLE { // Move if not less or equal (ZF=0 and SF=OF).
        override fun toString(): String = "cmovnle"
        override fun invert(): CMoveFlag = CMOVLE
    },
    CMOVNO { // Move if not overflow (OF=0).
        override fun toString(): String = "cmovno"
        override fun invert(): CMoveFlag = CMOVO
    },
    CMOVNP { // Move if not parity (PF=0).
        override fun toString(): String = "cmovnp"
        override fun invert(): CMoveFlag = CMOVP
    },
    CMOVNS { // Move if not sign (SF=0).
        override fun toString(): String = "cmovns"
        override fun invert(): CMoveFlag = CMOVS
    },
    CMOVNZ { // Move if not zero (ZF=0).
        override fun toString(): String = "cmovnz"
        override fun invert(): CMoveFlag = CMOVZ
    },
    CMOVO { // Move if overflow (OF=1).
        override fun toString(): String = "cmovo"
        override fun invert(): CMoveFlag = CMOVNO
    },
    CMOVP { // Move if parity (PF=1).
        override fun toString(): String = "cmovp"
        override fun invert(): CMoveFlag = CMOVNP
    },
    CMOVPE { // Move if parity even (PF=1).
        override fun toString(): String = "cmovpe"
        override fun invert(): CMoveFlag = CMOVPO
    },
    CMOVPO { // Move if parity odd (PF=0).
        override fun toString(): String = "cmovpo"
        override fun invert(): CMoveFlag = CMOVPE
    },
    CMOVS { // Move if sign (SF=1).
        override fun toString(): String = "cmovs"
        override fun invert(): CMoveFlag = CMOVNS
    },
    CMOVZ { // Move if zero (ZF=1).
        override fun toString(): String = "cmovz"
        override fun invert(): CMoveFlag = CMOVNZ
    };

    abstract fun invert(): CMoveFlag
}

internal data class CMOVcc(val size: Int, val flag: CMoveFlag, val src: Operand, val dst: Operand): CPUInstruction() {
    override fun toString(): String {
        return "$flag ${src.toString(size)}, ${dst.toString(size)}"
    }
}

internal data class Comment(val message: String): CPUInstruction() {
    override fun toString(): String = "# $message"
}