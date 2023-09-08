package ir.pass.ana

import ir.*
import ir.utils.CreationInfo
import ir.utils.TypeCheck

data class ValidateSSAErrorException(override val message: String): Exception(message)

class VerifySSA private constructor(private val functionData: FunctionData) {
    private val dominatorTree by lazy { functionData.blocks.dominatorTree() }
    private val creation by lazy { CreationInfo.create(functionData.blocks) }

    private fun pass() {
        for (bb in functionData.blocks) {
            validateBlock(bb)
        }
        validateExitBlock()
    }

    private fun validateExitBlock() {
        var exitBlocks = 0
        for (bb in functionData.blocks) {
            if (bb.flowInstruction() is Return) {
                exitBlocks += 1
            }
        }

        assert(exitBlocks == 1) { "Allowed only one exit block, but found $exitBlocks blocks."}
    }

    private fun validateBlock(bb: BasicBlock) {
        if (bb.equals(Label.entry)) {
            assert(bb.predecessors.isEmpty()) { "Begin block must not have predecessors." }
        }
        assert(!bb.isEmpty()) { "Block must not be empty" }

        when (bb.flowInstruction()) {
            is Branch -> {
                assert(bb.successors.size == 1) { "Block $bb has more that 1 predecessor." }
            }
            is BranchCond -> {
                assert(bb.successors.size == 2) { "Block $bb has more that 2 predecessors." }
            }
            is Return -> {
                assert(bb.successors.isEmpty()) { "Block $bb has predecessors ${bb.index()}." }
            }
        }

        validateInstructions(bb)
    }

    private fun validatePhi(phi: Phi, bb: BasicBlock) {
        for ((use, incoming) in phi.usages.zip(phi.incoming())) {
            if (use !is ValueInstruction) {
                continue
            }
            val actual = creation.get(use).block
            assert(dominatorTree.dominates(actual, incoming)) { "Inconsistent phi instruction: value defined in $incoming, used in $actual " }
        }

        val incoming = phi.incoming()
        val predecessors = bb.predecessors
        if (predecessors.size != incoming.size || !predecessors.containsAll(incoming)) {
            val message = StringBuilder().append("Inconsistent phi instruction: incoming blocks and predecessors are not equal. incoming=")
            incoming.joinTo(message)
            message.append(" predecessors=")
            predecessors.joinTo(message)

            throw ValidateSSAErrorException(message.toString())
        }
    }

    /** Check whether definition dominates to usage. */
    private fun validateDefUse(instruction: Instruction, bb: BasicBlock) {
        for (use in instruction.usages) {
            if (use !is ValueInstruction) {
                continue
            }
            val definedIn = creation.get(use).block
            val isDefDominatesUse = dominatorTree.dominates(definedIn, bb)
            assert(isDefDominatesUse) { "Definition doesn't dominate to usage: value defined in $definedIn, but used in $bb" }
        }
    }

    private fun validateTypes(bb: BasicBlock) {
        for (instruction in bb) {
            when (instruction) {
                is Phi -> {
                    assert(TypeCheck.checkPhi(instruction)) { "Inconsistent phi instruction '${instruction.dump()}': different types ${instruction.usages.map { it.type() }.joinToString()}" }
                }
                is ArithmeticUnary -> {
                    fun message() = "Unary instruction '${instruction.dump()}' must have the same type: destination=${instruction.type()} operand=${instruction.operand().type()}"
                    assert(TypeCheck.checkUnary(instruction)) { message() }
                }
                is ArithmeticBinary -> {
                    fun message() = "Binary arithmetic instruction '${instruction.dump()}' requires all operands to be of the same type: a=${instruction.first().type()}, b=${instruction.second().type()}"
                    assert(TypeCheck.checkBinary(instruction)) { message() }
                }
                is IntCompare -> {
                    fun message() = "Compare instruction '${instruction.dump()}' requires all operands to be of the same type: a=${instruction.first().type()}, b=${instruction.second().type()}"
                    assert(TypeCheck.checkIntCompare(instruction)) { message() }
                }
                is Load -> {
                    fun message() = "Load instruction '${instruction.dump()}' requires all operands to be of the same type."
                    assert(TypeCheck.checkLoad(instruction)) { message() }
                }
                is Store -> {
                    fun message() = "Store instruction '${instruction.dump()}' requires all operands to be of the same type."
                    assert(TypeCheck.checkStore(instruction)) { message() }
                }
                is Call -> {
                    fun message() = "Call instruction '${instruction.dump()}' has inconsistent return types."
                    assert(TypeCheck.checkCall(instruction)) { message() }
                }
                is Cast -> {
                    fun message() = "Cast instruction '${instruction.dump()}' has inconsistent types."
                    assert(TypeCheck.checkCast(instruction)) { message() }
                }
                is Select -> {
                    fun message() = "Select instruction '${instruction.dump()}' requires all operands to be of the same type."
                    assert(TypeCheck.checkSelect(instruction)) { message() }
                }
                is GetElementPtr -> {
                    fun message() = "Gep instruction '${instruction.dump()}' has inconsistent types."
                    assert(TypeCheck.checkGep(instruction)) { message() }
                }
            }
        }
    }

    private fun validateControlFlow(bb: BasicBlock) {
        for (instruction in bb) {
            when (instruction) {
                is Phi -> validatePhi(instruction, bb)
                else -> validateDefUse(instruction, bb)
            }
        }
    }

    private fun validateInstructions(bb: BasicBlock) {
        validateControlFlow(bb)
        validateTypes(bb)
    }

    companion object {
        fun run(module: Module): Module {
            module.functions.forEach { data ->
                VerifySSA(data).pass()
            }

            return module
        }

        private fun assert(condition: Boolean, message: () -> String) {
            if (!condition) {
                throw ValidateSSAErrorException(message())
            }
        }
    }
}