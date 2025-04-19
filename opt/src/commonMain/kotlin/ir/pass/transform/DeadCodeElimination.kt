package ir.pass.transform

import ir.instruction.Instruction
import ir.instruction.Projection
import ir.instruction.TerminateInstruction
import ir.module.FunctionData
import ir.module.Module
import ir.pass.common.TransformPass
import ir.pass.common.TransformPassFabric
import ir.value.ArgumentValue
import ir.value.LocalValue
import ir.value.constant.UndefValue


class DeadCodeEliminationPass internal constructor(module: Module): TransformPass(module) {
    override fun name(): String = "dce"
    override fun run(): Module {
        module.functions.values.forEach { fnData ->
            DeadCodeEliminationPassImpl(fnData).pass()
        }

        return module
    }
}

object DeadCodeElimination: TransformPassFabric() {
    override fun create(module: Module): TransformPass {
        return DeadCodeEliminationPass(module)
    }
}

internal class DeadCodeEliminationPassImpl(private val cfg: FunctionData) {
    private val worklist = arrayListOf<LocalValue>()

    private fun skipIf(vInst: LocalValue): Boolean {
        if (vInst is ArgumentValue) {
            return true
        }
        if (vInst is TerminateInstruction) {
            return true
        }
        if (vInst is Projection) {
            // TODO: handle projection correctly
            return true
        }
        if (vInst.usedIn().isNotEmpty()) {
            return true
        }

        return false
    }

    private fun setup() {
        for (bb in cfg) {
            for (vInst in bb) {
                if (vInst !is LocalValue) {
                    continue
                }

                if (skipIf(vInst)) {
                    continue
                }

                worklist.add(vInst as LocalValue)
            }
        }
    }

    private fun removeDeadInstruction() {
        while (worklist.isNotEmpty()) {
            val vInst = worklist.removeLast()
            vInst as Instruction

            for (op in vInst.operands()) {
                if (op !is LocalValue) {
                    continue
                }
                if (skipIf(op)) {
                    continue
                }

                worklist.add(op)
            }

            vInst.die(UndefValue)
        }
    }

    fun pass() {
        setup()
        removeDeadInstruction()
    }
}