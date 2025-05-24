package codegen

import parser.nodes.*
import ir.module.Module
import ir.module.builder.impl.ModuleBuilder
import ir.pass.analysis.ValidateSSAErrorException
import sema.SemanticAnalysis
import tokenizer.Position
import typedesc.StorageClass
import typedesc.TypeHolder


data class IRCodeGenError(override val message: String, val position: Position) : Exception(message)

object GenerateIR {
    fun apply(typeHolder: TypeHolder, node: ProgramNode): Module {
        return IRGen.apply(typeHolder, node)
    }
}

private class IRGen private constructor(typeHolder: TypeHolder): AbstractIRGenerator(ModuleBuilder.create(), SemanticAnalysis(typeHolder), VarStack(), NameGenerator()) {
    fun visit(programNode: ProgramNode) = vregStack.scoped {
        for (node in programNode.nodes) {
            when (node) {
                is FunctionDeclarationNode -> generateFunction(node.function)
                is GlobalDeclaration  -> generateDeclaration(node.declaration)
            }
        }
    }

    private fun generateFunction(node: FunctionNode) {
        val gen = FunGenInitializer(mb, node, vregStack, nameGenerator)
        gen.generate()
    }

    private fun generateDeclaration(node: Declaration) {
        val declSpec = node.declspec.specifyType(sema.typeHolder)
        if (declSpec.storageClass == StorageClass.TYPEDEF) {
            return
        }

        for (declarator in node.declarators()) {
            val varDesc = sema.declareVar(declarator, declSpec)
                ?: throw IRCodeGenError("Typedef is not supported in global declarations", node.begin())

            when (declarator) {
                is Declarator -> when (varDesc.storageClass) {
                    StorageClass.EXTERN -> generateExternDeclarator(varDesc, declarator)
                    else -> generateGlobalDeclarator(varDesc, declarator)
                }
                is InitDeclarator -> generateGlobalAssignmentDeclarator(varDesc, declarator)
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