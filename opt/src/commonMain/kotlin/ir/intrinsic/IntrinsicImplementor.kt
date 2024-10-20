package ir.intrinsic

import asm.Operand
import ir.types.NonTrivialType
import ir.platform.MacroAssembler


abstract class IntrinsicImplementor<in Masm: MacroAssembler>(val name: String, val inputsTypes: List<NonTrivialType>) {
    abstract fun implement(masm: Masm, inputs: List<Operand>)
}