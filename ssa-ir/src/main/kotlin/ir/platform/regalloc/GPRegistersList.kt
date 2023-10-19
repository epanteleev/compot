package ir.platform.regalloc

import asm.x64.GPRegister
import asm.x64.Operand
import asm.x64.Rbp
import asm.x64.Register
import ir.instruction.StackAlloc
import ir.Type
import ir.platform.x64.CallConvention
import ir.instruction.ValueInstruction

class GPRegistersList(argumentValue: List<Operand>) {
    private var freeRegisters = CallConvention.availableRegisters(argumentValue.filterIsInstance<GPRegister>()).toMutableList()
    private val usedCalleeSaveRegisters = mutableSetOf<GPRegister>(Rbp.rbp)

    fun pickRegister(value: ValueInstruction): Register? {
        require(value.type() == Type.U1 ||
                value.type().isArithmetic() ||
                value.type().isPointer()) {
            "found ${value.type()} in $value"
        }
        require(value !is StackAlloc) { "cannot be" }

        if (freeRegisters.isEmpty()) {
            return null
        }
        val reg = freeRegisters.removeLast()
        if (CallConvention.gpCalleeSaveRegs.contains(reg)) {
            usedCalleeSaveRegisters.add(reg)
        }

        val type = value.type()
        return if (type == Type.U1) {
            reg(1)
        } else {
            reg(value.type().size())
        }
    }

    fun returnRegister(reg: GPRegister) {
        if (reg.isArgument) {
            return
        }

        freeRegisters.add(reg(8))
    }
}