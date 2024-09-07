package types

import gen.VarStack


class TypeHolder(private val valueMap: VarStack<CType>): Scope {
    private val typeMap = VarStack<BaseType>()//TODO separate holder for struct, enum, union.
    private val functions = hashMapOf<String, CFunctionType>()
    private val typedefs = VarStack<CType>()

    operator fun get(varName: String): CType {
        return valueMap[varName] ?: functions[varName] ?: throw Exception("Type for variable '$varName' not found")
    }

    fun getTypeOrNull(name: String): BaseType? {
        return typeMap[name]
    }

    private fun getTypedefOrNull(name: String): CType? {
        return typedefs[name]
    }

    fun getTypedef(name: String): CType {
        return getTypedefOrNull(name) ?: throw Exception("Type for 'typedef $name' not found")
    }

    fun addTypedef(name: String, type: CType): CType {
        typedefs[name] = type
        return type
    }

    fun addVar(name: String, type: CType) {
        valueMap[name] = type
    }

    fun containsVar(varName: String): Boolean {
        return valueMap.containsKey(varName)
    }

    fun getStructType(name: String): BaseType {
        return getTypeOrNull(name) ?: throw Exception("Type for struct $name not found")
    }

    fun <T : BaseType> addNewType(name: String, type: T): T {
        typeMap[name] = type
        return type
    }

    fun getFunctionType(name: String): CType {
        return functions[name] ?: valueMap[name] ?: throw Exception("Type for function '$name' not found")
    }

    fun addFunctionType(name: String, type: CFunctionType) {
        functions[name] = type
    }

    override fun enter() {
        typeMap.enter()
        valueMap.enter()
    }

    override fun leave() {
        typeMap.leave()
        valueMap.leave()
    }

    companion object {
        fun default(): TypeHolder {
            return TypeHolder(VarStack<CType>())
        }
    }
}