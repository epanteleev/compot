package ir.platform.x64.pass.transform

import ir.*
import ir.types.Type
import asm.x64.ImmInt
import ir.module.Module
import ir.module.block.Block
import ir.types.PrimitiveType
import ir.module.FunctionData
import ir.platform.x64.CSSAModule
import ir.platform.x64.CallConvention


// Move large constant to data segment
class MoveLargeConstants private constructor(val functions: List<FunctionData>, val constants: MutableSet<GlobalValue>) {
    private var constantIndex = 0

    private fun run() {
        for (data in functions) {
            data.blocks.blocks().forEach {
                handleBlock(it)
            }
        }
    }

    private fun makeConstantOrNull(operand: Constant): GlobalValue? {
        val global = when {
            operand is U64Value /*&& canBeImm32(operand.u64)*/ -> GlobalValue.of("$prefix${constantIndex}", Type.U64, operand.u64)
            operand is I64Value /*&& canBeImm32(operand.i64)*/ -> GlobalValue.of("$prefix${constantIndex}", Type.I64, operand.i64)
            operand is F32Value ->  {
                val bits = operand.f32.toBits().toLong()
                if (bits == ImmInt.minusZeroFloat) { //TODO DANGER!!!!
                    GlobalValue.of(CallConvention.FLOAT_SUB_ZERO_SYMBOL, Type.U64, bits)
                } else {
                    GlobalValue.of("$prefix${constantIndex}", Type.I64, operand.f32.toBits())
                }
            }
            operand is F64Value -> {
                val bits = operand.f64.toBits()
                if (bits == ImmInt.minusZeroDouble) { //TODO DANGER!!!!
                    GlobalValue.of(CallConvention.DOUBLE_SUB_ZERO_SYMBOL, Type.U64, bits)
                } else {
                    GlobalValue.of("$prefix${constantIndex}", Type.I64, operand.f64.toBits())
                }
            }
            else -> null
        }
        constantIndex += 1
        return global
    }

    private fun handleBlock(bb: Block) {
        val instructions = bb.instructions()
        var idx = 0

        while (idx < instructions.size) {
            val inst = instructions[idx]

            for ((opIdx, operand) in inst.operands().withIndex()) {
                if (operand !is Constant) {
                    continue
                }

                val constant = makeConstantOrNull(operand) ?: continue

                constants.add(constant)

                val loadedConstant = bb.insert(inst) {
                    it.load(operand.type() as PrimitiveType, constant)
                }
                idx += 1
                inst.update(opIdx, loadedConstant)
            }
            idx += 1
        }
    }

    companion object {
        private const val prefix = CallConvention.CONSTANT_POOL_PREFIX
        private fun canBeImm32(constant: Long): Boolean {
            return Int.MIN_VALUE <= constant && constant <= Int.MAX_VALUE
        }

        fun run(module: Module): Module {
            val functions = module.functions.map { it }
            val constants = module.globals.mapTo(mutableSetOf()) { it }
            MoveLargeConstants(functions, constants).run()

            return CSSAModule(functions, module.externFunctions, constants, module.types)
        }
    }
}