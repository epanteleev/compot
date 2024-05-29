package asm.x64

import asm.x64.CPUInstruction.Companion.prefix


sealed interface CPUInstruction {
    companion object {
        fun prefix(size: Int): Char {
            return when (size) {
                8 -> 'q'
                4 -> 'l'
                2 -> 'w'
                1 -> 'b'
                else -> throw RuntimeException("Unknown operand size: $size")
            }
        }
    }
}

object Leave: CPUInstruction {
    override fun toString(): String = "leave"
}

object Ret: CPUInstruction {
    override fun toString(): String = "ret"
}

data class Push(val size: Int, val operand: Operand): CPUInstruction {
    override fun toString(): String {
        return "push${prefix(size)} ${operand.toString(size)}"
    }
}

data class Pop(val size: Int, val register: GPRegister): CPUInstruction {
    override fun toString(): String {
        return "pop${prefix(size)} ${register.toString(size)}"
    }
}

data class Mov(val size: Int, val src: Operand, val des: Operand): CPUInstruction {
    override fun toString(): String {
        return "mov${prefix(size)} ${src.toString(size)}, ${des.toString(size)}"
    }
}

data class MovAbs(val size: Int, val src: Operand, val des: Register): CPUInstruction {
    override fun toString(): String {
        return "movabs${prefix(size)} ${src.toString(size)}, ${des.toString(size)}"
    }
}

data class Movsx(val fromSize: Int, val toSize: Int, val src: Operand, val des: Operand): CPUInstruction {
    init {
        assert(fromSize <= toSize && fromSize < 4) {
            "cannot be: fromSize=$fromSize, toSize=$toSize"
        }
    }

    override fun toString(): String {
        return "movsx${prefix(fromSize)} ${src.toString(fromSize)}, ${des.toString(toSize)}"
    }
}

data class Movsxd(val fromSize: Int, val toSize: Int, val src: Operand, val des: Operand): CPUInstruction {
    init {
        assert(fromSize <= toSize && (fromSize == 4 || fromSize == 8)) {
            "cannot be: fromSize=$fromSize, toSize=$toSize"
        }
    }

    override fun toString(): String {
        return "movsxd ${src.toString(fromSize)}, ${des.toString(toSize)}"
    }
}

data class Movzx(val fromSize: Int, val toSize: Int, val src: Operand, val des: Operand): CPUInstruction {
    override fun toString(): String {
        return "movz${prefix(fromSize)}${prefix(toSize)} ${src.toString(fromSize)}, ${des.toString(toSize)}"
    }
}

data class Lea(val size: Int, val src: Operand, val des: Register): CPUInstruction {
    init {
        assert(size == 2 || size == 4 || size == 8) {
            "shoild be, but size=$size"
        }
    }

    override fun toString(): String {
        return "lea${prefix(size)} ${src.toString(size)}, ${des.toString(size)}"
    }
}

interface Arithmetic: CPUInstruction

data class Add(val size: Int, val first: Operand, val second: Operand): Arithmetic {
    override fun toString(): String {
        return "add${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

data class Sub(val size: Int, val first: Operand, val second: Operand): Arithmetic {
    override fun toString(): String {
        return "sub${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

data class iMull(val size: Int, val third: Imm32?, val first: Operand, val second: Operand): Arithmetic {
    override fun toString(): String {
        val t = if (third != null) {
            " ,$third"
        } else {
            ""
        }

        return "imul${prefix(size)}$t ${first.toString(size)}, ${second.toString(size)}"
    }
}

data class Xor(val size: Int, val src: Operand, val dst: Operand): Arithmetic {
    override fun toString(): String {
        return "xor${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class And(val size: Int, val src: Operand, val dst: Operand): Arithmetic {
    override fun toString(): String {
        return "and${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Or(val size: Int, val src: Operand, val dst: Operand): Arithmetic {
    override fun toString(): String {
        return "or${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Div(val size: Int, val divider: Operand): Arithmetic {
    override fun toString(): String {
        return "div${prefix(size)} ${divider.toString(size)}"
    }
}

data class Idiv(val size: Int, val divider: Operand): Arithmetic {
    override fun toString(): String {
        return "idiv${prefix(size)} ${divider.toString(size)}"
    }
}

data class Convert(val toSize: Int): CPUInstruction {
    override fun toString(): String {
        return when (toSize) {
            2 -> "cwd"
            4 -> "cdq"
            8 -> "cqo"
            else -> throw RuntimeException("Unknown size: $toSize")
        }
    }
}

data class Label(val id: String) {
    override fun toString(): String {
        return id
    }
}

enum class CondType {
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

data class Jcc(val jumpType: CondType, val label: String): CPUInstruction {
    override fun toString(): String {
        return "$jumpType $label"
    }
}

data class Jump(val label: String): CPUInstruction {
    override fun toString(): String {
        return "jmp $label"
    }
}

data class Cmp(val size: Int, val first: Operand, val second: Operand): CPUInstruction {
    override fun toString(): String {
        return "cmp${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

data class Call(private val data: String): CPUInstruction {

    constructor(regOrMem: Operand): this("*${regOrMem.toString(8)}")

    override fun toString(): String {
        return "callq $data"
    }
}

data class Test(val size: Int, val first: Register, val second: Operand): CPUInstruction {
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
}

data class SetCc(val size: Int, val tp: SetCCType, val reg: Operand): CPUInstruction {
    override fun toString(): String {
        return "$tp ${reg.toString(1)}"
    }
}

data class Addss(val size: Int, val src: Operand, val des: Operand): CPUInstruction {
    override fun toString(): String {
        return "addss ${src.toString(size)}, ${des.toString(size)}"
    }
}

data class Addsd(val size: Int, val src: Operand, val des: Operand): CPUInstruction {
    override fun toString(): String {
        return "addsd ${src.toString(size)}, ${des.toString(size)}"
    }
}

data class Movss(val size: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "movss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Movsd(val size: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "movsd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Movd(val size: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return if (src is XmmRegister) {
            "movd ${src.toString(16)}, ${dst.toString(size)}"
        } else if (dst is XmmRegister) {
            "movd ${src.toString(size)}, ${dst.toString(16)}"
        } else {
            throw RuntimeException("Internal error: src=$src, des=$dst")
        }
    }
}

data class Neg(val size: Int, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "neg${prefix(size)} ${dst.toString(size)}"
    }
}

data class Not(val size: Int, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "not${prefix(size)} ${dst.toString(size)}"
    }
}

data class Xorps(val size: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "xorps ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Xorpd(val size: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "xorpd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Subss(val size: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "subss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Subsd(val size: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "subsd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Mulss(val size: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "mulss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Mulsd(val size: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "mulsd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Divss(val size: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "divss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Divsd(val size: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "divsd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Ucomiss(val size: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "ucomiss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Ucomisd(val size: Int, val src1: Operand, val src2: Operand): CPUInstruction {
    override fun toString(): String {
        return "ucomisd ${src1.toString(size)}, ${src2.toString(size)}"
    }
}

data class Cvtsd2ss(val src1: Operand, val src2: Operand): CPUInstruction {
    override fun toString(): String {
        return "cvtsd2ss ${src1.toString(16)}, ${src2.toString(16)}"
    }
}

data class Cvtss2sd(val src1: Operand, val src2: Operand): CPUInstruction {
    override fun toString(): String {
        return "cvtss2sd ${src1.toString(16)}, ${src2.toString(16)}"
    }
}

data class Cvtsd2si(val toSize: Int, val src1: Operand, val src2: Operand): CPUInstruction {
    override fun toString(): String {
        return "cvtsd2si ${src1.toString(16)}, ${src2.toString(toSize)}"
    }
}

data class Cvtss2si(val toSize: Int, val src1: Operand, val src2: Operand): CPUInstruction {
    override fun toString(): String {
        return "cvtss2si ${src1.toString(16)}, ${src2.toString(toSize)}"
    }
}

data class Cvtsi2ss(val fromSize: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "cvtsi2ss ${src.toString(fromSize)}, ${dst.toString(16)}"
    }
}

data class Cvtsi2sd(val fromSize: Int, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "cvtsi2sd ${src.toString(fromSize)}, ${dst.toString(16)}"
    }
}

enum class CMoveFlag {
    CMOVA { // Move if above (CF=0 and ZF=0).
        override fun toString(): String = "cmova"
    },
    CMOVAE { // Move if above or equal (CF=0).
        override fun toString(): String = "cmovae"
    },
    CMOVB { // Move if below (CF=1).
        override fun toString(): String = "cmovb"
    },
    CMOVBE { // Move if below or equal (CF=1 or ZF=1).
        override fun toString(): String = "cmovbe"
    },
    CMOVC { // Move if carry (CF=1).
        override fun toString(): String = "cmovc"
    },
    CMOVE { // Move if equal (ZF=1).
        override fun toString(): String = "cmove"
    },
    CMOVG { // Move if greater (ZF=0 and SF=OF).
        override fun toString(): String = "cmovg"
    },
    CMOVGE { // Move if greater or equal (SF=OF).
        override fun toString(): String = "cmovge"
    },
    CMOVL { // Move if less (SF≠ OF).
        override fun toString(): String = "cmovl"
    },
    CMOVLE { // Move if less or equal (ZF=1 or SF≠ OF).
        override fun toString(): String = "cmovle"
    },
    CMOVNA { // Move if not above (CF=1 or ZF=1).
        override fun toString(): String = "cmovna"
    },
    CMOVNAE { // Move if not above or equal (CF=1).
        override fun toString(): String = "cmovnae"
    },
    CMOVNB { // Move if not below (CF=0).
        override fun toString(): String = "cmovnb"
    },
    CMOVNBE { // Move if not below or equal (CF=0 and ZF=0).
        override fun toString(): String = "cmovnbe"
    },
    CMOVNC { // Move if not carry (CF=0).
        override fun toString(): String = "cmovnc"
    },
    CMOVNE { // Move if not equal (ZF=0).
        override fun toString(): String = "cmovne"
    },
    CMOVNG { // Move if not greater (ZF=1 or SF≠ OF).
        override fun toString(): String = "cmovng"
    },
    CMOVNGE { // Move if not greater or equal (SF≠ OF).
        override fun toString(): String = "cmovnge"
    },
    CMOVNL { // Move if not less (SF=OF).
        override fun toString(): String = "cmovnl"
    },
    CMOVNLE { // Move if not less or equal (ZF=0 and SF=OF).
        override fun toString(): String = "cmovnle"
    },
    CMOVNO { // Move if not overflow (OF=0).
        override fun toString(): String = "cmovno"
    },
    CMOVNP { // Move if not parity (PF=0).
        override fun toString(): String = "cmovnp"
    },
    CMOVNS { // Move if not sign (SF=0).
        override fun toString(): String = "cmovns"
    },
    CMOVNZ { // Move if not zero (ZF=0).
        override fun toString(): String = "cmovnz"
    },
    CMOVO { // Move if overflow (OF=1).
        override fun toString(): String = "cmovo"
    },
    CMOVP { // Move if parity (PF=1).
        override fun toString(): String = "cmovp"
    },
    CMOVPE { // Move if parity even (PF=1).
        override fun toString(): String = "cmovpe"
    },
    CMOVPO { // Move if parity odd (PF=0).
        override fun toString(): String = "cmovpo"
    },
    CMOVS { // Move if sign (SF=1).
        override fun toString(): String = "cmovs"
    },
    CMOVZ { // Move if zero (ZF=1).
        override fun toString(): String = "cmovz"
    }
}

data class CMOVcc(val size: Int, val flag: CMoveFlag, val src: Operand, val dst: Operand): CPUInstruction {
    override fun toString(): String {
        return "$flag ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Comment(val message: String): CPUInstruction {
    override fun toString(): String = "# $message"
}