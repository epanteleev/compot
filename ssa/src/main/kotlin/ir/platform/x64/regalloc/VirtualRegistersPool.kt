package ir.platform.x64.regalloc

import ir.*
import asm.x64.*
import ir.types.*
import ir.instruction.*
import asm.x64.GPRegister.*
import ir.platform.x64.CallConvention


class VirtualRegistersPool private constructor(private val argumentSlots: List<Operand>) {
    private val frame = StackFrame.create()
    private val gpRegisters = GPRegistersList(argumentSlots.filterIsInstance<GPRegister>()) //TODO
    private val xmmRegisters = XmmRegisterList(argumentSlots.filterIsInstance<XmmRegister>()) //TODO

    fun allocSlot(value: Value): Operand {
        return when (value) {
            is Generate   -> frame.takeSlot(value)
            is Alloc      -> frame.takeSlot(value)
            is LocalValue -> {
                when (val tp = value.type()) {
                    is FloatingPointType -> xmmRegisters.pickRegister() ?: frame.takeSlot(value)
                    is PrimitiveType     -> gpRegisters.pickRegister() ?: frame.takeSlot(value)
                    else -> throw IllegalArgumentException("not allowed for this type=$tp")
                }
            }
            else -> throw IllegalArgumentException("not allowed for this value=$value")
        }
    }

    fun arguments(): List<Operand> = argumentSlots

    fun takeArgument(arg: ArgumentValue): Operand {
        return argumentSlots[arg.position()]
    }

    fun free(operand: Operand, size: Int) {
        when (operand) {
            is GPRegister  -> gpRegisters.returnRegister(operand)
            is XmmRegister -> xmmRegisters.returnRegister(operand)
            is Address     -> frame.returnSlot(operand, size)
            else           -> throw RuntimeException("unknown operand operand=$operand, size=$size")
        }
    }

    fun stackSize(): Int {
        return ((frame.size() + 7) / 8) * 8
    }

    companion object {
        private class ArgumentAllocator {
            private var freeArgumentRegisters = CallConvention.gpArgumentRegisters.toMutableList().asReversed()
            private var freeXmmArgumentRegisters = CallConvention.xmmArgumentRegister.toMutableList().asReversed()
            private var argumentSlotIndex: Int = 0

            private fun peakIntegerArgument(): Operand {
                return if (freeArgumentRegisters.isNotEmpty()) {
                    freeArgumentRegisters.removeLast()
                } else {
                    val old = argumentSlotIndex
                    argumentSlotIndex += 1
                    ArgumentSlot(rbp, old * 8 + 16)
                }
            }

            private fun peakFPArgument(): Operand {
                return if (freeXmmArgumentRegisters.isNotEmpty()) {
                    freeXmmArgumentRegisters.removeLast()
                } else {
                    val old = argumentSlotIndex
                    argumentSlotIndex += 1
                    ArgumentSlot(rbp, old * 16 + 16)
                }
            }

            private fun pickArgument(type: PrimitiveType): Operand {
                return when (type) {
                    is IntegerType, is PointerType -> peakIntegerArgument()
                    is FloatingPointType -> peakFPArgument()
                    else -> throw RuntimeException("type=$type")
                }
            }

            fun allocate(arguments: List<ArgumentValue>): List<Operand> {
                return arguments.mapTo(arrayListOf()) {
                    pickArgument(it.type() as PrimitiveType)
                }
            }
        }

        fun create(argumentValues: List<ArgumentValue>): VirtualRegistersPool {
            val allocator = ArgumentAllocator()
            return VirtualRegistersPool(allocator.allocate(argumentValues))
        }
    }
}