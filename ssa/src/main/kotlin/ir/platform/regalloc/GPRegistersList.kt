package ir.platform.regalloc

import asm.x64.GPRegister
import asm.x64.GPRegister.*
import asm.x64.Operand

import asm.x64.Register
import ir.instruction.Alloc
import ir.instruction.ValueInstruction
import ir.platform.x64.CallConvention
import ir.types.ArithmeticType
import ir.types.PointerType
import ir.types.Type

class GPRegistersList(argumentValue: List<Operand>) {
    private var freeRegisters = CallConvention.availableRegisters(argumentValue.filterIsInstance<GPRegister>()).toMutableList()
    private val usedCalleeSaveRegisters = mutableSetOf(rbp)

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
        if (reg.isCallEESave) {
            usedCalleeSaveRegisters.add(reg)
        }

        return reg
    }

    fun returnRegister(reg: GPRegister) {
        if (reg.isArgument) {
            return
        }

        freeRegisters.add(reg)
    }
}