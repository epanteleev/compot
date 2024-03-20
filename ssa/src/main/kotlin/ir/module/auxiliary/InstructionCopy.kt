package ir.module.auxiliary

import ir.*
import ir.instruction.*
import ir.instruction.Copy
import ir.module.block.Block
import ir.instruction.utils.Visitor


class InstructionCopy(private val oldValuesToNew: Map<LocalValue, LocalValue>, private val oldToNewBlock: Map<Block, Block>): Visitor<Instruction> {
    private inline fun<reified T> mapUsage(old: Value): T {
        val newValue = if (old is ArgumentValue || old is Constant || old is GlobalSymbol) {
            return old as T
        } else {
            val value = oldValuesToNew[old]?: throw RuntimeException("cannot find localValue=${old}")
            if (value !is T) {
                throw RuntimeException("unexpected type for value=$value")
            }

            value
        }

        return newValue
    }

    private fun mapBlock(old: Block): Block {
        return oldToNewBlock[old]?: throw RuntimeException("cannot find new block, oldBlock=$old")
    }

    override fun visit(alloc: Alloc): Instruction {
        return Alloc.make(alloc.name(), alloc.allocatedType)
    }

    override fun visit(generate: Generate): Instruction {
        return Generate.make(generate.name(), generate.type())
    }

    override fun visit(lea: Lea): Instruction {
        return Lea.make(lea.name(), mapUsage<Generate>(lea.generate()))
    }

    override fun visit(binary: ArithmeticBinary): Instruction {
        val first  = mapUsage<Value>(binary.first())
        val second = mapUsage<Value>(binary.second())

        return ArithmeticBinary.make(binary.name(), binary.type(), first ,binary.op, second)
    }

    override fun visit(neg: Neg): Instruction {
        val operand = mapUsage<Value>(neg.operand())
        return Neg.make(neg.name(), neg.type(), operand)
    }

    override fun visit(not: Not): Instruction {
        val operand = mapUsage<Value>(not.operand())
        return Not.make(not.name(), not.type(), operand)
    }

    override fun visit(branch: Branch): Instruction {
        return Branch.make(mapBlock(branch.target()))
    }

    override fun visit(branchCond: BranchCond): Instruction {
        val condition = mapUsage<Value>(branchCond.condition())
        val onTrue    = mapBlock(branchCond.onTrue())
        val onFalse   = mapBlock(branchCond.onFalse())

        return BranchCond.make(condition, onTrue, onFalse)
    }

    override fun visit(call: Call): Instruction {
        val newUsages = call.operands().map { mapUsage<Value>(it) } //TODO
        return Call.make(call.name(), call.prototype(), newUsages)
    }

    override fun visit(bitcast: Bitcast): Instruction {
        val operand = mapUsage<Value>(bitcast.value())
        return Bitcast.make(bitcast.name(), bitcast.type(), operand)
    }

    override fun visit(zext: ZeroExtend): Instruction {
        val operand = mapUsage<Value>(zext.value())
        return ZeroExtend.make(zext.name(), zext.type(), operand)
    }

    override fun visit(sext: SignExtend): Instruction {
        val operand = mapUsage<Value>(sext.value())
        return SignExtend.make(sext.name(), sext.type(), operand)
    }

    override fun visit(pcmp: PointerCompare): Instruction {
        val first  = mapUsage<Value>(pcmp.first())
        val second = mapUsage<Value>(pcmp.second())

        return PointerCompare.make(pcmp.name(), first, pcmp.predicate(), second)
    }

    override fun visit(trunc: Truncate): Instruction {
        val operand = mapUsage<Value>(trunc.value())
        return Truncate.make(trunc.name(), trunc.type(), operand)
    }

    override fun visit(fptruncate: FpTruncate): Instruction {
        val operand = mapUsage<Value>(fptruncate.value())
        return FpTruncate.make(fptruncate.name(), fptruncate.type(), operand)
    }

    override fun visit(fpext: FpExtend): Instruction {
        val operand = mapUsage<Value>(fpext.value())
        return FpExtend.make(fpext.name(), fpext.type(), operand)
    }

    override fun visit(fptosi: FloatToSigned): Instruction {
        val operand = mapUsage<Value>(fptosi.value())
        return FloatToSigned.make(fptosi.name(), fptosi.type(), operand)
    }

    override fun visit(copy: Copy): Instruction {
        val operand = mapUsage<Value>(copy.origin())
        return Copy.make(copy.name(), operand)
    }

    override fun visit(move: Move): Instruction {
        val fromValue = mapUsage<Generate>(move.fromValue())
        val toValue   = mapUsage<Value>(move.toValue())

        return Move.make(fromValue, toValue)
    }

    override fun visit(downStackFrame: DownStackFrame): Instruction {
        return DownStackFrame(downStackFrame.call())
    }

    override fun visit(gep: GetElementPtr): Instruction {
        val source = mapUsage<Value>(gep.source())
        val index  = mapUsage<Value>(gep.index())

        return GetElementPtr.make(gep.name(), gep.basicType, source, index)
    }

    override fun visit(gfp: GetFieldPtr): Instruction {
        val source = mapUsage<Value>(gfp.source())
        val index  = mapUsage<IntegerConstant>(gfp.index())

        return GetFieldPtr.make(gfp.name(), gfp.basicType, source, index)
    }

    override fun visit(icmp: SignedIntCompare): Instruction {
        val first  = mapUsage<Value>(icmp.first())
        val second = mapUsage<Value>(icmp.second())

        return SignedIntCompare.make(icmp.name(), first, icmp.predicate(), second)
    }

    override fun visit(ucmp: UnsignedIntCompare): Instruction {
        val first  = mapUsage<Value>(ucmp.first())
        val second = mapUsage<Value>(ucmp.second())

        return UnsignedIntCompare.make(ucmp.name(), first, ucmp.predicate(), second)
    }

    override fun visit(floatCompare: FloatCompare): Instruction {
        val first  = mapUsage<Value>(floatCompare.first())
        val second = mapUsage<Value>(floatCompare.second())

        return FloatCompare.make(floatCompare.name(), first, floatCompare.predicate(), second)
    }

    override fun visit(load: Load): Instruction {
        val pointer = mapUsage<Value>(load.operand())
        return Load.make(load.name(), load.type(), pointer)
    }

    override fun visit(phi: Phi): Instruction {
        val newUsages   = phi.operands().clone()
        val newIncoming = phi.incoming().map { mapBlock(it) } //TODO

        return Phi.make(phi.name(), phi.type(), newIncoming, newUsages) //TODO
    }

    override fun visit(returnValue: ReturnValue): Instruction {
        val value = mapUsage<Value>(returnValue.value())
        return ReturnValue.make(value)
    }

    override fun visit(returnVoid: ReturnVoid): Instruction {
        return returnVoid
    }

    override fun visit(indirectionCall: IndirectionCall): Instruction {
        val newUsages = indirectionCall.operands().map { mapUsage<Value>(it) }
        val pointer   = mapUsage<Value>(indirectionCall.pointer())

        return IndirectionCall.make(indirectionCall.name(), pointer, indirectionCall.prototype(), newUsages)
    }

    override fun visit(indirectionVoidCall: IndirectionVoidCall): Instruction {
        val newUsages = indirectionVoidCall.operands().map { mapUsage<Value>(it) }
        val pointer   = mapUsage<Value>(indirectionVoidCall.pointer())

        return IndirectionVoidCall.make(pointer, indirectionVoidCall.prototype(), newUsages)
    }

    override fun visit(select: Select): Instruction {
        val condition = mapUsage<Value>(select.condition())
        val onTrue    = mapUsage<Value>(select.onTrue())
        val onFalse   = mapUsage<Value>(select.onFalse())

        return Select.make(select.name(), select.type(), condition, onTrue, onFalse)
    }

    override fun visit(store: Store): Instruction {
        val pointer = mapUsage<Value>(store.pointer())
        val value   = mapUsage<Value>(store.value())

        return Store.make(pointer, value)
    }

    override fun visit(upStackFrame: UpStackFrame): Instruction {
        return UpStackFrame(upStackFrame.call())
    }

    override fun visit(voidCall: VoidCall): Instruction {
        val newUsages = voidCall.operands().map { mapUsage<Value>(it) }
        return VoidCall.make(voidCall.prototype(), newUsages)
    }

    companion object {
        fun copy(instructionMap: MutableMap<LocalValue, LocalValue>, oldToNewBlock: Map<Block, Block>, instruction: Instruction): Instruction {
            val visitor = InstructionCopy(instructionMap, oldToNewBlock)
            val newInstruction = instruction.visit(visitor)
            if (instruction is ValueInstruction) {
                instructionMap[instruction] = newInstruction as ValueInstruction
            }

            return newInstruction
        }
    }
}