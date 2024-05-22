package ir.instruction.utils

import ir.instruction.*
import ir.instruction.lir.*


interface IRInstructionVisitor<T> {
    fun visit(alloc: Alloc): T
    fun visit(generate: Generate): T
    fun visit(lea: Lea): T
    fun visit(binary: ArithmeticBinary): T
    fun visit(neg: Neg): T
    fun visit(not: Not): T
    fun visit(branch: Branch): T
    fun visit(branchCond: BranchCond): T
    fun visit(call: Call): T
    fun visit(flag2Int: Flag2Int): T
    fun visit(bitcast: Bitcast): T
    fun visit(itofp: Int2Float): T
    fun visit(zext: ZeroExtend): T
    fun visit(sext: SignExtend): T
    fun visit(pcmp: PointerCompare): T
    fun visit(trunc: Truncate): T
    fun visit(fptruncate: FpTruncate): T
    fun visit(fpext: FpExtend): T
    fun visit(fptosi: FloatToInt): T
    fun visit(copy: Copy): T
    fun visit(move: Move): T
    fun visit(move: MoveByIndex): T
    fun visit(downStackFrame: DownStackFrame): T
    fun visit(gep: GetElementPtr): T
    fun visit(gfp: GetFieldPtr): T
    fun visit(icmp: SignedIntCompare): T
    fun visit(ucmp: UnsignedIntCompare): T
    fun visit(fcmp: FloatCompare): T
    fun visit(load: Load): T
    fun visit(phi: Phi): T
    fun visit(returnValue: ReturnValue): T
    fun visit(returnVoid: ReturnVoid): T
    fun visit(indirectionCall: IndirectionCall): T
    fun visit(indirectionVoidCall: IndirectionVoidCall): T
    fun visit(select: Select): T
    fun visit(store: Store): T
    fun visit(upStackFrame: UpStackFrame): T
    fun visit(voidCall: VoidCall): T
    fun visit(int2ptr: Int2Pointer): T
    fun visit(ptr2Int: Pointer2Int): T
    fun visit(memcpy: Memcpy): T
    fun visit(copy: IndexedLoad): T
    fun visit(store: StoreOnStack): T
    fun visit(loadst: LoadFromStack): T
    fun visit(leaStack: LeaStack): T
    fun visit(binary: TupleDiv): T
    fun visit(proj: Projection): T
}