package gen

import gen.TypeConverter.toIRAttributes
import types.*
import ir.types.*
import parser.nodes.*
import ir.module.Module
import gen.TypeConverter.toIRType
import ir.attributes.GlobalValueAttribute
import ir.global.*
import ir.module.ExternFunction
import ir.module.builder.impl.ModuleBuilder
import ir.pass.analysis.ValidateSSAErrorException
import ir.value.Constant


data class IRCodeGenError(override val message: String) : Exception(message)

class IRGen private constructor(typeHolder: TypeHolder): AbstractIRGenerator(ModuleBuilder.create(), typeHolder, VarStack(), NameGenerator()) {

    fun visit(programNode: ProgramNode) = varStack.scoped {
        for (node in programNode.nodes) {
            when (node) {
                is FunctionNode -> generateFunction(node)
                is Declaration  -> generateDeclaration(node)
                else -> throw IRCodeGenError("Function expected")
            }
        }
    }

    private fun generateFunction(node: FunctionNode) {
        val gen = IrGenFunction(mb, typeHolder, varStack, nameGenerator)
        gen.visit(node)
    }

    private fun getExternFunction(name: String, returnType: Type, arguments: List<Type>, isVararg: Boolean = false): ExternFunction {
        val externFunction = mb.findExternFunctionOrNull(name)
        if (externFunction != null) {
            println("Warning: extern function $name already exists") //TODO implement warning mechanism
            return externFunction
        }

        return mb.createExternFunction(name, returnType, arguments, isVararg)
    }

    private fun generateDeclarator(decl: Declarator) {
        val name = decl.name()
        when (val type = decl.cType()) {
            is CFunctionType -> {
                val abstrType = type.functionType
                val argTypes  = abstrType.argsTypes.map {
                    mb.toIRType<NonTrivialType>(typeHolder, it)
                }
                val returnType = mb.toIRType<Type>(typeHolder, abstrType.retType)

                val isVararg = type.functionType.variadic
                getExternFunction(name, returnType, argTypes, isVararg)
            }
            is CPrimitiveType -> {
                val irType = mb.toIRType<NonTrivialType>(typeHolder, type)
                val attributes = toIRAttributes<GlobalValueAttribute>(type.qualifiers())
                val constant = GlobalConstant.of(createGlobalConstantName(), irType, 0)
                varStack[name] = mb.addGlobal(name, constant, attributes)
            }
            is CPointerType -> {
                val irType     = mb.toIRType<NonTrivialType>(typeHolder, type)
                val attributes = toIRAttributes<GlobalValueAttribute>(type.dereference().qualifiers()) //TODO don't dereference
                val constant = GlobalConstant.of(createGlobalConstantName(), irType, 0)
                varStack[name] = mb.addGlobal(name, constant, attributes)
            }
            is CArrayType -> {
                val irType = mb.toIRType<ArrayType>(typeHolder, type)
                val zero = Constant.of(irType.elementType(), 0 )
                val elements = generateSequence { zero }.take(irType.size).toList()
                val constant = ArrayGlobalConstant(createGlobalConstantName(), irType, elements)
                val attributes = toIRAttributes<GlobalValueAttribute>(type.qualifiers())
                varStack[name] = mb.addGlobal(name, constant, attributes)
            }
            else -> throw IRCodeGenError("Function or struct expected, but was '$type'")
        }
    }

    private fun generateAssignmentDeclarator(decl: InitDeclarator) {
        val cType = decl.cType()
        val lValueType = mb.toIRType<NonTrivialType>(typeHolder, cType)

        val result = constEvalExpression(lValueType, decl.rvalue) ?: throw IRCodeGenError("Unsupported declarator '$decl'")

        val constant = mb.addConstant(result)
        val attributes = toIRAttributes<GlobalValueAttribute>(cType.qualifiers())
        val global   = mb.addGlobal(createGlobalConstantName(), constant, attributes)
        varStack[decl.name()] = global
    }

    private fun generateDeclaration(node: Declaration) {
        for (declarator in node.nonTypedefDeclarators()) {
            when (declarator) {
                is Declarator     -> generateDeclarator(declarator)
                is InitDeclarator -> generateAssignmentDeclarator(declarator)
                else -> throw IRCodeGenError("Unsupported declarator $declarator")
            }
        }
    }

    companion object {
        fun apply(typeHolder: TypeHolder, node: ProgramNode): Module {
            val irGen = IRGen(typeHolder)
            irGen.visit(node)
            try {
                return irGen.mb.build()
            } catch (e: ValidateSSAErrorException) {
                println("Error: ${e.message}")
                println("Module:\n${e.module}")
                throw e
            }
        }
    }
}