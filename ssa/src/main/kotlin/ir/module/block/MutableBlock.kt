package ir.module.block

import ir.AnyFunctionPrototype
import ir.Value
import ir.instruction.*
import ir.types.PrimitiveType
import ir.types.Type

interface MutableBlock {
    fun arithmeticUnary(op: ArithmeticUnaryOp, value: Value): ArithmeticUnary
    fun arithmeticBinary(a: Value, op: ArithmeticBinaryOp, b: Value): ArithmeticBinary
    fun intCompare(a: Value, pred: IntPredicate, b: Value): IntCompare
    fun load(loadedType: PrimitiveType, ptr: Value): Load
    fun store(ptr: Value, value: Value)
    fun call(func: AnyFunctionPrototype, args: ArrayList<Value>): Call
    fun vcall(func: AnyFunctionPrototype, args: ArrayList<Value>)
    fun branch(target: Block)
    fun branchCond(value: Value, onTrue: Block, onFalse: Block)
    fun alloc(ty: Type): Alloc
    fun ret(value: Value)
    fun retVoid()
    fun gep(source: Value, ty: PrimitiveType, index: Value): GetElementPtr
    fun cast(value: Value, ty: Type, cast: CastType): Cast
    fun select(cond: Value, onTrue: Value, onFalse: Value): Select
    fun phi(incoming: List<Value>, labels: List<Block>): Phi
    fun uncompletedPhi(ty: PrimitiveType, incoming: Value): Phi
    fun copy(value: Value): Copy
    fun downStackFrame(callable: Callable)
    fun upStackFrame(callable: Callable)
}