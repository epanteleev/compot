package ir.platform.x64.codegen

import asm.Operand
import asm.x64.*
import asm.x64.GPRegister.rax
import common.assertion
import ir.Definitions.BYTE_SIZE
import ir.Definitions.QWORD_SIZE
import ir.attributes.VarArgAttribute
import ir.instruction.*
import ir.module.DirectFunctionPrototype
import ir.module.ExternFunction
import ir.module.FunctionPrototype
import ir.platform.MacroAssembler
import ir.platform.common.TargetPlatform
import ir.types.*


data class MacroAssemblerException(override val message: String): Exception(message)

class X64MacroAssembler(name: String, id: Int): Assembler(name, id), MacroAssembler {
    override fun platform(): TargetPlatform {
        return TargetPlatform.X64
    }

    private fun floatPredicateToSetCCType(jmpType: FloatPredicate): SetCCType = when (jmpType) {
        FloatPredicate.Oeq -> SetCCType.SETE
        FloatPredicate.Ogt -> SetCCType.SETA
        FloatPredicate.Oge -> SetCCType.SETAE
        FloatPredicate.Olt -> SetCCType.SETB
        FloatPredicate.Ole -> SetCCType.SETBE
        FloatPredicate.One -> SetCCType.SETNE
        FloatPredicate.Ord -> TODO()
        FloatPredicate.Ueq -> SetCCType.SETE
        FloatPredicate.Ugt -> SetCCType.SETA
        FloatPredicate.Uge -> SetCCType.SETAE
        FloatPredicate.Ult -> SetCCType.SETB
        FloatPredicate.Ule -> SetCCType.SETBE
        FloatPredicate.Uno -> TODO()
        FloatPredicate.Une -> SetCCType.SETNE
    }

    fun setccFloat(jmpType: FloatPredicate, dst: Operand) {
        val type = floatPredicateToSetCCType(jmpType)
        when (dst) {
            is Address    -> setcc(type, dst)
            is GPRegister -> setcc(type, dst)
            else -> throw MacroAssemblerException("unknown jmpType=$jmpType")
        }
    }

    private fun intPredicateToSetCCType(type: IntegerType, jmpType: IntPredicate): SetCCType = when(type) {
        is SignedIntType -> when (jmpType) {
            IntPredicate.Eq -> SetCCType.SETE
            IntPredicate.Ne -> SetCCType.SETNE
            IntPredicate.Gt -> SetCCType.SETG
            IntPredicate.Ge -> SetCCType.SETGE
            IntPredicate.Lt -> SetCCType.SETL
            IntPredicate.Le -> SetCCType.SETLE
        }
        is UnsignedIntType -> when (jmpType) {
            IntPredicate.Eq -> SetCCType.SETE
            IntPredicate.Ne -> SetCCType.SETNE
            IntPredicate.Gt -> SetCCType.SETA
            IntPredicate.Ge -> SetCCType.SETAE
            IntPredicate.Lt -> SetCCType.SETB
            IntPredicate.Le -> SetCCType.SETBE
        }
    }

    fun setccInt(operandsType: IntegerType, jmpType: IntPredicate, dst: Operand) {
        val type = intPredicateToSetCCType(operandsType, jmpType)
        when (dst) {
            is Address    -> setcc(type, dst)
            is GPRegister -> setcc(type, dst)
            else -> throw MacroAssemblerException("unknown jmpType=$jmpType")
        }
    }

    fun condType(cond: CompareInstruction, type: PrimitiveType): CondType {
        val convType = cond.predicate().invert()
        return when (type) {
            is SignedIntType -> when (convType) {
                IntPredicate.Eq -> CondType.JE
                IntPredicate.Ne -> CondType.JNE
                IntPredicate.Gt -> CondType.JG
                IntPredicate.Ge -> CondType.JGE
                IntPredicate.Lt -> CondType.JL
                IntPredicate.Le -> CondType.JLE
                else -> throw CodegenException("unknown conversion type: convType=$convType")
            }
            is UnsignedIntType, PtrType -> when (convType) {
                IntPredicate.Eq -> CondType.JE
                IntPredicate.Ne -> CondType.JNE
                IntPredicate.Gt -> CondType.JA
                IntPredicate.Ge -> CondType.JAE
                IntPredicate.Lt -> CondType.JB
                IntPredicate.Le -> CondType.JBE
                else -> throw CodegenException("unknown conversion type: convType=$convType")
            }
            is FloatingPointType -> when (convType) {
                FloatPredicate.Oeq -> CondType.JE // TODO Clang insert extra instruction 'jp ${labelName}"
                FloatPredicate.Ogt -> CondType.JA
                FloatPredicate.Oge -> CondType.JAE
                FloatPredicate.Olt -> CondType.JB
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
            is UndefType -> TODO()
        }
    }

    fun indirectCall(call: Callable, pointer: Operand) {
        emitFPVarargsCount(call)
        when (pointer) {
            is GPRegister -> call(pointer)
            is Address    -> call(pointer)
            else -> throw CodegenException("invalid operand: pointer=$pointer")
        }
    }

    fun copy(size: Int, src: GPRegister, dst: GPRegister) {
        if (src == dst) return
        mov(size, src, dst)
    }

    fun copy(size: Int, src: ImmInt, dst: GPRegister) {
        if (src.value() == 0L) {
            xor(size, dst, dst)
        } else {
            mov(size, src, dst)
        }
    }

    fun assertStackFrameLayout() {
        val currentLabel = currentLabel()
        val cont = anonLabel()
        switchTo(currentLabel).let {
            test(QWORD_SIZE, Imm32.of(0xf - 1), GPRegister.rsp)
            jcc(CondType.JNE, cont)
            mov(QWORD_SIZE, Imm32.of(0x0), rax)
            mov(QWORD_SIZE, Address.from(rax, 0), rax)
        }

        switchTo(cont)
    }

    fun callFunction(call: Callable, func: DirectFunctionPrototype) {
        emitFPVarargsCount(call)
        when (func) {
            is ExternFunction    -> call(func.name + "@PLT")
            is FunctionPrototype -> call(func.name)
        }
    }

    private fun emitFPVarargsCount(call: Callable) {
        if (!call.prototype().attributes.contains(VarArgAttribute)) {
            return
        }

        val numberOfFp = call.arguments().count { it.type() is FloatingPointType }
        assertion(numberOfFp < 255) { "numberOfFp=$numberOfFp" }

        copy(BYTE_SIZE, Imm32.of(numberOfFp.toLong()), rax)
    }
}