package ir.platform.x64.pass.analysis

import ir.value.*
import ir.instruction.*
import ir.module.FunctionData
import ir.instruction.matching.*


class FixedRegisterInstructionsAnalysisResult(val rdxFixedReg: List<LocalValue>, val rcxFixedReg: List<LocalValue>)

class FixedRegisterInstructionsAnalysis private constructor(private val cfg: FunctionData) {
    private val rdxFixedReg = arrayListOf<LocalValue>()
    private val rcxFixedReg = arrayListOf<LocalValue>()

    private fun traverse(inst: Instruction): Instruction {
        inst.match(shl(any(), constant().not())) { shl: Shl ->
            rcxFixedReg.add(shl.rhs().asValue())
            return inst
        }

        inst.match(shr(any(), constant().not())) { shr: Shr ->
            rcxFixedReg.add(shr.rhs().asValue())
            return inst
        }

        inst.match(tupleDiv(any(), any())) { tupleDiv: TupleDiv ->
            rdxFixedReg.add(tupleDiv.remainder()!!)
            val divider = tupleDiv.second()
            if (divider is LocalValue) {
                rcxFixedReg.add(divider)
            }

            return inst
        }

        inst.match(proj(int() or ptr(), tupleCall(), 1)) { proj: Projection ->
            rdxFixedReg.add(proj)
            return inst
        }

        inst.match(memcpy(any(), any(), any())) { memcpy: Memcpy ->
            rdxFixedReg.add(memcpy.destination().asValue())
            rcxFixedReg.add(memcpy.source().asValue())
            return inst
        }

        return inst
    }

    private fun isolateSpecialInstructions() {
        for (bb in cfg)  {
            for (inst in bb) {
                traverse(inst)
            }
        }
    }

    private fun pass(): FixedRegisterInstructionsAnalysisResult {
        isolateSpecialInstructions()
        return FixedRegisterInstructionsAnalysisResult(rdxFixedReg, rcxFixedReg)
    }

    companion object {
        fun run(cfg: FunctionData): FixedRegisterInstructionsAnalysisResult {
            return FixedRegisterInstructionsAnalysis(cfg).pass()
        }
    }
}