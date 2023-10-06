package ir

import ir.block.Block

interface BlockBuilderInterface {
    fun arithmeticUnary(op: ArithmeticUnaryOp, value: Value): Value
    fun arithmeticBinary(a: Value, op: ArithmeticBinaryOp, b: Value): Value
    fun intCompare(a: Value, pred: IntPredicate, b: Value): Value
    fun load(ptr: Value): Value
    fun store(ptr: Value, value: Value)
    fun call(func: AnyFunctionPrototype, args: ArrayList<Value>): Value
    fun branch(target: Block)
    fun branchCond(value: Value, onTrue: Block, onFalse: Block)
    fun stackAlloc(ty: Type, size: Long): Value
    fun ret(value: Value)
    fun gep(source: Value, index: Value): Value
    fun cast(value: Value, ty: Type, cast: CastType): Value
    fun select(cond: Value, onTrue: Value, onFalse: Value): Value
    fun phi(incoming: ArrayList<Value>, labels: ArrayList<Block>): Value
    fun uncompletedPhi(incoming: Value, bb: Block): Value
    fun copy(value: Value): Value
}