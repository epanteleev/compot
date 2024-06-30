package gen

import types.*
import ir.types.*
import parser.nodes.*
import ir.module.Module
import gen.TypeConverter.toIRType
import gen.consteval.ConstEvalContext
import gen.consteval.ConstEvalExpression
import ir.global.GlobalConstant
import ir.module.builder.impl.ModuleBuilder
import tokenizer.CToken


data class IRCodeGenError(override val message: String) : Exception(message)

class IRGen private constructor(typeHolder: TypeHolder): AbstractIRGenerator(ModuleBuilder.create(), typeHolder, VarStack()) {
    private var constantCounter = 0

    fun visit(programNode: ProgramNode) = varStack.scoped {
        for (node in programNode.nodes) {
            when (node) {
                is FunctionNode -> {
                    val gen = IrGenFunction(moduleBuilder, typeHolder, varStack, constantCounter)
                    gen.visit(node)
                    constantCounter = gen.constantCounter
                }
                is Declaration -> declare(node)
                else -> throw IRCodeGenError("Function expected")
            }
        }
    }

    private fun declareDeclarator(declarationSpecifier: DeclarationSpecifier, decl: Declarator) {
        when (val type = decl.resolveType(declarationSpecifier, typeHolder)) {
            is CFunctionType -> {
                val abstrType = type.functionType
                val argTypes = abstrType.argsTypes.map {
                    moduleBuilder.toIRType<NonTrivialType>(typeHolder, it)
                }
                val returnType = moduleBuilder.toIRType<Type>(typeHolder, abstrType.retType)

                val isVararg = type.functionType.variadic
                moduleBuilder.createExternFunction(decl.name(), returnType, argTypes, isVararg)
            }
            is CPrimitiveType -> {
                val irType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, type)
                val global = GlobalConstant.of(decl.name(), irType, 0)
                moduleBuilder.addConstant(global)
                varStack[decl.name()] = global
            }
            else -> throw IRCodeGenError("Function or struct expected, but was '$type'")
        }
    }

    private fun declare(node: Declaration) {
        for (decl in node.nonTypedefDeclarators()) {
            when (decl) {
                is Declarator -> declareDeclarator(node.declspec, decl)
                is AssignmentDeclarator -> {
                    val type = decl.resolveType(node.declspec, typeHolder)
                    val irType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, type)

                    val constEvalResult = ConstEvalExpression.eval(decl.rvalue, GlobalVariableConstEvalContext)

                    val global = GlobalConstant.of(decl.name(), irType, constEvalResult)
                    moduleBuilder.addConstant(global)
                    varStack[decl.name()] = global
                }
                else -> throw IRCodeGenError("Unsupported declarator $decl")
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

        object GlobalVariableConstEvalContext: ConstEvalContext {
            override fun getVariable(name: CToken): Int {
                throw IRCodeGenError("Variable access is not allowed in consteval context")
            }

            override fun callFunction(name: CToken, args: List<Int>): Int {
                throw IRCodeGenError("Function call is not allowed in consteval context")
            }

            override fun typeHolder(): TypeHolder {
                throw IRCodeGenError("TypeHolder is not available in consteval context")
            }
        }
    }
}