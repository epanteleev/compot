package preprocess


class PreprocessorContext private constructor(private val macroReplacements: MutableMap<String, MacroReplacement>,
                                              private val macroDefinitions: MutableSet<MacroDefinition>,
                                              private val headerHolder: HeaderHolder) {
    fun define(macros: MacroReplacement) {
        macroReplacements[macros.name] = macros
    }

    fun define(macros: MacroDefinition) {
        macroDefinitions.add(macros)
    }

    fun findMacroReplacement(name: String): MacroReplacement? {
        return macroReplacements[name]
    }

    fun findMacros(name: String): AnyMacros? {
        val def = MacroDefinition(name)
        if (macroDefinitions.contains(def)) {
            return def
        }
        return  macroReplacements[name]
    }

    fun undef(name: String) {
        macroReplacements.remove(name)
    }

    fun findHeader(name: String): Header? {
        return headerHolder.getHeader(name)
    }

    companion object {
        fun empty(headerHolder: HeaderHolder): PreprocessorContext {
            return PreprocessorContext(mutableMapOf(), mutableSetOf(), headerHolder)
        }
    }
}