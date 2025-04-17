package preprocess

import tokenizer.*
import tokenizer.tokens.*
import preprocess.macros.*
import types.INT
import types.LONG


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

    fun define(macros: MacroReplacement): MacroReplacement? {
        return macroReplacements.put(macros.name, macros)
    }

    fun define(macros: MacroDefinition): MacroDefinition? {
        return macroDefinitions.put(macros.name, macros)
    }

    fun define(macros: MacroFunction): MacroFunction? {
        return macroFunctions.put(macros.name, macros)
    }

    fun findPredefinedMacros(name: String): PredefinedMacros? {
        return predefinedMacroses[name]
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

    private fun findMacroDefinition(name: String): MacroDefinition? {
        return macroDefinitions[name]
    }

    fun undef(name: String) {
        macroReplacements.remove(name) ?: macroDefinitions.remove(name) ?: macroFunctions.remove(name)
    }

    fun findHeader(name: String, includeType: HeaderType): Header? {
        return headerHolder.getHeader(name, includeType)
    }

    fun addPragmaOnce(name: String) {
        headerHolder.addPragmaOnce(name)
    }

    fun isPragmaOnce(name: String): Boolean {
        return headerHolder.isPragmaOnce(name)
    }

    companion object {
        // 6.10.8.1 Mandatory macros
        private val LINE = PredefinedMacros("__LINE__") { tokenListOf(PPNumber(it.line(), INT, it)) }
        private val FILE = PredefinedMacros("__FILE__") { tokenListOf(StringLiteral(it.filename(), it)) }
        private val DATE = PredefinedMacros("__DATE__") { tokenListOf(StringLiteral("June  6 666", it)) }
        private val TIME = PredefinedMacros("__TIME__") { tokenListOf(StringLiteral("66:66:66", it)) }
        private val STDC = PredefinedMacros("__STDC__") { tokenListOf(PPNumber(1, INT, it)) }
        private val STDC_HOSTED  =
            MacroReplacement("__STDC_HOSTED__", tokenListOf(PPNumber(1, INT, Position.UNKNOWN)))
        private val STDC_VERSION =
            MacroReplacement("__STDC_VERSION__", tokenListOf(PPNumber(201112L, LONG, Position.UNKNOWN)))

        // 3.7.2 Common Predefined Macros
        // https://gcc.gnu.org/onlinedocs/cpp/Common-Predefined-Macros.html
        private val __SIZEOF_POINTER__ = MacroReplacement("__SIZEOF_POINTER__", tokenListOf(PPNumber(8, INT, Position.UNKNOWN)))
        private val __SIZEOF_LONG__    = MacroReplacement("__SIZEOF_LONG__", tokenListOf(PPNumber(8, INT, Position.UNKNOWN)))
        private val __SIZEOF_INT__     = MacroReplacement("__SIZEOF_INT__", tokenListOf(PPNumber(4, INT, Position.UNKNOWN)))
        private val __SIZEOF_SHORT__   = MacroReplacement("__SIZEOF_SHORT__", tokenListOf(PPNumber(2, INT, Position.UNKNOWN)))
        private val __SIZEOF_FLOAT__   = MacroReplacement("__SIZEOF_FLOAT__", tokenListOf(PPNumber(4, INT, Position.UNKNOWN)))
        private val __SIZEOF_DOUBLE__  = MacroReplacement("__SIZEOF_DOUBLE__", tokenListOf(PPNumber(8, INT, Position.UNKNOWN)))
        private val __INT32_MAX__      = MacroReplacement("__INT32_MAX__", tokenListOf(PPNumber(2147483647, INT, Position.UNKNOWN)))
        private val __SIZE_TYPE__      = MacroReplacement("__SIZE_TYPE__", tokenListOf(Keyword("unsigned", Position.UNKNOWN, Hideset()), Keyword("long", Position.UNKNOWN, Hideset())))

        // Implementation-defined macros
        private val PLATFORM = MacroReplacement("__x86_64__", tokenListOf(PPNumber(1, INT, Position.UNKNOWN)))
        private val LP64     = MacroReplacement("__LP64__", tokenListOf(PPNumber(1, INT, Position.UNKNOWN)))
        private val LINUX    = MacroReplacement("__linux__", tokenListOf(PPNumber(1, INT, Position.UNKNOWN)))
        private val UNIX     = MacroReplacement("__unix__", tokenListOf(PPNumber(1, INT, Position.UNKNOWN)))
        private val __func__ = MacroReplacement("__func__", tokenListOf(FunctionMark(Position.UNKNOWN)))

        private val predefined = hashMapOf(
            "__LINE__" to LINE,
            "__FILE__" to FILE,
            "__DATE__" to DATE,
            "__TIME__" to TIME,
            "__STDC__" to STDC,
        )

        fun create(headerHolder: HeaderHolder): PreprocessorContext {
            val macroReplacements = hashMapOf(
                // 6.10.8.1 Mandatory macros
                "__STDC_HOSTED__"  to STDC_HOSTED,
                "__STDC_VERSION__" to STDC_VERSION,

                // Implementation-defined macros
                "__x86_64__" to PLATFORM,
                "__LP64__"   to LP64,
                "__linux__"  to LINUX,
                "__unix__"   to UNIX,
                "__func__"   to __func__,

                // 3.7.2 Common Predefined Macros
                "__SIZEOF_POINTER__" to __SIZEOF_POINTER__,
                "__SIZEOF_LONG__"    to __SIZEOF_LONG__,
                "__SIZEOF_INT__"     to __SIZEOF_INT__,
                "__SIZEOF_SHORT__"   to __SIZEOF_SHORT__,
                "__SIZEOF_FLOAT__"   to __SIZEOF_FLOAT__,
                "__SIZEOF_DOUBLE__"  to __SIZEOF_DOUBLE__,
                "__INT32_MAX__"      to __INT32_MAX__,
                "__SIZE_TYPE__"      to __SIZE_TYPE__,
            )

            return PreprocessorContext(macroReplacements, hashMapOf(), hashMapOf(), predefined, headerHolder)
        }
    }
}