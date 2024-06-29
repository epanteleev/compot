package preprocess

import tokenizer.*


class PreprocessorContext private constructor(private val macroReplacements: MutableMap<String, MacroReplacement>,
                                              private val macroDefinitions: MutableMap<String, MacroDefinition>,
                                              private val macroFunctions: MutableMap<String, MacroFunction>,
                                              private val predefinedMacroses: MutableMap<String, PredefinedMacros>,
                                              private val headerHolder: HeaderHolder) {
    private var includeLevel = 0
    fun includeLevel(): Int = includeLevel

    fun enterInclude() {
        includeLevel += 1
    }
    fun exitInclude() {
        includeLevel -= 1
    }

    fun define(macros: MacroReplacement) {
        macroReplacements[macros.name] = macros
    }

    fun define(macros: MacroDefinition) {
        macroDefinitions[macros.name] = macros
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
        val macroDefinition = findMacroDefinition(name)
        if (macroDefinition != null) {
            return macroDefinition
        }

        val replacement = macroReplacements[name]
        if (replacement != null) {
            return replacement
        }

        val macroFunction = macroFunctions[name]
        if (macroFunction != null) {
            return macroFunction
        }

        return predefinedMacroses[name]
    }

    fun hasMacroDefinition(name: String): Boolean {
        return findMacroDefinition(name) != null
    }

    fun findMacroDefinition(name: String): MacroDefinition? {
        return macroDefinitions[name]
    }

    fun undef(name: String) {
        macroReplacements.remove(name)
        macroDefinitions.remove(name)
        macroFunctions.remove(name)
    }

    fun findHeader(name: String, includeType: HeaderType): Header? {
        return headerHolder.getHeader(name, includeType)
    }

    companion object {
        // 6.10.8.1 Mandatory macros
        private val LINE = PredefinedMacros("__LINE__") { tokenListOf(Numeric(it.line(), it)) }
        private val FILE = PredefinedMacros("__FILE__") { tokenListOf(StringLiteral.quote(it.filename(), it)) }
        private val DATE = PredefinedMacros("__DATE__") { tokenListOf(StringLiteral.quote("June  6 666", it)) }
        private val TIME = PredefinedMacros("__TIME__") { tokenListOf(StringLiteral.quote("66:66:66", it)) }
        private val STDC = PredefinedMacros("__STDC__") { tokenListOf(Numeric(1, it)) }
        private val STDC_HOSTED = PredefinedMacros("__STDC_HOSTED__") { tokenListOf(Numeric(1, it)) }
        private val STDC_VERSION = PredefinedMacros("__STDC_VERSION__") { tokenListOf(Numeric(201112L, it)) }

        // Implementation-defined macros
        private val PLATFORM = PredefinedMacros("__x86_64__") { tokenListOf(Numeric(1, it)) }
        private val LP64 = PredefinedMacros("__LP64__") { tokenListOf(Numeric(1, it)) }
        private val LINUX = PredefinedMacros("__linux__") { tokenListOf(Numeric(1, it)) }
        private val UNIX = PredefinedMacros("__unix__") { tokenListOf(Numeric(1, it)) }

        private val predefined = hashMapOf(
            "__LINE__" to LINE,
            "__FILE__" to FILE,
            "__DATE__" to DATE,
            "__TIME__" to TIME,
            "__STDC__" to STDC,
            "__STDC_HOSTED__" to STDC_HOSTED,
            "__STDC_VERSION__" to STDC_VERSION,
            "__x86_64__" to PLATFORM,
            "__LP64__" to LP64,
            "__linux__" to LINUX,
            "__unix__" to UNIX
        )

        fun empty(headerHolder: HeaderHolder): PreprocessorContext {
            return PreprocessorContext(hashMapOf(), hashMapOf(), hashMapOf(), predefined, headerHolder)
        }
    }
}