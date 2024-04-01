package ir.module.builder

import ir.ArgumentValue
import ir.FunctionPrototype
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.module.block.Block


abstract class AnyFunctionDataBuilder(protected val prototype: FunctionPrototype,
                                      protected val argumentValues: List<ArgumentValue>,
                                      protected val blocks: BasicBlocks) {
    protected var bb: Block = blocks.begin()
    private var allocatedLabel: Int = 0

    private fun allocateBlock(): Block {
        allocatedLabel += 1
        val bb = Block.empty(allocatedLabel)
        blocks.putBlock(bb)
        return bb
    }

    fun begin(): Block = blocks.begin()
    fun createLabel(): Block = allocateBlock()

    fun argument(index: Int): ArgumentValue = argumentValues[index]

    fun arguments(): List<ArgumentValue> = argumentValues

    abstract fun build(): FunctionData
}