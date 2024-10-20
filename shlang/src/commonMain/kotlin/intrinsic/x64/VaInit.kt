package intrinsic.x64

import asm.Operand
import ir.intrinsic.IntrinsicImplementor
import ir.platform.x64.codegen.X64MacroAssembler


class VaInit(): IntrinsicImplementor<X64MacroAssembler>("va_init", listOf()) {
    override fun implement(masm: X64MacroAssembler, inputs: List<Operand>) {
        TODO()
    }
}