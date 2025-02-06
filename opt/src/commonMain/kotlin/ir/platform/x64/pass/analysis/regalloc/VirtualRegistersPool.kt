package ir.platform.x64.pass.analysis.regalloc

import asm.Operand
import asm.Register
import asm.x64.*
import ir.Definitions
import ir.types.*
import ir.value.*
import ir.Definitions.QWORD_SIZE
import ir.instruction.lir.Generate


internal class VirtualRegistersPool private constructor(private val argumentSlots: List<Operand>) {
    private val frame = StackFrame.create()
    private val gpRegisters = GPRegistersList(argumentSlots.filterIsInstance<GPRegister>()) //TODO
    private val xmmRegisters = XmmRegisterList(argumentSlots.filterIsInstance<XmmRegister>()) //TODO

    fun allocSlot(value: LocalValue, excludeIf: (Register) -> Boolean): Operand = when (value) {
        is Generate -> frame.takeSlot(value)
        else -> when (val tp = value.type()) {
            is FloatingPointType -> xmmRegisters.pickRegister(excludeIf) ?: frame.takeSlot(value)
            is IntegerType, is PtrType -> gpRegisters.pickRegister(excludeIf) ?: frame.takeSlot(value)
            else -> throw IllegalArgumentException("not allowed for this type=$tp")
        }
    }

    fun arguments(): List<Operand> = argumentSlots

    fun takeArgument(arg: ArgumentValue): Operand {
        return argumentSlots[arg.position()]
    }

    fun free(operand: Operand, size: Int) = when (operand) {
        is GPRegister   -> gpRegisters.returnRegister(operand)
        is XmmRegister  -> xmmRegisters.returnRegister(operand)
        is Address      -> Unit
        // is Address      -> frame.returnSlot(operand, size) TODO free slot if it doesn't have 'lea' ????
        else            -> throw RuntimeException("unknown operand operand=$operand, size=$size")
    }

    fun spilledLocalsAreaSize(): Int {
        return Definitions.alignTo(frame.size(), QWORD_SIZE)
    }

    fun usedGPCalleeSaveRegisters(): List<GPRegister> {
        return gpRegisters.usedCalleeSaveRegisters()
    }

    fun usedXmmCalleeSaveRegisters(): List<XmmRegister> {
        return xmmRegisters.usedCalleeSaveRegisters()
    }

    fun callerArgumentAllocate(arguments: List<Value>): List<Operand?> {
        return CallerArgumentAllocator.alloc(frame, arguments)
    }

    companion object {
        fun create(argumentValues: List<ArgumentValue>): VirtualRegistersPool {
            return VirtualRegistersPool(CalleeArgumentAllocator.allocate(argumentValues))
        }
    }
}