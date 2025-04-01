package typedesc

import codegen.VarStack

class VarHolder {
    private val valueMap = VarStack<VarDescriptor>()

    operator fun get(varName: String): VarDescriptor {
        return getVarTypeOrNull(varName) ?: throw Exception("Type for variable '$varName' not found")
    }

    fun getVarTypeOrNull(varName: String): VarDescriptor? {
        return valueMap[varName]
    }

    fun addVar(varDesc: VarDescriptor) {
        valueMap[varDesc.name] = varDesc
    }

    companion object {
        fun create(): VarHolder {
            return VarHolder()
        }
    }
}