package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import asm.x64.GPRegister.rax
import ir.Definitions.BYTE_SIZE
import ir.instruction.AnyPredicateType
import ir.instruction.Flag2Int
import ir.instruction.FloatPredicate
import ir.instruction.IntPredicate
import ir.platform.MacroAssembler
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.CodegenException
import ir.platform.x64.codegen.X64MacroAssembler
import ir.platform.x64.codegen.visitors.GPOperandsVisitorUnaryOp
import ir.types.IntegerType
import ir.types.PrimitiveType
import ir.types.U8Type
import ir.types.asType


internal class Flag2IntCodegen(private val toSize: Int, private val fromType: PrimitiveType, private val predicate: AnyPredicateType, private val asm: X64MacroAssembler): GPOperandsVisitorUnaryOp {
    private fun emitPredicateRegister(dst: GPRegister) = when (predicate) {
        is IntPredicate -> asm.setccInt(fromType, predicate, dst)
        is FloatPredicate -> when (predicate) {
            FloatPredicate.One, FloatPredicate.Une -> {
                asm.setccFloat(predicate, dst)
                asm.setcc(CondFlagType.P, rax)

                asm.or(BYTE_SIZE, dst, rax)
                asm.and(BYTE_SIZE, Imm32.of(1), rax)
                asm.copy(toSize, rax, dst)
            }
            else -> {
                asm.setccFloat(predicate, dst)
                asm.setcc(CondFlagType.NP, rax)

                asm.and(BYTE_SIZE, dst, rax)
                asm.and(BYTE_SIZE, Imm32.of(1), rax)
                asm.copy(toSize, rax, dst)
            }
        }
    }

    private fun emitPredicateAddress(dst: Address) = when (predicate) {
        is IntPredicate -> {
            val type = asm.condIntType0(predicate, fromType)
            asm.setcc(type, dst)
        }
        is FloatPredicate -> when (predicate) {
            FloatPredicate.One, FloatPredicate.Une -> {
                asm.setccFloat(predicate, dst)
                asm.setcc(CondFlagType.P, rax)

                asm.or(BYTE_SIZE, dst, rax)
                asm.and(BYTE_SIZE, Imm32.of(1), rax)
                asm.mov(toSize, rax, dst)
            }
            else -> {
                asm.setccFloat(predicate, dst)
                asm.setcc(CondFlagType.NP, rax)

                asm.and(BYTE_SIZE, dst, rax)
                asm.and(BYTE_SIZE, Imm32.of(1), rax)
                asm.mov(toSize, rax, dst)
            }
        }
    }

    operator fun invoke(dst: VReg, src: VReg) {
        GPOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        emitPredicateRegister(dst)
        if (toSize == fromSize && dst == src) {
            return
        }

        asm.movzext(fromSize, toSize, src, dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        TODO("Not yet implemented")
    }

    override fun ar(dst: Address, src: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aa(dst: Address, src: Address) {
        emitPredicateAddress(dst)
        if (toSize == fromSize && dst == src) {
            return
        }

        asm.movzext(fromSize, toSize, src, temp1)
        asm.mov(toSize, temp1, dst)
    }

    override fun ri(dst: GPRegister, src: Imm) {
        TODO("Not yet implemented")
    }

    override fun ai(dst: Address, src: Imm) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Flag2Int.NAME}' dst=$dst, src=$$src")
    }

    companion object {
        private val fromSize = U8Type.sizeOf()
    }
}