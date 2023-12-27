package ir.platform.x64

import asm.x64.*
import ir.instruction.*
import ir.instruction.utils.Visitor
import ir.module.*
import ir.module.block.Label
import ir.platform.regalloc.RegisterAllocation
import ir.types.*
import ir.utils.OrderedLocation
import asm.x64.GPRegister.*
import ir.*
import ir.instruction.Call
import ir.instruction.Neg
import ir.instruction.Not
import ir.platform.x64.utils.*


data class CodegenException(override val message: String): Exception(message)

class CodeEmitter(private val data: FunctionData,
                  private val functionCounter: Int,
                  asm: Assembler,
                  private val valueToRegister: RegisterAllocation,
): Visitor {
    private val orderedLocation = evaluateOrder(data.blocks)
    private val objFunc: ObjFunction = asm.mkFunction(data.prototype.name)

    private fun emitPrologue() {
        val stackSize = valueToRegister.reservedStackSize()
        val calleeSaveRegisters = valueToRegister.calleeSaveRegisters

        objFunc.push(8, rbp)
        objFunc.mov(8, rsp, rbp)

        if (stackSize != 0L) {
            objFunc.sub(8, ImmInt(stackSize), rsp)
        }
        for (reg in calleeSaveRegisters) {
            objFunc.push(8, reg)
        }
    }

    private fun emitEpilogue() {
        val calleeSaveRegisters = valueToRegister.calleeSaveRegisters
        for (reg in calleeSaveRegisters.reversed()) {
            objFunc.pop(8, reg)
        }

        objFunc.leave()
    }

    override fun visit(binary: ArithmeticBinary) {
        var first  = valueToRegister.operand(binary.first())
        val second = valueToRegister.operand(binary.second())
        val dst    = valueToRegister.operand(binary) as Operand
        val size   = binary.type().size()

        when (binary.op) {
            ArithmeticBinaryOp.Add -> AddCodegen(binary.type(), objFunc)(dst, first, second)
            ArithmeticBinaryOp.Mul -> MulCodegen(binary.type(), objFunc)(dst, first, second)
            ArithmeticBinaryOp.Sub -> SubCodegen(binary.type(), objFunc)(dst, first, second)
            ArithmeticBinaryOp.Xor -> {
                if (first is Address) {
                    first = objFunc.movOld(size, first, temp1)
                }

                first = if (dst is Address) {
                    objFunc.movOld(size, first, temp2)
                } else {
                    objFunc.movOld(size, first, dst)
                }

                objFunc.xor(size, second, first as GPRegister)

                if (dst is Address) {
                    objFunc.movOld(size, first, dst)
                }
            }
            ArithmeticBinaryOp.Div -> {
                if (first is Address) {
                    first = objFunc.movOld(size, first, temp1)
                }

                first = if (dst is Address) {
                    objFunc.movOld(size, first, temp2)
                } else {
                    objFunc.movOld(size, first, dst)
                }

                objFunc.div(size, second, first as GPRegister)

                if (dst is Address) {
                    objFunc.movOld(size, first, dst)
                }
            }
            else -> {
                println("Unimplemented: ${binary.op}")
            }
        }
    }

    override fun visit(returnValue: ReturnValue) {
        val returnType = data.prototype.type()
        val retInstType = returnValue.value().type()
        val size = returnType.size()

        assert(returnType == retInstType) { //Todo fix VerifySSA
            "should be the same, but: function.return.type=$returnType, ret.type=$retInstType"
        }

        val value = valueToRegister.operand(returnValue.value())
        if (returnType is IntegerType || returnType is PointerType) {
            objFunc.movOld(size, value, temp1)
        } else if (returnType is FloatingPoint) {
            when (value) {
                is Address -> objFunc.movf(size, value, fpRet)
                is XmmRegister -> objFunc.movf(size, value, fpRet)
                else -> TODO()
            }
        }

        emitEpilogue()
        objFunc.ret()
    }

    override fun visit(returnVoid: ReturnVoid) {
        emitEpilogue()
        objFunc.ret()
    }

    override fun visit(neg: Neg) {
        val operand = valueToRegister.operand(neg.operand())
        val result  = valueToRegister.operand(neg)
        NegCodegen(neg.type(), objFunc, result, operand)
    }

    override fun visit(neg: Not) {
        val operand = valueToRegister.operand(neg.operand())
        val result  = valueToRegister.operand(neg)
        NotCodegen(neg.type(), objFunc, result, operand)
    }

    private fun emitCall(call: Callable) {
        objFunc.call(call.prototype().name)

        val retType = call.type()
        if (retType == Type.Void) {
            return
        }
        val size = call.type().size()
        when (retType) {
            is IntegerType, is PointerType, is BooleanType -> {
                objFunc.movOld(size, rax, valueToRegister.operand(call) as Operand)
            }

            is FloatingPoint -> {
                when (val op = valueToRegister.operand(call)) {
                    is Address -> objFunc.movf(size, fpRet, op)
                    is XmmRegister -> objFunc.movf(size, fpRet, op)
                    else -> TODO()
                }
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
        val value   = valueToRegister.operand(store.value())
        val type = store.value().type()
        assert(type is PrimitiveType) {
            "should be, but type=$type"
        }

        StoreCodegen(type as PrimitiveType, objFunc, value, pointer)
    }

    override fun visit(load: Load) {
        val pointer = valueToRegister.operand(load.operand())
        val value   = valueToRegister.operand(load) as Operand
        LoadCodegen(load.type(), objFunc, value, pointer)
    }

    override fun visit(intCompare: IntCompare) {
        var first = valueToRegister.operand(intCompare.first())
        val second = valueToRegister.operand(intCompare.second())
        val size = intCompare.first().type().size()

        first = if (first is Address2) {
            objFunc.movOld(size, first, temp1)
        } else {
            first
        }

        objFunc.cmp(size, first as GPRegister, second)
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
        CopyCodegen(copy.type(), objFunc, result, operand)
    }

    override fun visit(downStackFrame: DownStackFrame) {
        val savedRegisters = valueToRegister.callerSaveRegisters(orderedLocation[downStackFrame.call()]!!)
        for (arg in savedRegisters) {
            objFunc.push(8, arg)
        }

        val totalStackSize = valueToRegister.frameSize(savedRegisters)
        if (totalStackSize % 16L != 0L) {
            objFunc.sub(8, ImmInt(8), rsp)
        }
    }

    override fun visit(upStackFrame: UpStackFrame) {
        val savedRegisters = valueToRegister.callerSaveRegisters(orderedLocation[upStackFrame.call()]!!)
        val totalStackSize = valueToRegister.frameSize(savedRegisters)

        if (totalStackSize % 16L != 0L) {
            objFunc.add(8, ImmInt(8), rsp)
        }

        for (arg in savedRegisters.reversed()) {
            objFunc.pop(8, arg)
        }
    }

    override fun visit(getElementPtr: GetElementPtr) {
        val source = valueToRegister.operand(getElementPtr.source())
        val index  = valueToRegister.operand(getElementPtr.index())
        val dest   = valueToRegister.operand(getElementPtr)
        val size = getElementPtr.type().size()

        val indexReg = when (index) {
            is Address2 -> objFunc.movOld(size, index, temp2)
            is ImmInt -> index
            else   -> index
        }

        val destReg = if (dest is Address2) {
            objFunc.movOld(size, dest, temp2)
        } else {
            dest as GPRegister
        }

        val sourceReg = if (source is Address2) {
            when (indexReg) {
                is GPRegister -> {
                    Address.mem(source.base, source.offset, indexReg, getElementPtr.basicType.size().toLong())
                }
                is ImmInt -> {
                    val offset = indexReg.value * getElementPtr.basicType.size()
                    Address.mem(source.base, source.offset + offset)
                }
                else -> {
                    throw RuntimeException("error")
                }
            }
        } else {
            source as GPRegister
            when (indexReg) {
                is GPRegister -> {
                    Address.mem(source, 0, indexReg, getElementPtr.basicType.size().toLong())
                }
                is ImmInt -> {
                    val offset = indexReg.value * getElementPtr.basicType.size()
                    Address.mem(source, offset)
                }
                else -> {
                    throw RuntimeException("error")
                }
            }
        }

        objFunc.lea(size, sourceReg, destReg)
    }

    override fun visit(cast: Cast) {
        val des = valueToRegister.operand(cast) as Operand
        val src = valueToRegister.operand(cast.value())
        val toSize = cast.type().size()
        val fromSize = cast.value().type().size()

        val srcReg = if (src is Address2) {
            objFunc.movOld(toSize, src, temp1)
        } else {
            src as GPRegister
        }

        when (cast.castType) {
            CastType.SignExtend -> {
                objFunc.movsx(fromSize, toSize, srcReg, des)
            }
            CastType.ZeroExtend, CastType.Bitcast -> {
                objFunc.movOld(toSize, srcReg, des)
            }
            CastType.Truncate -> {
                objFunc.movOld(toSize, srcReg, des)
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
        val temp1 = CallConvention.temp1
        val temp2 = CallConvention.temp2
        val fpRet = XmmRegister.xmm0

        fun codegen(module: Module): Assembler {
            if (module !is CSSAModule) {
                throw CodegenException("cannot transform module")
            }

            val asm = Assembler()

            for (c in module.constants) {
                asm.mkSymbol(c)
            }

            for ((idx, data) in module.functions().withIndex()) {
                CodeEmitter(data, idx, asm, module.regAlloc(data)).emit()
            }

            return asm
        }
    }
}