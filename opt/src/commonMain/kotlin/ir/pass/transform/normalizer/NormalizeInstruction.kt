package ir.pass.transform.normalizer

import ir.instruction.*
import ir.instruction.lir.*
import ir.instruction.utils.IRInstructionVisitor
import ir.value.LocalValue
import ir.value.Value
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
            is U8Value -> lhs + rhs.asValue()
            is U16Value -> lhs + rhs.asValue()
            is U32Value -> lhs + rhs.asValue()
            is U64Value -> lhs + rhs.asValue()
            else -> add
        }
    }

    override fun visit(add: Add): Value {
        if (isConstexpr(add)) {
            return constExprAdd(add)
        }

        return add
    }

    override fun visit(and: And): Value = and

    override fun visit(sub: Sub): Value = sub

    override fun visit(mul: Mul): Value = mul

    override fun visit(or: Or): Value = or

    override fun visit(xor: Xor): Value = xor

    override fun visit(fadd: Fxor): Value = fadd

    override fun visit(shl: Shl): Value = shl

    override fun visit(shr: Shr): Value = shr

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

    override fun visit(zext: ZeroExtend): Value = zext

    override fun visit(sext: SignExtend): Value = sext

    override fun visit(trunc: Truncate): Value = trunc

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

    override fun visit(icmp: IntCompare): Value = icmp

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

    override fun visit(select: Select): Value = select

    override fun visit(store: Store): Value {
        TODO("Not yet implemented")
    }

    override fun visit(upStackFrame: UpStackFrame): Value {
        TODO("Not yet implemented")
    }

    override fun visit(voidCall: VoidCall): Value {
        TODO("Not yet implemented")
    }

    override fun visit(int2ptr: Int2Pointer): Value = int2ptr

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

    override fun visit(tupleDiv: TupleDiv): Value = tupleDiv

    override fun visit(proj: Projection): Value = proj

    override fun visit(switch: Switch): Value {
        TODO("Not yet implemented")
    }

    override fun visit(tupleCall: IndirectionTupleCall): Value = tupleCall

    override fun visit(intrinsic: Intrinsic): Value {
        TODO("Not yet implemented")
    }
}