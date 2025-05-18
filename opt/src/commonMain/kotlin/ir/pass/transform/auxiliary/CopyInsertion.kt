package ir.pass.transform.auxiliary

import common.assertion
import ir.instruction.*
import ir.instruction.lir.Lea
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.module.FunctionData
import ir.instruction.matching.*
import ir.pass.CompileContext
import ir.value.isa


internal class CopyInsertion private constructor(private val cfg: FunctionData, val ctx: CompileContext) {
    private fun isolatePhis(bb: Block, phi: Phi): Instruction {
        phi.zipWithIndex { incoming, operand, idx ->
            assertion(!bb.hasCriticalEdgeFrom(incoming)) {
                "Flow graph has critical edge from $incoming to $bb"
            }

            val last = incoming.last()
            val copy = if (ctx.pic() && operand.isa(gVisible())) {
                incoming.putBefore(last, Copy.copy(operand))

            } else if (operand.isa(gValue(anytype()))) {
                incoming.putBefore(last, Lea.lea(operand))

            } else {
                incoming.putBefore(last, Copy.copy(operand))
            }
            phi.value(idx, copy)
        }

        return phi.updateUsages(bb.putAfter(phi, Copy.copy(phi)))
    }

    fun pass() {
        for (bb in cfg) {
            bb.transform { phi ->
                if (phi !is Phi) {
                    return@transform bb.last()
                }
                isolatePhis(bb, phi)
            }
        }
    }

    companion object {
        fun run(module: Module, ctx: CompileContext): Module {
            module.functions().forEach { CopyInsertion(it, ctx).pass() }
            return SSAModule(module.functions, module.functionDeclarations, module.constantPool, module.globals, module.types)
        }
    }
}