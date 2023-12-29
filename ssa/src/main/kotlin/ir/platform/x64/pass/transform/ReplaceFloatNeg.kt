package ir.platform.x64.pass.transform

import asm.x64.ImmInt
import ir.*
import ir.instruction.*
import ir.module.FunctionData
import ir.module.Module
import ir.module.block.Block
import ir.platform.x64.CSSAModule
import ir.platform.x64.CallConvention.DOUBLE_SUB_ZERO_SYMBOL
import ir.platform.x64.CallConvention.FLOAT_SUB_ZERO_SYMBOL
import ir.types.FloatingPointType
import ir.types.Type


class ReplaceFloatNeg(val functions: List<FunctionData>) {


    private fun minusZero(tp: FloatingPointType): Constant {
        return when (tp) {
            Type.F32 -> F32Value(-0.0f)
            Type.F64 -> F64Value(-0.0)
            else -> TODO()
        }
    }

    private fun run() {
        for (data in functions) {
            data.blocks.blocks().forEach {
                handleBlock(it)
            }
        }
    }

    private fun handleBlock(bb: Block) {
        val instructions = bb.instructions()
        var idx = 0

        while (idx < instructions.size) {
            val inst = instructions[idx]
            idx += 1

            if (inst !is Neg) {
                continue
            }

            val type = inst.type()
            if (type !is FloatingPointType) {
                continue
            }

            val xor = bb.insert(idx - 1) {
                it.arithmeticBinary(inst.operand(), ArithmeticBinaryOp.Xor, minusZero(type))
            }

            for (user in inst.usedIn().map { it }) {
                for ((idxUse, use) in user.operands().withIndex()) {
                    if (use != inst) {
                        continue
                    }

                    user.update(idxUse, xor)
                }
            }
            bb.remove(idx)
        }
    }

    companion object {
        fun run(module: Module): Module {
            val functions = module.functions.map { it }
            ReplaceFloatNeg(functions).run()

            return CSSAModule(functions, module.externFunctions, module.constants)
        }
    }
}