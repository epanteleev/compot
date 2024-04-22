package gen

import types.*
import ir.module.Module
import ir.module.builder.impl.ModuleBuilder
import ir.types.*
import parser.nodes.*
import java.lang.Exception


data class IRCodeGenError(override val message: String) : Exception(message)

class IRGen private constructor() {
    private val moduleBuilder = ModuleBuilder.create()
    private val typeHolder = TypeHolder.default()

    fun visit(programNode: ProgramNode) {
        for (node in programNode.nodes) {
            when (node) {
                is FunctionNode -> IrGenFunction(moduleBuilder, typeHolder, node)
                is Declaration ->  {
                    val types = node.resolveType(typeHolder)
                    for (type in types) {
                        when (type) {
                            is CFunctionType -> {
                                val argTypes = type.argsTypes.map { TypeConverter.toIRType<NonTrivialType>(it) }
                                moduleBuilder.createExternFunction(type.name, TypeConverter.toIRType(type.retType), argTypes)
                            }
                            else -> throw IRCodeGenError("Function or struct expected")
                        }
                    }
                }
                else -> throw IRCodeGenError("Function expected")
            }
        }
    }

    companion object {
        fun apply(node: ProgramNode): Module {
            //println(node)
            val irGen = IRGen()
            irGen.visit(node)
            val module = irGen.moduleBuilder.build()
            return module
        }
    }
}