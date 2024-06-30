package gen

import types.*
import ir.types.*
import parser.nodes.*
import ir.module.Module
import gen.TypeConverter.toIRType
import ir.module.builder.impl.ModuleBuilder


data class IRCodeGenError(override val message: String) : Exception(message)

class IRGen private constructor(typeHolder: TypeHolder): AbstractIRGenerator(ModuleBuilder.create(), typeHolder, VarStack()) {
    private var constantCounter = 0

    fun visit(programNode: ProgramNode) = varStack.scoped {
        for (node in programNode.nodes) {
            when (node) {
                is FunctionNode -> {
                    val gen = IrGenFunction(moduleBuilder, typeHolder, varStack, constantCounter)
                    gen.emitIr(node)
                    constantCounter = gen.constantCounter
                }
                is Declaration -> declare(node)
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
                    val argTypes = abstrType.argsTypes.map {
                        moduleBuilder.toIRType<NonTrivialType>(typeHolder, it)
                    }
                    val returnType = moduleBuilder.toIRType<Type>(typeHolder, abstrType.retType)

                    val isVararg = type.functionType.variadic
                    moduleBuilder.createExternFunction(type.name, returnType, argTypes, isVararg)
                }
                else -> throw IRCodeGenError("Function or struct expected, but was '$type'")
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