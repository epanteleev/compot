package ir.module.builder

import ir.value.ArgumentValue
import ir.module.FunctionPrototype
import ir.module.FunctionData
import ir.module.block.Block
import ir.module.block.Label


abstract class AnyFunctionDataBuilder(protected val prototype: FunctionPrototype,
                                      private val argumentValues: List<ArgumentValue>) {
    protected val fd = FunctionData.create(prototype, argumentValues)
    protected var bb: Block = fd.begin()

    protected fun allocateBlock(): Block {
        return fd.blocks.createBlock()
    }

    fun switchLabel(label: Label) {
        bb = fd.blocks.findBlock(label)
    }

    fun currentLabel(): Label = bb

    fun begin(): Label = fd.begin()

    fun createLabel(): Label = allocateBlock()

    fun argument(index: Int): ArgumentValue {
        if (index >= argumentValues.size || index < 0) {
            throw IllegalArgumentException("Argument index $index is out of bounds")
        }

        return argumentValues[index]
    }

    fun arguments(): List<ArgumentValue> = argumentValues

    fun prototype(): FunctionPrototype = prototype

    abstract fun build(): FunctionData

    protected fun normalizeBlocks(): Boolean {
        val last = fd.blocks.lastOrNull()
            ?: throw IllegalStateException("Function '${prototype.name}' does not have return instruction")
        fd.blocks.swapBlocks(last.index, fd.blocks.size() - 1)
        return true
    }
}