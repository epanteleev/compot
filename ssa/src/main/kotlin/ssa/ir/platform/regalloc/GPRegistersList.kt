package ir.platform.regalloc

import asm.x64.*
import ir.instruction.Alloc
import ir.platform.x64.CallConvention
import ir.instruction.ValueInstruction
import ir.types.ArithmeticType
import ir.types.PointerType
import ir.types.Type

class GPRegistersList(argumentValue: List<Operand>) {
    private var freeRegisters = CallConvention.availableRegisters(argumentValue.filterIsInstance<GPRegister>()).toMutableList()
    private val usedCalleeSaveRegisters = mutableSetOf<GPRegister>(Rbp.rbp)

    fun pickRegister(value: ValueInstruction): Register? {
        require(value.type() == Type.U1 ||
                value.type() is ArithmeticType ||
                value.type() is PointerType) {
            "found ${value.type()} in $value"
        }
        require(value !is Alloc) { "cannot be" }

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