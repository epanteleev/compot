package ir.pass.transform.utils

import ir.value.*
import common.intMapOf
import ir.instruction.*
import ir.module.block.*
import ir.module.FunctionData
import ir.types.PrimitiveType
import ir.instruction.matching.*
import ir.pass.analysis.dominance.DominatorTree
import ir.pass.analysis.traverse.PreOrderFabric
import ir.pass.analysis.EscapeAnalysisPassFabric
import ir.types.Type
import ir.types.asType
import ir.value.constant.PrimitiveConstant
import ir.value.constant.UndefValue


sealed class AbstractRewritePrimitives(private val dominatorTree: DominatorTree) {
    protected fun rename(bb: Block, resultType: Type, oldValue: Value): Value {
        val valueType = oldValue.type()
        if (valueType !is PrimitiveType) {
            return oldValue
        }

        return tryRename(bb, resultType, oldValue)?: oldValue
    }

    fun tryRename(bb: Block, resultType: Type, oldValue: Value): Value? {
        if (oldValue !is LocalValue) {
            return oldValue
        }

        return findActualValueOrNull(bb, resultType, oldValue)
    }

    protected fun findActualValue(bb: Label, resultType: Type, value: Value): Value {
        return findActualValueOrNull(bb, resultType, value) ?: let {
            println("Warning: use uninitialized value: bb=$bb, value=$value")//TODO remove it in future
            UndefValue
        }
    }

    protected fun findActualValueOrNull(bb: Label, resultType: Type, value: Value): Value? {
        for (d in dominatorTree.dominators(bb)) {
            val newV = valueMap(d, value) ?: continue
            return when (newV) {
                is PrimitiveConstant -> newV.convertTo(resultType.asType())
                else -> newV
            }
        }

        return null
    }

    abstract fun valueMap(bb: Label, value: Value): Value?
}

internal class RewritePrimitivesUtil private constructor(val cfg: FunctionData, dominatorTree: DominatorTree): AbstractRewritePrimitives(dominatorTree) {
    private val escapeState = cfg.analysis(EscapeAnalysisPassFabric)
    private val bbToMapValues = setupValueMap()

    init {
        val phisToRewrite = hashSetOf<Phi>()
        for (bb in cfg.analysis(PreOrderFabric)) {
            rewriteValuesSetup(bb, phisToRewrite)
        }

        laterRewrite(phisToRewrite)
    }

    private fun setupValueMap(): MutableMap<Block, MutableMap<Value, Value>> {
        val bbToMapValues = intMapOf<Block, MutableMap<Value, Value>>(cfg.size()) { it: Label -> it.index }
        for (bb in cfg) {
            bbToMapValues[bb] = hashMapOf()
        }

        return bbToMapValues
    }

    // Rewrite phi instructions that was not inserted by mem2reg pass.
    private fun laterRewrite(phisToRewrite: MutableSet<Phi>) {
        for (phi in phisToRewrite) {
            phi.owner().updateDF(phi) { bb, v -> rename(bb, v.type(), v) }
        }
    }

    override fun valueMap(bb: Label, value: Value): Value? {
        return bbToMapValues[bb]!![value]
    }

    private fun rewriteValuesSetup(bb: Block, phisToRewrite: MutableSet<Phi>) {
        val valueMap = bbToMapValues[bb]!!
        for (instruction in bb) {
            if (instruction.isNoOperands()) {
                continue
            }

            if (instruction.isa(store(alloc(primitive()), value(primitive()))) && escapeState.isNoEscape((instruction as Store).pointer())) {
                val pointer = instruction.pointer() as Alloc
                val actual = findActualValueOrNull(bb, pointer.allocatedType, instruction.value())
                if (actual != null) {
                    valueMap[pointer] = actual
                } else {
                    valueMap[pointer] = instruction.value()
                }

                continue
            }

            if (instruction.isa(alloc(primitive())) && escapeState.isNoEscape(instruction as Alloc)) {
                valueMap[instruction] = UndefValue
                continue
            }

            if (instruction.isa(load(primitive(), alloc(primitive()))) && escapeState.isNoEscape((instruction as Load).operand())) {
                val actual = findActualValue(bb, instruction.type(), instruction.operand())
                valueMap[instruction] = actual
                continue
            }

            if (instruction is Phi) {
                // Will rewrite it later.
                phisToRewrite.add(instruction)
                continue
            }

            if (instruction is UncompletedPhi) {
                valueMap[instruction.value()] = instruction
                continue
            }

            bb.updateDF(instruction) { v -> rename(bb, v.type(), v) }
        }
    }

    companion object {
        fun run(cfg: FunctionData, dominatorTree: DominatorTree): RewritePrimitives {
            val ana = RewritePrimitivesUtil(cfg, dominatorTree)
            return RewritePrimitives(ana.bbToMapValues, dominatorTree)
        }
    }
}

internal class RewritePrimitives internal constructor(private val info: Map<Block, Map<Value, Value>>, dominatorTree: DominatorTree): AbstractRewritePrimitives(dominatorTree) {
    override fun toString(): String {
        val builder = StringBuilder()
        for ((bb, valueMap) in info) {
            builder.append("----- bb=$bb -----\n")
            for ((from, to) in valueMap) {
                builder.append("$from -> $to\n")
            }
        }

        return builder.toString()
    }

    override fun valueMap(bb: Label, value: Value): Value? = info[bb]!![value]
}