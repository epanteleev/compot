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
import common.forEachWith
import ir.Definitions.POINTER_SIZE
import ir.Definitions.QWORD_SIZE
import ir.attributes.GlobalValueAttribute
import ir.global.ExternValue
import ir.global.GlobalConstant
import ir.global.GlobalValue
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
import ir.instruction.matching.anytype
import ir.instruction.matching.fVisible
import ir.instruction.matching.gVisible
import ir.module.block.Label
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.pass.CompileContext
import ir.pass.analysis.LivenessAnalysisPassFabric
import ir.pass.analysis.traverse.PreOrderFabric
import ir.platform.common.AnyCodeGenerator
import ir.platform.common.CompiledModule
import ir.platform.x64.codegen.impl.*
import ir.platform.x64.CallConvention.retReg
import ir.platform.x64.pass.analysis.regalloc.LinearScanFabric
import ir.platform.x64.pass.analysis.regalloc.SavedContext
import ir.value.*
import ir.value.constant.*


internal data class CodegenException(override val message: String): Exception(message)

internal class X64CodeGenerator(val module: LModule, private val ctx: CompileContext): AnyCodeGenerator {
    override fun emit(): CompiledModule {
        return CodeEmitter.codegen(module, ctx)
    }
}

private class CodeEmitter(private val data: FunctionData, private val unit: CompilationUnit, private val ctx: CompileContext): IRInstructionVisitor<Unit>() {
    private val registerAllocation by lazy { data.analysis(LinearScanFabric) }
    private val liveness by lazy { data.analysis(LivenessAnalysisPassFabric) }
    private val asm = unit.function(data.prototype.name)
    private var next: Block? = null

    fun next(): Block = next?: throw RuntimeException("next block is null")

    private fun makeLabel(bb: Block) = unit.nameAssistant().newLocalLabel(asm, bb.index)

    private fun operand(value: Value): Operand {
        if (value.type() == UndefType) {
            return CallConvention.temp1
        }

        return operandOrNull(value) ?: throw IllegalArgumentException("cannot find operand for $value")
    }

    private fun operandOrNull(value: Value): Operand? = when (value) {
        is LocalValue -> registerAllocation.vRegOrNull(value)
        is U8Value -> Imm32.of(value.u8.toLong())
        is I8Value -> Imm32.of(value.i8.toLong())
        is U16Value -> Imm32.of(value.u16.toLong())
        is I16Value -> Imm32.of(value.i16.toLong())
        is U32Value -> Imm32.of(value.u32.toInt())
        is I32Value -> Imm32.of(value.i32.toLong())
        is I64Value -> Imm64.of(value.i64)
        is U64Value -> Imm64.of(value.u64.toLong())
        is GlobalConstant -> Address.internal(value.name())
        is FunctionPrototype -> if (ctx.pic() && value.isa(fVisible())) {
            Address.external(value.name())
        } else {
            Address.internal(value.name())
        }
        is ExternFunction -> Address.external(value.name())
        is ExternValue -> Address.external(value.name())
        is GlobalValue -> if (ctx.pic() && value.isa(gVisible())) {
            Address.external(value.name())
        } else {
            Address.internal(value.name())
        }
        is NullValue -> Imm64.of(0)
        else -> null
    }

    private fun vReg(value: LocalValue): VReg {
        return registerAllocation.vRegOrNull(value) ?: throw CodegenException("cannot find vReg for $value")
    }

    private fun callFunction(call: Callable, prototype: DirectFunctionPrototype) {
        val sym = when (prototype) {
            is FunctionPrototype -> if (ctx.pic() && prototype.isa(fVisible())) {
                ExternalFunSymbol(prototype.name())
            } else {
                InternalFunSymbol(prototype.name())
            }
            is ExternFunction -> ExternalFunSymbol(prototype.name())
        }

        asm.callFunction(call, sym)
    }

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
        val first  = operand(add.lhs())
        val second = operand(add.rhs())
        val dst    = vReg(add)

        when (val type = add.type()) {
            is IntegerType       -> AddCodegen(type, asm)(dst, first, second)
            is FloatingPointType -> FAddCodegen(type, asm)(dst, first, second)
            is UndefType         -> {}
        }
    }

    override fun visit(and: And) {
        val first  = operand(and.lhs())
        val second = operand(and.rhs())
        val dst    = vReg(and)

        AndCodegen(and.type(), asm)(dst, first, second)
    }

    override fun visit(or: Or) {
        val first  = operand(or.lhs())
        val second = operand(or.rhs())
        val dst    = vReg(or)

        OrCodegen(or.type(), asm)(dst, first, second)
    }

    override fun visit(xor: Xor) {
        val first  = operand(xor.lhs())
        val second = operand(xor.rhs())
        val dst    = vReg(xor)

        XorCodegen(xor.type(), asm)(dst, first, second)
    }

    override fun visit(fadd: Fxor) {
        val first  = operand(fadd.lhs())
        val second = operand(fadd.rhs())
        val dst    = vReg(fadd)

        FXorCodegen(fadd.type(), asm)(dst, first, second)
    }

    override fun visit(mul: Mul) {
        val first  = operand(mul.lhs())
        val second = operand(mul.rhs())
        val dst    = vReg(mul)

        when (val type = mul.type()) {
            is IntegerType -> IMulCodegen(type, asm)(dst, first, second)
            is FloatingPointType -> FMulCodegen(type, asm)(dst, first, second)
            is UndefType -> {}
        }
    }

    override fun visit(div: Div) {
        val first  = operand(div.lhs())
        val second = operand(div.rhs())
        val dst    = vReg(div)

        FloatDivCodegen(div.type(), asm)(dst, first, second)
    }

    override fun visit(shl: Shl) {
        val first  = operand(shl.lhs())
        val second = operand(shl.rhs())
        val dst    = vReg(shl)

        when (shl.type()) {
            is UnsignedIntType -> ShlCodegen(shl.type(), asm)(dst, first, second)
            is SignedIntType   -> SalCodegen(shl.type(), asm)(dst, first, second)
        }
    }

    override fun visit(shr: Shr) {
        val first  = operand(shr.lhs())
        val second = operand(shr.rhs())
        val dst    = vReg(shr)

        when (shr.type()) {
            is UnsignedIntType -> ShrCodegen(shr.type(), asm)(dst, first, second)
            is SignedIntType   -> SarCodegen(shr.type(), asm)(dst, first, second)
        }
    }

    override fun visit(sub: Sub) {
        val first  = operand(sub.lhs())
        val second = operand(sub.rhs())
        val dst    = vReg(sub)

        when (val type = sub.type()) {
            is IntegerType       -> SubCodegen(type, asm)(dst, first, second)
            is FloatingPointType -> FSubCodegen(type, asm)(dst, first, second)
            is UndefType         -> {}
        }
    }

    private fun emitRetValue(retInstType: PrimitiveType, returnOperand: Operand) = when (retInstType) {
        is IntegerType, is PtrType -> ReturnIntCodegen(retInstType, asm)(retReg, returnOperand)
        is FloatingPointType -> ReturnFloatCodegen(retInstType, asm)(fpRet, returnOperand)
        is UndefType -> TODO("undefined behavior")
    }

    override fun visit(returnValue: ReturnValue) {
        val value = operand(returnValue.returnValue(0))
        when (val returnType = returnValue.type()) {
            is PrimitiveType -> emitRetValue(returnType, value)
            is TupleType -> {
                when (val first = returnType.asInnerType<PrimitiveType>(0)) {
                    is IntegerType, is PtrType -> ReturnIntCodegen(first, asm)(retReg, value)
                    is FloatingPointType -> ReturnFloatCodegen(first, asm)(fpRet, value)
                    is UndefType -> TODO("undefined behavior")
                }

                val value1 = operand(returnValue.returnValue(1))
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
        val operand = operand(neg.operand())
        val result  = vReg(neg)
        NegCodegen(neg.type(), asm)(result, operand)
    }

    override fun visit(not: Not) {
        val operand = operand(not.operand())
        val result  = vReg(not)
        NotCodegen(not.type(), asm)(result, operand)
    }

    override fun visit(voidCall: VoidCall) {
        callFunction(voidCall, voidCall.prototype())

        assertion(voidCall.target() === next()) {
            // This is a bug in the compiler if this assertion fails
            "expected invariant failed: call.target=${voidCall.target()}, next=${next()}"
        }
    }

    private fun consumeTupleCallOutputs(tupleCall: TerminateTupleInstruction) {
        val retType = tupleCall.type()
        val firstProj = tupleCall.proj(0)
        if (firstProj != null) {
            val value = operand(firstProj)
            when (val first  = retType.asInnerType<PrimitiveType>(0)) {
                is IntegerType, is PtrType -> CallIntCodegen(first, asm)(value, retReg)
                is FloatingPointType -> CallFloatCodegen(first, asm)(value, fpRet)
                else -> throw CodegenException("unknown type=$first")
            }
        }

        val secondProj = tupleCall.proj(1)
        if (secondProj != null) {
            val value1 = operand(secondProj)
            when (val second = retType.asInnerType<PrimitiveType>(1)) {
                is IntegerType, is PtrType -> CallIntCodegen(second, asm)(value1, rdx)
                is FloatingPointType -> CallFloatCodegen(second, asm)(value1, XmmRegister.xmm1)
                else -> throw CodegenException("unknown type=$second")
            }
        }
    }

    override fun visit(tupleCall: TupleCall) { //TODO not compatible with linux C calling convention
        callFunction(tupleCall, tupleCall.prototype())
        consumeTupleCallOutputs(tupleCall)
    }

    override fun visit(int2ptr: Int2Pointer) {
        val dst = vReg(int2ptr)
        val src = operand(int2ptr.operand())
        when (val type = int2ptr.operand().asType<IntegerType>()) {
            is SignedIntType -> {
                if (type.sizeOf() == QWORD_SIZE) {
                    CopyIntCodegen(int2ptr.type(), asm)(dst, src)
                } else {
                    SignExtendCodegen(type, I64Type, asm)(dst, src)
                }
            }
            is UnsignedIntType -> CopyIntCodegen(int2ptr.type(), asm)(dst, src)
        }
    }

    override fun visit(ptr2Int: Pointer2Int) {
        val dst = vReg(ptr2Int)
        val src = operand(ptr2Int.operand())
        CopyIntCodegen(ptr2Int.type(), asm)(dst, src)
    }

    override fun visit(memcpy: Memcpy) {
        val dst = vReg(memcpy.destination().asValue())
        val src = vReg(memcpy.source().asValue())
        MemcpyCodegen(memcpy.length(), asm)(dst, src)
    }

    override fun visit(indexedLoad: IndexedLoad) {
        val dst = vReg(indexedLoad)
        val first = operand(indexedLoad.origin())
        val second = operand(indexedLoad.index())

        when (val type = indexedLoad.type()) {
            is FloatingPointType -> IndexedFloatLoadCodegen(type, indexedLoad.index().asType(), asm)(dst, first, second)
            is IntegerType, is PtrType -> {
                IndexedIntLoadCodegen(indexedLoad.type(), indexedLoad.index().asType(), asm)(dst, first, second)
            }
            is UndefType -> TODO("undefined behavior")
        }
    }

    override fun visit(store: StoreOnStack) {
        val pointerOperand = operand(store.destination())
        val value = operand(store.source())
        val index = operand(store.index())

        StoreOnStackCodegen(store.source().type().asType(), store.index().asType(), asm)(pointerOperand, value, index)
    }

    override fun visit(loadst: LoadFromStack) {
        val origin = operand(loadst.origin())
        val index  = operand(loadst.index())
        val dst    = vReg(loadst)

        LoadFromStackCodegen(loadst.type(), loadst.index().asType(), asm)(dst, origin, index)
    }

    override fun visit(leaStack: LeaStack) {
        val sourceOperand = operand(leaStack.origin())
        val index         = operand(leaStack.index())
        val dest          = vReg(leaStack)

        LeaStackCodegen(PtrType, leaStack.loadedType, asm)(dest, sourceOperand, index)
    }

    override fun visit(tupleDiv: TupleDiv) {
        val divType = tupleDiv.type()

        val first  = operand(tupleDiv.lhs())
        val second = operand(tupleDiv.rhs())
        val quotient = operand(tupleDiv.quotient())

        val remainderOperand = tupleDiv.remainder()
        assertion(operand(remainderOperand) == rdx) {
            "remainderOperand=${operand(remainderOperand)}, rdx=$rdx"
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
        // TODO: implement switch tables
        // TODO: implement switch-ranges approach
        val type = switch.value().asType<IntegerType>()
        val switchValue = operand(switch.value())
        switch.jumps().forEachWith(switch.table()) { target, value ->
            IntCmpCodegen(type, asm)(switchValue, Imm64.of(value.toInt()))
            asm.jcc(CondFlagType.EQ, makeLabel(target))
        }

        doJump(switch.default())
    }

    override fun visit(tupleCall: IndirectionTupleCall) {
        val pointer = operand(tupleCall.pointer())
        asm.indirectCall(tupleCall, pointer)
        consumeTupleCallOutputs(tupleCall)
    }

    override fun visit(intrinsic: Intrinsic) {
        val values = intrinsic.operands().map { vReg(it.asValue()) }
        intrinsic.implementor.implement(asm, values)
    }

    override fun visit(call: Call) {
        callFunction(call, call.prototype())

        val callOp = operand(call)
        when (val retType = call.type()) {
            is IntegerType, is PtrType -> CallIntCodegen(retType, asm)(callOp, retReg)
            is FloatingPointType -> CallFloatCodegen(retType, asm)(callOp, fpRet)
            is UndefType -> println("UB in call") //TODO remove this
        }

        assertion(call.target() === next()) {
            // This is a bug in the compiler if this assertion fails
            "expected invariant failed: call.target=${call.target()}, next=${next()}"
        }
    }

    override fun visit(flag2Int: Flag2Int) {
        val dst = vReg(flag2Int)
        val size = flag2Int.type().sizeOf()
        when (val compare = flag2Int.operand()) {
            is CompareInstruction -> Flag2IntCodegen(size, compare.operandsType(), compare.predicate(), asm)(dst, dst)
            is BoolValue -> {
                val res = when (compare) {
                    is TrueBoolValue  -> 1L
                    is FalseBoolValue -> 0L
                }
                when (dst) {
                    is Address    -> asm.mov(size, Imm32.of(res), dst)
                    is GPRegister -> asm.mov(size, Imm64.of(res), dst)
                    else -> throw CodegenException("unknown dst=$dst")
                }
            }
            else -> throw CodegenException("unknown compare=$compare")
        }
    }

    override fun visit(indirectionCall: IndirectionCall) {
        val pointer = operand(indirectionCall.pointer())
        asm.indirectCall(indirectionCall, pointer)

        val callOp = operand(indirectionCall)
        when (val retType = indirectionCall.type()) {
            is IntegerType, is PtrType -> CallIntCodegen(retType, asm)(callOp, retReg)
            is FloatingPointType -> CallFloatCodegen(retType, asm)(callOp, fpRet)
            is UndefType -> TODO("undefined behavior")
        }
    }

    override fun visit(indirectionVoidCall: IndirectionVoidCall) {
        val pointer = operand(indirectionVoidCall.pointer())
        asm.indirectCall(indirectionVoidCall, pointer)
    }

    override fun visit(store: Store) {
        val pointerOperand = operand(store.pointer())
        val value          = operand(store.value())
        when (val type = store.valueType()) {
            is FloatingPointType -> StoreFloatCodegen(type, asm)(value, pointerOperand)
            is IntegerType, is PtrType -> StoreIntCodegen(type, asm)(value, pointerOperand)
            is UndefType -> TODO("undefined behavior")
        }
    }

    override fun visit(load: Load) {
        val pointer = operand(load.operand())
        val value   = vReg(load)
        when (val type = load.type()) {
            is FloatingPointType -> LoadFloatCodegen(type, asm)(value, pointer)
            is IntegerType, is PtrType -> LoadIntCodegen(type, asm)(value, pointer)
            is UndefType -> TODO("undefined behavior")
        }
    }

    override fun visit(icmp: IntCompare) {
        val first  = operand(icmp.lhs())
        val second = operand(icmp.rhs())
        assertion(registerAllocation.vRegOrNull(icmp) == null) {
            "wasting register in icmp=${icmp}, operand=${registerAllocation.vRegOrNull(icmp)}"
        }

        IntCmpCodegen(icmp.lhs().asType(), asm)(first, second)
    }

    override fun visit(fcmp: FloatCompare) {
        val first  = operand(fcmp.lhs())
        val second = operand(fcmp.rhs())
        assertion(registerAllocation.vRegOrNull(fcmp) == null) {
            "wasting register in icmp=${fcmp}, operand=${registerAllocation.vRegOrNull(fcmp)}"
        }

        FloatCmpCodegen(fcmp.lhs().asType(), asm)(first, second)
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

    override fun visit(branchCond: BranchCond) = when (val cond = branchCond.condition()) {
        is BoolValue -> {
            // Condition is constant - true or false.
            // Just select necessary branch and jump to it.
            when (cond) {
                is TrueBoolValue  -> doJump(branchCond.onTrue())
                is FalseBoolValue -> doJump(branchCond.onFalse())
            }
        }
        is IntCompare -> {
            val jmpType = asm.condIntType0(cond.predicate().invert(), cond.operandsType())
            asm.jcc(jmpType, makeLabel(branchCond.onFalse()))
        }
        is FloatCompare -> when (cond.predicate()) {
            FloatPredicate.One, FloatPredicate.Une -> {
                val onTrue = makeLabel(branchCond.onTrue())
                val jmpType = asm.condFloatType0(cond.predicate())
                asm.jcc(jmpType, onTrue)
                asm.jcc(CondFlagType.P, onTrue)
                doJump(branchCond.onFalse())
            }
            else -> {
                val onFalse = makeLabel(branchCond.onFalse())
                val jmpType = asm.condFloatType0(cond.predicate().invert())
                asm.jcc(jmpType, onFalse)
                asm.jcc(CondFlagType.P, onFalse)
            }
        }
        else -> throw CodegenException("unknown condition type, cond=${cond}")
    }

    override fun visit(copy: Copy) {
        if (copy.operand() is UndefValue) {
            // Do nothing. UB is UB
            return
        }
        val result  = vReg(copy)
        val operand = operand(copy.operand())
        when (val type = copy.type()) {
            is IntegerType, is PtrType -> CopyIntCodegen(type, asm)(result, operand)
            is FloatingPointType -> CopyFloatCodegen(type, asm)(result, operand)
            is UndefType -> TODO("undefined behavior")
        }
    }

    override fun visit(move: Move) {
        val source      = operand(move.source())
        val destination = operand(move.destination())

        when (val type = move.source().asType<PrimitiveType>()) {
            is IntegerType, is PtrType -> MoveIntCodegen(type, asm)(destination, source)
            is FloatingPointType -> MoveFloatCodegen(type, asm)(destination, source)
            is UndefType -> TODO("undefined behavior")
        }
    }

    override fun visit(move: MoveByIndex) {
        val index = operand(move.index())
        val destination = operand(move.destination())
        val srcOperand = operand(move.source())

        when (val sourceType = move.source().asType<PrimitiveType>()) {
            is FloatingPointType -> MoveFloatByIndexCodegen(sourceType, move.index().asType(), asm)(destination, srcOperand, index)
            is IntegerType, is PtrType -> {
                val indexType = move.index().asType<PrimitiveType>()
                MoveIntByIndexCodegen(sourceType, indexType, asm)(destination, srcOperand, index)
            }
            is UndefType -> TODO("undefined behavior")
        }
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

    override fun visit(downStackFrame: DownStackFrame) {
        val call = downStackFrame.call()
        val context = getState(call)

        for (arg in context.savedGPRegisters) {
            asm.push(POINTER_SIZE, arg)
        }

        for ((idx, arg) in context.savedXmmRegisters.withIndex()) {
            asm.movf(QWORD_SIZE, arg, Address.from(rsp, -(QWORD_SIZE * idx + QWORD_SIZE)))
        }

        val argumentsSlotsSize = registerAllocation.overflowAreaSize(call)
        val size = context.adjustStackSize(argumentsSlotsSize)
        if (size != 0) {
            asm.sub(POINTER_SIZE, Imm32.of(size.toLong()), rsp)
        }
    }

    override fun visit(upStackFrame: UpStackFrame) {
        val call = upStackFrame.call()
        val context = getState(call)

        val argumentsSlotsSize = registerAllocation.overflowAreaSize(call)
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
        val sourceOperand = operand(gep.source())
        val index         = operand(gep.index())
        val dest          = vReg(gep)

        val elementSize = gep.index().asType<NonTrivialType>().sizeOf()
        GetElementPtrCodegen(gep.type(), elementSize, gep.basicType, asm)(dest, sourceOperand, index)
    }

    override fun visit(gfp: GetFieldPtr) {
       // TODO(gfp.dump())
        val sourceOperand = operand(gfp.source())
        val index         = operand(gfp.index())
        val dest          = vReg(gfp)

        GetFieldPtrCodegen(gfp.type(), gfp.basicType, asm)(dest, sourceOperand, index)
    }

    override fun visit(bitcast: Bitcast) {
        val des = operand(bitcast)
        val src = operand(bitcast.operand())
        BitcastCodegen(bitcast.type(), asm)(des, src)
    }

    override fun visit(itofp: Int2Float) {
        val dst = vReg(itofp)
        val src = operand(itofp.operand())
        Int2FloatCodegen(itofp.type(), itofp.fromType(), asm)(dst, src)
    }

    override fun visit(utofp: Unsigned2Float) {
        val dst = vReg(utofp)
        val src = operand(utofp.operand())
        Uint2FloatCodegen(utofp.type(), utofp.fromType(), asm)(dst, src)
    }

    override fun visit(zext: ZeroExtend) {
        val dst = vReg(zext)
        val src = operand(zext.operand())
        val toType   = zext.asType<IntegerType>()
        val fromType = zext.operand().asType<IntegerType>()
        ZeroExtendCodegen(fromType, toType, asm)(dst, src)
    }

    override fun visit(sext: SignExtend) {
        val dst = vReg(sext)
        val src = operand(sext.operand())
        SignExtendCodegen(sext.operand().asType(), sext.type(), asm)(dst, src)
    }

    override fun visit(trunc: Truncate) {
        val dst = vReg(trunc)
        val src = operand(trunc.operand())
        TruncateCodegen(trunc.operand().asType(), trunc.type(), asm)(dst, src)
    }

    override fun visit(fptruncate: FpTruncate) {
        val dst = vReg(fptruncate)
        val src = operand(fptruncate.operand())
        FptruncateCodegen(fptruncate.type(), asm)(dst, src)
    }

    override fun visit(fpext: FpExtend) {
        val dst = vReg(fpext)
        val src = operand(fpext.operand())
        FpExtendCodegen(fpext.type(), asm)(dst, src)
    }

    override fun visit(fptosi: Float2Int) {
        val dst = vReg(fptosi)
        val src = operand(fptosi.value())
        FloatToSignedCodegen(fptosi.type(), fptosi.value().asType(), asm)(dst, src)
    }

    override fun visit(select: Select) {
        val dst     = vReg(select)
        val onTrue  = operand(select.onTrue())
        val onFalse = operand(select.onFalse())

        when (val cond = select.condition()) {
            is CompareInstruction -> SelectCodegen(select.type(), cond, asm)(dst, onTrue, onFalse)
            is BoolValue -> when (cond) {
                is TrueBoolValue  -> CopyIntCodegen(select.type(), asm)(dst, onTrue)
                is FalseBoolValue -> CopyIntCodegen(select.type(), asm)(dst, onFalse)
            }
            else -> throw CodegenException("unknown condition type, cond=${cond}")
        }
    }

    override fun visit(phi: Phi) { /* nothing to do */ }

    override fun visit(phi: UncompletedPhi) {
        throw RuntimeException("${UncompletedPhi.NAME} should be handled before code generation")
    }

    override fun visit(alloc: Alloc) {
        throw CodegenException("${Alloc.NAME} should be handled before code generation")
    }

    override fun visit(generate: Generate) { /* nothing to do */ }

    override fun visit(lea: Lea) {
        val gen = operand(lea.operand()) as Address
        when (val dst = vReg(lea)) {
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

        fun codegen(module: LModule, ctx: CompileContext): CompilationUnit {
            val unit = CompilationUnit()
            for (data in module.functions()) {
                if (data.prototype.attributes.contains(GlobalValueAttribute.INTERNAL)) {
                    continue
                }

                unit.global(data.prototype.name)
            }

            //.data
            unit.section(DataSection)
            for (c in module.constantPool.values) {
                unit.mkConstant(c)
            }
            for (global in module.globals.values) {
                unit.makeGlobal(global)
            }

            //.text
            unit.section(TextSection)
            for (data in module.functions()) {
                CodeEmitter(data, unit, ctx).emit()
            }

            // Ubuntu requires this section to be present
            unit.section(Section("\".note.GNU-stack\"", "\"\"", SectionType.PROGBITS))
            return unit
        }
    }
}