package ir.pass.transform.utils

import ir.*
import ir.instruction.*
import ir.module.block.*
import ir.module.BasicBlocks
import ir.types.PrimitiveType
import ir.pass.transform.Mem2RegException
import ir.pass.transform.utils.Utils.isLocalVariable


internal object Utils {
    fun Alloc.isLocalVariable(): Boolean {
        return allocatedType is PrimitiveType
    }

    fun Load.isLocalVariable(): Boolean {
        val operand = operand()
        if (operand is Generate) {
            return true
        }
        if (operand is GlobalValue) {
            return false
        }
        return operand is Alloc && operand.allocatedType is PrimitiveType
    }

    fun Store.isLocalVariable(): Boolean {
        val pointer = pointer()
        return pointer is Load || pointer is Alloc || pointer is Generate
    }
}

internal class RewriteAssistant(cfg: BasicBlocks, private val dominatorTree: DominatorTree) {
    private val bbToMapValues = initialize(cfg)

    init {
        for (bb in cfg.preorder()) {
            rewriteValuesSetup(bb)
        }
    }

    fun rename(bb: Block, oldValue: Value): Value {
        return if (oldValue is ValueInstruction) {
            findActualValueOrNull(bb, oldValue) ?: oldValue
        } else {
            oldValue
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for ((bb, valueMap) in bbToMapValues) {
            builder.append("----- bb=$bb -----\n")
            for ((from, to) in valueMap) {
                builder.append("$from -> $to\n")
            }
        }

        return builder.toString()
    }

    private fun rewriteValuesSetup(bb: Block) {
        val instructions = bb.instructions()
        for (index in instructions.indices) {
            val instruction = instructions[index]
            if (instruction is Branch) {
                continue
            }

            if (instruction is Store && instruction.isLocalVariable()) {
                val actual = findActualValueOrNull(bb, instruction.value())
                if (actual != null) {
                    addValues(bb, instruction.pointer(), actual)
                } else {
                    addValues(bb, instruction.pointer(), instruction.value())
                }

                continue
            }

            if (instruction is Alloc && instruction.isLocalVariable()) {
                addValues(bb, instruction, Value.UNDEF)
                continue
            }

            if (instruction is Load && instruction.isLocalVariable()) {
                val actual = findActualValue(bb, instruction.operand())
                addValues(bb, instruction, actual)
                continue
            }

            if (instruction is Phi) {
                // Note: all used values are equal in uncompleted phi instruction.
                // Will take only first value.
                addValues(bb, instruction.operands().first(), instruction)
                continue
            }

            instruction.update(instruction.operands().mapTo(arrayListOf()) { v -> rename(bb, v) } )
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

    private fun addValues(bb: Block, from: Value, to: Value) {
        bbToMapValues[bb]!![from] = to
    }
}