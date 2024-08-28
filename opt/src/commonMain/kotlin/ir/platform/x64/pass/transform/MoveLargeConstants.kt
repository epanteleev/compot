package ir.platform.x64.pass.transform

import ir.types.Type
import asm.x64.ImmInt.Companion.canBeImm32
import ir.global.GlobalConstant
import ir.module.Module
import ir.module.block.Block
import ir.module.FunctionData
import ir.module.SSAModule
import ir.platform.x64.CallConvention
import ir.value.*


// Move large constant to constant pool
class MoveLargeConstants private constructor(val functions: List<FunctionData>, private val constants: MutableMap<String, GlobalConstant>) {
    private var constantIndex = 0

    private fun run() {
        for (data in functions) {
            data.blocks.forEach {
                handleBlock(it)
            }
        }
    }

    private fun makeConstantOrNull(operand: Constant): GlobalConstant? {
        val global = when {
            operand is U64Value && !canBeImm32(operand.u64) -> GlobalConstant.of("$prefix${constantIndex}", Type.U64, operand.u64)
            operand is I64Value && !canBeImm32(operand.i64) -> GlobalConstant.of("$prefix${constantIndex}", Type.I64, operand.i64)
            operand is F32Value -> GlobalConstant.of("$prefix${constantIndex}", Type.F32, operand.f32)
            operand is F64Value -> GlobalConstant.of("$prefix${constantIndex}", Type.F64, operand.f64)
            else -> null
        }
        constantIndex += 1
        return global
    }

    private fun handleBlock(bb: Block) {
        bb.transform { inst ->
            inst.operandsWithIndex { opIdx, operand ->
                if (operand !is Constant) {
                    return@operandsWithIndex
                }

                val constant = makeConstantOrNull(operand) ?: return@operandsWithIndex
                constants[constant.name()] = constant
                inst.update(opIdx, constant)
            }
            inst
        }
    }

    companion object {
        private const val prefix = CallConvention.CONSTANT_POOL_PREFIX

        fun run(module: Module): Module {
            val functions = module.functions.map { it }
            val constants = hashMapOf<String, GlobalConstant>()
            for ((name, global) in module.constantPool) {
                constants[name] = global
            }
            MoveLargeConstants(functions, constants).run()

            return SSAModule(functions, module.externFunctions, constants, module.globals, module.types)
        }
    }
}