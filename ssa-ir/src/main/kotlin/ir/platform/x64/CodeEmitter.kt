package ir.platform.x64

import asm.x64.*
import ir.instruction.*
import ir.module.block.Label
import ir.module.Module
import ir.module.block.Block
import ir.instruction.Call
import ir.module.FunctionData
import ir.platform.regalloc.RegisterAllocation
import ir.types.*
import ir.types.ArithmeticType

import ir.utils.OrderedLocation

data class CodegenException(override val message: String): Exception(message)

class CodeEmitter(private val data: FunctionData,
                  private val functionCounter: Int,
                  private val objFunc: ObjFunction,
                  private val valueToRegister: RegisterAllocation
) {
    private fun emitPrologue() {
        val stackSize = valueToRegister.reservedStackSize()
        val calleeSaveRegisters = valueToRegister.calleeSaveRegisters

        objFunc.push(Rbp.rbp)
        objFunc.mov(Rsp.rsp, Rbp.rbp)

        if (stackSize != 0L) {
            objFunc.sub(Imm(stackSize, 8), Rsp.rsp)
        }
        for (reg in calleeSaveRegisters) {
            objFunc.push(reg)
        }
    }

    private fun emitEpilogue() {
        val calleeSaveRegisters = valueToRegister.calleeSaveRegisters
        for (reg in calleeSaveRegisters.reversed()) {
            objFunc.pop(reg)
        }

        objFunc.leave()
    }

    private fun emitArithmeticBinary(binary: ArithmeticBinary) {
        var first       = valueToRegister.operand(binary.first())
        val second      = valueToRegister.operand(binary.second())
        val destination = valueToRegister.operand(binary) as Operand

        if (first is Mem) {
            first = objFunc.mov(first, temp1(first.size))
        }

        first = if (destination is Mem) {
            objFunc.mov(first, temp2(second.size))
        } else {
            objFunc.mov(first, destination)
        }

        when (binary.op) {
            ArithmeticBinaryOp.Add -> {
                objFunc.add(second, first as Register)
            }
            ArithmeticBinaryOp.Sub -> {
                objFunc.sub(second, first as Register)
            }
            ArithmeticBinaryOp.Xor -> {
                objFunc.xor(second, first as Register)
            }
            ArithmeticBinaryOp.Mul -> {
                objFunc.mul(second, first as Register)
            }
            ArithmeticBinaryOp.Div -> {
                objFunc.div(second, first as Register)
            }
            else -> {
                println("Unimplemented: ${binary.op}")
            }
        }

        if (destination is Mem) {
            objFunc.mov(first, destination)
        }
    }

    private fun emitReturn(ret: Return) {
        val returnType = data.prototype.type()
        if (returnType is ArithmeticType || returnType is PointerType) {
            val value = valueToRegister.operand(ret.value())
            objFunc.mov(value, temp1(value.size))
        }

        emitEpilogue()
        objFunc.ret()
    }

    private fun emitArithmeticUnary(unary: ArithmeticUnary) {
        val operand = valueToRegister.operand(unary.operand())
        val result  = valueToRegister.operand(unary)

        if (unary.op == ArithmeticUnaryOp.Neg) {
            val second = if (operand is Mem) {
                objFunc.mov(operand, temp1(8))
            } else {
                operand as Register
            }
            objFunc.xor(Imm(-1, 8), second)
            objFunc.mov(second, result as Operand)

        } else if (unary.op == ArithmeticUnaryOp.Not) {
            val second = if (result is Mem) {
                objFunc.mov(result, temp1(result.size))
            } else {
                result as Register
            }
            objFunc.xor(second, second)
            objFunc.sub(operand, second)

            if (result is Mem) {
                objFunc.mov(temp1(result.size), result)
            }

        } else {
            throw RuntimeException("Internal error")
        }
    }

    private fun emitCall(call: Callable) {
        objFunc.call(call.prototype().name)

        val retType = call.type()
        if (retType == Type.Void) {
            return
        }

        if (retType is ArithmeticType || retType is PointerType || retType == Type.U1) {
            objFunc.mov(Rax.rax(call.type().size()), valueToRegister.operand(call) as Operand)
        } else if (retType is FloatingPoint) {
            TODO()
        } else {
            throw RuntimeException("unknown value type=$retType")
        }
    }

    private fun emitStore(instruction: Store) {
        val pointer = valueToRegister.operand(instruction.pointer()) as Operand
        var value   = valueToRegister.operand(instruction.value())

        if (value is Mem) {
            value = objFunc.mov(value, temp2(value.size))
        }

        when (pointer) {
            is Mem        -> objFunc.mov(value, pointer)
            is GPRegister -> objFunc.mov(value, Mem.mem(pointer, 0, value.size))
            else -> throw RuntimeException("unsupported pointer=$pointer")
        }
    }

    private fun emitLoad(instruction: Load) {
        val pointer = valueToRegister.operand(instruction.operand())
        val value   = valueToRegister.operand(instruction) as Operand

        val operand = if (value is Mem) {
            objFunc.mov(value, temp1(value.size))
        } else {
            value
        }

        objFunc.mov(pointer, operand)

        if (value is Mem) {
            objFunc.mov(temp1(value.size), value)
        }
    }

    private fun emitIntCompare(isMultiplyUsages: Boolean, intCompare: IntCompare) {
        var first = valueToRegister.operand(intCompare.first())
        val second = valueToRegister.operand(intCompare.second())

        first = if (first is Mem) {
            objFunc.mov(first, temp1(first.size))
        } else {
            first
        }

        objFunc.cmp(first as GPRegister, second)
        if (isMultiplyUsages) {
            println("multiply usages $intCompare")
        }
    }

    private fun emitBranch(branch: Branch) {
        objFunc.jump(JmpType.JMP, ".L$functionCounter.${branch.target().index}")
    }

    private fun emitBranchCond(branchCond: BranchCond) {
        val cond = branchCond.condition()
        if (cond is IntCompare) {
            val jmpType = when (cond.predicate().invert()) {
                IntPredicate.Eq -> JmpType.JE
                IntPredicate.Ne -> JmpType.JNE
                IntPredicate.Ugt -> JmpType.JG
                IntPredicate.Uge -> JmpType.JGE
                IntPredicate.Ult -> JmpType.JL
                IntPredicate.Ule -> JmpType.JLE
                IntPredicate.Sgt -> JmpType.JG
                IntPredicate.Sge -> JmpType.JGE
                IntPredicate.Slt -> JmpType.JL
                IntPredicate.Sle -> JmpType.JLE
            }

            objFunc.jump(jmpType, ".L$functionCounter.${branchCond.onFalse().index}")
        } else {
            println("unsupported $branchCond")
        }
    }

    private fun emitCopy(copy: Copy) {
        val result  = valueToRegister.operand(copy)
        val operand = valueToRegister.operand(copy.origin())

        if (result is Mem && operand is Mem) {
            val temp = temp1(operand.size)
            objFunc.mov(operand, temp)
            objFunc.mov(temp, result)
        } else {
            objFunc.mov(operand, result as Operand)
        }
    }

    private fun emitDownStackFrame(dsf: DownStackFrame, map: Map<Callable, OrderedLocation>) {
        val savedRegisters = valueToRegister.callerSaveRegisters(map[dsf.call()]!!)
        for (arg in savedRegisters) {
            objFunc.push(arg)
        }

        val totalStackSize = valueToRegister.frameSize(savedRegisters)
        if (totalStackSize % 16L != 0L) {
            objFunc.sub(Imm(8, 8), Rsp.rsp)
        }
    }

    private fun emitUpStackFrame(usf: UpStackFrame, map: Map<Callable, OrderedLocation>) {
        val savedRegisters = valueToRegister.callerSaveRegisters(map[usf.call()]!!)
        val totalStackSize = valueToRegister.frameSize(savedRegisters)

        if (totalStackSize % 16L != 0L) {
            objFunc.add(Imm(8, 8), Rsp.rsp)
        }

        for (arg in savedRegisters.reversed()) {
            objFunc.pop(arg)
        }
    }

    private fun emitGep(gep: GetElementPtr) {
        val source = valueToRegister.operand(gep.source())
        val index  = valueToRegister.operand(gep.index())
        val dest   = valueToRegister.operand(gep)

        val indexReg = when (index) {
            is Mem -> objFunc.mov(index, temp2(source.size))
            is Imm -> index
            else   -> index
        }

        val destReg = if (dest is Mem) {
            objFunc.mov(dest, temp2(dest.size))
        } else {
            dest as Register
        }

        val sourceReg = if (source is Mem) {
            when (indexReg) {
                is GPRegister -> {
                    Mem.mem(source.base, source.offset, indexReg, gep.type().size().toLong(), dest.size)
                }
                is Imm -> {
                    val offset = indexReg.value * gep.type().dereference().size()
                    Mem.mem(source.base, source.offset + offset, dest.size)
                }
                else -> {
                    throw RuntimeException("error")
                }
            }
        } else {
            source
        }

        objFunc.lea(sourceReg, destReg)
    }

    private fun emitBasicBlock(bb: Block, map: Map<Callable, OrderedLocation>) {
        for (instruction in bb) {
            when (instruction) {
                is ArithmeticBinary -> emitArithmeticBinary(instruction)
                is Store            -> emitStore(instruction)
                is Return           -> emitReturn(instruction)
                is Load             -> emitLoad(instruction)
                is Call             -> emitCall(instruction)
                is ArithmeticUnary  -> emitArithmeticUnary(instruction)
                is IntCompare       -> emitIntCompare(false, instruction)
                is Branch           -> emitBranch(instruction)
                is BranchCond       -> emitBranchCond(instruction)
                is VoidCall         -> emitCall(instruction)
                is Copy             -> emitCopy(instruction)
                is DownStackFrame   -> emitDownStackFrame(instruction, map)
                is UpStackFrame     -> emitUpStackFrame(instruction, map)
                is Phi              -> {/* skip */}
                is Alloc            -> {/* skip */}
                is GetElementPtr    -> emitGep(instruction)
                else                -> println("Unsupported: $instruction")
            }
        }
    }

    private fun evaluateOrder(): Map<Callable, OrderedLocation> {
        val orderedLocation = hashMapOf<Callable, OrderedLocation>()
        var order = 0
        for (bb in data.blocks.linearScanOrder()) {
            for ((idx, call) in bb.instructions().withIndex()) {
                if (call is Callable) {
                    orderedLocation[call] = OrderedLocation(bb, idx, order)
                }
                order += 1
            }
        }

        return orderedLocation
    }

    private fun emit() {
        val orderedLocation = evaluateOrder()

        emitPrologue()
        for (bb in data.blocks.preorder()) {
            if (!bb.equals(Label.entry)) {
                objFunc.label(".L$functionCounter.${bb.index}")
            }

            emitBasicBlock(bb, orderedLocation)
        }
    }

    companion object {
        fun temp1(size: Int): GPRegister {
            return CallConvention.temp1(size)
        }

        fun temp2(size: Int): GPRegister {
            return CallConvention.temp2(size)
        }

        fun codegen(module: Module): Assembler {
            if (module !is CSSAModule) {
                throw CodegenException("cannot transform module")
            }

            val asm = Assembler()

            for ((idx, data) in module.functions().withIndex()) {
                CodeEmitter(data, idx, asm.mkFunction(data.prototype.name), module.regAlloc(data)).emit()
            }

            return asm
        }
    }
}