package ir.instruction

import ir.Value
import ir.instruction.utils.Visitor
import ir.types.*

class Load private constructor(name: String, ptr: Value):
    ValueInstruction(name, (ptr.type() as PointerType).dereference(), arrayOf(ptr)) {
    override fun dump(): String {
        return "%$identifier = load $tp ${operand()}"
    }

    fun operand(): Value {
        assert(operands.size == 1) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun copy(newUsages: List<Value>): Load {
        assert(newUsages.size == 1) {
            "should be, but newUsages=$newUsages"
        }

        return make(identifier, newUsages[0])
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        fun make(name: String, operand: Value): Load {
            val type = operand.type()
            require(isAppropriateTypes(type)) {
                "should be pointer to primitive type, but operand.type=$type"
            }

            return registerUser(Load(name, operand), operand)
        }

        private fun isAppropriateTypes(tp: Type): Boolean {
            return tp is PointerType && tp.dereference() is PrimitiveType
        }

        fun isCorrect(load: Load): Boolean {
            return isAppropriateTypes(load.operand().type())
        }
    }
}