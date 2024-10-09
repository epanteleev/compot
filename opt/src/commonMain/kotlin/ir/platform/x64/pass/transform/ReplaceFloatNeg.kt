package ir.platform.x64.pass.transform

import ir.types.*
import ir.instruction.*
import ir.module.FunctionData
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.value.constant.Constant
import ir.value.constant.F32Value
import ir.value.constant.F64Value


class ReplaceFloatNeg private constructor(val functions: Map<String, FunctionData>) {
    private fun minusZero(tp: FloatingPointType): Constant {
        return when (tp) {
            Type.F32 -> F32Value(-0.0f)
            Type.F64 -> F64Value(-0.0)
            else -> TODO()
        }
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
            if (inst !is Neg) {
                return inst
            }

            val type = inst.type()
            if (type !is FloatingPointType) {
                return inst
            }

            return bb.replace(inst) { it.xor(inst.operand(), minusZero(type)) }
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