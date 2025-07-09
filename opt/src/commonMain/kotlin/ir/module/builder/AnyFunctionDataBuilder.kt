package ir.module.builder

import ir.instruction.Return
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
        return fd.blocks().createBlock()
    }

    fun switchLabel(label: Label) {
        bb = fd.blocks().findBlock(label)
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

    protected fun normalizeBlocks() {
        val idx = fd.blocks().blocks().indexOfFirst { it.last() is Return }
        if (idx < 0) {
            throw IllegalStateException("Function data must have a return block")
        }

        fd.blocks().swapBlocks(idx, fd.blocks().size() - 1)
    }
}