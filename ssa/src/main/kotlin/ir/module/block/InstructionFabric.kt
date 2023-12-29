package ir.module.block

import ir.Value
import ir.types.*
import ir.instruction.*
import ir.AnyFunctionPrototype


interface InstructionFabric {
    fun neg(value: Value): Neg
    fun not(value: Value): Not
    fun arithmeticBinary(a: Value, op: ArithmeticBinaryOp, b: Value): ArithmeticBinary
    fun intCompare(a: Value, predicate: IntPredicate, b: Value): IntCompare
    fun floatCompare(a: Value, predicate: FloatPredicate, b: Value): FloatCompare
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
    fun cast(value: Value, ty: PrimitiveType, cast: CastType): Cast
    fun select(cond: Value, onTrue: Value, onFalse: Value): Select
    fun phi(incoming: List<Value>, labels: List<Block>): Phi
}

interface InternalInstructionFabric {
    fun uncompletedPhi(ty: PrimitiveType, incoming: Value): Phi
    fun copy(value: Value): Copy
    fun downStackFrame(callable: Callable)
    fun upStackFrame(callable: Callable)
}

interface AnyFabric : InstructionFabric, InternalInstructionFabric