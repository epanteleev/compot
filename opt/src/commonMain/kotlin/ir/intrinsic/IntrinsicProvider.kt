package ir.intrinsic

import asm.x64.Operand
import ir.platform.MacroAssembler


abstract class IntrinsicProvider(val name: String) {
    abstract fun<Masm: MacroAssembler> implement(masm: Masm, inputs: List<Operand>)
}