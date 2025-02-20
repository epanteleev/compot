package ir.platform.x64.codegen

import asm.*
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
import common.forEachWith
import ir.Definitions
import ir.Definitions.POINTER_SIZE
import ir.Definitions.QWORD_SIZE
import ir.attributes.GlobalValueAttribute
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
import ir.platform.x64.CallConvention.retReg
import ir.platform.x64.pass.analysis.regalloc.LinearScanFabric
import ir.platform.x64.pass.analysis.regalloc.SavedContext
import ir.value.*
import ir.value.constant.BoolValue
import ir.value.constant.UndefValue


internal data class CodegenException(override val message: String): Exception(message)

internal class X64CodeGenerator(val module: Module): AnyCodeGenerator {
    override fun emit(): CompiledModule {
        return CodeEmitter.codegen(module)
    }
}

private class CodeEmitter(private val data: FunctionData, private val unit: CompilationUnit): IRInstructionVisitor<Unit>() {
    private val registerAllocation by lazy { data.analysis(LinearScanFabric) }
    private val liveness by lazy { data.analysis(LivenessAnalysisPassFabric) }
    private val  asm = unit.function(data.prototype.name)
    private var next: Block? = null

    fun next(): Block = next?: throw RuntimeException("next block is null")

    private fun makeLabel(bb: Block) = unit.nameAssistant().newLocalLabel(asm, bb.index)

    private fun emitPrologue() {
        val stackSize = registerAllocation.spilledLocalsSize()
        val calleeSaveRegisters = registerAllocation.calleeSaveRegisters

        // Stack frame layout check.
        //asm.assertStackFrameLayout()

        asm.push(QWORD_SIZE, rbp)
        asm.copy(QWORD_SIZE, rsp, rbp)

        if (stackSize != 0) {
            asm.sub(QWORD_SIZE, Imm32.of(stackSize.toLong()), rsp)
        }
        for (reg in calleeSaveRegisters) {
            asm.push(QWORD_SIZE, reg)
        }
    }

    private fun emitEpilogue() {
        val calleeSaveRegisters = registerAllocation.calleeSaveRegisters
        for (idx in calleeSaveRegisters.indices.reversed()) {
            asm.pop(QWORD_SIZE, calleeSaveRegisters[idx])
        }

        asm.leave()
    }

    override fun visit(add: Add) {
        val first  = registerAllocation.operand(add.lhs())
        val second = registerAllocation.operand(add.rhs())
        val dst    = registerAllocation.operand(add)

        when (val type = add.type()) {
            is IntegerType       -> AddCodegen(type, asm)(dst, first, second)
            is FloatingPointType -> FAddCodegen(type, asm)(dst, first, second)
            is UndefType         -> {}
        }
    }

    override fun visit(and: And) {
        val first  = registerAllocation.operand(and.lhs())
        val second = registerAllocation.operand(and.rhs())
        val dst    = registerAllocation.operand(and)

        AndCodegen(and.type(), asm)(dst, first, second)
    }

    override fun visit(or: Or) {
        val first  = registerAllocation.operand(or.lhs())
        val second = registerAllocation.operand(or.rhs())
        val dst    = registerAllocation.operand(or)

        OrCodegen(or.type(), asm)(dst, first, second)
    }

    override fun visit(xor: Xor) {
        val first  = registerAllocation.operand(xor.lhs())
        val second = registerAllocation.operand(xor.rhs())
        val dst    = registerAllocation.operand(xor)

        XorCodegen(xor.type(), asm)(dst, first, second)
    }

    override fun visit(fadd: Fxor) {
        val first  = registerAllocation.operand(fadd.lhs())
        val second = registerAllocation.operand(fadd.rhs())
        val dst    = registerAllocation.operand(fadd)

        FXorCodegen(fadd.type(), asm)(dst, first, second)
    }

    override fun visit(mul: Mul) {
        val first  = registerAllocation.operand(mul.lhs())
        val second = registerAllocation.operand(mul.rhs())
        val dst    = registerAllocation.operand(mul)

        when (val type = mul.type()) {
            is IntegerType     -> IMulCodegen(type, asm)(dst, first, second)
            is FloatingPointType -> FMulCodegen(type, asm)(dst, first, second)
            is UndefType       -> {}
        }
    }

    override fun visit(div: Div) {
        val first  = registerAllocation.operand(div.lhs())
        val second = registerAllocation.operand(div.rhs())
        val dst    = registerAllocation.operand(div)

        FloatDivCodegen(div.type(), asm)(dst, first, second)
    }

    override fun visit(shl: Shl) {
        val first  = registerAllocation.operand(shl.lhs())
        val second = registerAllocation.operand(shl.rhs())
        val dst    = registerAllocation.operand(shl)

        when (shl.type()) {
            is UnsignedIntType -> ShlCodegen(shl.type(), asm)(dst, first, second)
            is SignedIntType   -> SalCodegen(shl.type(), asm)(dst, first, second)
        }
    }

    override fun visit(shr: Shr) {
        val first  = registerAllocation.operand(shr.lhs())
        val second = registerAllocation.operand(shr.rhs())
        val dst    = registerAllocation.operand(shr)

        when (shr.type()) {
            is UnsignedIntType -> ShrCodegen(shr.type(), asm)(dst, first, second)
            is SignedIntType   -> SarCodegen(shr.type(), asm)(dst, first, second)
        }
    }

    override fun visit(sub: Sub) {
        val first  = registerAllocation.operand(sub.lhs())
        val second = registerAllocation.operand(sub.rhs())
        val dst    = registerAllocation.operand(sub)

        when (val type = sub.type()) {
            is IntegerType       -> SubCodegen(type, asm)(dst, first, second)
            is FloatingPointType -> FSubCodegen(type, asm)(dst, first, second)
            is UndefType         -> {}
        }
    }

    private fun emitRetValue(retInstType: PrimitiveType, returnOperand: Operand) = when (retInstType) {
        is IntegerType, is PtrType -> {
            ReturnIntCodegen(retInstType, asm)(retReg, returnOperand)
        }
        is FloatingPointType -> {
            ReturnFloatCodegen(retInstType, asm)(fpRet, returnOperand)
        }
        is UndefType -> TODO("undefined behavior")
    }

    override fun visit(returnValue: ReturnValue) {
        val value = registerAllocation.operand(returnValue.returnValue(0))
        when (val returnType = returnValue.type()) {
            is PrimitiveType -> emitRetValue(returnType, value)
            is TupleType -> {
                when (val first = returnType.asInnerType<PrimitiveType>(0)) {
                    is IntegerType, is PtrType -> ReturnIntCodegen(first, asm)(retReg, value)
                    is FloatingPointType -> ReturnFloatCodegen(first, asm)(fpRet, value)
                    is UndefType -> TODO("undefined behavior")
                }

                val value1 = registerAllocation.operand(returnValue.returnValue(1))
                when (val second = returnType.asInnerType<PrimitiveType>(1)) {
                    is IntegerType, is PtrType -> ReturnIntCodegen(second, asm)(rdx, value1)
                    is FloatingPointType -> ReturnFloatCodegen(second, asm)(XmmRegister.xmm1, value1)
                    is UndefType -> TODO("undefined behavior")
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
        val operand = registerAllocation.operand(neg.operand())
        val result  = registerAllocation.operand(neg)
        NegCodegen(neg.type(), asm)(result, operand)
    }

    override fun visit(not: Not) {
        val operand = registerAllocation.operand(not.operand())
        val result  = registerAllocation.operand(not)
        NotCodegen(not.type(), asm)(result, operand)
    }

    override fun visit(voidCall: VoidCall) {
        asm.callFunction(voidCall, voidCall.prototype())

        assertion(voidCall.target() === next()) {
            // This is a bug in the compiler if this assertion fails
            "expected invariant failed: call.target=${voidCall.target()}, next=${next()}"
        }
    }

    private fun consumeTupleCallOutputs(tupleCall: TerminateTupleInstruction) {
        val retType = tupleCall.type()
        val firstProj = tupleCall.proj(0)
        if (firstProj != null) {
            val value = registerAllocation.operand(firstProj)
            when (val first  = retType.asInnerType<PrimitiveType>(0)) {
                is IntegerType, is PtrType -> CallIntCodegen(first, asm)(value, retReg)
                is FloatingPointType -> CallFloatCodegen(first, asm)(value, fpRet)
                else -> throw CodegenException("unknown type=$first")
            }
        }

        val secondProj = tupleCall.proj(1)
        if (secondProj != null) {
            val value1 = registerAllocation.operand(secondProj)
            when (val second = retType.asInnerType<PrimitiveType>(1)) {
                is IntegerType, is PtrType -> CallIntCodegen(second, asm)(value1, rdx)
                is FloatingPointType -> CallFloatCodegen(second, asm)(value1, XmmRegister.xmm1)
                else -> throw CodegenException("unknown type=$second")
            }
        }
    }

    override fun visit(tupleCall: TupleCall) { //TODO not compatible with linux C calling convention
        asm.callFunction(tupleCall, tupleCall.prototype())
        consumeTupleCallOutputs(tupleCall)
    }

    override fun visit(int2ptr: Int2Pointer) {
        val dst = registerAllocation.operand(int2ptr)
        val src = registerAllocation.operand(int2ptr.value())
        when (val type = int2ptr.value().asType<IntegerType>()) {
            is SignedIntType -> {
                if (type.sizeOf() == QWORD_SIZE) {
                    CopyCodegen(int2ptr.type(), asm)(dst, src)
                } else {
                    SignExtendCodegen(type, I64Type, asm)(dst, src)
                }
            }
            is UnsignedIntType -> CopyCodegen(int2ptr.type(), asm)(dst, src)
        }
    }

    override fun visit(ptr2Int: Pointer2Int) {
        val dst = registerAllocation.operand(ptr2Int)
        val src = registerAllocation.operand(ptr2Int.value())
        CopyCodegen(ptr2Int.type(), asm)(dst, src)
    }

    override fun visit(memcpy: Memcpy) {
        val dst = registerAllocation.operand(memcpy.destination())
        val src = registerAllocation.operand(memcpy.source())
        MemcpyCodegen(memcpy.length(), asm)(dst, src)
    }

    override fun visit(indexedLoad: IndexedLoad) {
        val dst = registerAllocation.operand(indexedLoad)
        val first = registerAllocation.operand(indexedLoad.origin())
        val second = registerAllocation.operand(indexedLoad.index())
        IndexedLoadCodegen(indexedLoad.type(), indexedLoad.index().asType(), asm)(dst, first, second)
    }

    override fun visit(store: StoreOnStack) {
        val pointerOperand = registerAllocation.operand(store.destination())
        val value = registerAllocation.operand(store.source())
        val index = registerAllocation.operand(store.index())

        StoreOnStackCodegen(store.source().type().asType(), store.index().asType(), asm)(pointerOperand, value, index)
    }

    override fun visit(loadst: LoadFromStack) {
        val origin = registerAllocation.operand(loadst.origin())
        val index  = registerAllocation.operand(loadst.index())
        val dst    = registerAllocation.operand(loadst)

        LoadFromStackCodegen(loadst.type(), loadst.index().asType(), asm)(dst, origin, index)
    }

    override fun visit(leaStack: LeaStack) {
        val sourceOperand = registerAllocation.operand(leaStack.origin())
        val index         = registerAllocation.operand(leaStack.index())
        val dest          = registerAllocation.operand(leaStack)

        LeaStackCodegen(PtrType, leaStack.loadedType, asm)(dest, sourceOperand, index)
    }

    override fun visit(tupleDiv: TupleDiv) {
        val divType = tupleDiv.type()

        val first  = registerAllocation.operand(tupleDiv.first())
        val second = registerAllocation.operand(tupleDiv.second())
        val quotient = registerAllocation.operand(tupleDiv.quotient())

        val remainderOperand = tupleDiv.remainder()
        assertion(registerAllocation.operand(remainderOperand) == rdx) {
            "remainderOperand=${registerAllocation.operand(remainderOperand)}, rdx=$rdx"
        }

        when (val type = divType.asInnerType<IntegerType>(1)) {
            is SignedIntType   -> IntDivCodegen(type, asm)(quotient, first, second)
            is UnsignedIntType -> UIntDivCodegen(type, asm)(quotient, first, second)
        }
    }

    override fun visit(proj: Projection) {
        // Skip. Projection must be handled by its user
    }

    override fun visit(switch: Switch) {
        val type = switch.value().asType<IntegerType>()
        switch.jumps().forEachWith(switch.table()) { target, value ->
            IntCmpCodegen(type, asm)(registerAllocation.operand(switch.value()), Imm64.of(value.toInt()))
            asm.jcc(CondType.JE, makeLabel(target))
        }
        asm.jump(makeLabel(switch.default()))
    }

    override fun visit(tupleCall: IndirectionTupleCall) {
        val pointer = registerAllocation.operand(tupleCall.pointer())
        asm.indirectCall(tupleCall, pointer)
        consumeTupleCallOutputs(tupleCall)
    }

    override fun visit(intrinsic: Intrinsic) {
        val values = intrinsic.operands().map { registerAllocation.operand(it) }
        intrinsic.implementor.implement(asm, values)
    }

    override fun visit(call: Call) {
        asm.callFunction(call, call.prototype())

        when (val retType = call.type()) {
            is IntegerType, is PtrType -> {
                CallIntCodegen(retType, asm)(registerAllocation.operand(call), retReg)
            }
            is FloatingPointType -> {
                CallFloatCodegen(retType, asm)(registerAllocation.operand(call), fpRet)
            }
            is UndefType -> println("UB in call") //TODO remove this
        }

        assertion(call.target() === next()) {
            // This is a bug in the compiler if this assertion fails
            "expected invariant failed: call.target=${call.target()}, next=${next()}"
        }
    }

    override fun visit(flag2Int: Flag2Int) {
        val dst = registerAllocation.operand(flag2Int)

        when (val compare = flag2Int.value()) {
            is CompareInstruction -> {
                when (val jmpType = compare.predicate()) {
                    is IntPredicate   -> asm.setccInt(compare.operandsType().asType(), jmpType, dst)
                    is FloatPredicate -> asm.setccFloat(jmpType, dst)
                }
                Flag2IntCodegen(flag2Int.type().sizeOf(), asm)(dst, dst)
            }
            is BoolValue -> {
                val size = flag2Int.type().sizeOf()
                val res = if (compare.bool) 1L else 0L
                when (dst) {
                    is Address    -> asm.mov(size, Imm64.of(res), dst)
                    is GPRegister -> asm.mov(size, Imm64.of(res), dst)
                    else -> throw CodegenException("unknown dst=$dst")
                }
            }
            else -> throw CodegenException("unknown compare=$compare")
        }
    }

    override fun visit(indirectionCall: IndirectionCall) {
        val pointer = registerAllocation.operand(indirectionCall.pointer())
        asm.indirectCall(indirectionCall, pointer)

        when (val retType = indirectionCall.type()) {
            is IntegerType, is PtrType -> {
                CallIntCodegen(retType, asm)(registerAllocation.operand(indirectionCall), retReg)
            }
            is FloatingPointType -> {
                CallFloatCodegen(retType, asm)(registerAllocation.operand(indirectionCall), fpRet)
            }
            is UndefType -> TODO("undefined behavior")
        }
    }

    override fun visit(indirectionVoidCall: IndirectionVoidCall) {
        val pointer = registerAllocation.operand(indirectionVoidCall.pointer())
        asm.indirectCall(indirectionVoidCall, pointer)
    }

    override fun visit(store: Store) {
        val pointer = store.pointer()

        val pointerOperand = registerAllocation.operand(pointer)
        val value          = registerAllocation.operand(store.value())
        val type = store.value().type()

        StoreCodegen(type.asType(), asm)(value, pointerOperand)
    }

    override fun visit(load: Load) {
        val pointer = registerAllocation.operand(load.operand())
        val value   = registerAllocation.operand(load)
        LoadCodegen(load.type(), asm)(value, pointer)
    }

    override fun visit(icmp: IntCompare) {
        val first  = registerAllocation.operand(icmp.first())
        val second = registerAllocation.operand(icmp.second())
        assertion(registerAllocation.operandOrNull(icmp) == null) {
            "wasting register in icmp=${icmp}, operand=${registerAllocation.operandOrNull(icmp)}"
        }

        IntCmpCodegen(icmp.first().asType(), asm)(first, second)
    }

    override fun visit(fcmp: FloatCompare) {
        val first  = registerAllocation.operand(fcmp.first())
        val second = registerAllocation.operand(fcmp.second())
        assertion(registerAllocation.operandOrNull(fcmp) == null) {
            "wasting register in icmp=${fcmp}, operand=${registerAllocation.operandOrNull(fcmp)}"
        }

        FloatCmpCodegen(fcmp.first().asType(), asm)(first, second)
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
        if (copy.origin() is UndefValue) {
            // Do nothing. UB is UB
            return
        }
        val result  = registerAllocation.operand(copy)
        val operand = registerAllocation.operand(copy.origin())
        CopyCodegen(copy.type(), asm)(result, operand)
    }

    override fun visit(move: Move) {
        val source      = registerAllocation.operand(move.source())
        val destination = registerAllocation.operand(move.destination())

        val type = move.source().asType<PrimitiveType>()
        MoveCodegen(type, asm)(destination, source)
    }

    override fun visit(move: MoveByIndex) {
        val index       = registerAllocation.operand(move.index())
        val destination = registerAllocation.operand(move.destination())

        val indexType  = move.index().asType<PrimitiveType>()
        val sourceType = move.source().asType<PrimitiveType>()
        val srcOperand = registerAllocation.operand(move.source())
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

        return registerAllocation.callerSaveRegisters(liveOut, exclude)
    }

    private fun overflowAreaSize(call: Callable): Int {
        var argumentsSlotsSize = 0
        for ((idx, reg) in registerAllocation.callArguments(call).withIndex()) { // TODO refactor
            if (reg !is Address) {
                continue
            }
            val prototype = call.prototype()
            val byVal = prototype.byValue(idx)
            if (byVal == null) {
                argumentsSlotsSize += POINTER_SIZE
                continue
            }

            val type = prototype.argument(idx) ?: throw CodegenException("argument type is null")
            assertion(type is AggregateType) { "type=$type" }

            argumentsSlotsSize += Definitions.alignTo(type.sizeOf(), QWORD_SIZE)
        }

        return argumentsSlotsSize
    }


    override fun visit(downStackFrame: DownStackFrame) {
        val call = downStackFrame.call()
        val context = getState(call)

        for (arg in context.savedGPRegisters) {
            asm.push(POINTER_SIZE, arg)
        }

        for ((idx, arg) in context.savedXmmRegisters.withIndex()) {
            asm.movf(QWORD_SIZE, arg, Address.from(rsp, -(QWORD_SIZE * idx + QWORD_SIZE)))
        }

        val argumentsSlotsSize = overflowAreaSize(call)

        val size = context.adjustStackSize(argumentsSlotsSize)
        if (size != 0) {
            asm.sub(POINTER_SIZE, Imm32.of(size.toLong()), rsp)
        }
    }

    override fun visit(upStackFrame: UpStackFrame) {
        val call = upStackFrame.call()
        val context = getState(call)

        val argumentsSlotsSize = overflowAreaSize(call)

        val size = context.adjustStackSize(argumentsSlotsSize)
        if (size != 0) {
            asm.add(POINTER_SIZE, Imm32.of(size.toLong()), rsp)
        }

        for ((idx, arg) in context.savedXmmRegisters.reversed().withIndex()) {
            asm.movf(QWORD_SIZE, Address.from(rsp, -(QWORD_SIZE * (context.savedXmmRegisters.size - idx - 1) + QWORD_SIZE)), arg)
        }

        for (arg in context.savedGPRegisters.reversed()) {
            asm.pop(POINTER_SIZE, arg)
        }
    }

    override fun visit(gep: GetElementPtr) {
        val sourceOperand = registerAllocation.operand(gep.source())
        val index         = registerAllocation.operand(gep.index())
        val dest          = registerAllocation.operand(gep)

        val elementSize = gep.index().asType<NonTrivialType>().sizeOf()
        GetElementPtrCodegen(gep.type(), elementSize, gep.basicType, asm)(dest, sourceOperand, index)
    }

    override fun visit(gfp: GetFieldPtr) {
       // TODO(gfp.dump())
        val sourceOperand = registerAllocation.operand(gfp.source())
        val index         = registerAllocation.operand(gfp.index())
        val dest          = registerAllocation.operand(gfp)

        GetFieldPtrCodegen(gfp.type(), gfp.basicType, asm)(dest, sourceOperand, index)
    }

    override fun visit(bitcast: Bitcast) {
        val des = registerAllocation.operand(bitcast)
        val src = registerAllocation.operand(bitcast.value())
        BitcastCodegen(bitcast.type(), asm)(des, src)
    }

    override fun visit(itofp: Int2Float) {
        val dst = registerAllocation.operand(itofp)
        val src = registerAllocation.operand(itofp.value())
        val type = itofp.fromType()
        Int2FloatCodegen(itofp.type(), type, asm)(dst, src)
    }

    override fun visit(utofp: Unsigned2Float) {
        val dst = registerAllocation.operand(utofp)
        val src = registerAllocation.operand(utofp.value())
        val type = utofp.fromType()
        Uint2FloatCodegen(utofp.type(), type, asm)(dst, src)
    }

    override fun visit(zext: ZeroExtend) {
        val dst = registerAllocation.operand(zext)
        val src = registerAllocation.operand(zext.value())
        val toType   = zext.asType<IntegerType>()
        val fromType = zext.value().asType<IntegerType>()
        ZeroExtendCodegen(fromType, toType, asm)(dst, src)
    }

    override fun visit(sext: SignExtend) {
        val dst = registerAllocation.operand(sext)
        val src = registerAllocation.operand(sext.value())
        SignExtendCodegen(sext.value().asType(), sext.type(), asm)(dst, src)
    }

    override fun visit(trunc: Truncate) {
        val dst = registerAllocation.operand(trunc)
        val src = registerAllocation.operand(trunc.value())
        TruncateCodegen(trunc.value().asType(), trunc.type(), asm)(dst, src)
    }

    override fun visit(fptruncate: FpTruncate) {
        val dst = registerAllocation.operand(fptruncate)
        val src = registerAllocation.operand(fptruncate.value())
        FptruncateCodegen(fptruncate.type(), asm)(dst, src)
    }

    override fun visit(fpext: FpExtend) {
        val dst = registerAllocation.operand(fpext)
        val src = registerAllocation.operand(fpext.value())
        FpExtendCodegen(fpext.type(), asm)(dst, src)
    }

    override fun visit(fptosi: Float2Int) {
        val dst = registerAllocation.operand(fptosi)
        val src = registerAllocation.operand(fptosi.value())
        FloatToSignedCodegen(fptosi.type(), fptosi.value().asType(), asm)(dst, src)
    }

    override fun visit(select: Select) {
        val dst     = registerAllocation.operand(select)
        val onTrue  = registerAllocation.operand(select.onTrue())
        val onFalse = registerAllocation.operand(select.onFalse())

        SelectCodegen(select.type(), select.condition() as IntCompare, asm)(dst, onTrue, onFalse)
    }

    override fun visit(phi: Phi) { /* nothing to do */ }

    override fun visit(phi: UncompletedPhi) {
        throw RuntimeException("UncompletedPhi should be handled before code generation")
    }

    override fun visit(alloc: Alloc) {
        throw CodegenException("${Alloc.NAME} should be handled before code generation")
    }

    override fun visit(generate: Generate) { /* nothing to do */ }

    override fun visit(lea: Lea) {
        val dst = registerAllocation.operand(lea)
        val gen = registerAllocation.operand(lea.operand()) as Address

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

            if (!bb.equals(Label.entry)) {
                asm.label(makeLabel(bb))
            }

            bb.instructions { instruction ->
                asm.comment(instruction.dump())
                instruction.accept(this)
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
                if (data.prototype.attributes.contains(GlobalValueAttribute.INTERNAL)) {
                    continue
                }
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

            // Ubuntu requires this section to be present
            unit.section(Section("\".note.GNU-stack\"", "\"\"", SectionType.PROGBITS))

            return unit
        }
    }
}