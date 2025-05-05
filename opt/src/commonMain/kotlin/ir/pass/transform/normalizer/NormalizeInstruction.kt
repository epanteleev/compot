package ir.pass.transform.normalizer

import ir.instruction.*
import ir.instruction.lir.*
import ir.instruction.utils.IRInstructionVisitor
import ir.types.SignedIntType
import ir.types.UnsignedIntType
import ir.value.LocalValue
import ir.value.TupleValue
import ir.value.Value
import ir.value.asType
import ir.value.asValue
import ir.value.constant.*


internal object NormalizeInstruction: IRInstructionVisitor<Value>() {
    fun normalize(inst: LocalValue): Value {
        if (inst !is Instruction) {
            throw IllegalArgumentException("Instruction expected, but got ${inst::class.simpleName}")
        }

        return inst.accept(this)
    }

    private fun isConstexpr(instruction: Instruction): Boolean {
        return instruction.operands().all { it is Constant }
    }

    override fun visit(alloc: Alloc): Value = alloc

    override fun visit(generate: Generate): Value = generate

    override fun visit(lea: Lea): Value = lea

    private fun constExprAdd(add: Add): Value {
        val rhs = add.rhs()
        return when (val lhs = add.lhs()) {
            is UnsignedIntegerConstant -> lhs + rhs.asValue()
            is SignedIntegerConstant -> lhs + rhs.asValue()
            else -> add
        }
    }

    private fun constExprSub(sub: Sub): Value {
        val rhs = sub.rhs()
        return when (val lhs = sub.lhs()) {
            is UnsignedIntegerConstant -> lhs - rhs.asValue()
            is SignedIntegerConstant -> lhs - rhs.asValue()
            else -> sub
        }
    }

    private fun constExprMul(mul: Mul): Value {
        val rhs = mul.rhs()
        return when (val lhs = mul.lhs()) {
            is UnsignedIntegerConstant -> lhs * rhs.asValue()
            is SignedIntegerConstant -> lhs * rhs.asValue()
            else -> mul
        }
    }

    private fun constExprTupleDiv(div: TupleDiv): Value {
        val rhs = div.rhs()
        return when (val lhs = div.lhs()) {
            is UnsignedIntegerConstant -> TupleConstant.of(lhs / rhs.asValue(), lhs % rhs.asValue())
            is SignedIntegerConstant -> TupleConstant.of(lhs / rhs.asValue(), lhs % rhs.asValue())
            else -> div
        }
    }

    private fun constExprOr(or: Or): Value {
        val rhs = or.rhs()
        return when (val lhs = or.lhs()) {
            is UnsignedIntegerConstant -> lhs or rhs.asValue()
            is SignedIntegerConstant -> lhs or rhs.asValue()
            else -> or
        }
    }

    private fun constExprAnd(and: And): Value {
        val rhs = and.rhs()
        return when (val lhs = and.lhs()) {
            is UnsignedIntegerConstant -> lhs and rhs.asValue()
            is SignedIntegerConstant -> lhs and rhs.asValue()
            else -> and
        }
    }

    private fun constExprXor(xor: Xor): Value {
        val rhs = xor.rhs()
        return when (val lhs = xor.lhs()) {
            is UnsignedIntegerConstant -> lhs xor rhs.asValue()
            is SignedIntegerConstant -> lhs xor rhs.asValue()
            else -> xor
        }
    }

    private fun constExprShl(shl: Shl): Value {
        val rhs = shl.rhs()
        return when (val lhs = shl.lhs()) {
            is UnsignedIntegerConstant -> lhs shl rhs.asValue()
            is SignedIntegerConstant -> lhs shl rhs.asValue()
            else -> shl
        }
    }

    private fun constExprShr(shr: Shr): Value {
        val rhs = shr.rhs()
        return when (val lhs = shr.lhs()) {
            is UnsignedIntegerConstant -> lhs shr rhs.asValue()
            is SignedIntegerConstant -> lhs shr rhs.asValue()
            else -> shr
        }
    }

    override fun visit(add: Add): Value {
        if (isConstexpr(add)) {
            return constExprAdd(add)
        }

        return add
    }

    override fun visit(and: And): Value {
        if (isConstexpr(and)) {
            return constExprAnd(and)
        }

        return and
    }

    override fun visit(sub: Sub): Value {
        if (isConstexpr(sub)) {
            return constExprSub(sub)
        }

        return sub
    }

    override fun visit(mul: Mul): Value {
        if (isConstexpr(mul)) {
            return constExprMul(mul)
        }

        return mul
    }

    override fun visit(or: Or): Value {
        if (isConstexpr(or)) {
            return constExprOr(or)
        }

        return or
    }

    override fun visit(xor: Xor): Value {
        if (isConstexpr(xor)) {
            return constExprXor(xor)
        }

        return xor
    }

    override fun visit(fadd: Fxor): Value = fadd

    override fun visit(shl: Shl): Value {
        if (isConstexpr(shl)) {
            return constExprShl(shl)
        }

        return shl
    }

    override fun visit(shr: Shr): Value {
        if (isConstexpr(shr)) {
            return constExprShr(shr)
        }

        return shr
    }

    override fun visit(div: Div): Value = div

    override fun visit(neg: Neg): Value = neg

    override fun visit(not: Not): Value = not

    override fun visit(branch: Branch): Value {
        TODO("Not yet implemented")
    }

    override fun visit(branchCond: BranchCond): Value {
        TODO("Not yet implemented")
    }

    override fun visit(call: Call): Value = call

    override fun visit(tupleCall: TupleCall): Value = tupleCall

    override fun visit(flag2Int: Flag2Int): Value = flag2Int

    override fun visit(bitcast: Bitcast): Value = bitcast

    override fun visit(itofp: Int2Float): Value = itofp

    override fun visit(utofp: Unsigned2Float): Value = utofp

    override fun visit(zext: ZeroExtend): Value {
        val operand = zext.operand()
        if (operand is UnsignedIntegerConstant) {
            return UnsignedIntegerConstant.of(zext.type(), operand.value())
        }

        return zext
    }

    override fun visit(sext: SignExtend): Value {
        val operand = sext.operand()
        if (operand is SignedIntegerConstant) {
            return SignedIntegerConstant.of(sext.type(), operand.value())
        }

        return sext
    }

    override fun visit(trunc: Truncate): Value {
        val operand = trunc.operand()
        return when (operand) {
            is UnsignedIntegerConstant -> UnsignedIntegerConstant.of(trunc.asType(), operand.value())
            is SignedIntegerConstant -> SignedIntegerConstant.of(trunc.asType(), operand.value())
            else -> trunc
        }
    }

    override fun visit(fptruncate: FpTruncate): Value = fptruncate

    override fun visit(fpext: FpExtend): Value = fpext

    override fun visit(fptosi: Float2Int): Value = fptosi

    override fun visit(copy: Copy): Value = copy

    override fun visit(move: Move): Value {
        TODO("Not yet implemented")
    }

    override fun visit(move: MoveByIndex): Value {
        TODO("Not yet implemented")
    }

    override fun visit(downStackFrame: DownStackFrame): Value {
        TODO("Not yet implemented")
    }

    override fun visit(gep: GetElementPtr): Value = gep

    override fun visit(gfp: GetFieldPtr): Value = gfp

    private fun constExprUIntCompare(icmp: IntCompare, lhs: UnsignedIntegerConstant, rhs: UnsignedIntegerConstant): Value {
        val lhsValue = lhs.value().toULong()
        val rhsValue = rhs.value().toULong()
        val flag = when (icmp.predicate()) {
            IntPredicate.Eq -> lhsValue == rhsValue
            IntPredicate.Ne -> lhsValue != rhsValue
            IntPredicate.Gt -> lhsValue > rhsValue
            IntPredicate.Ge -> lhsValue >= rhsValue
            IntPredicate.Lt -> lhsValue < rhsValue
            IntPredicate.Le -> lhsValue <= rhsValue
        }

        return if (flag) TrueBoolValue else FalseBoolValue
    }

    private fun constExprSIntCompare(icmp: IntCompare, lhs: SignedIntegerConstant, rhs: SignedIntegerConstant): Value {
        val lhsValue = lhs.value()
        val rhsValue = rhs.value()
        val flag = when (icmp.predicate()) {
            IntPredicate.Eq -> lhsValue == rhsValue
            IntPredicate.Ne -> lhsValue != rhsValue
            IntPredicate.Gt -> lhsValue > rhsValue
            IntPredicate.Ge -> lhsValue >= rhsValue
            IntPredicate.Lt -> lhsValue < rhsValue
            IntPredicate.Le -> lhsValue <= rhsValue
        }

        return if (flag) TrueBoolValue else FalseBoolValue
    }

    private fun constExprIntCompare(icmp: IntCompare): Value {
        val rhs = icmp.rhs()
        return when (val lhs = icmp.lhs()) {
            is UnsignedIntegerConstant -> constExprUIntCompare(icmp, lhs, rhs.asValue())
            is SignedIntegerConstant -> constExprSIntCompare(icmp, lhs, rhs.asValue())
            else -> icmp
        }
    }

    override fun visit(icmp: IntCompare): Value {
        if (isConstexpr(icmp)) {
            return constExprIntCompare(icmp)
        }

        return icmp
    }

    override fun visit(fcmp: FloatCompare): Value = fcmp

    override fun visit(load: Load): Value = load

    override fun visit(phi: Phi): Value = phi

    override fun visit(phi: UncompletedPhi): Value = phi

    override fun visit(returnValue: ReturnValue): Value {
        TODO("Not yet implemented")
    }

    override fun visit(returnVoid: ReturnVoid): Value {
        TODO("Not yet implemented")
    }

    override fun visit(indirectionCall: IndirectionCall): Value = indirectionCall

    override fun visit(indirectionVoidCall: IndirectionVoidCall): Value {
        TODO("Not yet implemented")
    }

    override fun visit(select: Select): Value {
        val cond = select.condition()
        if (cond is BoolValue) {
            return when (cond) {
                TrueBoolValue -> select.onTrue()
                FalseBoolValue -> select.onFalse()
            }
        }

        return select
    }

    override fun visit(store: Store): Value {
        TODO("Not yet implemented")
    }

    override fun visit(upStackFrame: UpStackFrame): Value {
        TODO("Not yet implemented")
    }

    override fun visit(voidCall: VoidCall): Value {
        TODO("Not yet implemented")
    }

    override fun visit(int2ptr: Int2Pointer): Value {
        val operand = int2ptr.operand()
        if (operand is IntegerConstant && operand.value() == 0L) {
            return NullValue
        }

        return int2ptr
    }

    override fun visit(ptr2Int: Pointer2Int): Value = ptr2Int

    override fun visit(memcpy: Memcpy): Value {
        TODO("Not yet implemented")
    }

    override fun visit(indexedLoad: IndexedLoad): Value = indexedLoad

    override fun visit(store: StoreOnStack): Value {
        TODO("Not yet implemented")
    }

    override fun visit(loadst: LoadFromStack): Value = loadst

    override fun visit(leaStack: LeaStack): Value = leaStack

    override fun visit(tupleDiv: TupleDiv): Value {
        if (isConstexpr(tupleDiv)) {
            return constExprTupleDiv(tupleDiv)
        }

        return tupleDiv
    }

    override fun visit(proj: Projection): Value {
        val operand = proj.tuple()
        if (operand is TupleConstant) {
            return operand.inner(proj.index())
        }

        return proj
    }

    override fun visit(switch: Switch): Value {
        TODO("Not yet implemented")
    }

    override fun visit(tupleCall: IndirectionTupleCall): Value = tupleCall

    override fun visit(intrinsic: Intrinsic): Value {
        TODO("Not yet implemented")
    }
}