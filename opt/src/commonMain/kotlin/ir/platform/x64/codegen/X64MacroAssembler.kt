package ir.platform.x64.codegen

import asm.x64.Operand
import asm.x64.*
import asm.x64.GPRegister.rax
import common.assertion
import ir.Definitions.BYTE_SIZE
import ir.attributes.VarArgAttribute
import ir.instruction.*
import ir.platform.MacroAssembler
import ir.platform.common.TargetPlatform
import ir.types.*


data class MacroAssemblerException(override val message: String): Exception(message)

class X64MacroAssembler(name: String, id: Int): Assembler(name, id), MacroAssembler {
    override fun platform(): TargetPlatform {
        return TargetPlatform.X64
    }

    fun setccFloat(jmpType: FloatPredicate, dst: Operand) {
        val type = condFloatType0(jmpType)
        when (dst) {
            is Address    -> setcc(type, dst)
            is GPRegister -> setcc(type, dst)
            else -> throw MacroAssemblerException("unknown jmpType=$jmpType")
        }
    }

    fun setccInt(operandsType: PrimitiveType, jmpType: IntPredicate, dst: VReg) {
        val type = condIntType0(jmpType, operandsType)
        when (dst) {
            is Address    -> setcc(type, dst)
            is GPRegister -> setcc(type, dst)
            else -> throw MacroAssemblerException("unknown jmpType=$jmpType")
        }
    }

    fun condIntType0(convType: IntPredicate, type: PrimitiveType): CondFlagType = when (type) {
        is SignedIntType, is PtrType -> when (convType) {
            IntPredicate.Eq -> CondFlagType.EQ
            IntPredicate.Ne -> CondFlagType.NE
            IntPredicate.Gt -> CondFlagType.G
            IntPredicate.Ge -> CondFlagType.GE
            IntPredicate.Lt -> CondFlagType.L
            IntPredicate.Le -> CondFlagType.LE
        }
        is UnsignedIntType -> when (convType) {
            IntPredicate.Eq -> CondFlagType.EQ
            IntPredicate.Ne -> CondFlagType.NE
            IntPredicate.Gt -> CondFlagType.A
            IntPredicate.Ge -> CondFlagType.AE
            IntPredicate.Lt -> CondFlagType.B
            IntPredicate.Le -> CondFlagType.BE
        }
        else -> throw CodegenException("unknown conversion type: type=$type")
    }

    fun condFloatType0(predicate: FloatPredicate): CondFlagType = when (predicate) {
        FloatPredicate.Oeq, FloatPredicate.Ueq -> CondFlagType.EQ
        FloatPredicate.Ogt, FloatPredicate.Ugt -> CondFlagType.A
        FloatPredicate.Oge, FloatPredicate.Uge -> CondFlagType.AE
        FloatPredicate.Olt, FloatPredicate.Ult -> CondFlagType.B
        FloatPredicate.Ole, FloatPredicate.Ule -> CondFlagType.BE
        FloatPredicate.One, FloatPredicate.Une -> CondFlagType.NE
        FloatPredicate.Ord -> TODO()
        FloatPredicate.Uno -> TODO()
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

    fun copy(size: Int, src: Imm, dst: GPRegister) {
        if (src.value() == 0L) {
            xor(size, dst, dst)
        } else {
            mov(size, src, dst)
        }
    }

    fun callFunction(call: Callable, func: FunSymbol) {
        emitFPVarargsCount(call)
        call(func)
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