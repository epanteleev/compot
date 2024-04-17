package ir.platform.x64.codegen

import asm.x64.*
import ir.types.*
import ir.module.*
import ir.GlobalValue
import ir.instruction.*
import ir.platform.x64.*
import ir.instruction.Neg
import ir.instruction.Not
import ir.instruction.Call
import asm.x64.GPRegister.*
import common.identityHashMapOf
import ir.instruction.Lea
import ir.module.block.Label
import ir.utils.OrderedLocation
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.platform.AnyCodeGenerator
import ir.platform.CompiledModule
import ir.platform.x64.codegen.impl.*
import ir.platform.x64.regalloc.RegisterAllocation
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.CallConvention.POINTER_SIZE
import ir.platform.x64.CallConvention.DOUBLE_SUB_ZERO_SYMBOL
import ir.platform.x64.CallConvention.FLOAT_SUB_ZERO_SYMBOL


data class CodegenException(override val message: String): Exception(message)


internal class X64CodeGenerator(val module: Module): AnyCodeGenerator {
    override fun emit(): CompiledModule {
        return CodeEmitter.codegen(module)
    }
}

private class CodeEmitter(private val data: FunctionData,
                          private val functionCounter: Int,
                          private val unit: CompilationUnit,
                          private val valueToRegister: RegisterAllocation,
): IRInstructionVisitor<Unit> {
    private val asm: Assembler = unit.mkFunction(data.prototype.name)
    private val orderedLocation = run {
        val orderedLocation = identityHashMapOf<Callable, OrderedLocation>()
        var order = 0
        for (bb in data.blocks.linearScanOrder(data.blocks.loopInfo())) {
            for ((idx, call) in bb.instructions().withIndex()) {
                if (call is Callable) {
                    orderedLocation[call] = OrderedLocation(bb, idx, order)
                }
                order += 1
            }
        }

        orderedLocation
    }

    private fun makeLabel(bb: Block) = ".L$functionCounter.${bb.index}"

    private fun emitPrologue() {
        val stackSize = valueToRegister.spilledLocalsSize()
        val calleeSaveRegisters = valueToRegister.calleeSaveRegisters

        asm.push(POINTER_SIZE, rbp)
        asm.mov(POINTER_SIZE, rsp, rbp)

        if (stackSize != 0) {
            asm.sub(POINTER_SIZE, Imm32(stackSize.toLong()), rsp)
        }
        for (reg in calleeSaveRegisters) {
            asm.push(POINTER_SIZE, reg)
        }
    }

    private fun emitEpilogue() {
        val calleeSaveRegisters = valueToRegister.calleeSaveRegisters
        for (reg in calleeSaveRegisters.reversed()) { //TODO created new ds
            asm.pop(POINTER_SIZE, reg)
        }

        asm.leave()
    }

    override fun visit(binary: ArithmeticBinary) {
        val first  = valueToRegister.operand(binary.first())
        val second = valueToRegister.operand(binary.second())
        val dst    = valueToRegister.operand(binary)

        when (binary.op) {
            ArithmeticBinaryOp.Add -> AddCodegen(binary.type(), asm)(dst, first, second)
            ArithmeticBinaryOp.Mul -> MulCodegen(binary.type(), asm)(dst, first, second)
            ArithmeticBinaryOp.Sub -> SubCodegen(binary.type(), asm)(dst, first, second)
            ArithmeticBinaryOp.Xor -> XorCodegen(binary.type(), asm)(dst, first, second)
            ArithmeticBinaryOp.And -> AndCodegen(binary.type(), asm)(dst, first, second)
            ArithmeticBinaryOp.Or  -> OrCodegen(binary.type(), asm)(dst, first, second)
            ArithmeticBinaryOp.Div -> DivCodegen(binary.type(), asm)(dst, first, second)
            else -> println("Unimplemented: ${binary.op}")
        }
    }

    override fun visit(returnValue: ReturnValue) {
        val returnType = data.prototype.returnType()
        val retInstType = returnValue.type()
        val size = retInstType.size()

        assert(returnType == retInstType) { //Todo fix VerifySSA
            "should be the same, but: function.return.type=$returnType, ret.type=$retInstType"
        }

        val value = valueToRegister.operand(returnValue.value())
        if (returnType is IntegerType || returnType is PointerType) {
            asm.movOld(size, value, temp1)
        } else if (returnType is FloatingPointType) {
            when (value) {
                is Address -> asm.movf(size, value, fpRet)
                is XmmRegister -> asm.movf(size, value, fpRet)
                else -> TODO()
            }
        }

        emitEpilogue()
        asm.ret()
    }

    override fun visit(returnVoid: ReturnVoid) {
        emitEpilogue()
        asm.ret()
    }

    override fun visit(neg: Neg) {
        val operand = valueToRegister.operand(neg.operand())
        val result  = valueToRegister.operand(neg)
        if (neg.type() == Type.F32) {
            unit.mkSymbol(GlobalValue.of(FLOAT_SUB_ZERO_SYMBOL, Type.U64, ImmInt.minusZeroFloat))
        } else if (neg.type() == Type.F64) {
            unit.mkSymbol(GlobalValue.of(DOUBLE_SUB_ZERO_SYMBOL, Type.U64, ImmInt.minusZeroDouble))
        }

        NegCodegen(neg.type(), asm)(result, operand)
    }

    override fun visit(not: Not) {
        val operand = valueToRegister.operand(not.operand())
        val result  = valueToRegister.operand(not)
        NotCodegen(not.type(), asm)(result, operand)
    }

    private fun emitCall(call: Callable) {
        asm.call(call.prototype().name)

        val retType = call.type()
        if (retType == Type.Void) {
            return
        }

        when (retType) {
            is IntegerType, is PointerType, is BooleanType -> {
                retType as NonTrivialType
                val size = retType.size()
                call as ValueInstruction
                asm.movOld(size, rax, valueToRegister.operand(call))
            }

            is FloatingPointType -> {
                val size = retType.size()
                call as ValueInstruction
                when (val op = valueToRegister.operand(call)) {
                    is Address -> asm.movf(size, fpRet, op)
                    is XmmRegister -> asm.movf(size, fpRet, op)
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

    override fun visit(indirectionCall: IndirectionCall) {
        TODO("Not yet implemented")
    }

    override fun visit(indirectionVoidCall: IndirectionVoidCall) {
        when (val pointer = valueToRegister.operand(indirectionVoidCall.pointer())) {
            is GPRegister -> asm.call(pointer)
            is Address -> asm.call(pointer)
            else -> throw CodegenException("invalid operand: pointer=$pointer")
        }
    }

    override fun visit(store: Store) {
        val pointer = store.pointer()

        val pointerOperand = valueToRegister.operand(pointer)
        val value          = valueToRegister.operand(store.value())
        val type = store.value().type()

        StoreCodegen(type as PrimitiveType, asm)( value, pointerOperand)
    }

    override fun visit(load: Load) {
        val operand = load.operand()
        val pointer = valueToRegister.operand(operand)
        val value   = valueToRegister.operand(load)
        LoadCodegen(load.type(), asm)( value, pointer)
    }

    override fun visit(icmp: SignedIntCompare) { //TODO
        var first  = valueToRegister.operand(icmp.first())
        val second = valueToRegister.operand(icmp.second())
        val size = icmp.first().type().size()

        first = if (first is Address2 || first is ImmInt) { //TODO???
            asm.movOld(size, first, temp1)
        } else {
            first
        }

        asm.cmp(size, second, first as GPRegister)
    }

    override fun visit(pcmp: PointerCompare) { //TODO
        var first  = valueToRegister.operand(pcmp.first())
        val second = valueToRegister.operand(pcmp.second())
        val size = pcmp.first().type().size()

        first = if (first is Address2) {
            asm.movOld(size, first, temp1)
        } else {
            first
        }

        asm.cmp(size, second, first as GPRegister)
    }

    override fun visit(ucmp: UnsignedIntCompare) { //TODO
        var first  = valueToRegister.operand(ucmp.first())
        val second = valueToRegister.operand(ucmp.second())
        val size = ucmp.first().type().size()

        first = if (first is Address2) {
            asm.movOld(size, first, temp1)
        } else {
            first
        }

        asm.cmp(size, second, first as GPRegister)
    }

    override fun visit(floatCompare: FloatCompare) {
        val first  = valueToRegister.operand(floatCompare.first())
        val second = valueToRegister.operand(floatCompare.second())
        val size = floatCompare.first().type().size()

        when (first) {
            is XmmRegister -> {
                when (second) {
                    is XmmRegister -> {
                        asm.cmpf(size, second, first)
                    }
                    is Address -> {
                        asm.movf(size, second, xmmTemp1)
                        asm.cmpf(size, xmmTemp1, first)
                    }
                }
            }
            is Address -> {
                when (second) {
                    is XmmRegister -> {
                        asm.movf(size, first, xmmTemp1)
                        asm.cmpf(size, second, xmmTemp1)
                    }
                    is Address -> {
                        asm.movf(size, first, xmmTemp1)
                        asm.cmpf(size, second, xmmTemp1)
                    }
                }
            }
            is ImmInt -> {
                TODO()
            }
        }
    }

    override fun visit(branch: Branch) {
        asm.jump(makeLabel(branch.target()))
    }

    override fun visit(branchCond: BranchCond) {
        val cond = branchCond.condition()
        cond as CompareInstruction
        val jmpType = when (cond) {
            is SignedIntCompare -> {
                when (val convType = cond.predicate().invert()) {
                    IntPredicate.Eq -> JmpType.JE
                    IntPredicate.Ne -> JmpType.JNE
                    IntPredicate.Gt -> JmpType.JG
                    IntPredicate.Ge -> JmpType.JGE
                    IntPredicate.Lt -> JmpType.JL
                    IntPredicate.Le -> JmpType.JLE
                    else -> throw CodegenException("unknown conversion type: convType=$convType")
                }
            }
            is UnsignedIntCompare, is PointerCompare -> {
                when (val convType = cond.predicate().invert()) {
                    IntPredicate.Eq -> JmpType.JE
                    IntPredicate.Ne -> JmpType.JNE
                    IntPredicate.Gt -> JmpType.JA
                    IntPredicate.Ge -> JmpType.JAE
                    IntPredicate.Lt -> JmpType.JB
                    IntPredicate.Le -> JmpType.JBE
                    else -> throw CodegenException("unknown conversion type: convType=$convType")
                }
            }
            is FloatCompare -> {
                when (val convType = cond.predicate().invert()) {
                    FloatPredicate.Oeq -> JmpType.JE // TODO Clang insert extra instruction 'jp ${labelName}"
                    FloatPredicate.Ogt -> TODO()
                    FloatPredicate.Oge -> JmpType.JAE
                    FloatPredicate.Olt -> TODO()
                    FloatPredicate.Ole -> JmpType.JBE
                    FloatPredicate.One -> JmpType.JNE // TODO Clang insert extra instruction 'jp ${labelName}"
                    FloatPredicate.Ord -> TODO()
                    FloatPredicate.Ueq -> TODO()
                    FloatPredicate.Ugt -> TODO()
                    FloatPredicate.Uge -> TODO()
                    FloatPredicate.Ult -> TODO()
                    FloatPredicate.Ule -> JmpType.JBE
                    FloatPredicate.Uno -> TODO()
                    FloatPredicate.Une -> TODO()
                    else -> throw CodegenException("unknown conversion type: convType=$convType")
                }
            }
            else -> throw CodegenException("unknown type instruction=$cond")
        }
        asm.jcc(jmpType, makeLabel(branchCond.onFalse()))
    }

    override fun visit(copy: Copy) {
        val result  = valueToRegister.operand(copy)
        val operand = valueToRegister.operand(copy.origin())
        CopyCodegen(copy.type(), asm)(result, operand)
    }

    override fun visit(move: Move) {
        val pointer        = move.toValue()
        val pointerOperand = valueToRegister.operand(pointer)
        val value          = valueToRegister.operand(move.fromValue())
        val type = move.fromValue().type()

        MoveCodegen(type as PrimitiveType, asm)(value, pointerOperand)
    }

    override fun visit(downStackFrame: DownStackFrame) {
        val sdf = orderedLocation[downStackFrame.call()]
        val context = valueToRegister.callerSaveRegisters(sdf!!)
        for (arg in context.savedRegisters) {
            asm.push(POINTER_SIZE, arg)
        }

        for ((idx, arg) in context.savedXmmRegisters.withIndex()) {
            asm.movf(16, arg, Address.from(rsp, 16 * idx)) //TODO 16???
        }

        val size = context.adjustStackSize()
        if (size != 0) {
            asm.sub(POINTER_SIZE, Imm32(size.toLong()), rsp)
        }
    }

    override fun visit(upStackFrame: UpStackFrame) {
        val usf = orderedLocation[upStackFrame.call()]!!
        val context = valueToRegister.callerSaveRegisters(usf)

        val size = context.adjustStackSize()
        if (size != 0) {
            asm.add(POINTER_SIZE, Imm32(size.toLong()), rsp)
        }

        for ((idx, arg) in context.savedXmmRegisters.reversed().withIndex()) {
            asm.movf(16, Address.from(rsp, 16 * idx), arg) //TODO 16???
        }

        for (arg in context.savedRegisters.reversed()) {
            asm.pop(POINTER_SIZE, arg)
        }
    }

    override fun visit(gep: GetElementPtr) {
        val source = gep.source()
        val sourceOperand = valueToRegister.operand(source)
        val index         = valueToRegister.operand(gep.index())
        val dest          = valueToRegister.operand(gep)

        if (source is Alloc) {
            GetElementPtrCodegenForAlloc(gep.type(), gep.basicType, asm)(dest, sourceOperand, index)
        } else {
            GetElementPtrCodegen(gep.type(), gep.index().type().size(), gep.basicType, asm)(dest, sourceOperand, index)
        }
    }

    override fun visit(gfp: GetFieldPtr) {
        val source = gfp.source()
        val sourceOperand = valueToRegister.operand(source)
        val index         = valueToRegister.operand(gfp.index())
        val dest          = valueToRegister.operand(gfp)

        if (source is Alloc) {
            GetFieldPtrCodegenForAlloc(gfp.type(), gfp.basicType, asm)(dest, sourceOperand, index)
        } else {
            GetFieldPtrCodegen(gfp.type(), gfp.basicType, asm)(dest, sourceOperand, index)
        }
    }

    override fun visit(bitcast: Bitcast) {
        val des = valueToRegister.operand(bitcast)
        val src = valueToRegister.operand(bitcast.value())
        BitcastCodegen(bitcast.type(), asm)(des, src)
    }

    override fun visit(zext: ZeroExtend) {
        val dst = valueToRegister.operand(zext)
        val src = valueToRegister.operand(zext.value())
        ZeroExtendCodegen(zext.type(), asm)(dst, src)
    }

    override fun visit(sext: SignExtend) {
        val dst = valueToRegister.operand(sext)
        val src = valueToRegister.operand(sext.value())
        SignExtendCodegen(sext.value().type() as IntegerType, sext.type(), asm)(dst, src)
    }

    override fun visit(trunc: Truncate) {
        val dst = valueToRegister.operand(trunc)
        val src = valueToRegister.operand(trunc.value())
        TruncateCodegen(trunc.value().type() as IntegerType, trunc.type(), asm)(dst, src)
    }

    override fun visit(fptruncate: FpTruncate) {
        val dst = valueToRegister.operand(fptruncate)
        val src = valueToRegister.operand(fptruncate.value())
        FptruncateCodegen(fptruncate.type(), asm)(dst, src)
    }

    override fun visit(fpext: FpExtend) {
        val dst = valueToRegister.operand(fpext)
        val src = valueToRegister.operand(fpext.value())
        FpExtendCodegen(fpext.type(), asm)(dst, src)
    }

    override fun visit(fptosi: FloatToSigned) {
        val dst = valueToRegister.operand(fptosi)
        val src = valueToRegister.operand(fptosi.value())
        FloatToSignedCodegen(fptosi.type(), fptosi.value().type() as FloatingPointType, asm)(dst, src)
    }

    override fun visit(select: Select) {
        val dst = valueToRegister.operand(select)
        val onTrue = valueToRegister.operand(select.onTrue())
        val onFalse = valueToRegister.operand(select.onFalse())
        SelectCodegen(select.type(), select.condition() as CompareInstruction, asm)(dst, onTrue, onFalse)
    }

    override fun visit(phi: Phi) { /* nothing to do */ }
    override fun visit(alloc: Alloc) { /* nothing to do */ }
    override fun visit(generate: Generate) { /* nothing to do */ }

    override fun visit(lea: Lea) {
        val dst = valueToRegister.operand(lea)
        val gen = valueToRegister.operand(lea.generate()) as Address

        when (dst) {
            is Address -> {
                asm.lea(POINTER_SIZE, gen, temp1)
                asm.mov(POINTER_SIZE, temp1, dst)
            }
            is GPRegister -> {
                asm.lea(POINTER_SIZE, gen, dst)
            }
            else -> TODO()
        }
    }

    private fun emit() {
        emitPrologue()
        for (bb in data.blocks.preorder()) {
            if (!bb.equals(Label.entry)) {
                asm.label(makeLabel(bb))
            }

            val instructions = bb.instructions()
            for (idx in instructions.indices) {
                val instruction = instructions[idx]
                asm.comment(instruction.dump())
                instruction.visit(this)
            }
        }
    }

    companion object {
        val temp1 = CallConvention.temp1
        val fpRet = CallConvention.fpRet

        fun codegen(module: Module): CompilationUnit {
            if (module !is CSSAModule) {
                throw CodegenException("cannot transform module")
            }
            val asm = CompilationUnit()

            for (c in module.globals) {
                if (c !is GlobalValue) {
                    continue
                }

                asm.mkSymbol(c)
            }

            for ((idx, data) in module.functions().withIndex()) {
                CodeEmitter(data, idx, asm, module.regAlloc(data)).emit()
            }

            return asm
        }
    }
}