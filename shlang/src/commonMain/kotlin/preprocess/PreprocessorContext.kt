package preprocess

import tokenizer.Numeric
import tokenizer.StringLiteral


class PreprocessorContext private constructor(private val macroReplacements: MutableMap<String, MacroReplacement>,
                                              private val macroDefinitions: MutableSet<MacroDefinition>,
                                              private val macroFunctions: MutableMap<String, MacroFunction>,
                                              private val predefinedMacroses: MutableMap<String, PredefinedMacros>,
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

    fun findPredefinedMacros(name: String): PredefinedMacros? {
        return predefinedMacroses[name]
    }

    fun findMacroFunction(name: String): MacroFunction? {
        return macroFunctions[name]
    }

    fun findMacros(name: String): Macros? {
        val definition = MacroDefinition(name)
        if (macroDefinitions.contains(definition)) {
            return definition
        }
        val replacement = macroReplacements[name]
        if (replacement != null) {
            return replacement
        }

        return predefinedMacroses[name]
    }

    fun hasMacroDefinition(name: String): Boolean {
        return macroDefinitions.contains(MacroDefinition(name))
    }

    fun undef(name: String) {
        macroReplacements.remove(name)
    }

    fun findHeader(name: String, includeType: HeaderType): Header? {
        return headerHolder.getHeader(name, includeType)
    }

    companion object {
        // 6.10.8.1 Mandatory macros
        private val LINE = PredefinedMacros("__LINE__") { Numeric(it.line(), it) }
        private val FILE = PredefinedMacros("__FILE__") { StringLiteral.quote(it.filename(), it) }
        private val DATE = PredefinedMacros("__DATE__") { StringLiteral.quote("June  6 666", it)}
        private val TIME = PredefinedMacros("__TIME__") { StringLiteral.quote("66:66:66", it) }
        private val STDC = PredefinedMacros("__STDC__") { Numeric(1, it) }
        private val STDC_HOSTED = PredefinedMacros("__STDC_HOSTED__") { Numeric(1, it) }
        private val STDC_VERSION = PredefinedMacros("__STDC_VERSION__") { Numeric(201112L, it) }

        // Implementation-defined macros
        private val platform = PredefinedMacros("__x86_64__") { Numeric(1, it) }
        private val lp64 = PredefinedMacros("__LP64__") { Numeric(1, it) }

        private val predefined = mutableMapOf(
            "__LINE__" to LINE,
            "__FILE__" to FILE,
            "__DATE__" to DATE,
            "__TIME__" to TIME,
            "__STDC__" to STDC,
            "__STDC_HOSTED__" to STDC_HOSTED,
            "__STDC_VERSION__" to STDC_VERSION,
            "__x86_64__" to platform
        )

        fun empty(headerHolder: HeaderHolder): PreprocessorContext {
            return PreprocessorContext(mutableMapOf(), mutableSetOf(), mutableMapOf(), predefined, headerHolder)
        }
    }
}