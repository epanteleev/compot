package ir.instruction.utils

import ir.instruction.*

interface Visitor {
    fun visit(alloc: Alloc)
    fun visit(binary: ArithmeticBinary)
    fun visit(neg: Neg)
    fun visit(neg: Not)
    fun visit(branch: Branch)
    fun visit(branchCond: BranchCond)
    fun visit(call: Call)
    fun visit(cast: Cast)
    fun visit(copy: Copy)
    fun visit(downStackFrame: DownStackFrame)
    fun visit(getElementPtr: GetElementPtr)
    fun visit(intCompare: IntCompare)
    fun visit(floatCompare: FloatCompare)
    fun visit(load: Load)
    fun visit(phi: Phi)
    fun visit(returnValue: ReturnValue)
    fun visit(returnVoid: ReturnVoid)
    fun visit(select: Select)
    fun visit(store: Store)
    fun visit(upStackFrame: UpStackFrame)
    fun visit(voidCall: VoidCall)
}