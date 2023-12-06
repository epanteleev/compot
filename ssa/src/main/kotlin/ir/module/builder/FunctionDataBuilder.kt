package ir.module.builder

import ir.AnyFunctionPrototype
import ir.ArgumentValue
import ir.FunctionPrototype
import ir.Value
import ir.instruction.*
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.module.block.Block
import ir.module.block.Label
import ir.types.PrimitiveType
import ir.types.Type

class FunctionDataBuilder private constructor(
    private val prototype: FunctionPrototype,
    private var argumentValues: List<ArgumentValue>,
    private val blocks: BasicBlocks
) {
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

    fun arithmeticUnary(op: ArithmeticUnaryOp, value: Value): Value {
        return bb.arithmeticUnary(op, value)
    }

    fun arithmeticBinary(a: Value, op: ArithmeticBinaryOp, b: Value): Value {
        return bb.arithmeticBinary(a, op, b)
    }

    fun intCompare(a: Value, pred: IntPredicate, b: Value): Value {
        return bb.intCompare(a, pred, b)
    }

    fun load(loadedType: PrimitiveType, ptr: Value): Value {
        return bb.load(loadedType, ptr)
    }

    fun store(ptr: Value, value: Value) {
        return bb.store(ptr, value)
    }

    fun call(func: AnyFunctionPrototype, args: ArrayList<Value>): Call {
        return bb.call(func, args)
    }

    fun vcall(func: AnyFunctionPrototype, args: ArrayList<Value>) {
        bb.vcall(func, args)
    }

    fun branch(target: Label) {
        branch(blocks.findBlock(target))
    }

    fun branch(target: Block) {
        bb.branch(target)
    }

    fun branchCond(value: Value, onTrue: Block, onFalse: Block) {
        bb.branchCond(value, onTrue, onFalse)
    }

    fun branchCond(value: Value, onTrue: Label, onFalse: Label) {
        branchCond(value, blocks.findBlock(onTrue), blocks.findBlock(onFalse))
    }

    fun stackAlloc(ty: Type): Value {
        return bb.alloc(ty)
    }

    fun ret(value: Value) {
        bb.ret(value)
    }

    fun gep(source: Value, type: PrimitiveType, index: Value): Value {
        return bb.gep(source, type, index)
    }

    fun cast(value: Value, ty: PrimitiveType, cast: CastType): Value {
        return bb.cast(value, ty, cast)
    }

    fun select(cond: Value, onTrue: Value, onFalse: Value): Value {
        return bb.select(cond, onTrue, onFalse)
    }

    fun phi(incoming: ArrayList<Value>, labels: ArrayList<Block>): Value {
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