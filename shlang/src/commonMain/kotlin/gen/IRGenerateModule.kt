package gen

import types.*
import ir.types.*
import parser.nodes.*
import ir.module.Module
import gen.TypeConverter.toIRType
import gen.consteval.*
import ir.global.*
import ir.module.builder.impl.ModuleBuilder


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
                    val result = constEval(decl.rvalue)?: stringLiteralInitializer(decl.rvalue)
                    if (result == null) {
                        throw IRCodeGenError("Unsupported declarator '$decl'")
                    }

                    constantCounter++
                    moduleBuilder.addConstant(result)
                    varStack[decl.name()] = result
                }
                else -> throw IRCodeGenError("Unsupported declarator $decl")
            }
        }
    }

    private fun constEval(expr: Expression): GlobalConstant? {
        val type = expr.resolveType(typeHolder)

        return when (type) {
            CType.INT, CType.SHORT, CType.CHAR, CType.UINT, CType.USHORT, CType.UCHAR -> {
                val ctx = CommonConstEvalContext<Int>(typeHolder)
                val result = ConstEvalExpression.eval(expr, ConstEvalExpressionInt(ctx))
                return I32GlobalValue("v${constantCounter++}", result)
            }
            CType.LONG, CType.ULONG -> {
                val ctx = CommonConstEvalContext<Long>(typeHolder)
                val result = ConstEvalExpression.eval(expr, ConstEvalExpressionLong(ctx))
                return I64GlobalValue("v${constantCounter++}", result)
            }
            CType.FLOAT -> {
                val ctx = CommonConstEvalContext<Float>(typeHolder)
                val result = ConstEvalExpression.eval(expr, ConstEvalExpressionFloat(ctx))
                return F32GlobalValue("v${constantCounter++}", result)
            }
            CType.DOUBLE -> {
                val ctx = CommonConstEvalContext<Double>(typeHolder)
                val result = ConstEvalExpression.eval(expr, ConstEvalExpressionDouble(ctx))
                return F64GlobalValue("v${constantCounter++}", result)
            }
            else -> null
        }
    }

    private fun stringLiteralInitializer(expr: Expression): GlobalConstant? {
        if (expr !is StringNode) {
            return null
        }
        val content = expr.str.unquote()
        return StringLiteralGlobal("str${constantCounter++}", ArrayType(Type.U8, content.length), content)
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