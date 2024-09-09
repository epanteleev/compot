package ir.platform.x64.codegen

import asm.x64.*
import ir.types.*
import ir.module.*
import ir.instruction.*
import ir.platform.x64.*
import ir.instruction.Neg
import ir.instruction.Not
import ir.instruction.Call
import asm.x64.GPRegister.*
import common.assertion
import ir.Definitions.POINTER_SIZE
import ir.Definitions.QWORD_SIZE
import ir.global.GlobalConstant
import ir.instruction.Add
import ir.instruction.And
import ir.instruction.Div
import ir.instruction.Or
import ir.instruction.Shl
import ir.instruction.Shr
import ir.instruction.Sub
import ir.instruction.Xor
import ir.instruction.lir.*
import ir.instruction.lir.Lea
import ir.module.block.Label
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.pass.analysis.LivenessAnalysisPassFabric
import ir.pass.analysis.traverse.PreOrderFabric
import ir.platform.common.AnyCodeGenerator
import ir.platform.common.CompiledModule
import ir.platform.x64.codegen.impl.*
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.CallConvention.DOUBLE_SUB_ZERO_SYMBOL
import ir.platform.x64.CallConvention.FLOAT_SUB_ZERO_SYMBOL
import ir.platform.x64.CallConvention.retReg
import ir.platform.x64.pass.analysis.regalloc.LinearScanFabric
import ir.platform.x64.pass.analysis.regalloc.SavedContext
import ir.value.*


internal data class CodegenException(override val message: String): Exception(message)

internal class X64CodeGenerator(val module: Module): AnyCodeGenerator {
    override fun emit(): CompiledModule {
        return CodeEmitter.codegen(module)
    }
}

private class CodeEmitter(private val data: FunctionData, private val unit: CompilationUnit): IRInstructionVisitor<Unit>() {
    private val valueToRegister by lazy { data.analysis(LinearScanFabric) }
    private val liveness by lazy { data.analysis(LivenessAnalysisPassFabric) }
    private val asm = unit.function(data.prototype.name)
    private var previous: Block? = null
    private var next: Block? = null

    fun next(): Block = next?: throw RuntimeException("next block is null")
    fun previous(): Block = previous?: throw RuntimeException("previous block is null")

    private fun makeLabel(bb: Block) = unit.nameAssistant().newLocalLabel(asm, bb.index)

    private fun emitPrologue() {
        val stackSize = valueToRegister.spilledLocalsSize()
        val calleeSaveRegisters = valueToRegister.calleeSaveRegisters

        asm.push(QWORD_SIZE, rbp)
        asm.mov(QWORD_SIZE, rsp, rbp)

        if (stackSize != 0) {
            asm.sub(QWORD_SIZE, Imm32.of(stackSize.toLong()), rsp)
        }
        for (reg in calleeSaveRegisters) {
            asm.push(QWORD_SIZE, reg)
        }
    }

    private fun emitEpilogue() {
        val calleeSaveRegisters = valueToRegister.calleeSaveRegisters
        for (reg in calleeSaveRegisters.reversed()) { //TODO created new ds
            asm.pop(QWORD_SIZE, reg)
        }

        asm.leave()
    }

    override fun visit(add: Add) {
        val first  = valueToRegister.operand(add.first())
        val second = valueToRegister.operand(add.second())
        val dst    = valueToRegister.operand(add)

        AddCodegen(add.type(), asm)(dst, first, second)
    }

    override fun visit(and: And) {
        val first  = valueToRegister.operand(and.first())
        val second = valueToRegister.operand(and.second())
        val dst    = valueToRegister.operand(and)

        AndCodegen(and.type(), asm)(dst, first, second)
    }

    override fun visit(or: Or) {
        val first  = valueToRegister.operand(or.first())
        val second = valueToRegister.operand(or.second())
        val dst    = valueToRegister.operand(or)

        OrCodegen(or.type(), asm)(dst, first, second)
    }

    override fun visit(xor: Xor) {
        val first  = valueToRegister.operand(xor.first())
        val second = valueToRegister.operand(xor.second())
        val dst    = valueToRegister.operand(xor)

        XorCodegen(xor.type(), asm)(dst, first, second)
    }

    override fun visit(mul: Mul) {
        val first  = valueToRegister.operand(mul.first())
        val second = valueToRegister.operand(mul.second())
        val dst    = valueToRegister.operand(mul)

        MulCodegen(mul.type(), asm)(dst, first, second)
    }

    override fun visit(div: Div) {
        val first  = valueToRegister.operand(div.first())
        val second = valueToRegister.operand(div.second())
        val dst    = valueToRegister.operand(div)

        FloatDivCodegen(div.type(), asm)(dst, first, second)
    }

    override fun visit(shl: Shl) {
        val first  = valueToRegister.operand(shl.first())
        val second = valueToRegister.operand(shl.second())
        val dst    = valueToRegister.operand(shl)

        when (shl.type()) {
            is UnsignedIntType -> ShlCodegen(shl.type(), asm)(dst, first, second)
            is SignedIntType   -> SalCodegen(shl.type(), asm)(dst, first, second)
        }
    }

    override fun visit(shr: Shr) {
        val first  = valueToRegister.operand(shr.first())
        val second = valueToRegister.operand(shr.second())
        val dst    = valueToRegister.operand(shr)

        when (shr.type()) {
            is UnsignedIntType -> ShrCodegen(shr.type(), asm)(dst, first, second)
            is SignedIntType   -> SarCodegen(shr.type(), asm)(dst, first, second)
        }
    }

    override fun visit(sub: Sub) {
        val first  = valueToRegister.operand(sub.first())
        val second = valueToRegister.operand(sub.second())
        val dst    = valueToRegister.operand(sub)

        SubCodegen(sub.type(), asm)(dst, first, second)
    }

    private fun emitRetValue(retInstType: PrimitiveType, returnOperand: Operand, returnRegister: Register) = when (retInstType) {
        is IntegerType, is PointerType -> {
            ReturnIntCodegen(retInstType, asm)(returnRegister as GPRegister, returnOperand)
        }
        is FloatingPointType -> {
            ReturnFloatCodegen(retInstType, asm)(returnRegister as XmmRegister, returnOperand)
        }
        else -> throw CodegenException("unknown type=$retInstType")
    }

    override fun visit(returnValue: ReturnValue) {
        when (val returnType = returnValue.type()) {
            is IntegerType, is PointerType -> { returnType as PrimitiveType
                val value = valueToRegister.operand(returnValue.returnValue(0))
                emitRetValue(returnType, value, retReg)
            }
            is FloatingPointType -> {
                val value = valueToRegister.operand(returnValue.returnValue(0))
                emitRetValue(returnType, value, fpRet)
            }
            is TupleType -> {
                val value = valueToRegister.operand(returnValue.returnValue(0))
                when (val first  = returnType.asInnerType<PrimitiveType>(0)) {
                    is IntegerType, is PointerType -> {
                        ReturnIntCodegen(first, asm)(retReg, value)
                    }
                    is FloatingPointType -> {
                        ReturnFloatCodegen(first, asm)(fpRet, value)
                    }
                    else -> throw CodegenException("unknown type=$first")
                }

                val value1 = valueToRegister.operand(returnValue.returnValue(1))
                when (val second = returnType.asInnerType<PrimitiveType>(1)) {
                    is IntegerType, is PointerType -> {
                        ReturnIntCodegen(second, asm)(rdx, value1)
                    }
                    is FloatingPointType -> {
                        ReturnFloatCodegen(second, asm)(XmmRegister.xmm1, value1)
                    }
                    else -> throw CodegenException("unknown type=$second")
                }
            }

            else -> throw CodegenException("unknown type=$returnType")
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
            unit.mkConstant(GlobalConstant.of(FLOAT_SUB_ZERO_SYMBOL, Type.U64, ImmInt.minusZeroFloat))
        } else if (neg.type() == Type.F64) {
            unit.mkConstant(GlobalConstant.of(DOUBLE_SUB_ZERO_SYMBOL, Type.U64, ImmInt.minusZeroDouble))
        }

        NegCodegen(neg.type(), asm)(result, operand)
    }

    override fun visit(not: Not) {
        val operand = valueToRegister.operand(not.operand())
        val result  = valueToRegister.operand(not)
        NotCodegen(not.type(), asm)(result, operand)
    }

    override fun visit(voidCall: VoidCall) {
        asm.callFunction(voidCall)

        assertion(voidCall.target() === next()) {
            // This is a bug in the compiler if this assertion fails
            "expected invariant failed: call.target=${voidCall.target()}, next=${next()}"
        }
    }

    override fun visit(tupleCall: TupleCall) { //TODO not compatible with linux C calling convention
        asm.callFunction(tupleCall)
        val retType = tupleCall.type()

        val first  = retType.asInnerType<PrimitiveType>(0)
        val second = retType.asInnerType<PrimitiveType>(1)

        val value = valueToRegister.operand(tupleCall.proj(0)!!)
        if (first is IntegerType || first is PointerType) {
            CallIntCodegen(first, asm)(value, retReg)
        } else if (first is FloatingPointType) {
            CallFloatCodegen(first, asm)(value, fpRet)
        } else {
            throw CodegenException("unknown type=$first")
        }

        val value1 = valueToRegister.operand(tupleCall.proj(1)!!)
        if (second is IntegerType || second is PointerType) {
            CallIntCodegen(second, asm)(value1, rdx)
        } else if (second is FloatingPointType) {
            CallFloatCodegen(second, asm)(value1, XmmRegister.xmm1)
        } else {
            throw CodegenException("unknown type=$second")
        }
    }

    override fun visit(int2ptr: Int2Pointer) {
        val dst = valueToRegister.operand(int2ptr)
        val src = valueToRegister.operand(int2ptr.value())
        CopyCodegen(int2ptr.type(), asm)(dst, src)
    }

    override fun visit(ptr2Int: Pointer2Int) {
        val dst = valueToRegister.operand(ptr2Int)
        val src = valueToRegister.operand(ptr2Int.value())
        CopyCodegen(ptr2Int.type(), asm)(dst, src)
    }

    override fun visit(memcpy: Memcpy) {
        val dst = valueToRegister.operand(memcpy.destination())
        val src = valueToRegister.operand(memcpy.source())
        MemcpyCodegen(memcpy.length(), asm)(dst, src)
    }

    override fun visit(indexedLoad: IndexedLoad) {
        val dst = valueToRegister.operand(indexedLoad)
        val first = valueToRegister.operand(indexedLoad.origin())
        val second = valueToRegister.operand(indexedLoad.index())
        IndexedLoadCodegen(indexedLoad.type(), indexedLoad.index().asType<PrimitiveType>(), asm)(dst, first, second)
    }

    override fun visit(store: StoreOnStack) {
        val pointerOperand = valueToRegister.operand(store.destination())
        val value = valueToRegister.operand(store.source())
        val index = valueToRegister.operand(store.index())

        StoreOnStackCodegen(store.source().type() as PrimitiveType, asm)(pointerOperand, value, index)
    }

    override fun visit(loadst: LoadFromStack) {
        val origin = valueToRegister.operand(loadst.origin())
        val index  = valueToRegister.operand(loadst.index())
        val dst    = valueToRegister.operand(loadst)

        LoadFromStackCodegen(loadst.type(), loadst.index().asType(), asm)(dst, origin, index)
    }

    override fun visit(leaStack: LeaStack) {
        val sourceOperand = valueToRegister.operand(leaStack.origin())
        val index         = valueToRegister.operand(leaStack.index())
        val dest          = valueToRegister.operand(leaStack)

        LeaStackCodegen(Type.Ptr, leaStack.loadedType, asm)(dest, sourceOperand, index)
    }

    override fun visit(binary: TupleDiv) {
        val divType = binary.type()

        val first  = valueToRegister.operand(binary.first())
        val second = valueToRegister.operand(binary.second())

        val quotientOperand = run {
            val quotient = binary.quotient()
            if (quotient != null) {
                valueToRegister.operand(quotient)
            } else {
                null
            }
        }

        val remainderOperand = run {
            val remainder = binary.remainder()
            if (remainder != null) {
                valueToRegister.operand(remainder)
            } else {
                null
            }
        }

        if (quotientOperand != rdx && remainderOperand != rdx) {
            asm.push(POINTER_SIZE, rdx) //TODO pessimistic spill rdx
        }
        when (val type = divType.asInnerType<ArithmeticType>(1)) {
            is SignedIntType   -> IntDivCodegen(type, remainderOperand ?: rdx, asm)(quotientOperand ?: rax, first, second)
            is UnsignedIntType -> UIntDivCodegen(type, remainderOperand ?: rdx, asm)(quotientOperand ?: rax, first, second)
            else -> throw RuntimeException("type=$type")
        }
        if (quotientOperand != rdx && remainderOperand != rdx) {
            asm.pop(POINTER_SIZE, rdx)
        }
    }

    override fun visit(proj: Projection) {
        // Skip. Projection must be handled by its user
    }

    override fun visit(switch: Switch) {
        TODO("Not yet implemented")
    }

    override fun visit(call: Call) {
        asm.callFunction(call)

        when (val retType = call.type()) {
            is IntegerType, is PointerType, is BooleanType -> { retType as PrimitiveType
                CallIntCodegen(retType, asm)(valueToRegister.operand(call), retReg)
            }

            is FloatingPointType -> {
                CallFloatCodegen(retType, asm)(valueToRegister.operand(call), fpRet)
            }
            else -> throw RuntimeException("unknown value type=$retType")
        }

        assertion(call.target() === next()) {
            // This is a bug in the compiler if this assertion fails
            "expected invariant failed: call.target=${call.target()}, next=${next()}"
        }
    }

    override fun visit(flag2Int: Flag2Int) {
        val dst = valueToRegister.operand(flag2Int)
        val src = valueToRegister.operand(flag2Int.value())
        val compare = flag2Int.value() as CompareInstruction

        val isNeighbour = flag2Int.prev() != null && flag2Int.prev() == flag2Int.value()
        if (isNeighbour) {
            asm.setcc(compare.predicate(), dst)
            Flag2IntCodegen(flag2Int.type().sizeOf(), asm)(dst, dst)
        } else {
            Flag2IntCodegen(flag2Int.type().sizeOf(), asm)(dst, src)
        }

    }

    override fun visit(indirectionCall: IndirectionCall) {
        val pointer = valueToRegister.operand(indirectionCall.pointer())
        asm.indirectCall(indirectionCall, pointer)

        when (val retType = indirectionCall.type()) {
            is IntegerType, is PointerType, is BooleanType -> { retType as PrimitiveType
                CallIntCodegen(retType, asm)(valueToRegister.operand(indirectionCall), retReg)
            }
            is FloatingPointType -> {
                CallFloatCodegen(retType, asm)(valueToRegister.operand(indirectionCall), fpRet)
            }
            else -> throw RuntimeException("unknown value type=$retType")
        }
    }

    override fun visit(indirectionVoidCall: IndirectionVoidCall) {
        val pointer = valueToRegister.operand(indirectionVoidCall.pointer())
        asm.indirectCall(indirectionVoidCall, pointer)
    }

    override fun visit(store: Store) {
        val pointer = store.pointer()

        val pointerOperand = valueToRegister.operand(pointer)
        val value          = valueToRegister.operand(store.value())
        val type = store.value().type()

        StoreCodegen(type as PrimitiveType, asm)(value, pointerOperand)
    }

    override fun visit(load: Load) {
        val pointer = valueToRegister.operand(load.operand())
        val value   = valueToRegister.operand(load)
        LoadCodegen(load.type(), asm)(value, pointer)
    }

    override fun visit(icmp: IntCompare) {
        val first  = valueToRegister.operand(icmp.first())
        val second = valueToRegister.operand(icmp.second())
        val dst    = valueToRegister.operand(icmp)

        IntCmpCodegen(icmp.first().asType(), asm)(first, second)
        if (needSetcc(icmp)) {
            asm.setcc(icmp.predicate(), dst)
        }
    }

    private fun needSetcc(cmp: CompareInstruction): Boolean {
        val users = cmp.usedIn()
        return users.size > 1 || users.first() != cmp.next()
    }

    override fun visit(fcmp: FloatCompare) {
        val first  = valueToRegister.operand(fcmp.first())
        val second = valueToRegister.operand(fcmp.second())
        val dst    = valueToRegister.operand(fcmp)

        FloatCmpCodegen(fcmp.first().asType(), asm)(first, second)
        if (needSetcc(fcmp)) {
            asm.setcc(fcmp.predicate(), dst)
        }
    }

    private fun doJump(target: Block) {
        if (target == next()) {
            // Not necessary to emit jump instruction
            // because the next block is the target of the branch
            return
        }

        asm.jump(makeLabel(target))
    }

    override fun visit(branch: Branch) {
        doJump(branch.target())
    }

    override fun visit(branchCond: BranchCond) {
        when (val cond = branchCond.condition()) {
            is BoolValue -> {
                // Condition is constant - true or false.
                // Just select necessary branch and jump to it.
                if (cond.bool) {
                    doJump(branchCond.onTrue())
                } else {
                    doJump(branchCond.onFalse())
                }
            }
            is CompareInstruction -> {
                val jmpType = asm.condType(cond, cond.operandsType())
                asm.jcc(jmpType, makeLabel(branchCond.onFalse()))
            }
            else -> throw CodegenException("unknown condition type, cond=${cond}")
        }
    }

    override fun visit(copy: Copy) {
        if (copy.origin() is UndefinedValue) {
            // Do nothing. UB is UB
            return
        }
        val result  = valueToRegister.operand(copy)
        val operand = valueToRegister.operand(copy.origin())
        CopyCodegen(copy.type(), asm)(result, operand)
    }

    override fun visit(move: Move) {
        val source      = valueToRegister.operand(move.source())
        val destination = valueToRegister.operand(move.destination())

        val type = move.source().asType<PrimitiveType>()
        MoveCodegen(type, asm)(destination, source)
    }

    override fun visit(move: MoveByIndex) {
        val index       = valueToRegister.operand(move.index())
        val destination = valueToRegister.operand(move.destination())

        val indexType  = move.index().asType<PrimitiveType>()
        val sourceType = move.source().asType<PrimitiveType>()
        val srcOperand = valueToRegister.operand(move.source())
        MoveByIndexCodegen(sourceType, indexType, asm)(destination, srcOperand, index)
    }

    private fun getState(call: Callable): SavedContext {
        // Any callable instruction is TerminateInstruction
        // so that we can easily get the caller save registers
        // from the live-out of the block
        call as Instruction
        val liveOut = liveness.liveOut(call.owner())
        val exclude = if (call is LocalValue) {
            // Exclude call from liveOut
            // because this is value haven't been existed when the call is executed
            setOf(call)
        } else {
            setOf()
        }

        return valueToRegister.callerSaveRegisters(liveOut, exclude)
    }

    override fun visit(downStackFrame: DownStackFrame) {
        val call = downStackFrame.call()
        val context = getState(call)

        for (arg in context.savedRegisters) {
            asm.push(POINTER_SIZE, arg)
        }

        for ((idx, arg) in context.savedXmmRegisters.withIndex()) {
            asm.movf(8, arg, Address.from(rsp, -(QWORD_SIZE * idx + QWORD_SIZE)))
        }

        val size = context.adjustStackSize()
        if (size != 0) {
            asm.sub(POINTER_SIZE, Imm32.of(size.toLong()), rsp)
        }
    }

    override fun visit(upStackFrame: UpStackFrame) {
        val call = upStackFrame.call()
        val context = getState(call)

        val size = context.adjustStackSize()
        if (size != 0) {
            asm.add(POINTER_SIZE, Imm32.of(size.toLong()), rsp)
        }

        for ((idx, arg) in context.savedXmmRegisters.reversed().withIndex()) {
            asm.movf(8, Address.from(rsp, -(QWORD_SIZE * (context.savedXmmRegisters.size - idx - 1) + QWORD_SIZE)), arg)
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

        val elementSize = gep.index().asType<NonTrivialType>().sizeOf()
        GetElementPtrCodegen(gep.type(), elementSize, gep.basicType, asm)(dest, sourceOperand, index)
    }

    override fun visit(gfp: GetFieldPtr) {
        val source = gfp.source()
        val sourceOperand = valueToRegister.operand(source)
        val index         = valueToRegister.operand(gfp.index(0))
        val dest          = valueToRegister.operand(gfp)

        GetFieldPtrCodegen(gfp.type(), gfp.basicType, asm)(dest, sourceOperand, index)
    }

    override fun visit(bitcast: Bitcast) {
        val des = valueToRegister.operand(bitcast)
        val src = valueToRegister.operand(bitcast.value())
        BitcastCodegen(bitcast.type(), asm)(des, src)
    }

    override fun visit(itofp: Int2Float) {
        val dst = valueToRegister.operand(itofp)
        val src = valueToRegister.operand(itofp.value())
        Int2FloatCodegen(itofp.type(), itofp.value().asType<IntegerType>(), asm)(dst, src)
    }

    override fun visit(zext: ZeroExtend) {
        val dst = valueToRegister.operand(zext)
        val src = valueToRegister.operand(zext.value())
        val toType   = zext.asType<IntegerType>()
        val fromType = zext.value().asType<IntegerType>()
        ZeroExtendCodegen(fromType, toType, asm)(dst, src)
    }

    override fun visit(sext: SignExtend) {
        val dst = valueToRegister.operand(sext)
        val src = valueToRegister.operand(sext.value())
        SignExtendCodegen(sext.value().asType<IntegerType>(), sext.type(), asm)(dst, src)
    }

    override fun visit(trunc: Truncate) {
        val dst = valueToRegister.operand(trunc)
        val src = valueToRegister.operand(trunc.value())
        TruncateCodegen(trunc.value().asType<IntegerType>(), trunc.type(), asm)(dst, src)
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

    override fun visit(fptosi: FloatToInt) {
        val dst = valueToRegister.operand(fptosi)
        val src = valueToRegister.operand(fptosi.value())
        FloatToSignedCodegen(fptosi.type(), fptosi.value().asType<FloatingPointType>(), asm)(dst, src)
    }

    override fun visit(select: Select) {
        val dst     = valueToRegister.operand(select)
        val onTrue  = valueToRegister.operand(select.onTrue())
        val onFalse = valueToRegister.operand(select.onFalse())
        SelectCodegen(select.type(), select.condition() as IntCompare, asm)(dst, onTrue, onFalse)
    }

    override fun visit(phi: Phi) { /* nothing to do */ }

    override fun visit(alloc: Alloc) {
        throw CodegenException("${Alloc.NAME} should be handled before code generation")
    }

    override fun visit(generate: Generate) { /* nothing to do */ }

    override fun visit(lea: Lea) {
        val dst = valueToRegister.operand(lea)
        val gen = valueToRegister.operand(lea.operand()) as Address

        when (dst) {
            is Address -> {
                asm.lea(POINTER_SIZE, gen, temp1)
                asm.mov(POINTER_SIZE, temp1, dst)
            }
            is GPRegister -> {
                asm.lea(POINTER_SIZE, gen, dst)
            }
            else -> throw CodegenException("unknown dst=$dst")
        }
    }

    private fun emit() {
        emitPrologue()
        val order = data.analysis(PreOrderFabric)

        for (idx in order.indices) {
            val bb = order[idx]
            next = if (idx + 1 < order.size) {
                order[idx + 1]
            } else {
                null
            }
            previous = if (idx - 1 >= 0) {
                order[idx - 1]
            } else {
                null
            }

            if (!bb.equals(Label.entry)) {
                asm.label(makeLabel(bb))
            }

            bb.instructions { instruction ->
                asm.comment(instruction.dump())
                instruction.visit(this)
            }
        }
    }

    companion object {
        val temp1 = CallConvention.temp1
        val fpRet = CallConvention.fpRet

        fun codegen(module: Module): CompilationUnit {
            if (module !is LModule) {
                throw CodegenException("cannot transform module")
            }
            val unit = CompilationUnit()

            for (data in module.functions()) {
                unit.global(data.prototype.name)
            }

            if (module.globals.isNotEmpty() || module.constantPool.isNotEmpty()) {
                unit.section(DataSection)
                for (c in module.constantPool.values) {
                    unit.mkConstant(c)
                }

                for (global in module.globals.values) {
                    unit.makeGlobal(global)
                }
            }

            if (module.functions().isEmpty()) {
                return unit
            }
            unit.section(TextSection)
            for (data in module.functions()) {
                CodeEmitter(data, unit).emit()
            }

            return unit
        }
    }
}