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
import common.identityHashMapOf
import ir.global.GlobalConstant
import ir.instruction.lir.*
import ir.instruction.lir.Lea
import ir.module.block.Label
import ir.utils.OrderedLocation
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

    private var aboveNeighbour: Instruction? = null
    private var belowNeighbour: Instruction? = null

    private inline fun<reified T: Instruction> above(): Instruction {
        if (aboveNeighbour is T) {
            throw RuntimeException("above neighbour is not ${T::class.simpleName}")
        }

        return aboveNeighbour!!
    }

    private inline fun<reified T: Instruction> below(): Instruction {
        if (belowNeighbour is T) {
            throw RuntimeException("below neighbour is not ${T::class.simpleName}, but ${belowNeighbour!!::class.simpleName}")
        }

        return belowNeighbour!!
    }

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
            ArithmeticBinaryOp.Div -> DivCodegen(binary.type(), asm)(dst, first, second)
            else -> println("Unimplemented: ${binary.op}")
        }
    }

    override fun visit(returnValue: ReturnValue) {
        val returnType = data.prototype.returnType()
        val retInstType = returnValue.type()
        val size = retInstType.size()

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

    override fun visit(instruction: LoadFromStack) {
        val origin = valueToRegister.operand(instruction.origin())
        val index = valueToRegister.operand(instruction.index())
        val dst = valueToRegister.operand(instruction)

        LoadFromStackCodegen(instruction.type(), asm)(dst, origin, index)
    }

    override fun visit(lea: LeaStack) {
        val sourceOperand = valueToRegister.operand(lea.origin())
        val index         = valueToRegister.operand(lea.index())
        val dest          = valueToRegister.operand(lea)

        GetElementPtrCodegenForAlloc(Type.Ptr, lea.type(), asm)(dest, sourceOperand, index)
    }

    override fun visit(call: Call) {
        emitCall(call)
    }

    override fun visit(flag2Int: Flag2Int) {
        val dst = valueToRegister.operand(flag2Int)
        val src = valueToRegister.operand(flag2Int.value())
        val compare = flag2Int.value() as CompareInstruction

        val isNeighbour = aboveNeighbour != null && aboveNeighbour == flag2Int.value()
        if (isNeighbour) {
            setcc(compare.predicate(), dst)
        }
        Flag2IntCodegen(flag2Int.type().size(), asm)(dst, src)
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
        val size = icmp.first().type().size()

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
        return users.size > 1 || users.first() != belowNeighbour
    }

    override fun visit(pcmp: PointerCompare) { //TODO
        var first  = valueToRegister.operand(pcmp.first())
        val second = valueToRegister.operand(pcmp.second())
        val dst    = valueToRegister.operand(pcmp)
        val size = pcmp.first().type().size()

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
        val size = ucmp.first().type().size()

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
        val size = fcmp.first().type().size()

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

    override fun visit(branch: Branch) {
        asm.jump(makeLabel(branch.target()))
    }

    override fun visit(branchCond: BranchCond) {
        val cond = branchCond.condition()
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
        MoveByIndexCodegen(type, asm)(destination, source, index)
    }

    override fun visit(downStackFrame: DownStackFrame) {
        val sdf = orderedLocation[downStackFrame.call()]
        val context = valueToRegister.callerSaveRegisters(sdf!!)
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
        val usf = orderedLocation[upStackFrame.call()]!!
        val context = valueToRegister.callerSaveRegisters(usf)

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
            var idx = 0
            while (idx < instructions.size) {
                val instruction = instructions[idx]
                aboveNeighbour = if (idx == 0) null else instructions[idx - 1]
                belowNeighbour = if (idx == instructions.size - 1) null else instructions[idx + 1]
                asm.comment(instruction.dump())
                instruction.visit(this)
                idx += 1
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