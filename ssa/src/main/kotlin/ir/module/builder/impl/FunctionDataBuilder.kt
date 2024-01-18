package ir.module.builder.impl

import ir.*
import ir.instruction.*
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.module.block.Block
import ir.module.block.Label
import ir.module.block.InstructionFabric
import ir.types.*


class FunctionDataBuilder private constructor(
    private val prototype: FunctionPrototype,
    private var argumentValues: List<ArgumentValue>,
    private val blocks: BasicBlocks
): InstructionFabric {
    private var allocatedLabel: Int = 0
    private var bb: Block = blocks.begin()

    private fun allocateBlock(): Block {
        allocatedLabel += 1
        val bb = Block.empty(allocatedLabel)
        blocks.putBlock(bb)
        return bb
    }

    fun begin(): Block {
        return blocks.begin()
    }

    fun build(): FunctionData {
        return FunctionData.create(prototype, blocks, argumentValues)
    }

    fun createLabel(): Block = allocateBlock()

    fun switchLabel(label: Label) {
        bb = blocks.findBlock(label)
    }

    fun argument(index: Int): ArgumentValue = argumentValues[index]

    fun arguments(): List<ArgumentValue> {
        return argumentValues
    }

    override fun not(value: Value): Not {
        return bb.not(value)
    }

    override fun neg(value: Value): Neg {
        return bb.neg(value)
    }

    override fun arithmeticBinary(a: Value, op: ArithmeticBinaryOp, b: Value): ArithmeticBinary {
        return bb.arithmeticBinary(a, op, b)
    }

    override fun icmp(a: Value, predicate: IntPredicate, b: Value): SignedIntCompare {
        return bb.icmp(a, predicate, b)
    }

    override fun ucmp(a: Value, predicate: IntPredicate, b: Value): UnsignedIntCompare {
        return bb.ucmp(a, predicate, b)
    }

    override fun fcmp(a: Value, predicate: FloatPredicate, b: Value): FloatCompare {
        return bb.fcmp(a, predicate, b)
    }

    override fun load(loadedType: PrimitiveType, ptr: Value): Load {
        return bb.load(loadedType, ptr)
    }

    override fun store(ptr: Value, value: Value) {
        return bb.store(ptr, value)
    }

    override fun call(func: AnyFunctionPrototype, args: List<Value>): Call {
        return bb.call(func, args)
    }

    override fun vcall(func: AnyFunctionPrototype, args: List<Value>) {
        bb.vcall(func, args)
    }

    override fun icall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>): IndirectionCall {
        return bb.icall(pointer, func, args)
    }

    override fun ivcall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>) {
        bb.ivcall(pointer, func, args)
    }

    fun branch(target: Label) {
        branch(blocks.findBlock(target))
    }

    override fun branch(target: Block) {
        bb.branch(target)
    }

    override fun branchCond(value: Value, onTrue: Block, onFalse: Block) {
        bb.branchCond(value, onTrue, onFalse)
    }

    fun branchCond(value: Value, onTrue: Label, onFalse: Label) {
        branchCond(value, blocks.findBlock(onTrue), blocks.findBlock(onFalse))
    }

    override fun alloc(ty: Type): Alloc {
        return bb.alloc(ty)
    }

    override fun ret(value: Value) {
        bb.ret(value)
    }

    override fun retVoid() {
        bb.retVoid()
    }

    override fun gep(source: Value, ty: PrimitiveType, index: Value): GetElementPtr {
        return bb.gep(source, ty, index)
    }

    override fun gfp(source: Value, ty: AggregateType, index: IntegerConstant): GetFieldPtr {
        return bb.gfp(source, ty, index)
    }

    override fun bitcast(value: Value, ty: PrimitiveType): Bitcast {
        return bb.bitcast(value, ty)
    }

    override fun zext(value: Value, toType: IntegerType): ZeroExtend {
        return bb.zext(value, toType)
    }

    override fun sext(value: Value, toType: IntegerType): SignExtend {
        return bb.sext(value, toType)
    }

    override fun trunc(value: Value, toType: IntegerType): Truncate {
        return bb.trunc(value, toType)
    }

    override fun select(cond: Value, type: PrimitiveType, onTrue: Value, onFalse: Value): Select {
        return bb.select(cond, type, onTrue, onFalse)
    }

    override fun fptrunc(value: Value, toType: FloatingPointType): FpTruncate {
        return bb.fptrunc(value, toType)
    }

    override fun fpext(value: Value, toType: FloatingPointType): FpExtend {
        return bb.fpext(value, toType)
    }

    override fun fptosi(value: Value, toType: SignedIntType): FloatToSigned {
        return bb.fptosi(value, toType)
    }

    override fun phi(incoming: List<Value>, labels: List<Block>): Phi {
        return bb.phi(incoming, labels)
    }

    companion object {
        fun create(
            name: String,
            returnType: Type,
            arguments: List<Type>,
            argumentValues: List<ArgumentValue>
        ): FunctionDataBuilder {
            val prototype = FunctionPrototype(name, returnType, arguments)
            val startBB = Block.empty(Label.entry.index)
            val basicBlocks = BasicBlocks.create(startBB)

            val builder = FunctionDataBuilder(prototype, argumentValues, basicBlocks)
            builder.switchLabel(startBB)
            return builder
        }

        fun create(name: String, returnType: Type, argumentTypes: List<Type>): FunctionDataBuilder {
            val argumentValues = argumentTypes.withIndex().mapTo(arrayListOf()) {
                ArgumentValue(it.index, it.value)
            }

            return create(name, returnType, argumentTypes, argumentValues)
        }
    }
}