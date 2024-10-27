package ir.intrinsic

import asm.Operand
import ir.types.NonTrivialType
import ir.platform.MacroAssembler


abstract class IntrinsicImplementor(val name: String, val inputsTypes: List<NonTrivialType>) {
    abstract fun<Masm: MacroAssembler> implement(masm: Masm, inputs: List<Operand>)
}