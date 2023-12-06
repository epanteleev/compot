package ir.platform.regalloc

import asm.x64.*
import ir.ArgumentValue
import ir.Value
import ir.instruction.Alloc
import ir.instruction.ValueInstruction
import ir.platform.x64.CallConvention
import ir.types.*

class VirtualRegistersPool private constructor(private val argumentSlots: List<Operand>) {
    private val frame = StackFrame.create()
    private val gpRegisters = GPRegistersList(argumentSlots)

    fun allocSlot(value: Value): Operand {
        return when (value) {
            is Alloc -> frame.takeSlot(value)
            is ValueInstruction -> {
                gpRegisters.pickRegister(value) ?: frame.takeSlot(value)
            }
            else -> {
                throw IllegalArgumentException("not allowed for this value=$value")
            }
        }
    }

    fun arguments(): List<Operand> {
        return argumentSlots
    }

    fun takeArgument(arg: ArgumentValue): Operand {
        return argumentSlots[arg.position()]
    }

    fun free(operand: Operand) {
        when (operand) {
            is GPRegister -> gpRegisters.returnRegister(operand)
            is Address2        -> frame.returnSlot(operand)
            else          -> TODO("Unimplemented")
        }
    }

    fun stackSize(): Long {
        return ((frame.size() + 7) / 8) * 8
    }

    companion object {
        private class ArgumentAllocator {
            private var freeArgumentRegisters = CallConvention.gpArgumentRegisters.toMutableList().asReversed()
            private var argumentSlotIndex: Long = 0

            private fun hardwareSize(type: Type): Int {
                return if (type == Type.U1) {
                    1
                } else {
                    type.size()
                }
            }

            private fun cast(reg: GPRegister, type: PrimitiveType): GPRegister {
                return reg(hardwareSize(type))
            }

            fun pickArgument(type: PrimitiveType): Operand {
                assert(type is IntType || type is UIntType || type is PointerType)

                val slot = if (freeArgumentRegisters.isNotEmpty()) {
                    cast(freeArgumentRegisters.removeLast(), type)
                } else {
                    val old = argumentSlotIndex
                    argumentSlotIndex += 1
                    ArgumentSlot(Rbp.rbp, old * 8 + 16, hardwareSize(type))
                }

                return slot
            }
        }

        fun create(argumentValue: List<ArgumentValue>): VirtualRegistersPool {
            val allocator = ArgumentAllocator()
            return VirtualRegistersPool(argumentValue.map { allocator.pickArgument(it.type() as PrimitiveType) })
        }
    }
}