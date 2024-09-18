package gen

import types.*
import ir.types.*
import parser.nodes.*
import ir.module.Module
import gen.TypeConverter.toIRType
import ir.global.StringLiteralGlobalConstant
import ir.module.ExternFunction
import ir.module.builder.impl.ModuleBuilder
import ir.pass.analysis.ValidateSSAErrorException
import ir.value.AggregateConstant
import ir.value.BoolValue
import ir.value.Constant
import ir.value.F32Value
import ir.value.F64Value
import ir.value.I16Value
import ir.value.I32Value
import ir.value.I64Value
import ir.value.I8Value
import ir.value.InitializerListValue
import ir.value.NullValue
import ir.value.PointerLiteral
import ir.value.PrimitiveConstant
import ir.value.StringLiteralConstant
import ir.value.U16Value
import ir.value.U32Value
import ir.value.U64Value
import ir.value.U8Value
import ir.value.UndefinedValue
import ir.value.Value
import tokenizer.StringLiteral


data class IRCodeGenError(override val message: String) : Exception(message)

class IRGen private constructor(typeHolder: TypeHolder): AbstractIRGenerator(ModuleBuilder.create(), typeHolder, VarStack<Value>(), NameGenerator()) {
    fun visit(programNode: ProgramNode) = varStack.scoped {
        for (node in programNode.nodes) {
            when (node) {
                is FunctionNode -> generateFunction(node)
                is Declaration  -> generateDeclaration(node)
                else -> throw IRCodeGenError("Function expected")
            }
        }
    }

    private fun generateFunction(node: FunctionNode) = typeHolder.scoped {
        val gen = IrGenFunction(mb, typeHolder, varStack, nameGenerator)
        gen.visit(node)
    }

    private fun getExternFunction(name: String, returnType: Type, arguments: List<NonTrivialType>, isVararg: Boolean = false): ExternFunction {
        val externFunction = mb.findExternFunctionOrNull(name)
        if (externFunction != null) {
            println("Warning: extern function $name already exists") //TODO implement warning mechanism
            return externFunction
        }

        return mb.createExternFunction(name, returnType, arguments, isVararg)
    }

    private fun makeGlobalValue(name: String, type: VarDescriptor) {
        val irType = mb.toIRType<NonTrivialType>(typeHolder, type.type.baseType())
        val value = if (type.storageClass == StorageClass.EXTERN) {
            mb.addExternValue(name, irType)
        } else {
            val constant = Constant.of(irType, 0)
            mb.addGlobal(name, irType, constant)
        }
        varStack[name] = value
    }

    private fun generateDeclarator(decl: Declarator) {
        val name = decl.name()
        val varDesc = decl.cType()
        when (val type = varDesc.type.baseType()) {
            is CBaseFunctionType -> {
                val abstrType = type.functionType
                val argTypes  = abstrType.argsTypes.map {
                    mb.toIRType<NonTrivialType>(typeHolder, it.baseType())
                }
                val returnType = mb.toIRType<Type>(typeHolder, abstrType.retType.baseType())

                val isVararg = type.functionType.variadic
                varStack[name] = getExternFunction(name, returnType, argTypes, isVararg)
            }
            is CPrimitive -> makeGlobalValue(name, varDesc)
            is CArrayType -> {
                val irType = mb.toIRType<ArrayType>(typeHolder, type)
                if (varDesc.storageClass == StorageClass.EXTERN) {
                    varStack[name] = mb.addExternValue(name, irType)
                    return
                }
                val zero = Constant.of(irType.elementType(), 0 )
                val elements = generateSequence { zero }.take(irType.length).toList()
                val constant = InitializerListValue(irType, elements)
                varStack[name] = mb.addGlobal(name, irType, constant)
            }
            is CStructType -> {
                val irType = mb.toIRType<StructType>(typeHolder, type)
                if (varDesc.storageClass == StorageClass.EXTERN) {
                    varStack[name] = mb.addExternValue(name, irType)
                    return
                }
                val elements = arrayListOf<Constant>()
                for (field in type.fields()) {
                    val zero = Constant.of(mb.toIRType<NonTrivialType>(typeHolder, field.second.baseType()), 0)
                    elements.add(zero)
                }
                val constant = InitializerListValue(irType, elements)
                varStack[name] = mb.addGlobal(name, irType, constant)
            }
            else -> throw IRCodeGenError("Function or struct expected, but was '$type'")
        }
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
                println("Function:\n${e.functionData}")
                throw e
            }
        }
    }
}