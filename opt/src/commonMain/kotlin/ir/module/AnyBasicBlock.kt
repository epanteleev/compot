package ir.module

import ir.module.block.AnyBlock

abstract class AnyBasicBlocks<BB: AnyBlock<*>> : Iterable<BB> {
    protected val modificationCounter = ModificationCounter()

    internal fun marker(): MutationMarker = modificationCounter.mutations()

    abstract fun begin(): BB
    abstract fun end(): BB
    abstract fun size(): Int
}