package ir.instruction

import common.assertion
import common.forEachWith
import ir.value.Value
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.IntegerType
import ir.value.constant.IntegerConstant


class Switch private constructor(id: Identity, owner: Block,
                                 value: Value,
                                 default: Block,
                                 private val table: Array<IntegerConstant>,
                                 targets: Array<Block>):
    TerminateInstruction(id, owner, arrayOf(value), targets + arrayOf(default)) {
    override fun dump(): String = buildString {
        append("$NAME ")
        append(value().type().toString())
        append(' ')
        append(value().toString())
        append(", label ")
        append(default())

        append(" [")
        table.forEachWith(targets) { value, bb, i ->
            if (bb == default()) {
                return@forEachWith
            }

            append("$value: $bb")
            if (i < table.size - 1) {
                append(", ")
            }
        }
        append(']')
    }

    fun value(): Value {
        assertion(operands.size == 1) {
            "inconsistent operands in '$id': ${operands.joinToString()}"
        }

        return operands[0]
    }

    fun jumps(): List<Block> = targets.toList().take(table.size) //TODO

    fun default(): Block = targets.last()

    fun table(): Array<IntegerConstant> = table

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "switch"

        fun switch(value: Value, default: Block, table: Array<IntegerConstant>, targets: Array<Block>): InstBuilder<Switch> = {
            id: Identity, owner: Block -> make(id, owner, value, default, table, targets)
        }

        private fun make(id: Identity, owner: Block, value: Value, default: Block, table: Array<IntegerConstant>, targets: Array<Block>): Switch {
            require(isAppropriateType(value)) {
                "inconsistent types in '$id': value='${value}:${value.type()}', table='${table.joinToString { it.type().toString() }}'"
            }

            return registerUser(Switch(id, owner, value, default, table, targets), value)
        }

        private fun isAppropriateType(value: Value): Boolean {
            return value.type() is IntegerType
        }

        fun typeCheck(switch: Switch): Boolean {
            return true
        }
    }
}