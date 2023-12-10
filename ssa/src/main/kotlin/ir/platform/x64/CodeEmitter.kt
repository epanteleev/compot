package ir.platform.x64

import asm.x64.*
import ir.instruction.*
import ir.instruction.Call
import ir.instruction.utils.Visitor
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.module.Module
import ir.module.block.Label
import ir.platform.regalloc.RegisterAllocation
import ir.types.*
import ir.utils.OrderedLocation

data class CodegenException(override val message: String): Exception(message)

class CodeEmitter(private val data: FunctionData,
                  private val functionCounter: Int,
                  private val objFunc: ObjFunction,
                  private val valueToRegister: RegisterAllocation,
): Visitor {
    private val orderedLocation = evaluateOrder(data.blocks)

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

    override fun visit(binary: ArithmeticBinary) {
        var first       = valueToRegister.operand(binary.first())
        val second      = valueToRegister.operand(binary.second())
        val destination = valueToRegister.operand(binary) as Operand

        if (first is Address2) {
            first = objFunc.mov(first, temp1(first.size))
        }

        first = if (destination is Address2) {
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

        if (destination is Address2) {
            objFunc.mov(first, destination)
        }
    }

    override fun visit(returnValue: ReturnValue) {
        val returnType = data.prototype.type()
        val retInstType = returnValue.value().type()
        assert(returnType == retInstType) { //Todo fix VerifySSA
            "should be the same, but: function.return.type=$returnType, ret.type=$retInstType"
        }

        if (returnType is ArithmeticType || returnType is PointerType) {
            val value = valueToRegister.operand(returnValue.value())
            objFunc.mov(value, temp1(value.size))
        } else {
            TODO()
        }

        emitEpilogue()
        objFunc.ret()
    }

    override fun visit(returnVoid: ReturnVoid) {
        emitEpilogue()
        objFunc.ret()
    }

    override fun visit(unary: ArithmeticUnary) {
        val operand = valueToRegister.operand(unary.operand())
        val result  = valueToRegister.operand(unary)

        if (unary.op == ArithmeticUnaryOp.Neg) {
            val second = if (operand is Address2) {
                objFunc.mov(operand, temp1(8))
            } else {
                operand as Register
            }
            objFunc.xor(Imm(-1, 8), second)
            objFunc.mov(second, result as Operand)

        } else if (unary.op == ArithmeticUnaryOp.Not) {
            val second = if (result is Address2) {
                objFunc.mov(result, temp1(result.size))
            } else {
                result as Register
            }
            objFunc.xor(second, second)
            objFunc.sub(operand, second)

            if (result is Address2) {
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

        when (retType) {
            is ArithmeticType, is PointerType, is BooleanType -> {
                objFunc.mov(Rax.rax(call.type().size()), valueToRegister.operand(call) as Operand)
            }

            is FloatingPoint -> {
                TODO()
            }

            else -> {
                throw RuntimeException("unknown value type=$retType")
            }
        }
    }

    override fun visit(voidCall: VoidCall) {
        emitCall(voidCall)
    }

    override fun visit(call: Call) {
        emitCall(call)
    }

    override fun visit(store: Store) {
        val pointer = valueToRegister.operand(store.pointer()) as Operand
        var value   = valueToRegister.operand(store.value())

        if (value is Address) {
            value = objFunc.mov(value, temp2(value.size))
        }

        when (pointer) {
            is Address    -> objFunc.mov(value, pointer)
            is GPRegister -> objFunc.mov(value, Address.mem(pointer, 0, value.size))
            else -> throw RuntimeException("unsupported pointer=$pointer")
        }
    }

    override fun visit(load: Load) {
        val pointer = valueToRegister.operand(load.operand())
        val value   = valueToRegister.operand(load) as Operand

        val operand = if (value is Address) {
            objFunc.mov(value, temp1(value.size))
        } else {
            value
        }

        objFunc.mov(pointer, operand)

        if (value is Address) {
            objFunc.mov(temp1(value.size), value)
        }
    }

    override fun visit(intCompare: IntCompare) {
        var first = valueToRegister.operand(intCompare.first())
        val second = valueToRegister.operand(intCompare.second())

        first = if (first is Address2) {
            objFunc.mov(first, temp1(first.size))
        } else {
            first
        }

        objFunc.cmp(first as GPRegister, second)
    }

    override fun visit(branch: Branch) {
        objFunc.jump(JmpType.JMP, ".L$functionCounter.${branch.target().index}")
    }

    override fun visit(branchCond: BranchCond) {
        val cond = branchCond.condition()
        if (cond is IntCompare) {
            val jmpType = when (cond.predicate().invert()) {
                IntPredicate.Eq  -> JmpType.JE
                IntPredicate.Ne  -> JmpType.JNE
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

    override fun visit(copy: Copy) {
        val result  = valueToRegister.operand(copy)
        val operand = valueToRegister.operand(copy.origin())

        if (result is Address && operand is Address) {
            val temp = temp1(operand.size)
            if (operand is AddressLiteral) {
                objFunc.lea(operand, temp)
            } else {
                objFunc.mov(operand, temp)
            }

            objFunc.mov(temp, result)
        } else if (operand is AddressLiteral) {
            val dest =  result as GPRegister
            objFunc.lea(operand, dest)

        } else {
            result as Operand
            objFunc.mov(operand, result)
        }
    }

    override fun visit(downStackFrame: DownStackFrame) {
        val savedRegisters = valueToRegister.callerSaveRegisters(orderedLocation[downStackFrame.call()]!!)
        for (arg in savedRegisters) {
            objFunc.push(arg)
        }

        val totalStackSize = valueToRegister.frameSize(savedRegisters)
        if (totalStackSize % 16L != 0L) {
            objFunc.sub(Imm(8, 8), Rsp.rsp)
        }
    }

    override fun visit(upStackFrame: UpStackFrame) {
        val savedRegisters = valueToRegister.callerSaveRegisters(orderedLocation[upStackFrame.call()]!!)
        val totalStackSize = valueToRegister.frameSize(savedRegisters)

        if (totalStackSize % 16L != 0L) {
            objFunc.add(Imm(8, 8), Rsp.rsp)
        }

        for (arg in savedRegisters.reversed()) {
            objFunc.pop(arg)
        }
    }

    override fun visit(getElementPtr: GetElementPtr) {
        val source = valueToRegister.operand(getElementPtr.source())
        val index  = valueToRegister.operand(getElementPtr.index())
        val dest   = valueToRegister.operand(getElementPtr)

        val indexReg = when (index) {
            is Address2 -> objFunc.mov(index, temp2(source.size))
            is Imm -> index
            else   -> index
        }

        val destReg = if (dest is Address2) {
            objFunc.mov(dest, temp2(dest.size))
        } else {
            dest as Register
        }

        val pointer = getElementPtr.type()
        val sourceReg = if (source is Address2) {
            when (indexReg) {
                is GPRegister -> {
                    Address.mem(source.base, source.offset, indexReg(source.base.size), getElementPtr.basicType.size().toLong(), pointer.size())
                }
                is Imm -> {
                    val offset = indexReg.value * getElementPtr.basicType.size()
                    Address.mem(source.base, source.offset + offset, getElementPtr.basicType.size())
                }
                else -> {
                    throw RuntimeException("error")
                }
            }
        } else {
            source as GPRegister
            when (indexReg) {
                is GPRegister -> {
                    Address.mem(source, 0, indexReg(source.size), getElementPtr.basicType.size().toLong(), pointer.size())
                }
                is Imm -> {
                    val offset = indexReg.value * getElementPtr.basicType.size()
                    Address.mem(source, offset, getElementPtr.basicType.size())
                }
                else -> {
                    throw RuntimeException("error")
                }
            }
        }

        objFunc.lea(sourceReg, destReg)
    }

    override fun visit(cast: Cast) {
        val des = valueToRegister.operand(cast) as Operand
        val src = valueToRegister.operand(cast.value())

        val srcReg = if (src is Address2) {
            objFunc.mov(src, temp1(src.size))
        } else {
            src as GPRegister
        }

        when (cast.castType) {
            CastType.SignExtend -> {
                objFunc.movsx(srcReg, des)
            }
            CastType.ZeroExtend, CastType.Bitcast -> {
                objFunc.mov(srcReg(des.size), des)
            }
            CastType.Truncate -> {
                objFunc.mov(srcReg, des(srcReg.size))
            }
        }
    }

    override fun visit(select: Select) {
        TODO("Not yet implemented")
    }

    override fun visit(phi: Phi) { /* nothing to do */ }
    override fun visit(alloc: Alloc) { /* nothing to do */ }

    private fun evaluateOrder(blocks: BasicBlocks): Map<Callable, OrderedLocation> {
        val orderedLocation = hashMapOf<Callable, OrderedLocation>()
        var order = 0
        for (bb in blocks.linearScanOrder()) {
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
        emitPrologue()
        for (bb in data.blocks.preorder()) {
            if (!bb.equals(Label.entry)) {
                objFunc.label(".L$functionCounter.${bb.index}")
            }

            for (instruction in bb) {
                instruction.visit(this)
            }
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

            for (c in module.constants) {
                asm.mkSymbol(c)
            }

            for ((idx, data) in module.functions().withIndex()) {
                CodeEmitter(data, idx, asm.mkFunction(data.prototype.name), module.regAlloc(data)).emit()
            }

            return asm
        }
    }
}