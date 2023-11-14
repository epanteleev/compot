package ir.instruction

import ir.Value
import ir.module.block.Block

class ReturnValue(value: Value): Return(arrayOf(value)) {
    override fun dump(): String {
        return "ret ${value().type()} ${value()}"
    }

    fun value(): Value {
        assert(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun copy(usages: List<Value>, newTargets: Array<Block>): Return {
        return ReturnValue(usages[0])
    }

    override fun copy(newUsages: List<Value>): Return {
        return ReturnValue(newUsages[0])
    }
}