package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.instruction.AnyPredicateType
import ir.platform.x64.codegen.utils.ApplyClosure
import ir.platform.x64.codegen.utils.GPOperandFlagInstrunctionVisitor


class Flag2IntCodegen(private val conditionType: AnyPredicateType, val asm: Assembler): GPOperandFlagInstrunctionVisitor {

    operator fun invoke(dst: Operand) {
        ApplyClosure(dst, this as GPOperandFlagInstrunctionVisitor)
    }

    override fun r(dst: GPRegister) {
        TODO()
    }

    override fun a(dst: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand) {
        TODO("Not yet implemented")
    }
}