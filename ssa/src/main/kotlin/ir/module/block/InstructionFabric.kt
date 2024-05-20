package ir.module.block

import ir.*
import ir.types.*
import ir.instruction.*
import ir.instruction.lir.*
import ir.module.AnyFunctionPrototype
import ir.module.IndirectFunctionPrototype


interface InstructionFabric {
    fun neg(value: Value): Neg
    fun not(value: Value): Not
    fun arithmeticBinary(a: Value, op: ArithmeticBinaryOp, b: Value): ArithmeticBinary
    fun icmp(a: Value, predicate: IntPredicate, b: Value): SignedIntCompare
    fun ucmp(a: Value, predicate: IntPredicate, b: Value): UnsignedIntCompare
    fun pcmp(a: Value, predicate: IntPredicate, b: Value): PointerCompare
    fun fcmp(a: Value, predicate: FloatPredicate, b: Value): FloatCompare
    fun load(loadedType: PrimitiveType, ptr: Value): Load
    fun store(ptr: Value, value: Value): Store
    fun call(func: AnyFunctionPrototype, args: List<Value>, target: Label): Call
    fun vcall(func: AnyFunctionPrototype, args: List<Value>, target: Label): VoidCall
    fun icall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>): IndirectionCall
    fun ivcall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, target: Block): IndirectionVoidCall
    fun branch(target: Block): Branch
    fun branchCond(value: Value, onTrue: Block, onFalse: Block): BranchCond //TODO Labels
    fun alloc(ty: NonTrivialType): Alloc
    fun ret(value: Value): Return
    fun retVoid(): ReturnVoid
    fun gep(source: Value, elementType: PrimitiveType, index: Value): GetElementPtr
    fun gfp(source: Value, ty: AggregateType, index: IntegerConstant): GetFieldPtr
    fun flag2int(value: Value, ty: IntegerType): Flag2Int
    fun int2fp(value: Value, ty: FloatingPointType): Int2Float
    fun bitcast(value: Value, ty: PrimitiveType): Bitcast
    fun zext(value: Value, toType: UnsignedIntType): ZeroExtend
    fun sext(value: Value, toType: SignedIntType): SignExtend
    fun trunc(value: Value, toType: IntegerType): Truncate
    fun fptrunc(value: Value, toType: FloatingPointType): FpTruncate
    fun fpext(value: Value, toType: FloatingPointType): FpExtend
    fun fp2Int(value: Value, toType: IntegerType): FloatToInt
    fun select(cond: Value, type: PrimitiveType, onTrue: Value, onFalse: Value): Select
    fun phi(incoming: List<Value>, labels: List<Label>): Phi
    fun int2ptr(value: Value): Int2Pointer
    fun ptr2int(value: Value, toType: IntegerType): Pointer2Int
    fun memcpy(dst: Value, src: Value, length: UnsignedIntegerConstant)
}

interface InternalInstructionFabric {
    fun gen(ty: NonTrivialType): Generate
    fun lea(generate: Value): Lea
    fun uncompletedPhi(ty: PrimitiveType, incoming: Value): Phi
    fun copy(value: Value): Copy
    fun move(dst: Generate, fromValue: Value): Move
    fun move(dst: Value, base: Value, index: Value): MoveByIndex
    fun indexedLoad(origin: Value, loadedType: PrimitiveType, index: Value): IndexedLoad
    fun storeOnStack(destination: Value, index: Value, source: Value): StoreOnStack
    fun loadFromStack(origin: Value, loadedType: PrimitiveType, index: Value): LoadFromStack
    fun leaStack(origin: Value, loadedType: PrimitiveType, index: Value): LeaStack
    fun downStackFrame(callable: Callable): DownStackFrame
    fun upStackFrame(callable: Callable): UpStackFrame
}

interface AnyInstructionFabric : InstructionFabric, InternalInstructionFabric