package ir.module.builder

import ir.ArgumentValue
import ir.module.FunctionPrototype
import ir.instruction.Return
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.module.block.Block
import ir.module.block.Label


abstract class AnyFunctionDataBuilder(protected val prototype: FunctionPrototype,
                                      protected val argumentValues: List<ArgumentValue>,
                                      protected val blocks: BasicBlocks) {
    protected var bb: Block = blocks.begin()
    private var allocatedLabel: Int = 0

    protected fun allocateBlock(): Block {
        allocatedLabel += 1
        val bb = Block.empty(allocatedLabel)
        blocks.putBlock(bb)
        return bb
    }

    fun currentLabel(): Label = bb

    fun begin(): Label = blocks.begin()
    fun createLabel(): Label = allocateBlock()

    fun argument(index: Int): ArgumentValue = argumentValues[index]

    fun arguments(): List<ArgumentValue> = argumentValues

    fun prototype(): FunctionPrototype = prototype

    abstract fun build(): FunctionData

    protected fun normalizeBlocks(): Boolean {
        val last = blocks.blocks().find {
            it.lastOrNull() is Return
        }
        if (last == null) {
            throw IllegalStateException("Function '${prototype.name}' does not have return instruction")
        }
        blocks.swapBlocks(last.index, blocks.size() - 1)
        return true
    }
}