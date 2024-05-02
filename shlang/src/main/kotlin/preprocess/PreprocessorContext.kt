package preprocess


class PreprocessorContext private constructor(private val macroReplacements: MutableMap<String, MacroReplacement>,
                                              private val macroDefinitions: MutableSet<MacroDefinition>,
                                              private val macroFunctions: MutableMap<String, MacroFunction>,
                                              private val headerHolder: HeaderHolder) {
    fun define(macros: MacroReplacement) {
        macroReplacements[macros.name] = macros
    }

    fun define(macros: MacroDefinition) {
        macroDefinitions.add(macros)
    }

    fun define(macros: MacroFunction) {
        macroFunctions[macros.name] = macros
    }

    fun findMacroReplacement(name: String): MacroReplacement? {
        return macroReplacements[name]
    }

    fun findMacroFunction(name: String): MacroFunction? {
        return macroFunctions[name]
    }

    fun findMacros(name: String): Macros? {
        val def = MacroDefinition(name)
        if (macroDefinitions.contains(def)) {
            return def
        }
        return  macroReplacements[name]
    }

    fun hasMacroDefinition(name: String): Boolean {
        return macroDefinitions.contains(MacroDefinition(name))
    }

    fun undef(name: String) {
        macroReplacements.remove(name)
    }

    fun findHeader(name: String): Header? {
        return headerHolder.getHeader(name)
    }

    companion object {
        fun empty(headerHolder: HeaderHolder): PreprocessorContext {
            return PreprocessorContext(mutableMapOf(), mutableSetOf(), mutableMapOf(), headerHolder)
        }
    }
}