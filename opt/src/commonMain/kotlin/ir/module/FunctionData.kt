package ir.module

import ir.value.ArgumentValue
import ir.module.auxiliary.CopyCFG
import ir.module.block.Block
import ir.pass.AnalysisPassCache
import ir.pass.analysis.traverse.iterator.*
import ir.pass.analysis.traverse.iterator.PostorderIterator
import ir.pass.common.AnalysisResult
import ir.pass.common.FunctionAnalysisPassFabric


class FunctionData private constructor(val prototype: FunctionPrototype, private var argumentValues: List<ArgumentValue>, val blocks: BasicBlocks) {
    private val cache = AnalysisPassCache()

    fun marker(): MutationMarker {
        return blocks.marker()
    }

    fun arguments(): List<ArgumentValue> {
        return argumentValues
    }

    fun copy(): FunctionData {
        return CopyCFG.copy(this)
    }

    fun name(): String {
        return prototype.name
    }

    fun cache(): AnalysisPassCache {
        return cache
    }

    inline fun <reified T: AnalysisResult, reified U: FunctionAnalysisPassFabric<T>> analysis(analysisType: U, useCache: Boolean = true): T {
        if (!useCache) {
            return analysisType.create(this)
        }
        val cached = cache().get(analysisType, marker())
        if (cached != null) {
            return cached
        }

        return cache().put(analysisType, analysisType.create(this))
    }

    operator fun iterator(): Iterator<Block> {
        return blocks.iterator()
    }

    operator fun get(index: Int): Block {
        return blocks.blocks()[index]
    }

    fun size(): Int {
        return blocks.size()
    }

    fun begin(): Block {
        return blocks.begin()
    }

    fun end(): Block {
        return blocks.end()
    }

    override fun hashCode(): Int {
        return prototype.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FunctionData
        return prototype == other.prototype
    }

    companion object {
        fun create(prototype: FunctionPrototype, argumentValues: List<ArgumentValue>): FunctionData {
            val basicBlocks = BasicBlocks.create()
            return FunctionData(prototype, argumentValues, basicBlocks)
        }
    }
}