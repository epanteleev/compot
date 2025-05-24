package parser.nodes

import tokenizer.Position


sealed class InitializerListEntry {
    abstract fun begin(): Position
    abstract fun initializer(): Initializer
}

class SingleInitializer(val expr: Initializer) : InitializerListEntry() {
    override fun begin(): Position = expr.begin()
    override fun initializer(): Initializer = expr
}

class DesignationInitializer(val designation: Designation, val initializer: Initializer) : InitializerListEntry() {
    override fun begin(): Position = designation.begin()
    override fun initializer(): Initializer = initializer
}

class InitializerList(private val begin: Position, val initializers: List<InitializerListEntry>) {
    fun begin(): Position = begin
    fun length(): Int = initializers.size
}