package ir.instruction

import ir.LocalValue
import ir.Type
import ir.Value


abstract class ValueInstruction(protected val identifier: String, tp: Type, usages: Array<Value>):
    Instruction(tp, usages),
    LocalValue {

    override fun name(): String {
        return identifier
    }

    override fun toString(): String {
        return "%$identifier"
    }

    override fun type(): Type {
        return tp
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValueInstruction

        return identifier == other.identifier && tp == other.tp
    }

    override fun hashCode(): Int {
        return identifier.hashCode() + tp.hashCode()
    }
}