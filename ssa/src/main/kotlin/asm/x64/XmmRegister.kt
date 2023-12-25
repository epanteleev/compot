package asm.x64

import java.lang.IllegalArgumentException

enum class XmmRegister : Register {
    xmm0 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm0"
                16 -> "%xmm0"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },
    xmm1 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm1"
                16 -> "%xmm1"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm2 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm2"
                16 -> "%xmm2"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm3 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm3"
                16 -> "%xmm3"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm4 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm4"
                16 -> "%xmm4"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm5 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm5"
                16 -> "%xmm5"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm6 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm6"
                16 -> "%xmm6"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm7 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = true
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm7"
                16 -> "%xmm7"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm8 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm8"
                16 -> "%xmm8"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm9 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm9"
                16 -> "%xmm9"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm10 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm10"
                16 -> "%xmm10"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm11 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm11"
                16 -> "%xmm11"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm12 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm12"
                16 -> "%xmm12"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm13 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm13"
                16 -> "%xmm13"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm14 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm14"
                16 -> "%xmm14"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },

    xmm15 {
        override val isCallEESave: Boolean = false
        override val isCallERSave: Boolean = true
        override val isArgument: Boolean = false
        override fun toString(size: Int): String {
            val string = when (size) {
                32 -> "%ymm15"
                16 -> "%xmm15"
                else -> throw IllegalArgumentException("size=$size")
            }
            return string
        }
    },
}