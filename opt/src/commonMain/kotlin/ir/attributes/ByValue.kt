package ir.attributes

class ByValue(val argumentIndex: Int): FunctionAttribute, ArgumentValueAttribute {
    override fun toString(): String = "!byval[$argumentIndex]"
}