package ir.instruction

import ir.types.*
import ir.value.Value
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class Load private constructor(id: Identity, owner: Block, loadedType: PrimitiveType, ptr: Value):
    Unary(id, owner, loadedType, ptr) {
    override fun dump(): String {
        return "%${name()} = $NAME $tp ${operand()}"
    }

    override fun type(): PrimitiveType = tp

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "load"

        fun load(loadedType: PrimitiveType, operand: Value): InstBuilder<Load> = { id: Identity, owner: Block ->
            make(id, owner, loadedType, operand)
        }

        private fun make(id: Identity, owner: Block, loadedType: PrimitiveType, operand: Value): Load {
            val type = operand.type()
            require(isAppropriateTypes(type)) {
                "inconsistent types in '$id' type=${loadedType}, but operand=${operand}:$type"
            }

            return registerUser(Load(id, owner, loadedType, operand), operand)
        }

        private fun isAppropriateTypes(tp: Type): Boolean {
            return tp is PtrType
        }

        fun typeCheck(load: Load): Boolean {
            return isAppropriateTypes(load.operand().type())
        }
    }
}