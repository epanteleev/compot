package ir.module.builder.impl

import common.arrayFrom
import ir.attributes.ArgumentValueAttribute
import ir.attributes.ByValue
import ir.attributes.FunctionAttribute
import ir.types.*
import ir.value.*
import ir.module.*
import ir.value.Value
import ir.instruction.*
import ir.intrinsic.IntrinsicProvider
import ir.module.block.Block
import ir.module.block.Label
import ir.module.block.InstructionFabric
import ir.module.builder.AnyFunctionDataBuilder
import ir.value.constant.IntegerConstant
import ir.value.constant.UnsignedIntegerConstant


class FunctionDataBuilder private constructor(prototype: FunctionPrototype, argumentValues: List<ArgumentValue>):
    AnyFunctionDataBuilder(prototype, argumentValues), InstructionFabric {

    init {
        switchLabel(fd.begin())
    }

    override fun build(): FunctionData {
        normalizeBlocks()
        return fd
    }

    //TODO bad design???
    fun last(): Instruction? {
        return bb.lastOrNull()
    }

    override fun not(value: Value): Not {
        return bb.put(Not.not(value))
    }

    override fun neg(value: Value): Neg {
        return bb.put(Neg.neg(value))
    }

    override fun add(a: Value, b: Value): Add {
        return bb.put(Add.add(a, b))
    }

    override fun xor(a: Value, b: Value): Xor {
        return bb.put(Xor.xor(a, b))
    }

    override fun sub(a: Value, b: Value): Sub {
        return bb.put(Sub.sub(a, b))
    }

    override fun mul(a: Value, b: Value): Mul {
        return bb.put(Mul.mul(a, b))
    }

    override fun shr(a: Value, b: Value): Shr {
        return bb.put(Shr.shr(a, b))
    }

    override fun or(a: Value, b: Value): Or {
        return bb.put(Or.or(a, b))
    }

    override fun shl(a: Value, b: Value): Shl {
        return bb.put(Shl.shl(a, b))
    }

    override fun and(a: Value, b: Value): And {
        return bb.put(And.and(a, b))
    }

    override fun div(a: Value, b: Value): Div {
        return bb.put(Div.div(a, b))
    }

    override fun tupleDiv(a: Value, b: Value): TupleDiv {
        return bb.put(TupleDiv.div(a, b))
    }

    override fun icmp(a: Value, predicate: IntPredicate, b: Value): IntCompare {
        return bb.put(IntCompare.icmp(a, predicate, b))
    }

    override fun fcmp(a: Value, predicate: FloatPredicate, b: Value): FloatCompare {
        return bb.put(FloatCompare.fcmp(a, predicate, b))
    }

    override fun load(loadedType: PrimitiveType, ptr: Value): Load {
        return bb.put(Load.load(loadedType, ptr))
    }

    override fun store(ptr: Value, value: Value): Store {
        return bb.put(Store.store(ptr, value))
    }

    override fun call(func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): Call {
        val call = Call.call(func, args, attributes, target as Block)
        return bb.put(call)
    }

    override fun tupleCall(func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): TupleCall {
        val call = TupleCall.call(func, args, attributes, target as Block)
        return bb.put(call)
    }

    override fun vcall(func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): VoidCall {
        val call = VoidCall.call(func, args, attributes, target as Block)
        return bb.put(call)
    }

    override fun icall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): IndirectionCall {
        val call = IndirectionCall.call(pointer, func, args, attributes, target as Block)
        return bb.put(call)
    }

    override fun ivcall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): IndirectionVoidCall {
        val call = IndirectionVoidCall.call(pointer, func, args, attributes, target as Block)
        return bb.put(call)
    }

    fun branch(target: Label) {
        branch(fd.blocks.findBlock(target))
    }

    override fun branch(target: Block): Branch {
        return bb.put(Branch.br(target))
    }

    override fun branchCond(value: Value, onTrue: Label, onFalse: Label): BranchCond {
        val br = BranchCond.br(value, onTrue as Block, onFalse as Block)
        return bb.put(br)
    }

    override fun alloc(ty: NonTrivialType): Alloc {
        return bb.put(Alloc.alloc(ty))
    }

    override fun ret(returnType: Type, values: Array<Value>): Return {
        return bb.put(ReturnValue.ret(returnType, values))
    }

    override fun retVoid(): ReturnVoid {
        return bb.put(ReturnVoid.ret())
    }

    override fun gep(source: Value, elementType: NonTrivialType, index: Value): GetElementPtr {
        val gep = GetElementPtr.gep(source, elementType, index)
        return bb.put(gep)
    }

    override fun gfp(source: Value, ty: AggregateType, index: IntegerConstant): GetFieldPtr {
        val gfp = GetFieldPtr.gfp(source, ty, index)
        return bb.put(gfp)
    }

    override fun flag2int(value: Value, ty: IntegerType): Flag2Int {
        val flag2Int = Flag2Int.flag2int(value, ty)
        return bb.put(flag2Int)
    }

    override fun int2fp(value: Value, ty: FloatingPointType): Int2Float {
        val int2fp = Int2Float.int2fp(value, ty)
        return bb.put(int2fp)
    }

    override fun uint2fp(value: Value, ty: FloatingPointType): Unsigned2Float {
        val uint2fp = Unsigned2Float.uint2fp(value, ty)
        return bb.put(uint2fp)
    }

    override fun bitcast(value: Value, ty: IntegerType): Bitcast {
        val bitcast = Bitcast.bitcast(value, ty)
        return bb.put(bitcast)
    }

    override fun zext(value: Value, toType: UnsignedIntType): ZeroExtend {
        val zext = ZeroExtend.zext(value, toType)
        return bb.put(zext)
    }

    override fun sext(value: Value, toType: SignedIntType): SignExtend {
        val sext = SignExtend.sext(value, toType)
        return bb.put(sext)
    }

    override fun trunc(value: Value, toType: IntegerType): Truncate {
        val trunc = Truncate.trunc(value, toType)
        return bb.put(trunc)
    }

    override fun select(cond: Value, type: IntegerType, onTrue: Value, onFalse: Value): Select {
        val sel = Select.select(cond, type, onTrue, onFalse)
        return bb.put(sel)
    }

    override fun fptrunc(value: Value, toType: FloatingPointType): FpTruncate {
        val fptrunc = FpTruncate.fptrunc(value, toType)
        return bb.put(fptrunc)
    }

    override fun fpext(value: Value, toType: FloatingPointType): FpExtend {
        val fpext = FpExtend.fpext(value, toType)
        return bb.put(fpext)
    }

    override fun fp2Int(value: Value, toType: IntegerType): Float2Int {
        val fp2Int = Float2Int.fp2int(value, toType)
        return bb.put(fp2Int)
    }

    override fun phi(incoming: List<Value>, labels: List<Label>): Phi {
        return bb.put(Phi.phi(labels.mapTo(arrayListOf()) { it as Block }, incoming.toTypedArray()))
    }

    override fun int2ptr(value: Value): Int2Pointer {
        val int2ptr = Int2Pointer.int2ptr(value)
        return bb.put(int2ptr)
    }

    override fun ptr2int(value: Value, toType: IntegerType): Pointer2Int {
        val ptr2int = Pointer2Int.ptr2int(value, toType)
        return bb.put(ptr2int)
    }

    override fun memcpy(dst: Value, src: Value, length: UnsignedIntegerConstant): Memcpy {
        val memcpy = Memcpy.memcpy(dst, src, length)
        return bb.put(memcpy)
    }

    override fun proj(tuple: Value, index: Int): Projection {
        val proj = Projection.proj(tuple, index)
        return bb.put(proj)
    }

    override fun switch(value: Value, default: Label, table: List<IntegerConstant>, targets: List<Label>): Switch {
        val switch = Switch.switch(value, default as Block, table.toTypedArray(), arrayFrom(targets) { it -> it as Block })
        return bb.put(switch)
    }

    override fun intrinsic(inputs: List<Value>, implementor: IntrinsicProvider, target: Label): Intrinsic {
        val intrinsic = Intrinsic.intrinsic(inputs.toTypedArray(), implementor, target as Block)
        return bb.put(intrinsic)
    }

    companion object {
        fun create(
            name: String,
            returnType: Type,
            arguments: List<NonTrivialType>,
            argumentValues: List<ArgumentValue>,
            attributes: Set<FunctionAttribute>
        ): FunctionDataBuilder {
            val prototype = FunctionPrototype(name, returnType, arguments, attributes)
            return FunctionDataBuilder(prototype, argumentValues)
        }

        fun create(name: String, returnType: Type, argumentTypes: List<NonTrivialType>, attributes: Set<FunctionAttribute>): FunctionDataBuilder {
            val argumentValues = arrayListOf<ArgumentValue>()
            val argAttributes = attributes.filterIsInstance<ArgumentValueAttribute>()
            for ((idx, arg) in argumentTypes.withIndex()) { //TODO simplify!??
                val byValue = argAttributes.find { it is ByValue && it.argumentIndex == idx }
                val argAttr = if (byValue != null) {
                    hashSetOf(byValue)
                } else {
                    hashSetOf()
                }
                argumentValues.add(ArgumentValue(idx, arg, argAttr))
            }

            return create(name, returnType, argumentTypes, argumentValues, attributes)
        }
    }
}