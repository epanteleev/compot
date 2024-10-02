package ir.pass.transform.utils

import common.intMapOf
import ir.value.*
import ir.types.Type
import ir.instruction.*
import ir.instruction.matching.alloc
import ir.instruction.matching.load
import ir.instruction.matching.nop
import ir.instruction.matching.primitive
import ir.instruction.matching.store
import ir.instruction.matching.value
import ir.module.block.*
import ir.pass.analysis.dominance.DominatorTree
import ir.module.FunctionData
import ir.pass.analysis.EscapeAnalysisPassFabric
import ir.pass.analysis.traverse.PreOrderFabric
import ir.types.NonTrivialType


abstract class AbstractRewritePrimitives(private val dominatorTree: DominatorTree) {
    protected fun rename(bb: Block, oldValue: Value): Value {
        return tryRename(bb, oldValue, oldValue.type())?: oldValue
    }

    fun tryRename(bb: Block, oldValue: Value, expectedType: Type): Value? {
        if (oldValue !is LocalValue) {
            return oldValue
        }

        val newValue = findActualValueOrNull(bb, oldValue)
            ?: return null
        return RewritePrimitivesUtil.convertOrSkip(expectedType, newValue)
    }

    protected fun findActualValue(bb: Label, value: Value): Value {
        return findActualValueOrNull(bb, value) ?: let {
            println("Warning: use uninitialized value: bb=$bb, value=$value")//TODO remove it in future
            Value.UNDEF
        }
    }

    protected fun findActualValueOrNull(bb: Label, value: Value): Value? { //TODO Duplicate code
        for (d in dominatorTree.dominators(bb)) {
            val newV = valueMap()[d]!![value]
            if (newV != null) {
                return newV
            }
        }

        return null
    }

    abstract fun valueMap(): Map<Block, Map<Value, Value>>
}

class RewritePrimitivesUtil private constructor(val cfg: FunctionData, val insertedPhis: Set<Phi>, dominatorTree: DominatorTree): AbstractRewritePrimitives(dominatorTree) {
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
            phi.owner().updateDF(phi) { bb, v -> rename(bb, v) }
        }
    }

    override fun valueMap(): Map<Block, Map<Value, Value>> {
        return bbToMapValues
    }

    private fun rewriteValuesSetup(bb: Block, phisToRewrite: MutableSet<Phi>) {
        val valueMap = bbToMapValues[bb]!!
        for (instruction in bb) {
            if (instruction.emptyOperands()) {
                continue
            }

            if (store(nop(), value(primitive())) (instruction) && escapeState.isNoEscape((instruction as Store).pointer())) {
                val actual = findActualValueOrNull(bb, instruction.value())
                val pointer = instruction.pointer()
                if (actual != null) {
                    valueMap[pointer] = actual
                } else {
                    valueMap[pointer] = instruction.value()
                }

                continue
            }

            if (alloc(primitive()) (instruction) && escapeState.isNoEscape(instruction as Alloc)) {
                valueMap[instruction] = Value.UNDEF
                continue
            }

            if (load(primitive(), nop()) (instruction) && escapeState.isNoEscape((instruction as Load).operand())) {
                val actual = findActualValue(bb, instruction.operand())
                valueMap[instruction] = actual
                continue
            }

            if (instruction is Phi) {
                if (!insertedPhis.contains(instruction)) {
                    // Phi instruction is not inserted by mem2reg pass.
                    // Will rewrite it later.
                    phisToRewrite.add(instruction)
                    continue
                }
                // Note: all used values are equal in uncompleted phi instruction.
                // Will take only first value.
                valueMap[instruction.operand(0)] = instruction
                continue
            }

            bb.updateDF(instruction) { v -> rename(bb, v) }
        }
    }

    companion object {
        internal fun convertOrSkip(type: Type, value: Value): Value {
            if (value !is PrimitiveConstant) {
                return value
            }

            return PrimitiveConstant.from(type as NonTrivialType, value)
        }

        fun run(cfg: FunctionData, insertedPhis: Set<Phi>, dominatorTree: DominatorTree): RewritePrimitives {
            val ana = RewritePrimitivesUtil(cfg, insertedPhis, dominatorTree)
            return RewritePrimitives(ana.bbToMapValues, dominatorTree)
        }
    }
}

class RewritePrimitives internal constructor(private val info: Map<Block, Map<Value, Value>>, dominatorTree: DominatorTree): AbstractRewritePrimitives(dominatorTree) {
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

    override fun valueMap(): Map<Block, Map<Value, Value>> = info
}