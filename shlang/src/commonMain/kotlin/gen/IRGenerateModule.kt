package gen

import types.*
import parser.nodes.*
import ir.module.Module
import ir.module.builder.impl.ModuleBuilder
import ir.pass.analysis.ValidateSSAErrorException
import ir.value.Value


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

    private fun generateDeclaration(node: Declaration) {
        for (declarator in node.nonTypedefDeclarators()) {
            when (declarator) {
                is Declarator     -> generateGlobalDeclarator(declarator)
                is InitDeclarator -> generateGlobalAssignmentDeclarator(declarator)
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