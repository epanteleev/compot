package asm.x64

import asm.x64.CPUInstruction.Companion.prefix

interface CPUInstruction {

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

data class Movss(val size: Int, val src: AnyOperand, val des: Register): CPUInstruction {
    override fun toString(): String {
        return "movss${prefix(size)} ${src.toString(size)}, ${des.toString(size)}"
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

data class Xor(val size: Int, val first: AnyOperand, val second: Operand): Arithmetic {
    override fun toString(): String {
        return "xor${prefix(size)} ${first.toString(size)}, ${second.toString(size)}"
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
    JE {
        override fun toString(): String {
            return "je"
        }
    },
    JNE {
        override fun toString(): String {
            return "jne"
        }
    },
    JG {
        override fun toString(): String {
            return "jg"
        }
    },
    JGE  {
        override fun toString(): String {
            return "jge"
        }
    },
    JL {
        override fun toString(): String {
            return "jl"
        }
    },
    JLE {
        override fun toString(): String {
            return "jle"
        }
    },
    JMP {
        override fun toString(): String {
            return "jmp"
        }
    },
}

data class Jump(val jumpType: JmpType, val label: String): CPUInstruction {
    override fun toString(): String {
        return "$jumpType $label"
    }
}

data class Cmp(val size: Int, val first: AnyOperand, val second: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "cmp${prefix(size)} ${second.toString(size)}, ${first.toString(size)}"
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