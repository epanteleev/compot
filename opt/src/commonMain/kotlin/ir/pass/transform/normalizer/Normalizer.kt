package ir.pass.transform.normalizer

import ir.value.*
import ir.instruction.Instruction
import ir.instruction.TupleDiv
import ir.module.FunctionData
import ir.module.Module
import ir.pass.CompileContext
import ir.pass.analysis.traverse.PreOrderFabric
import ir.pass.common.TransformPass
import ir.pass.common.TransformPassFabric
import ir.value.constant.UndefValue


class NormalizerPass internal constructor(module: Module, ctx: CompileContext): TransformPass(module, ctx) {
    override fun name(): String = "normalizer"
    override fun run(): Module {
        module.functions.values.forEach { fnData ->
            NormalizerPassImpl(fnData).pass()
        }

        return module
    }
}

object Normalizer: TransformPassFabric() {
    override fun create(module: Module, ctx: CompileContext): TransformPass {
        return NormalizerPass(module.copy(), ctx)
    }
}

internal class NormalizerPassImpl(private val cfg: FunctionData) {
    private val worklist = arrayListOf<LocalValue>()
    private val deadPool = hashSetOf<TupleDiv>()

    private fun addToWorkList(operands: List<Instruction>) {
        for (operand in operands) {
            if (operand !is LocalValue) {
                continue
            }

            worklist.add(operand)
        }
    }

    private fun canonicalize(vInst: LocalValue): Value {
        vInst as Instruction
        val new = NormalizeInstruction.normalize(vInst)
        if (new == vInst) {
            return vInst
        }

        addToWorkList(vInst.usedIn())
        vInst.updateUsages(new)
        if (vInst is TupleDiv) {
            deadPool.add(vInst)
        }

        return new
    }

    private fun propagate() {
        while (worklist.isNotEmpty()) {
            val v = worklist.removeLast()
            if (v !is Instruction) {
                continue
            }

            canonicalize(v)
        }
    }

    private fun killDeadInstructions() {
        for (v in deadPool) {
            v.die(UndefValue)
        }
    }

    fun pass() {
        val preorder = cfg.analysis(PreOrderFabric)
        for (bb in preorder) {
            for (vInst in bb) {
                if (vInst !is LocalValue) {
                    continue
                }

                canonicalize(vInst)
                propagate()
            }
        }

        killDeadInstructions()
    }
}
