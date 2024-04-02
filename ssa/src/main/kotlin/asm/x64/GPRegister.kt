package asm.x64

import java.lang.IllegalArgumentException

enum class GPRegister : Register {
    rax {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false

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
    },

    rbx {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false

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
    },

    rcx {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true

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
    },

    rsi {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true

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
    },

    rdi {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
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
    },

    rdx {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
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
    },

    rbp {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false
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
    },

    r8 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
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
    },

    r9 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
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
    },

    r10 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false
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
    },

    r11 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false
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
    },

    r12 {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false
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
    },

    r13 {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false
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
    },

    r14 {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false
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
    },

    r15 {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false
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
    },

    rsp {
        override val isCallEESave: Boolean = true
        override val isCallERSave: Boolean = false
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                8 -> "%rsp"
                4 -> "%esp"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    };
}
