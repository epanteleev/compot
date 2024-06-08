package ir.platform.x64.pass.transform

import ir.*
import ir.types.Type
import asm.x64.ImmInt
import asm.x64.ImmInt.Companion.canBeImm32
import ir.global.GlobalConstant
import ir.global.GlobalSymbol
import ir.instruction.Instruction
import ir.module.Module
import ir.module.block.Block
import ir.types.PrimitiveType
import ir.module.FunctionData
import ir.module.SSAModule
import ir.platform.x64.CallConvention


// Move large constant to constant pool
class MoveLargeConstants private constructor(val functions: List<FunctionData>, private val constants: MutableSet<GlobalSymbol>) {
    private var constantIndex = 0

    private fun run() {
        for (data in functions) {
            data.blocks.blocks().forEach {
                handleBlock(it)
            }
        }
    }

    private fun makeConstantOrNull(operand: Constant): GlobalConstant? {
        val global = when {
            operand is U64Value && !canBeImm32(operand.u64) -> GlobalConstant.of("$prefix${constantIndex}", Type.U64, operand.u64)
            operand is I64Value && !canBeImm32(operand.i64) -> GlobalConstant.of("$prefix${constantIndex}", Type.I64, operand.i64)
            operand is F32Value ->  {
                val bits = operand.f32.toBits().toLong()
                if (bits == ImmInt.minusZeroFloat) { //TODO DANGER!!!!
                    GlobalConstant.of(CallConvention.FLOAT_SUB_ZERO_SYMBOL, Type.U64, bits)
                } else {
                    GlobalConstant.of("$prefix${constantIndex}", Type.I64, operand.f32.toBits())
                }
            }
            operand is F64Value -> {
                val bits = operand.f64.toBits()
                if (bits == ImmInt.minusZeroDouble) { //TODO DANGER!!!!
                    GlobalConstant.of(CallConvention.DOUBLE_SUB_ZERO_SYMBOL, Type.U64, bits)
                } else {
                    GlobalConstant.of("$prefix${constantIndex}", Type.I64, operand.f64.toBits())
                }
            }
            else -> null
        }
        constantIndex += 1
        return global
    }

    private fun handleBlock(bb: Block) {
        bb.transform { inst ->
            var lastInserted: Instruction? = null
            for ((opIdx, operand) in inst.operands().withIndex()) {
                if (operand !is Constant) {
                    continue
                }

                val constant = makeConstantOrNull(operand) ?: continue
                constants.add(constant)

                val loadedConstant = bb.insertBefore(inst) {
                    it.load(operand.type() as PrimitiveType, constant)
                }
                lastInserted = loadedConstant
                inst.update(opIdx, loadedConstant)
            }
            lastInserted?: inst
        }
    }

    companion object {
        private const val prefix = CallConvention.CONSTANT_POOL_PREFIX

        fun run(module: Module): Module {
            val functions = module.functions.map { it }
            val constants = module.globals.mapTo(mutableSetOf()) { it }
            MoveLargeConstants(functions, constants).run()

            return SSAModule(functions, module.externFunctions, constants, module.types)
        }
    }
}