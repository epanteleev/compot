package ir.platform.x64.pass.transform

import ir.types.*
import ir.instruction.*
import ir.instruction.lir.Fxor
import ir.instruction.matching.fp
import ir.instruction.matching.neg
import ir.instruction.matching.nop
import ir.module.FunctionData
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.value.asType
import ir.value.constant.Constant
import ir.value.constant.F32Value
import ir.value.constant.F64Value


internal class ReplaceFloatNeg private constructor(val functions: Map<String, FunctionData>) {
    private fun minusZero(tp: FloatingPointType): Constant = when (tp) {
        F32Type -> F32Value(-0.0f)
        F64Type -> F64Value(-0.0)
    }

    private fun run() {
        for (data in functions.values) {
            data.blocks.forEach {
                handleBlock(it)
            }
        }
    }

    private fun handleBlock(bb: Block) {
        fun closure(bb: Block, inst: Instruction): Instruction {
            if (!inst.isa(neg(fp(), nop()))) {
                return inst
            }
            inst as Neg
            return bb.replace(inst, Fxor.xor(inst.operand(), minusZero(inst.asType())))
        }

        bb.transform { inst -> closure(bb, inst) }
    }

    companion object {
        fun run(module: Module): Module {
            val functions = module.functions.toMutableMap()
            ReplaceFloatNeg(functions).run()

            return SSAModule(functions, module.externFunctions, module.constantPool, module.globals, module.types)
        }
    }
}