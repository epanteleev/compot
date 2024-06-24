package gen

import gen.TypeConverter.toIRType
import types.*
import ir.module.Module
import ir.module.builder.impl.ModuleBuilder
import ir.types.*
import parser.nodes.*


data class IRCodeGenError(override val message: String) : Exception(message)

class IRGen private constructor(val typeHolder: TypeHolder) {
    private val moduleBuilder = ModuleBuilder.create()
    private var constantCounter = 0

    fun visit(programNode: ProgramNode) {
        for (node in programNode.nodes) {
            when (node) {
                is FunctionNode -> {
                    val gen = IrGenFunction(moduleBuilder, typeHolder, node, constantCounter)
                    constantCounter = gen.counstantCounter
                }
                is Declaration ->  declare(node)
                else -> throw IRCodeGenError("Function expected")
            }
        }
    }

    private fun declare(node: Declaration) {
        val types = node.resolveType(typeHolder)
        for (type in types) {
            when (type) {
                is CFunctionType -> {
                    val abstrType = type.functionType
                    val argTypes = abstrType.argsTypes.map { moduleBuilder.toIRType<NonTrivialType>(typeHolder, it) }
                    val returnType = moduleBuilder.toIRType<Type>(typeHolder, abstrType.retType)

                    val isVararg = type.functionType.variadic
                    moduleBuilder.createExternFunction(type.name, returnType, argTypes, isVararg)
                }
                else -> throw IRCodeGenError("Function or struct expected")
            }
        }
    }

    companion object {
        fun apply(typeHolder: TypeHolder, node: ProgramNode): Module {
            //println(node)
            val irGen = IRGen(typeHolder)
            irGen.visit(node)
            val module = irGen.moduleBuilder.build()
            return module
        }
    }
}