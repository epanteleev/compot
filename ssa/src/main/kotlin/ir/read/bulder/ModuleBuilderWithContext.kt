package ir.read.bulder

import ir.*
import ir.read.*
import ir.module.*
import ir.types.StructType
import ir.pass.ana.VerifySSA


interface TypeResolver {
    fun resolve(name: String): StructType
}

class ModuleBuilderWithContext: TypeResolver {
    private val functions = arrayListOf<FunctionDataBuilderWithContext>()
    private val externFunctions = mutableSetOf<ExternFunction>()
    private val globals = mutableSetOf<GlobalValue>()
    private val structs = arrayListOf<StructType>()

    fun createFunction(functionName: SymbolValue, returnType: TypeToken, argumentTypes: List<TypeToken>, argumentValues: List<LocalValueToken>): FunctionDataBuilderWithContext {
        val args        = argumentTypes.mapTo(arrayListOf()) { it.type(this) }
        val prototype   = FunctionPrototype(functionName.name, returnType.type(this), args)

        val data = FunctionDataBuilderWithContext.create(this, prototype, argumentValues)
        functions.add(data)
        return data
    }

    fun findGlobal(name: String): GlobalValue {
        return globals.find { it.name() == name } ?: throw RuntimeException("not found name=$name")
    }

    fun addGlobal(global: GlobalValue) = globals.add(global)

    fun addStructType(structType: StructType) = structs.add(structType)

    fun findStructType(name: String): StructType {
        return structs.find { it.name == name } ?: throw RuntimeException("not found name=$name")
    }

    override fun resolve(name: String): StructType {
        return findStructType(name)
    }

    fun createExternFunction(functionName: SymbolValue, returnType: TypeToken, arguments: List<TypeToken>): ExternFunction {
        val extern = ExternFunction(functionName.name, returnType.type(this), arguments.map { it.type(this) })
        externFunctions.add(extern)
        return extern
    }

    fun build(): Module {
        val fns = functions.mapTo(arrayListOf()) {
            it.build()
        }

        val ssa = SSAModule(fns, externFunctions, globals, structs)
        return VerifySSA.run(ssa)
    }

    companion object {
        fun create() : ModuleBuilderWithContext {
            return ModuleBuilderWithContext()
        }
    }
}