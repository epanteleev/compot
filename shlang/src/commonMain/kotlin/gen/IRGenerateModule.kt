package gen

import types.*
import ir.types.*
import parser.nodes.*
import ir.module.Module
import gen.TypeConverter.toIRType
import gen.consteval.*
import ir.global.*
import ir.module.ExternFunction
import ir.module.builder.impl.ModuleBuilder
import ir.value.Constant


data class IRCodeGenError(override val message: String) : Exception(message)

class IRGen private constructor(typeHolder: TypeHolder): AbstractIRGenerator(ModuleBuilder.create(), typeHolder, VarStack()) {
    private var constantCounter = 0

    fun visit(programNode: ProgramNode) = varStack.scoped {
        for (node in programNode.nodes) {
            when (node) {
                is FunctionNode -> {
                    val gen = IrGenFunction(mb, typeHolder, varStack, constantCounter)
                    gen.visit(node)
                    constantCounter = gen.constantCounter
                }
                is Declaration -> declare(node)
                else -> throw IRCodeGenError("Function expected")
            }
        }
    }

    private fun getExternFunction(name: String, returnType: Type, arguments: List<Type>, isVararg: Boolean = false): ExternFunction {
        val externFunction = mb.findExternFunctionOrNull(name)
        if (externFunction != null) {
            println("Warning: extern function $name already exists") //TODO implement warning mechanism
            return externFunction
        }

        return mb.createExternFunction(name, returnType, arguments, isVararg)
    }

    private fun declareDeclarator(declarationSpecifier: DeclarationSpecifier, decl: Declarator) {
        when (val type = decl.declareType(declarationSpecifier, typeHolder)) {
            is CFunctionType -> {
                val abstrType = type.functionType
                val argTypes  = abstrType.argsTypes.map {
                    mb.toIRType<NonTrivialType>(typeHolder, it)
                }
                val returnType = mb.toIRType<Type>(typeHolder, abstrType.retType)

                val isVararg = type.functionType.variadic
                getExternFunction(decl.name(), returnType, argTypes, isVararg)
            }
            is CPrimitiveType -> {
                val irType = mb.toIRType<NonTrivialType>(typeHolder, type)
                val global = GlobalConstant.of(decl.name(), irType, 0)
                mb.addConstant(global)
                varStack[decl.name()] = global
            }
            is CPointerType -> {
                val irType = mb.toIRType<NonTrivialType>(typeHolder, type)
                val global = GlobalConstant.of(decl.name(), irType, 0)
                mb.addConstant(global)
                varStack[decl.name()] = global
            }
            is CArrayType -> {
                val irType = mb.toIRType<ArrayType>(typeHolder, type)
                val zero = Constant.of(irType.elementType(), 0 )
                val elements = generateSequence { zero }.take(irType.size).toList()
                val global = ArrayGlobalConstant(decl.name(), irType, elements)
                mb.addConstant(global)
                varStack[decl.name()] = global
            }
            else -> throw IRCodeGenError("Function or struct expected, but was '$type'")
        }
    }

    private fun declare(node: Declaration) {
        for (decl in node.nonTypedefDeclarators()) {
            when (decl) {
                is Declarator -> {
                    declareDeclarator(node.declspec, decl)
                }
                is AssignmentDeclarator -> {
                    val cType = decl.declareType(node.declspec, typeHolder)
                    val lValueType = mb.toIRType<NonTrivialType>(typeHolder, cType)

                    val result = constEvalExpression(lValueType, decl.rvalue) ?: throw IRCodeGenError("Unsupported declarator '$decl'")

                    val constant = mb.addConstant(result)
                    val global = mb.addGlobal(".v${constantCounter++}", constant)
                    varStack[decl.name()] = global
                }
                else -> throw IRCodeGenError("Unsupported declarator $decl")
            }
        }
    }

    private fun constEvalExpression(lValueType: Type, expr: Expression): GlobalConstant? {
        val result = constEvalExpression0(expr)
        if (result != null) {
            return GlobalConstant.of(".v${constantCounter++}", lValueType, result)
        }
        return stringLiteralInitializer(expr)
    }

    private fun constEvalExpression0(expr: Expression): Number? {
        val type = expr.resolveType(typeHolder)

        return when (type) {
            CType.INT, CType.SHORT, CType.CHAR, CType.UINT, CType.USHORT, CType.UCHAR -> {
                val ctx = CommonConstEvalContext<Int>(typeHolder)
                ConstEvalExpression.eval(expr, ConstEvalExpressionInt(ctx))
            }
            CType.LONG, CType.ULONG -> {
                val ctx = CommonConstEvalContext<Long>(typeHolder)
                ConstEvalExpression.eval(expr, ConstEvalExpressionLong(ctx))
            }
            CType.FLOAT -> {
                val ctx = CommonConstEvalContext<Float>(typeHolder)
                ConstEvalExpression.eval(expr, ConstEvalExpressionFloat(ctx))
            }
            CType.DOUBLE -> {
                val ctx = CommonConstEvalContext<Double>(typeHolder)
                ConstEvalExpression.eval(expr, ConstEvalExpressionDouble(ctx))
            }
            else -> null
        }
    }

    private fun stringLiteralInitializer(expr: Expression): GlobalConstant? {
        if (expr is StringNode) {
            val content = expr.str.data()
            return StringLiteralConstant("str${constantCounter++}", ArrayType(Type.U8, content.length), content)
        } else if (expr is InitializerList) {
            val type = expr.resolveType(typeHolder)

            if (type is CompoundType) {
                val typeExpr = mb.toIRType<AggregateType>(typeHolder, type)

                val elements = expr.initializers.map { constEvalExpression0(it) }
                val convertedElements = elements.mapIndexed { it, num ->
                    Constant.of(typeExpr.field(it), num as Number)
                }

                return ArrayGlobalConstant("str${constantCounter++}", typeExpr as ArrayType, convertedElements)
            } else {
                throw IRCodeGenError("Unsupported type $type")
            }
        } else {
            return null
        }
    }

    companion object {
        fun apply(typeHolder: TypeHolder, node: ProgramNode): Module {
            //println(node)
            val irGen = IRGen(typeHolder)
            irGen.visit(node)
            val module = irGen.mb.build()
            return module
        }
    }
}