package ir.platform.x64.pass.transform

import asm.x64.Imm.Companion.canBeImm32
import ir.global.*
import ir.instruction.IntCompare
import ir.instruction.Unary
import ir.module.Module
import ir.module.block.Block
import ir.module.FunctionData
import ir.module.SSAModule
import ir.platform.x64.CallConvention
import ir.types.FloatingPointType
import ir.value.constant.*


// Move large constant to constant pool
class MoveLargeConstants private constructor(val functions: Map<String, FunctionData>, private val constants: MutableMap<String, GlobalConstant>) {
    private var constantIndex = 0

    private fun run() {
        for (data in functions.values) {
            data.blocks.forEach {
                handleBlock(it)
            }
        }
    }

    private fun makeConstantOrNull(operand: Constant): GlobalConstant? {
        val global = when {
            operand is U64Value && !canBeImm32(operand.u64) -> U64ConstantValue("$PREFIX${constantIndex}", operand.u64.toULong())
            operand is I64Value && !canBeImm32(operand.i64) -> I64ConstantValue("$PREFIX${constantIndex}", operand.i64)
            operand is F32Value -> F32ConstantValue("$PREFIX${constantIndex}", operand.f32)
            operand is F64Value -> F64ConstantValue("$PREFIX${constantIndex}", operand.f64)
            else -> null
        }
        constantIndex += 1
        return global
    }

    private fun handleBlock(bb: Block) {
        bb.transform { inst ->
            if (inst is Unary && inst.type() !is FloatingPointType) {
                return@transform inst
            }
            if (inst is IntCompare) {
                return@transform inst
            }
            for ((i, operand) in inst.operands().withIndex()) {
                if (operand !is Constant) {
                    continue
                }

                val constant = makeConstantOrNull(operand) ?: continue
                constants[constant.name()] = constant
                inst.update(i, constant)
            }
            inst
        }
    }

    companion object {
        private const val PREFIX = CallConvention.CONSTANT_POOL_PREFIX

        fun run(module: Module): Module {
            val functions = module.functions.toMutableMap()
            val constants = hashMapOf<String, GlobalConstant>()
            for ((name, global) in module.constantPool) {
                constants[name] = global
            }
            MoveLargeConstants(functions, constants).run()

            return SSAModule(functions, module.functionDeclarations, constants, module.globals, module.types)
        }
    }
}