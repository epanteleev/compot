package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor
import ir.types.BooleanType
import ir.types.PrimitiveType
import ir.types.Type

class Select private constructor(name: String, ty: Type, cond: Value, onTrue: Value, onFalse: Value) :
    ValueInstruction(name, ty, arrayOf(cond, onTrue, onFalse)) {
    override fun dump(): String {
        return "%$identifier = select $tp ${condition()} ${onTrue()}, ${onFalse()}"
    }

    fun condition(): Value {
        assert(operands.size == 3) {
            "size should be 3 in $this instruction"
        }

        return operands[0]
    }

    fun onTrue(): Value {
        assert(operands.size == 3) {
            "size should be 3 in $this instruction"
        }

        return operands[1]
    }

    fun onFalse(): Value {
        assert(operands.size == 3) {
            "size should be 3 in $this instruction"
        }

        return operands[2]
    }

    override fun copy(newUsages: List<Value>): Select {
        assert(newUsages.size == 3) {
            "should be, but newUsages=$newUsages"
        }

        return make(identifier, tp, newUsages[0], newUsages[1], newUsages[2])
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        fun make(name: String, ty: Type, cond: Value, onTrue: Value, onFalse: Value): Select {
            val onTrueType = onTrue.type()
            val onFalseType = onFalse.type()
            val condType = cond.type()
            require(isAppropriateType(ty, condType, onTrueType, onFalseType)) {
                "inconsistent types: ty=$ty, condType=$condType, "
            }

            return registerUser(Select(name, ty, cond, onTrue, onFalse), cond, onTrue, onFalse)
        }

        private fun isAppropriateType(ty: Type, condType: Type, onTrueType: Type, onFalseType: Type): Boolean {
            if (condType !is BooleanType) {
                return false
            }

            return (ty == onFalseType) && (ty == onTrueType) && (ty is PrimitiveType)
        }

        fun isCorrect(select: Select): Boolean {
            return isAppropriateType(select.type(),
                select.condition().type(),
                select.onTrue().type(),
                select.onFalse().type())
        }
    }
}