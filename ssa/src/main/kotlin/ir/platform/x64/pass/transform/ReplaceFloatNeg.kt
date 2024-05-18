package ir.platform.x64.pass.transform

import ir.*
import ir.instruction.*
import ir.module.FunctionData
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.types.FloatingPointType
import ir.types.Type


class ReplaceFloatNeg private constructor(val functions: List<FunctionData>) {
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
        fun closure(bb: Block, inst: Instruction): Instruction {
            if (inst !is Neg) {
                return inst
            }

            val type = inst.type()
            if (type !is FloatingPointType) {
                return inst
            }

            val xor = bb.insertBefore(inst) {
                it.arithmeticBinary(inst.operand(), ArithmeticBinaryOp.Xor, minusZero(type))
            }

            ValueInstruction.replaceUsages(inst, xor)
            bb.kill(inst)
            return xor
        }

        bb.transform { inst -> closure(bb, inst) }
    }

    companion object {
        fun run(module: Module): Module {
            val functions = module.functions.map { it }
            ReplaceFloatNeg(functions).run()

            return SSAModule(functions, module.externFunctions, module.globals, module.types)
        }
    }
}