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
import ir.BoolValue
import ir.LocalValue
import ir.UndefinedValue
import ir.asType
import ir.global.GlobalConstant
import ir.instruction.lir.*
import ir.instruction.lir.Lea
import ir.module.block.Label
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.platform.common.AnyCodeGenerator
import ir.platform.common.CompiledModule
import ir.platform.x64.codegen.impl.*
import ir.platform.x64.regalloc.RegisterAllocation
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.CallConvention.POINTER_SIZE
import ir.platform.x64.CallConvention.DOUBLE_SUB_ZERO_SYMBOL
import ir.platform.x64.CallConvention.FLOAT_SUB_ZERO_SYMBOL
import ir.platform.x64.CallConvention.retReg
import ir.platform.x64.regalloc.SavedContext


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
    private var previous: Block? = null
    private var next: Block? = null

    fun next(): Block = next?: throw RuntimeException("next block is null")
    fun previous(): Block = previous?: throw RuntimeException("previous block is null")

    private fun makeLabel(bb: Block) = ".L$functionCounter.${bb.index}"


    private fun setccFLoat(jmpType: FloatPredicate, dst: Operand) {
        when (jmpType) {
            FloatPredicate.Ult -> {
                when (dst) {
                    is Address    -> asm.setcc(8, SetCCType.SETB, dst)
                    is GPRegister -> asm.setcc(8, SetCCType.SETB, dst)
                    else -> throw CodegenException("unknown jmpType=$jmpType")
                }
            }
            else -> throw CodegenException("unknown jmpType=$jmpType")
        }
    }

    private fun setccInt(jmpType: IntPredicate, dst: Operand) {
        when (jmpType) {
            IntPredicate.Eq -> {
                when (dst) {
                    is Address    -> asm.setcc(8, SetCCType.SETE, dst)
                    is GPRegister -> asm.setcc(8, SetCCType.SETE, dst)
                    else -> throw CodegenException("unknown jmpType=$jmpType")
                }
            }
            IntPredicate.Ne -> {
                when (dst) {
                    is Address    -> asm.setcc(8, SetCCType.SETNE, dst)
                    is GPRegister -> asm.setcc(8, SetCCType.SETNE, dst)
                    else -> throw CodegenException("unknown jmpType=$jmpType")
                }
            }
            IntPredicate.Gt -> {
                when (dst) {
                    is Address    -> asm.setcc(8, SetCCType.SETG, dst)
                    is GPRegister -> asm.setcc(8, SetCCType.SETG, dst)
                    else -> throw CodegenException("unknown jmpType=$jmpType")
                }
            }
            IntPredicate.Ge -> {
                when (dst) {
                    is Address    -> asm.setcc(8, SetCCType.SETGE, dst)
                    is GPRegister -> asm.setcc(8, SetCCType.SETGE, dst)
                    else -> throw CodegenException("unknown jmpType=$jmpType")
                }
            }
            IntPredicate.Lt -> {
                when (dst) {
                    is Address    -> asm.setcc(8, SetCCType.SETL, dst)
                    is GPRegister -> asm.setcc(8, SetCCType.SETL, dst)
                    else -> throw CodegenException("unknown jmpType=$jmpType")
                }
            }
            IntPredicate.Le -> {
                when (dst) {
                    is Address    -> asm.setcc(8, SetCCType.SETLE, dst)
                    is GPRegister -> asm.setcc(8, SetCCType.SETLE, dst)
                    else -> throw CodegenException("unknown jmpType=$jmpType")
                }
            }
        }
    }

    fun setcc(jmpType: AnyPredicateType, dst: Operand) {
        when (jmpType) {
            is IntPredicate   -> setccInt(jmpType, dst)
            is FloatPredicate -> setccFLoat(jmpType, dst)
            else -> throw CodegenException("unknown jmpType=$jmpType")
        }
    }

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
            // Floating point division ignores the second operand.
            ArithmeticBinaryOp.Div -> {
                asm.push(POINTER_SIZE, rdx) //TODO pessimistic spill rdx
                DivCodegen(binary.type(), rdx, asm)(dst, first, second)
                asm.pop(POINTER_SIZE, rdx)
            }
            else -> println("Unimplemented: ${binary.op}")
        }
    }

    override fun visit(returnValue: ReturnValue) {
        val returnType = data.prototype.returnType()
        val retInstType = returnValue.type()
        val size = retInstType.size()

        val value = valueToRegister.operand(returnValue.value())
        if (returnType is IntegerType || returnType is PointerType) {
            asm.movOld(size, value, retReg)
        } else if (returnType is FloatingPointType) {
            when (value) {
                is Address     -> asm.movf(size, value, fpRet)
                is XmmRegister -> asm.movf(size, value, fpRet)
                else -> throw CodegenException("unknown value=$value")
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

    private fun emitCall(call: Callable) {
        call as TerminateInstruction
        asm.call(call.prototype().name)

        val retType = call.type()
        if (retType == Type.Void) {
            return
        }

        emitReturnValue(call)
        assert(call.target() === next()) {
            // This is a bug in the compiler if this assertion fails
            "expected invariant failed: call.target=${call.target()}, next=${next()}"
        }
    }

    override fun visit(voidCall: VoidCall) {
        emitCall(voidCall)
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

    override fun visit(copy: IndexedLoad) {
        val dst = valueToRegister.operand(copy)
        val first = valueToRegister.operand(copy.origin())
        val second = valueToRegister.operand(copy.index())
        IndexedLoadCodegen(copy.type(), asm)(dst, first, second)
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

        LoadFromStackCodegen(loadst.type(), asm)(dst, origin, index)
    }

    override fun visit(leaStack: LeaStack) {
        val sourceOperand = valueToRegister.operand(leaStack.origin())
        val index         = valueToRegister.operand(leaStack.index())
        val dest          = valueToRegister.operand(leaStack)

        GetElementPtrCodegenForAlloc(Type.Ptr, leaStack.loadedType, asm)(dest, sourceOperand, index)
    }

    override fun visit(binary: TupleDiv) {
        val first  = valueToRegister.operand(binary.first())
        val second = valueToRegister.operand(binary.second())

        val quotientOperand = run {
            val quotient = binary.quotient()
            if (quotient != null) {
                valueToRegister.operand(quotient)
            } else {
                rax
            }
        }

        val remainderOperand = run {
            val remainder = binary.remainder()
            if (remainder != null) {
                valueToRegister.operand(remainder)
            } else {
                rdx
            }
        }

        asm.push(POINTER_SIZE, rdx) //TODO pessimistic spill rdx
        DivCodegen(binary.type().innerType(1) as ArithmeticType, rdx, asm)(quotientOperand, first, second)
        asm.pop(POINTER_SIZE, rdx)
    }

    override fun visit(proj: Projection) {
        // Skip. Projection must be handled by its user
    }

    override fun visit(call: Call) {
        emitCall(call)
    }

    override fun visit(flag2Int: Flag2Int) {
        val dst = valueToRegister.operand(flag2Int)
        val src = valueToRegister.operand(flag2Int.value())
        val compare = flag2Int.value() as CompareInstruction

        val isNeighbour = flag2Int.prev() != null && flag2Int.prev() == flag2Int.value()
        if (isNeighbour) {
            setcc(compare.predicate(), dst)
        }
        Flag2IntCodegen(flag2Int.type().size(), asm)(dst, src)
    }

    private fun emitReturnValue(call: Callable) {
        val retType = call.type()
        if (retType == Type.Void) {
            return
        }

        call as TerminateValueInstruction
        when (retType) {
            is IntegerType, is PointerType, is BooleanType -> {
                retType as NonTrivialType
                val size = retType.size()

                asm.movOld(size, retReg, valueToRegister.operand(call))
            }

            is FloatingPointType -> {
                val size = retType.size()
                when (val op = valueToRegister.operand(call)) {
                    is Address     -> asm.movf(size, fpRet, op)
                    is XmmRegister -> asm.movf(size, fpRet, op)
                    else -> throw CodegenException("unknown value type=$op")
                }
            }

            else -> {
                throw RuntimeException("unknown value type=$retType")
            }
        }
    }

    private fun indirectCall(pointer: Operand) {
        when (pointer) {
            is GPRegister -> asm.call(pointer)
            is Address    -> asm.call(pointer)
            else -> throw CodegenException("invalid operand: pointer=$pointer")
        }
    }

    override fun visit(indirectionCall: IndirectionCall) {
        val pointer = valueToRegister.operand(indirectionCall.pointer())
        indirectCall(pointer)
        emitReturnValue(indirectionCall)
    }

    override fun visit(indirectionVoidCall: IndirectionVoidCall) {
        val pointer = valueToRegister.operand(indirectionVoidCall.pointer())
        indirectCall(pointer)
    }

    override fun visit(store: Store) {
        val pointer = store.pointer()

        val pointerOperand = valueToRegister.operand(pointer)
        val value          = valueToRegister.operand(store.value())
        val type = store.value().type()

        StoreCodegen(type as PrimitiveType, asm)(value, pointerOperand)
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
        val dst    = valueToRegister.operand(icmp)
        val size = icmp.first().asType<NonTrivialType>().size()

        first = if (first is Address2 || first is ImmInt) { //TODO???
            asm.movOld(size, first, temp1)
        } else {
            first
        }

        asm.cmp(size, second, first as GPRegister)
        if (needSetcc(icmp)) {
            setcc(icmp.predicate(), dst)
        }
    }

    private fun needSetcc(cmp: CompareInstruction): Boolean {
        val users = cmp.usedIn()
        return users.size > 1 || users.first() != cmp.next()
    }

    override fun visit(pcmp: PointerCompare) { //TODO
        var first  = valueToRegister.operand(pcmp.first())
        val second = valueToRegister.operand(pcmp.second())
        val dst    = valueToRegister.operand(pcmp)
        val size = pcmp.first().asType<NonTrivialType>().size()

        first = if (first is Address2) {
            asm.movOld(size, first, temp1)
        } else {
            first
        }

        asm.cmp(size, second, first as GPRegister)
        if (needSetcc(pcmp)) {
            setcc(pcmp.predicate(), dst)
        }
    }

    override fun visit(ucmp: UnsignedIntCompare) { //TODO
        var first  = valueToRegister.operand(ucmp.first())
        val second = valueToRegister.operand(ucmp.second())
        val dst    = valueToRegister.operand(ucmp)
        val size = ucmp.first().asType<NonTrivialType>().size()

        first = if (first is Address2) {
            asm.movOld(size, first, temp1)
        } else if (first is ImmInt) {
            asm.movOld(size, first, temp1)
        } else {
            first
        }

        asm.cmp(size, second, first as GPRegister)
        if (needSetcc(ucmp)) {
            setcc(ucmp.predicate(), dst)
        }
    }

    override fun visit(fcmp: FloatCompare) {
        val first  = valueToRegister.operand(fcmp.first())
        val second = valueToRegister.operand(fcmp.second())
        val dst    = valueToRegister.operand(fcmp)
        val size = fcmp.first().asType<NonTrivialType>().size()

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
        if (needSetcc(fcmp)) {
            setcc(fcmp.predicate(), dst)
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
        val cond = branchCond.condition()
        if (cond is BoolValue) {
            if (cond.bool) {
                doJump(branchCond.onTrue())
            } else {
                doJump(branchCond.onFalse())
            }
            return
        }

        cond as CompareInstruction
        val jmpType = when (cond) {
            is SignedIntCompare -> {
                when (val convType = cond.predicate().invert()) {
                    IntPredicate.Eq -> CondType.JE
                    IntPredicate.Ne -> CondType.JNE
                    IntPredicate.Gt -> CondType.JG
                    IntPredicate.Ge -> CondType.JGE
                    IntPredicate.Lt -> CondType.JL
                    IntPredicate.Le -> CondType.JLE
                    else -> throw CodegenException("unknown conversion type: convType=$convType")
                }
            }
            is UnsignedIntCompare, is PointerCompare -> {
                when (val convType = cond.predicate().invert()) {
                    IntPredicate.Eq -> CondType.JE
                    IntPredicate.Ne -> CondType.JNE
                    IntPredicate.Gt -> CondType.JA
                    IntPredicate.Ge -> CondType.JAE
                    IntPredicate.Lt -> CondType.JB
                    IntPredicate.Le -> CondType.JBE
                    else -> throw CodegenException("unknown conversion type: convType=$convType")
                }
            }
            is FloatCompare -> {
                when (val convType = cond.predicate().invert()) {
                    FloatPredicate.Oeq -> CondType.JE // TODO Clang insert extra instruction 'jp ${labelName}"
                    FloatPredicate.Ogt -> TODO()
                    FloatPredicate.Oge -> CondType.JAE
                    FloatPredicate.Olt -> TODO()
                    FloatPredicate.Ole -> CondType.JBE
                    FloatPredicate.One -> CondType.JNE // TODO Clang insert extra instruction 'jp ${labelName}"
                    FloatPredicate.Ord -> TODO()
                    FloatPredicate.Ueq -> TODO()
                    FloatPredicate.Ugt -> TODO()
                    FloatPredicate.Uge -> TODO()
                    FloatPredicate.Ult -> TODO()
                    FloatPredicate.Ule -> CondType.JBE
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

        val type = move.source().type() as PrimitiveType
        MoveCodegen(type, asm)(destination, source)
    }

    override fun visit(move: MoveByIndex) {
        val source      = valueToRegister.operand(move.source())
        val destination = valueToRegister.operand(move.destination())

        val type = move.source().type() as PrimitiveType
        val movIdx = move.index()
        val index = valueToRegister.operand(movIdx)
        MoveByIndexCodegen(type, movIdx.asType(), asm)(destination, source, index)
    }

    fun getState(call: Callable): SavedContext {
        // Any callable instruction is TerminateInstruction
        // so that we can easily get the caller save registers
        // from the live-out of the block
        call as Instruction
        val liveOut = valueToRegister.liveness.liveOut(call.owner())
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
            asm.movf(8, arg, Address.from(rsp, -(8 * idx + 8))) //TODO 16???
        }

        val size = context.adjustStackSize()
        if (size != 0) {
            asm.sub(POINTER_SIZE, Imm32(size.toLong()), rsp)
        }
    }

    override fun visit(upStackFrame: UpStackFrame) {
        val call = upStackFrame.call()
        val context = getState(call)

        val size = context.adjustStackSize()
        if (size != 0) {
            asm.add(POINTER_SIZE, Imm32(size.toLong()), rsp)
        }

        for ((idx, arg) in context.savedXmmRegisters.reversed().withIndex()) {
            asm.movf(8, Address.from(rsp, -(8 * (context.savedXmmRegisters.size - idx - 1) + 8)), arg) //TODO 16???
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
            GetElementPtrCodegen(gep.type(), gep.index().asType<NonTrivialType>().size(), gep.basicType, asm)(dest, sourceOperand, index)
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

    override fun visit(itofp: Int2Float) {
        val dst = valueToRegister.operand(itofp)
        val src = valueToRegister.operand(itofp.value())
        Int2FloatCodegen(itofp.type(), itofp.value().type() as IntegerType, asm)(dst, src)
    }

    override fun visit(zext: ZeroExtend) {
        val dst = valueToRegister.operand(zext)
        val src = valueToRegister.operand(zext.value())
        ZeroExtendCodegen(zext.value().type() as IntegerType, asm)(dst, src)
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

    override fun visit(fptosi: FloatToInt) {
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
        val order = data.blocks.preorder().order()

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
            val asm = CompilationUnit()

            for (c in module.globals) {
                if (c !is GlobalConstant) {
                    continue
                }

                asm.mkConstant(c)
            }

            for ((idx, data) in module.functions().withIndex()) {
                CodeEmitter(data, idx, asm, module.regAlloc(data)).emit()
            }

            return asm
        }
    }
}