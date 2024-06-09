package ir.platform.x64.codegen

import asm.x64.*
import asm.x64.GPRegister.rdx
import common.assertion
import ir.instruction.*
import ir.platform.x64.CallConvention.QWORD_SIZE


data class MacroAssemblerException(override val message: String): Exception(message)


class MacroAssembler(name: String): Assembler(name) {
    /*** Move reminder from 'rdx' register to @param rem. */
    fun moveRem(size: Int, rem: Operand) {
        assertion(size == 1 || size == 2 || size == 4 || size == 8) {
            "expects given operand size, but size=${size}"
        }

        if (rem == rdx) {
            return
        }
        when (rem) {
            is GPRegister -> mov(size, rdx, rem)
            is Address    -> mov(size, rdx, rem)
            else -> throw MacroAssemblerException("rem=$rem")
        }
    }

    private fun setccFloat(jmpType: FloatPredicate, dst: Operand) {
        when (jmpType) {
            FloatPredicate.Ult -> {
                when (dst) {
                    is Address    -> setcc(QWORD_SIZE, SetCCType.SETB, dst)
                    is GPRegister -> setcc(QWORD_SIZE, SetCCType.SETB, dst)
                    else -> throw MacroAssemblerException("unknown jmpType=$jmpType")
                }
            }
            //TODO not fully implemented
            else -> throw MacroAssemblerException("unknown jmpType=$jmpType")
        }
    }

    private fun setccInt(jmpType: IntPredicate, dst: Operand) {
        when (jmpType) {
            IntPredicate.Eq -> {
                when (dst) {
                    is Address    -> setcc(QWORD_SIZE, SetCCType.SETE, dst)
                    is GPRegister -> setcc(QWORD_SIZE, SetCCType.SETE, dst)
                }
            }
            IntPredicate.Ne -> {
                when (dst) {
                    is Address    -> setcc(QWORD_SIZE, SetCCType.SETNE, dst)
                    is GPRegister -> setcc(QWORD_SIZE, SetCCType.SETNE, dst)
                }
            }
            IntPredicate.Gt -> {
                when (dst) {
                    is Address    -> setcc(QWORD_SIZE, SetCCType.SETG, dst)
                    is GPRegister -> setcc(QWORD_SIZE, SetCCType.SETG, dst)
                }
            }
            IntPredicate.Ge -> {
                when (dst) {
                    is Address    -> setcc(QWORD_SIZE, SetCCType.SETGE, dst)
                    is GPRegister -> setcc(QWORD_SIZE, SetCCType.SETGE, dst)
                }
            }
            IntPredicate.Lt -> {
                when (dst) {
                    is Address    -> setcc(QWORD_SIZE, SetCCType.SETL, dst)
                    is GPRegister -> setcc(QWORD_SIZE, SetCCType.SETL, dst)
                }
            }
            IntPredicate.Le -> {
                when (dst) {
                    is Address    -> setcc(QWORD_SIZE, SetCCType.SETLE, dst)
                    is GPRegister -> setcc(QWORD_SIZE, SetCCType.SETLE, dst)
                }
            }
        }
    }

    fun setcc(jmpType: AnyPredicateType, dst: Operand) {
        assertion(dst is Address || dst is GPRegister) { "dst=${dst}" }
        when (jmpType) {
            is IntPredicate   -> setccInt(jmpType, dst)
            is FloatPredicate -> setccFloat(jmpType, dst)
            else -> throw MacroAssemblerException("unknown jmpType=$jmpType")
        }
    }

    fun condType(cond: CompareInstruction): CondType {
        val convType = cond.predicate().invert()
        return when (cond) {
            is SignedIntCompare -> {
                when (convType) {
                    IntPredicate.Eq -> CondType.JE
                    IntPredicate.Ne -> CondType.JNE
                    IntPredicate.Gt -> CondType.JG
                    IntPredicate.Ge -> CondType.JGE
                    IntPredicate.Lt -> CondType.JL
                    IntPredicate.Le -> CondType.JLE
                    else -> throw CodegenException("unknown conversion type: convType=$convType")
                }
            }
            is UnsignedIntCompare, is PointerCompare -> {
                when (convType) {
                    IntPredicate.Eq -> CondType.JE
                    IntPredicate.Ne -> CondType.JNE
                    IntPredicate.Gt -> CondType.JA
                    IntPredicate.Ge -> CondType.JAE
                    IntPredicate.Lt -> CondType.JB
                    IntPredicate.Le -> CondType.JBE
                    else -> throw CodegenException("unknown conversion type: convType=$convType")
                }
            }
            is FloatCompare -> {
                when (convType) {
                    FloatPredicate.Oeq -> CondType.JE // TODO Clang insert extra instruction 'jp ${labelName}"
                    FloatPredicate.Ogt -> TODO()
                    FloatPredicate.Oge -> CondType.JAE
                    FloatPredicate.Olt -> TODO()
                    FloatPredicate.Ole -> CondType.JBE
                    FloatPredicate.One -> CondType.JNE // TODO Clang insert extra instruction 'jp ${labelName}"
                    FloatPredicate.Ord -> TODO()
                    FloatPredicate.Ueq -> TODO()
                    FloatPredicate.Ugt -> TODO()
                    FloatPredicate.Uge -> TODO()
                    FloatPredicate.Ult -> TODO()
                    FloatPredicate.Ule -> CondType.JBE
                    FloatPredicate.Uno -> TODO()
                    FloatPredicate.Une -> TODO()
                    else -> throw CodegenException("unknown conversion type: convType=$convType")
                }
            }
            else -> throw CodegenException("unknown type instruction=$cond")
        }
    }
}