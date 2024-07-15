package asm.x64

enum class GPRegister : Register {
    rax {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rax"
                4 -> "%eax"
                2 -> "%ax"
                1 -> "%al"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 0
    },

    rbx {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rbx"
                4 -> "%ebx"
                2 -> "%bx"
                1 -> "%bl"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 3
    },

    rcx {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rcx"
                4 -> "%ecx"
                2 -> "%cx"
                1 -> "%cl"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 1
    },

    rsi {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rsi"
                4 -> "%esi"
                2 -> "%si"
                1 -> "%sil"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 6
    },

    rdi {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rdi"
                4 -> "%edi"
                2 -> "%di"
                1 -> "%dil"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 7
    },

    rdx {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rdx"
                4 -> "%edx"
                2 -> "%dx"
                1 -> "%dl"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 2
    },

    rbp {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rbp"
                4 -> "%ebp"
                2 -> "%bp"
                1 -> "%bpl"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 5
    },

    r8 {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r8"
                4 -> "%r8d"
                2 -> "%r8w"
                1 -> "%r8b"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 8
    },

    r9 {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r9"
                4 -> "%r9d"
                2 -> "%r9w"
                1 -> "%r9b"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 9
    },

    r10 {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r10"
                4 -> "%r10d"
                2 -> "%r10w"
                1 -> "%r10b"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 10
    },

    r11 {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r11"
                4 -> "%r11d"
                2 -> "%r11w"
                1 -> "%r11b"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 11
    },

    r12 {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r12"
                4 -> "%r12d"
                2 -> "%r12w"
                1 -> "%r12b"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 12
    },

    r13 {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r13"
                4 -> "%r13d"
                2 -> "%r13w"
                1 -> "%r13b"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 13
    },

    r14 {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r14"
                4 -> "%r14d"
                2 -> "%r14w"
                1 -> "%r14b"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 14
    },

    r15 {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%r15"
                4 -> "%r15d"
                2 -> "%r15w"
                1 -> "%r15b"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 15
    },

    rsp {
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rsp"
                4 -> "%esp"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }

        override fun encoding(): Int = 4
    };

    companion object {
        const val NUMBER_OF_GP_REGISTERS = 16
    }
}