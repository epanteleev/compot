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

    private fun intPredicateToSetCCType(type: PrimitiveType, jmpType: IntPredicate): SetCCType = when(type) {
        is SignedIntType, is PtrType -> when (jmpType) {
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
        is UndefType -> TODO("undefined behavior")
        is FloatingPointType -> throw MacroAssemblerException("invalid type: type=$type")
    }

    fun setccInt(operandsType: PrimitiveType, jmpType: IntPredicate, dst: VReg) {
        val type = intPredicateToSetCCType(operandsType, jmpType)
        when (dst) {
            is Address    -> setcc(type, dst)
            is GPRegister -> setcc(type, dst)
            else -> throw MacroAssemblerException("unknown jmpType=$jmpType")
        }
    }

    fun condIntType(convType: IntPredicate, type: PrimitiveType): CondType = when (type) {
        is SignedIntType, is PtrType -> when (convType) {
            IntPredicate.Eq -> CondType.JE
            IntPredicate.Ne -> CondType.JNE
            IntPredicate.Gt -> CondType.JG
            IntPredicate.Ge -> CondType.JGE
            IntPredicate.Lt -> CondType.JL
            IntPredicate.Le -> CondType.JLE
        }
        is UnsignedIntType -> when (convType) {
            IntPredicate.Eq -> CondType.JE
            IntPredicate.Ne -> CondType.JNE
            IntPredicate.Gt -> CondType.JA
            IntPredicate.Ge -> CondType.JAE
            IntPredicate.Lt -> CondType.JB
            IntPredicate.Le -> CondType.JBE
        }
        else -> throw CodegenException("unknown conversion type: type=$type")
    }

    fun cMoveCondition(convType: IntPredicate, type: PrimitiveType): CMoveFlag = when (type) {
        is SignedIntType, is PtrType -> when (convType) {
            IntPredicate.Eq -> CMoveFlag.CMOVE
            IntPredicate.Ne -> CMoveFlag.CMOVNE
            IntPredicate.Gt -> CMoveFlag.CMOVG
            IntPredicate.Ge -> CMoveFlag.CMOVGE
            IntPredicate.Lt -> CMoveFlag.CMOVL
            IntPredicate.Le -> CMoveFlag.CMOVLE
        }
        is UnsignedIntType -> when (convType) {
            IntPredicate.Eq -> CMoveFlag.CMOVE
            IntPredicate.Ne -> CMoveFlag.CMOVNE
            IntPredicate.Gt -> CMoveFlag.CMOVA
            IntPredicate.Ge -> CMoveFlag.CMOVAE
            IntPredicate.Lt -> CMoveFlag.CMOVB
            IntPredicate.Le -> CMoveFlag.CMOVBE
        }
        else -> throw RuntimeException("unexpected condition type: condition=$convType")
    }

    fun cMoveCondition(convType: FloatPredicate): CMoveFlag = when (convType) {
        FloatPredicate.Oeq -> CMoveFlag.CMOVE
        FloatPredicate.Ogt -> CMoveFlag.CMOVG
        FloatPredicate.Oge -> CMoveFlag.CMOVGE
        FloatPredicate.Olt -> CMoveFlag.CMOVL
        FloatPredicate.Ole -> CMoveFlag.CMOVLE
        FloatPredicate.One -> CMoveFlag.CMOVNE
        FloatPredicate.Ord -> TODO()
        FloatPredicate.Ueq -> CMoveFlag.CMOVE
        FloatPredicate.Ugt -> CMoveFlag.CMOVA
        FloatPredicate.Uge -> CMoveFlag.CMOVAE
        FloatPredicate.Ult -> CMoveFlag.CMOVB
        FloatPredicate.Ule -> CMoveFlag.CMOVBE
        FloatPredicate.Uno -> TODO()
        FloatPredicate.Une -> CMoveFlag.CMOVNE
    }

    fun condFloatType(predicate: FloatPredicate): CondType = when (predicate) {
        FloatPredicate.Oeq -> CondType.JE
        FloatPredicate.Ogt -> CondType.JA
        FloatPredicate.Oge -> CondType.JAE
        FloatPredicate.Olt -> CondType.JB
        FloatPredicate.Ole -> CondType.JBE
        FloatPredicate.One -> CondType.JNE
        FloatPredicate.Ord -> TODO()
        FloatPredicate.Ueq -> TODO()
        FloatPredicate.Ugt -> CondType.JA
        FloatPredicate.Uge -> TODO()
        FloatPredicate.Ult -> TODO()
        FloatPredicate.Ule -> CondType.JBE
        FloatPredicate.Uno -> TODO()
        FloatPredicate.Une -> CondType.JNE
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