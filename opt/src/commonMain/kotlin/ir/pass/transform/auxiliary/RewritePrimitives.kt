package ir.pass.transform.auxiliary

import ir.value.*
import common.intMapOf
import ir.instruction.*
import ir.module.block.*
import ir.module.FunctionData
import ir.types.PrimitiveType
import ir.instruction.matching.*
import ir.pass.analysis.dominance.DominatorTree
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

    protected fun findActualValue(bb: Block, resultType: Type, value: Value): Value {
        return findActualValueOrNull(bb, resultType, value) ?: UndefValue
    }

    protected fun findActualValueOrNull(bb: Block, resultType: Type, value: Value): Value? {
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

internal class RewritePrimitivesUtil private constructor(val cfg: FunctionData, private val uncompletedPhi: Map<Phi, Alloc>, dominatorTree: DominatorTree): AbstractRewritePrimitives(dominatorTree) {
    private val escapeState = cfg.analysis(EscapeAnalysisPassFabric)
    private val bbToMapValues = setupValueMap()

    init {
        val phisToRewrite = hashSetOf<Phi>()
        for (bb in dominatorTree) {
            rewriteValuesSetup(bb.bb, phisToRewrite)
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
    private fun laterRewrite(phisToRewrite: Set<Phi>) {
        for (phi in phisToRewrite) {
            phi.values { bb, v -> rename(bb, v.type(), v) }
        }
    }

    override fun valueMap(bb: Label, value: Value): Value? {
        return bbToMapValues[bb]!![value]
    }

    private fun rewriteValuesSetup(bb: Block, phisToRewrite: MutableSet<Phi>) {
        val valueMap = bbToMapValues[bb]!!
        for (inst in bb) {
            if (inst.isNoOperands()) {
                continue
            }

            if (inst.isa(store(alloc(primitive()), value(primitive()))) && escapeState.isNoEscape((inst as Store).pointer())) {
                val pointer = inst.pointer() as Alloc
                val actual = findActualValueOrNull(bb, pointer.allocatedType, inst.value()) ?: inst.value()
                valueMap[pointer] = actual
                continue
            }

            if (inst.isa(alloc(primitive())) && escapeState.isNoEscape(inst as Alloc)) {
                valueMap[inst] = UndefValue
                continue
            }

            if (inst.isa(load(primitive(), alloc(primitive()))) && escapeState.isNoEscape((inst as Load).operand())) {
                valueMap[inst] = findActualValue(bb, inst.type(), inst.operand())
                continue
            }

            if (inst is Phi) {
                val alloc = uncompletedPhi[inst]
                if (alloc != null) {
                    valueMap[alloc] = inst
                } else {
                    phisToRewrite.add(inst)
                }
                // Will rewrite it later.
                continue
            }

            inst.update { v -> rename(bb, v.type(), v) }
        }
    }

    companion object {
        fun run(cfg: FunctionData, uncompletedPhi: Map<Phi, Alloc>, dominatorTree: DominatorTree): RewritePrimitives {
            val ana = RewritePrimitivesUtil(cfg, uncompletedPhi, dominatorTree)
            return RewritePrimitives(ana.bbToMapValues, dominatorTree)
        }
    }
}

internal class RewritePrimitives internal constructor(private val info: Map<Block, Map<Value, Value>>, dominatorTree: DominatorTree): AbstractRewritePrimitives(dominatorTree) {
    override fun valueMap(bb: Label, value: Value): Value? = info[bb]!![value]
}