package asm.x64

import ir.platform.x64.codegen.X64MacroAssembler

abstract class ObjModule(private val nameAssistant: NameAssistant): ObjBuilder {
    private val symbols = arrayListOf<AnyDirective>()
    private var arrayToAppend = symbols
    private val namedDirectives = hashMapOf<String, NamedDirective>()

    private fun newConstantName() = nameAssistant.nextConstant()

    private inline fun<reified T: NamedDirective> addSymbol(objSymbol: T): T {
        val has = namedDirectives.put(objSymbol.name, objSymbol)
        if (has != null) {
            throw IllegalArgumentException("symbol already exists: $objSymbol")
        }
        symbols.add(objSymbol)
        return objSymbol
    }

    protected fun findLabel(name: String): ObjLabel {
        val directive = namedDirectives[name] ?: throw IllegalArgumentException("label not found: $name")
        if (directive !is ObjLabel) {
            throw IllegalArgumentException("label not found: $name")
        }

        return directive
    }

    fun nameAssistant(): NameAssistant = nameAssistant

    fun global(name: String) {
        symbols.add(GlobalDirective(name))
    }

    fun section(section: SectionDirective) {
        symbols.add(section)
    }

    fun label(name: String, builder: ObjBuilder.() -> Unit): ObjLabel {
        val obj = addSymbol(ObjLabel(name))
        arrayToAppend = obj.anonymousDirective
        builder()
        arrayToAppend = symbols
        return obj
    }

    fun anonConstant(builder: ObjBuilder.() -> Unit): ObjLabel {
        val name = newConstantName()
        return label(name, builder)
    }

    fun function(name: String): X64MacroAssembler {
        val fn = X64MacroAssembler(name, nameAssistant.nextFunction())
        val obj = addSymbol(ObjLabel(name))
        obj.anonymousDirective.add(fn)
        return fn
    }

    override fun byte(value: Byte) {
        arrayToAppend.add(ByteDirective(value.toString()))
    }

    override fun short(value: Short) {
        arrayToAppend.add(ShortDirective(value.toString()))
    }

    override fun long(value: Int) {
        arrayToAppend.add(LongDirective(value.toString()))
    }

    override fun quad(value: Long) {
        arrayToAppend.add(QuadDirective(value.toString(), 0))
    }

    override fun quad(label: ObjLabel) {
        if (!namedDirectives.contains(label.name)) {
            throw IllegalArgumentException("label not found: $label")
        }

        arrayToAppend.add(QuadDirective(label.name, 0))
    }

    override fun quad(label: ObjLabel, offset: Int) {
        if (!namedDirectives.contains(label.name)) {
            throw IllegalArgumentException("label not found: $label")
        }

        arrayToAppend.add(QuadDirective(label.name, offset))
    }

    override fun string(value: String) {
        arrayToAppend.add(StringDirective(value))
    }

    override fun ascii(value: String) {
        arrayToAppend.add(AsciiDirective(value))
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for ((idx, symbol) in symbols.withIndex()) {
            builder.append(symbol)
            if (idx < symbols.size - 1) {
                builder.append("\n")
            }
        }
        builder.append("\n")
        return builder.toString()
    }
}