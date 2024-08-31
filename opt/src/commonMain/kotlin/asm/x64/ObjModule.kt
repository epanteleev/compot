package asm.x64

import ir.platform.x64.codegen.MacroAssembler

abstract class ObjModule(private val nameAssistant: NameAssistant): ObjBuilder {
    private val symbols = arrayListOf<AnyDirective>()
    private var arrayToAppend = symbols
    private val namedDirectives = hashSetOf<NamedDirective>()

    private fun newConstantName() = nameAssistant.nextConstant()

    private inline fun<reified T: NamedDirective> addSymbol(objSymbol: T): T {
        val has = namedDirectives.add(objSymbol)
        if (!has) {
            throw IllegalArgumentException("symbol already exists: $objSymbol")
        }
        symbols.add(objSymbol)
        return objSymbol
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

    fun function(name: String): MacroAssembler {
        val fn = MacroAssembler(name, nameAssistant.nextFunction())
        val obj = addSymbol(ObjLabel(name))
        obj.anonymousDirective.add(fn)
        return fn
    }

    override fun byte(value: String) {
        arrayToAppend.add(ByteDirective(value))
    }

    override fun short(value: String) {
        arrayToAppend.add(ShortDirective(value))
    }

    override fun long(value: String) {
        arrayToAppend.add(LongDirective(value))
    }

    override fun quad(value: String) {
        arrayToAppend.add(QuadDirective(value))
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