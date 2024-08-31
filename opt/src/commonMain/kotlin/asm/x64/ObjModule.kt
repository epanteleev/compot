package asm.x64

class ObjModule(val nameAssistant: NameAssistant) {
    private val symbols = arrayListOf<AnyDirective>()
    private val namedDirectives = hashMapOf<NamedDirective, MutableList<AnonymousDirective>>()

    private inline fun<reified T: NamedDirective> addSymbol(objSymbol: T): T {
        val has = namedDirectives.put(objSymbol, arrayListOf())
        if (has != null) {
            throw IllegalArgumentException("symbol already exists: $objSymbol")
        }
        symbols.add(objSymbol)
        return objSymbol
    }

    fun section(section: SectionDirective) {
        symbols.add(section)
    }

    fun label(name: String): ObjLabel {
        return addSymbol(ObjLabel(name))
    }

    fun function(name: String, asm: Assembler): ObjLabel {
        val label = addSymbol(ObjLabel(name))
        namedDirectives[label] = arrayListOf(asm)
        return label
    }
}