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

data class Push(val size: Int, val operand: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "push${prefix(size)} ${operand.toString(size)}"
    }
}

data class Pop(val size: Int, val register: GPRegister): CPUInstruction {
    override fun toString(): String {
        return "pop${prefix(size)} ${register.toString(size)}"
    }
}

data class Mov(val size: Int, val src: AnyOperand, val des: Operand): CPUInstruction {
    override fun toString(): String {
        return "mov${prefix(size)} ${src.toString(size)}, ${des.toString(size)}"
    }
}

data class MovAbs(val size: Int, val src: AnyOperand, val des: Register): CPUInstruction {
    override fun toString(): String {
        return "movabs${prefix(size)} ${src.toString(size)}, ${des.toString(size)}"
    }
}

data class Movsx(val fromSize: Int, val toSize: Int, val src: GPRegister, val des: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "movsx${prefix(fromSize)} ${src.toString(fromSize)}, ${des.toString(toSize)}"
    }
}

data class Lea(val size: Int, val src: AnyOperand, val des: Register): CPUInstruction {
    override fun toString(): String {
        return "lea${prefix(size)} ${src.toString(size)}, ${des.toString(size)}"
    }
}

interface Arithmetic: CPUInstruction

data class Add(val size: Int, val first: AnyOperand, val second: Operand): Arithmetic {
    override fun toString(): String {
        return "add${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

data class Sub(val size: Int, val first: AnyOperand, val second: Operand): Arithmetic {
    override fun toString(): String {
        return "sub${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

data class iMull(val size: Int, val first: AnyOperand, val second: Operand): Arithmetic {
    override fun toString(): String {
        return "imul${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

data class Xor(val size: Int, val src: AnyOperand, val dst: Operand): Arithmetic {
    override fun toString(): String {
        return "xor${prefix(size)} ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Div(val size: Int, val first: AnyOperand, val second: Operand): Arithmetic {
    override fun toString(): String {
        return "div${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

data class Label(val id: String) {
    override fun toString(): String {
        return id
    }
}

enum class JmpType {
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
    JMP { // TODO
        override fun toString(): String = "jmp"
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

data class Jump(val jumpType: JmpType, val label: String): CPUInstruction {
    override fun toString(): String {
        return "$jumpType $label"
    }
}

data class Cmp(val size: Int, val first: AnyOperand, val second: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "cmp${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

data class Call(val name: String): CPUInstruction {
    override fun toString(): String {
        return "callq $name"
    }
}

data class Test(val size: Int, val first: Register, val second: Operand): CPUInstruction {
    override fun toString(): String {
        return "test${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
    }
}

enum class SetCCType {
    SETL {
        override fun toString(): String {
            return "setl"
        }
    },
    SETE {
        override fun toString(): String {
            return "sete"
        }
    },
    SETG {
        override fun toString(): String {
            return "setg"
        }
    },
    SETGE {
        override fun toString(): String {
            return "setge"
        }
    },
    SETLE  {
        override fun toString(): String {
            return "setle"
        }
    },
    SETNE {
        override fun toString(): String {
            return "setne"
        }
    },
}

data class SetCc(val size: Int, val tp: SetCCType, val reg: GPRegister): CPUInstruction {
    override fun toString(): String {
        return "$tp${prefix(size)} ${reg.toString(size)}"
    }
}

data class Addss(val size: Int, val src: AnyOperand, val des: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "addss ${src.toString(size)}, ${des.toString(size)}"
    }
}

data class Addsd(val size: Int, val src: AnyOperand, val des: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "addsd ${src.toString(size)}, ${des.toString(size)}"
    }
}

data class Movss(val size: Int, val src: AnyOperand, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "movss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Movsd(val size: Int, val src: AnyOperand, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "movsd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Movd(val size: Int, val src: AnyOperand, val dst: AnyOperand): CPUInstruction {
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

data class Neg(val size: Int, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "neg${prefix(size)} ${dst.toString(size)}"
    }
}

data class Not(val size: Int, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "not${prefix(size)} ${dst.toString(size)}"
    }
}

data class Xorps(val size: Int, val src: AnyOperand, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "xorps ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Xorpd(val size: Int, val src: AnyOperand, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "xorpd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Subss(val size: Int, val src: AnyOperand, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "subss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Subsd(val size: Int, val src: AnyOperand, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "subsd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Mulss(val size: Int, val src: AnyOperand, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "mulss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Mulsd(val size: Int, val src: AnyOperand, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "mulsd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Divss(val size: Int, val src: AnyOperand, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "divss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Divsd(val size: Int, val src: AnyOperand, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "divsd ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Ucomiss(val size: Int, val src: AnyOperand, val dst: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "ucomiss ${src.toString(size)}, ${dst.toString(size)}"
    }
}

data class Ucomisd(val size: Int, val src1: AnyOperand, val src2: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "ucomisd ${src1.toString(size)}, ${src2.toString(size)}"
    }
}