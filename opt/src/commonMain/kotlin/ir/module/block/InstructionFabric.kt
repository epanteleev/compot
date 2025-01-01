package ir.module.block

import ir.attributes.FunctionAttribute
import ir.types.*
import ir.instruction.*
import ir.instruction.lir.*
import ir.intrinsic.IntrinsicProvider
import ir.module.DirectFunctionPrototype
import ir.module.IndirectFunctionPrototype
import ir.value.UsableValue
import ir.value.constant.IntegerConstant
import ir.value.constant.UnsignedIntegerConstant
import ir.value.Value


interface InstructionFabric {
    fun neg(value: Value): Neg
    fun not(value: Value): Not
    fun add(a: Value, b: Value): Add
    fun and(a: Value, b: Value): And
    fun or(a: Value, b: Value): Or
    fun xor(a: Value, b: Value): Xor
    fun div(a: Value, b: Value): Div
    fun shl(a: Value, b: Value): Shl
    fun shr(a: Value, b: Value): Shr
    fun sub(a: Value, b: Value): Sub
    fun mul(a: Value, b: Value): Mul
    fun tupleDiv(a: Value, b: Value): TupleDiv
    fun icmp(a: Value, predicate: IntPredicate, b: Value): IntCompare
    fun fcmp(a: Value, predicate: FloatPredicate, b: Value): FloatCompare
    fun load(loadedType: PrimitiveType, ptr: Value): Load
    fun store(ptr: Value, value: Value): Store
    fun call(func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): Call
    fun tupleCall(func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): TupleCall
    fun vcall(func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): VoidCall
    fun icall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): IndirectionCall
    fun ivcall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): IndirectionVoidCall
    fun branch(target: Block): Branch
    fun branchCond(value: Value, onTrue: Label, onFalse: Label): BranchCond //TODO Labels
    fun alloc(ty: NonTrivialType): Alloc
    fun ret(returnType: Type, values: Array<Value>): Return
    fun retVoid(): ReturnVoid
    fun gep(source: Value, elementType: NonTrivialType, index: Value): GetElementPtr
    fun gfp(source: Value, ty: AggregateType, index: IntegerConstant): GetFieldPtr
    fun flag2int(value: Value, ty: IntegerType): Flag2Int
    fun int2fp(value: Value, ty: FloatingPointType): Int2Float
    fun uint2fp(value: Value, ty: FloatingPointType): Unsigned2Float
    fun bitcast(value: Value, ty: IntegerType): Bitcast
    fun zext(value: Value, toType: UnsignedIntType): ZeroExtend
    fun sext(value: Value, toType: SignedIntType): SignExtend
    fun trunc(value: Value, toType: IntegerType): Truncate
    fun fptrunc(value: Value, toType: FloatingPointType): FpTruncate
    fun fpext(value: Value, toType: FloatingPointType): FpExtend
    fun fp2Int(value: Value, toType: IntegerType): Float2Int
    fun select(cond: Value, type: IntegerType, onTrue: Value, onFalse: Value): Select
    fun itupleCall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): IndirectionTupleCall
    fun phi(incoming: List<Value>, labels: List<Label>): Phi
    fun int2ptr(value: Value): Int2Pointer
    fun ptr2int(value: Value, toType: IntegerType): Pointer2Int
    fun memcpy(dst: Value, src: Value, length: UnsignedIntegerConstant): Memcpy
    fun proj(tuple: Value, index: Int): Projection
    fun switch(value: Value, default: Label, table: List<IntegerConstant>, targets: List<Label>): Switch
    fun intrinsic(inputs: List<Value>, implementor: IntrinsicProvider, target: Label): Intrinsic
}

interface AnyInstructionFabric : InstructionFabric