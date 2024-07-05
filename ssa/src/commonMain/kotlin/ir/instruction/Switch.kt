package ir.instruction

import common.assertion
import common.forEachWith
import ir.value.Value
import ir.instruction.Bitcast.Companion
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.IntegerType


class Switch private constructor(id: Identity, owner: Block,
                                 private val value: Value,
                                 private val default: Block,
                                 private val table: Array<Value>,
                                 targets: Array<Block>):
    TerminateInstruction(id, owner, arrayOf(value) + table, arrayOf(default) + targets) {
    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("switch ")
            .append(value.type().toString())
            .append(' ')
            .append(value.toString())
            .append(", label ")
            .append(default)

        builder.append(" [")
        table.forEachWith(targets) { value, bb, i ->
            builder.append("$value: $bb")
            if (i < table.size - 1) {
                builder.append(", ")
            }
        }
        builder.append(']')
        return builder.toString()
    }

    fun value() = value
    fun default(): Block = default
    fun table() = table

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        fun make(id: Identity, owner: Block, value: Value, default: Block, table: Array<Value>, targets: Array<Block>): Switch {
            require(isAppropriateType(value, table)) {
                "inconsistent types in '$id': value='${value}:${value.type()}', table='${table.joinToString { it.type().toString() }}'"
            }

            return Switch(id, owner, value, default, table, targets)
        }

        private fun isAppropriateType(value: Value, table: Array<Value>): Boolean {
            if (value.type() !is IntegerType) {
                return false
            }

            for (v in table) {
                if (v.type() !is IntegerType) {
                    return false
                }
            }

            return true
        }

        fun typeCheck(switch: Switch): Boolean {
            return true
        }
    }
}