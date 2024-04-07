package ir.pass.transform.utils

import ir.*
import ir.dominance.DominatorTree
import ir.instruction.*
import ir.module.block.*
import ir.module.BasicBlocks
import ir.pass.isLocalVariable
import ir.pass.transform.Mem2RegException
import ir.types.Type


class ReachingDefinitionAnalysis private constructor(cfg: BasicBlocks, private val dominatorTree: DominatorTree) {
    private val bbToMapValues = initialize(cfg)

    init {
        for (bb in cfg.preorder()) {
            rewriteValuesSetup(bb)
        }
    }

    private fun rename(bb: Block, oldValue: Value): Value {
        if (oldValue !is ValueInstruction) {
            return oldValue
        }

        val newValue = findActualValueOrNull(bb, oldValue)?: return oldValue
        return convertOrSkip(oldValue.type(), newValue)
    }

    private fun convertOrSkip(type: Type, value: Value): Value {
        if (value !is Constant) {
            return value
        }

        return Constant.from(type, value)
    }

    private fun rewriteValuesSetup(bb: Block) {
        val instructions = bb.instructions()
        val valueMap = bbToMapValues[bb]!!
        for (index in instructions.indices) {
            val instruction = instructions[index]
            if (instruction is Branch || instruction is ReturnVoid) {
                continue
            }

            if (instruction is Store && instruction.isLocalVariable()) {
                val actual = findActualValueOrNull(bb, instruction.value())
                val pointer = instruction.pointer()
                if (actual != null) {
                    valueMap[pointer] = actual
                } else {
                    valueMap[pointer] = instruction.value()
                }

                continue
            }

            if (instruction is Alloc && instruction.isLocalVariable()) {
                valueMap[instruction] = Value.UNDEF
                continue
            }

            if (instruction is Load && instruction.isLocalVariable()) {
                val actual = findActualValue(bb, instruction.operand())
                valueMap[instruction] = actual
                continue
            }

            if (instruction is Phi) {
                // Note: all used values are equal in uncompleted phi instruction.
                // Will take only first value.
                valueMap[instruction.operands().first()] = instruction
                continue
            }

            instruction.update { v -> rename(bb, v) }
        }
    }

    private fun initialize(cfg: BasicBlocks): MutableMap<Block, MutableMap<Value, Value>> {
        val bbToMapValues = hashMapOf<Block, MutableMap<Value, Value>>()
        for (bb in cfg) {
            bbToMapValues[bb] = hashMapOf()
        }

        return bbToMapValues
    }

    private fun findActualValue(bb: Label, value: Value): Value {
        return findActualValueOrNull(bb, value)
            ?: throw Mem2RegException("cannot find: basicBlock=$bb, value=$value")
    }

    private fun findActualValueOrNull(bb: Label, value: Value): Value? {
        for (d in dominatorTree.dominators(bb)) {
            val newV = bbToMapValues[d]!![value]
            if (newV != null) {
                return newV
            }
        }

        return null
    }

    companion object {
        fun run(cfg: BasicBlocks, dominatorTree: DominatorTree): ReachingDefinition {
            val ana = ReachingDefinitionAnalysis(cfg, dominatorTree)
            return ReachingDefinition(ana.bbToMapValues, dominatorTree)
        }
    }
}

class ReachingDefinition(private val info: MutableMap<Block, MutableMap<Value, Value>>, private val dominatorTree: DominatorTree) {
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

    private fun findActualValueOrNull(bb: Label, value: Value): Value? {
        for (d in dominatorTree.dominators(bb)) {
            val newV = info[d]!![value]
            if (newV != null) {
                return newV
            }
        }

        return null
    }

    fun rename(bb: Block, oldValue: ValueInstruction): Value {
        return findActualValueOrNull(bb, oldValue) ?: oldValue
    }
}