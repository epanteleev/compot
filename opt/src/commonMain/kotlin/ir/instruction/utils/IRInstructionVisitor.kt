package ir.instruction.utils

import ir.instruction.*
import ir.instruction.lir.*


abstract class IRInstructionVisitor<T> {
    abstract fun visit(alloc: Alloc): T
    abstract fun visit(generate: Generate): T
    abstract fun visit(lea: Lea): T
    abstract fun visit(add: Add): T
    abstract fun visit(and: And): T
    abstract fun visit(sub: Sub): T
    abstract fun visit(mul: Mul): T
    abstract fun visit(or: Or): T
    abstract fun visit(xor: Xor): T
    abstract fun visit(fadd: Fxor): T
    abstract fun visit(shl: Shl): T
    abstract fun visit(shr: Shr): T
    abstract fun visit(div: Div): T
    abstract fun visit(neg: Neg): T
    abstract fun visit(not: Not): T
    abstract fun visit(branch: Branch): T
    abstract fun visit(branchCond: BranchCond): T
    abstract fun visit(call: Call): T
    abstract fun visit(tupleCall: TupleCall): T
    abstract fun visit(flag2Int: Flag2Int): T
    abstract fun visit(bitcast: Bitcast): T
    abstract fun visit(itofp: Int2Float): T
    abstract fun visit(utofp: Unsigned2Float): T
    abstract fun visit(zext: ZeroExtend): T
    abstract fun visit(sext: SignExtend): T
    abstract fun visit(trunc: Truncate): T
    abstract fun visit(fptruncate: FpTruncate): T
    abstract fun visit(fpext: FpExtend): T
    abstract fun visit(fptosi: Float2Int): T
    abstract fun visit(copy: Copy): T
    abstract fun visit(move: Move): T
    abstract fun visit(move: MoveByIndex): T
    abstract fun visit(downStackFrame: DownStackFrame): T
    abstract fun visit(gep: GetElementPtr): T
    abstract fun visit(gfp: GetFieldPtr): T
    abstract fun visit(icmp: IntCompare): T
    abstract fun visit(fcmp: FloatCompare): T
    abstract fun visit(load: Load): T
    abstract fun visit(phi: Phi): T
    abstract fun visit(phi: UncompletedPhi): T
    abstract fun visit(returnValue: ReturnValue): T
    abstract fun visit(returnVoid: ReturnVoid): T
    abstract fun visit(indirectionCall: IndirectionCall): T
    abstract fun visit(indirectionVoidCall: IndirectionVoidCall): T
    abstract fun visit(select: Select): T
    abstract fun visit(store: Store): T
    abstract fun visit(upStackFrame: UpStackFrame): T
    abstract fun visit(voidCall: VoidCall): T
    abstract fun visit(int2ptr: Int2Pointer): T
    abstract fun visit(ptr2Int: Pointer2Int): T
    abstract fun visit(memcpy: Memcpy): T
    abstract fun visit(indexedLoad: IndexedLoad): T
    abstract fun visit(store: StoreOnStack): T
    abstract fun visit(loadst: LoadFromStack): T
    abstract fun visit(leaStack: LeaStack): T
    abstract fun visit(tupleDiv: TupleDiv): T
    abstract fun visit(proj: Projection): T
    abstract fun visit(switch: Switch): T
    abstract fun visit(tupleCall: IndirectionTupleCall): T
    abstract fun visit(intrinsic: Intrinsic): T
}