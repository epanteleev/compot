package preprocess

import tokenizer.*


class PreprocessorContext private constructor(private val macroReplacements: MutableMap<String, MacroReplacement>,
                                              private val macroDefinitions: MutableMap<String, MacroDefinition>,
                                              private val macroFunctions: MutableMap<String, MacroFunction>,
                                              private val predefinedMacroses: Map<String, PredefinedMacros>,
                                              private val headerHolder: HeaderHolder) {
    private var includeLevel = 0
    fun includeLevel(): Int = includeLevel

    fun enterInclude() {
        includeLevel += 1
    }
    fun exitInclude() {
        includeLevel -= 1
    }

    fun macroReplacements(): Map<String, MacroReplacement> = macroReplacements
    fun macroDefinitions(): Map<String, MacroDefinition> = macroDefinitions
    fun macroFunctions(): Map<String, MacroFunction> = macroFunctions

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
        macroReplacements.remove(name) ?: macroDefinitions.remove(name) ?: macroFunctions.remove(name)
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
        private val STDC_HOSTED  = MacroReplacement("__STDC_HOSTED__", tokenListOf(Numeric(1, OriginalPosition.UNKNOWN)))
        private val STDC_VERSION = MacroReplacement("__STDC_VERSION__", tokenListOf(Numeric(201112L, OriginalPosition.UNKNOWN)))

        // Implementation-defined macros
        private val PLATFORM = MacroReplacement("__x86_64__", tokenListOf(Numeric(1, OriginalPosition.UNKNOWN)))
        private val LP64     = MacroReplacement("__LP64__", tokenListOf(Numeric(1, OriginalPosition.UNKNOWN)))
        private val LINUX    = MacroReplacement("__linux__", tokenListOf(Numeric(1, OriginalPosition.UNKNOWN)))
        private val UNIX     = MacroReplacement("__unix__", tokenListOf(Numeric(1, OriginalPosition.UNKNOWN)))

        // TODO attributes to ignore
        private val __fortified_attr_access = MacroFunction("__fortified_attr_access", cTokenListOf(), tokenListOf())
        private val __attr_access = MacroFunction("__attr_access", cTokenListOf(), tokenListOf())

        private val predefined = hashMapOf(
            "__LINE__" to LINE,
            "__FILE__" to FILE,
            "__DATE__" to DATE,
            "__TIME__" to TIME,
            "__STDC__" to STDC,
        )

        fun empty(headerHolder: HeaderHolder): PreprocessorContext {
            val macroReplacements = hashMapOf(
                "__STDC_HOSTED__" to STDC_HOSTED,
                "__STDC_VERSION__" to STDC_VERSION,
                "__x86_64__" to PLATFORM,
                "__LP64__" to LP64,
                "__linux__" to LINUX,
                "__unix__" to UNIX
            )

            val macroFunctions = hashMapOf(
                "__fortified_attr_access" to __fortified_attr_access,
                "__attr_access" to __attr_access
            )

            return PreprocessorContext(macroReplacements, hashMapOf(), macroFunctions, predefined, headerHolder)
        }
    }
}