package ir.module

import ir.value.ArgumentValue
import ir.module.auxiliary.CopyCFG
import ir.module.auxiliary.DumpFunctionData
import ir.module.auxiliary.DumpSSAFunctionData
import ir.module.block.Block
import ir.pass.AnalysisPassCache
import ir.pass.analysis.traverse.iterator.*
import ir.pass.analysis.traverse.iterator.PostorderIterator
import ir.pass.common.AnalysisResult
import ir.pass.common.FunctionAnalysisPassFabric


class FunctionData private constructor(val prototype: FunctionPrototype, private var argumentValues: List<ArgumentValue>, val blocks: BasicBlocks) {
    private val cache = AnalysisPassCache()

    fun marker(): MutationMarker = blocks.marker()

    fun arguments(): List<ArgumentValue> = argumentValues

    fun arg(idx: Int): ArgumentValue = argumentValues[idx]

    fun copy(): FunctionData {
        return CopyCFG.copy(this)
    }

    fun name(): String {
        return prototype.name
    }

    fun cache(): AnalysisPassCache {
        return cache
    }

    inline fun<T> immutable(fn: () -> T): T {
        val begin = marker()
        val result = fn()
        val end = marker()
        if (begin != end) {
            throw IllegalStateException("Analysis pass has mutated the function data")
        }
        return result
    }

    inline fun <reified T: AnalysisResult, reified U: FunctionAnalysisPassFabric<T>> analysis(analysisType: U, useCache: Boolean = true): T = immutable {
        if (!useCache) {
            return@immutable analysisType.create(this)
        }
        val cached = cache().get(analysisType, marker())
        if (cached != null) {
            return@immutable cached
        }

        return@immutable cache().put(analysisType, analysisType.create(this))
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

    override fun toString(): String {
        return DumpSSAFunctionData(this).toString()
    }

    companion object {
        fun create(prototype: FunctionPrototype, argumentValues: List<ArgumentValue>): FunctionData {
            val basicBlocks = BasicBlocks.create()
            return FunctionData(prototype, argumentValues, basicBlocks)
        }
    }
}