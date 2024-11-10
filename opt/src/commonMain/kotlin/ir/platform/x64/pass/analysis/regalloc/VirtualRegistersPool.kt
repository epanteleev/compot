package ir.platform.x64.pass.analysis.regalloc

import asm.Operand
import asm.Register
import asm.x64.*
import ir.types.*
import ir.value.*
import ir.instruction.*
import ir.Definitions.QWORD_SIZE


class VirtualRegistersPool private constructor(private val argumentSlots: List<Operand>) {
    private val frame = StackFrame.create()
    private val gpRegisters = GPRegistersList(argumentSlots.filterIsInstance<GPRegister>()) //TODO
    private val xmmRegisters = XmmRegisterList(argumentSlots.filterIsInstance<XmmRegister>()) //TODO

    fun allocSlot(value: Value, excludeIf: (Register) -> Boolean): Operand = when (value) {
        is Generate   -> frame.takeSlot(value)
        is LocalValue -> when (val tp = value.type()) {
            is FloatingPointType -> xmmRegisters.pickRegister(excludeIf) ?: frame.takeSlot(value)
            is IntegerType, is PointerType -> gpRegisters.pickRegister(excludeIf) ?: frame.takeSlot(value)
            else -> throw IllegalArgumentException("not allowed for this type=$tp")
        }
        else -> throw IllegalArgumentException("not allowed for this value=$value")
    }

    fun arguments(): List<Operand> = argumentSlots

    fun takeArgument(arg: ArgumentValue): Operand {
        return argumentSlots[arg.position()]
    }

    fun free(operand: Operand, size: Int) = when (operand) {
        is GPRegister   -> gpRegisters.returnRegister(operand)
        is XmmRegister  -> xmmRegisters.returnRegister(operand)
        is ArgumentSlot -> Unit
        is Address      -> frame.returnSlot(operand, size)
        else            -> throw RuntimeException("unknown operand operand=$operand, size=$size")
    }

    fun spilledLocalsAreaSize(): Int {
        return ((frame.size() + (QWORD_SIZE - 1)) / QWORD_SIZE) * QWORD_SIZE
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