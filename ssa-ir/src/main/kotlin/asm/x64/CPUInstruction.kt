package asm.x64

interface CPUInstruction {
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

data class Push(val operand: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "push${prefix(operand.size)} $operand"
    }
}

data class Pop(val register: GPRegister): CPUInstruction {
    override fun toString(): String {
        return "pop${prefix(register.size)} $register"
    }
}

data class Mov(val src: AnyOperand, val des: Operand): CPUInstruction {
    override fun toString(): String {
        return "mov${prefix(des.size)} $src, $des"
    }
}

data class MovAbs(val src: AnyOperand, val des: Register): CPUInstruction {
    override fun toString(): String {
        return "movabs${prefix(des.size)} $src, $des"
    }
}

data class Movss(val src: AnyOperand, val des: Register): CPUInstruction {
    override fun toString(): String {
        return "movss${prefix(des.size)} $src, $des"
    }
}

interface Arithmetic: CPUInstruction

data class Add(val first: AnyOperand, val second: Operand): Arithmetic {
    override fun toString(): String {
        return "add${prefix(first.size)} $first, $second"
    }
}

data class Sub(val first: AnyOperand, val second: Operand): Arithmetic {
    override fun toString(): String {
        return "sub${prefix(first.size)} $first, $second"
    }
}

data class iMull(val first: AnyOperand, val second: Operand): Arithmetic {
    override fun toString(): String {
        return "imul${prefix(first.size)} $first, $second"
    }
}

data class Xor(val first: AnyOperand, val second: Operand): Arithmetic {
    override fun toString(): String {
        return "xor${prefix(first.size)} $first, $second"
    }
}

data class Div(val first: AnyOperand, val second: Operand): Arithmetic {
    override fun toString(): String {
        return "div${prefix(first.size)} $first, $second"
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

data class Cmp(val first: AnyOperand, val second: AnyOperand): CPUInstruction {
    override fun toString(): String {
        return "cmp${prefix(first.size)} $second, $first"
    }
}

data class Call(val name: String): CPUInstruction {
    override fun toString(): String {
        return "callq $name"
    }
}

data class Test(val first: Register, val second: Operand): CPUInstruction {
    override fun toString(): String {
        return "test${prefix(first.size)} $first, $second"
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

data class SetCc(val tp: SetCCType, val reg: GPRegister): CPUInstruction {
    override fun toString(): String {
        return "$tp${prefix(reg.size)} $reg"
    }
}