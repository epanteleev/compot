package ir.pass.ana

import ir.AnyFunctionPrototype
import ir.instruction.*
import ir.module.FunctionData
import ir.module.Module
import ir.module.block.Block
import ir.module.block.Label
import ir.utils.CreationInfo

data class ValidateSSAErrorException(override val message: String): Exception(message)

class VerifySSA private constructor(private val functionData: FunctionData, private val prototypes: List<AnyFunctionPrototype>) {
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
            if (bb.last() is Return) {
                exitBlocks += 1
            }
        }

        assert(exitBlocks == 1) { "Allowed only one exit block, but found $exitBlocks blocks."}
    }

    private fun validateBlock(bb: Block) {
        if (bb.equals(Label.entry)) {
            assert(bb.predecessors().isEmpty()) { "Begin block must not have predecessors." }
        }
        assert(!bb.isEmpty()) { "Block must not be empty" }
        val successors = bb.successors()
        when (val flow = bb.last()) {
            is Branch -> {
                val target = flow.target()
                assert(successors.size == 1) {
                    "Block $bb has other count of successors: successors=$successors"
                }
                assert(target == successors[0]) {
                    "Block $bb has inconsistent successors: branch=${flow.targets.joinToString { it.toString() }}, successors=${successors}"
                }
                assert(target.predecessors().contains(bb)) {
                    "Block $target has inconsistent predecessors: branch=${flow.targets.joinToString { it.toString() }}, predecessors=${target.predecessors()}"
                }
            }
            is BranchCond -> {
                val onTrue  = flow.onTrue()
                val onFalse = flow.onFalse()
                assert(bb.successors().size == 2) { "Block $bb has not 2 successors exactly: ${bb.successors()}" }
                assert(onTrue == successors[0]) {
                    "Block $bb has inconsistent successors: branch=${flow.targets.joinToString { it.toString() }}, successors=${successors}"
                }
                assert(onFalse == successors[1]) {
                    "Block $bb has inconsistent successors: branch=${flow.targets.joinToString { it.toString() }}, successors=${successors}"
                }
                assert(onTrue.predecessors().contains(bb)) {
                    "Block $onTrue has inconsistent predecessors: branch=${flow.targets.joinToString { it.toString() }}, predecessors=${onTrue.predecessors()}"
                }
                assert(onFalse.predecessors().contains(bb)) {
                    "Block $onTrue has inconsistent predecessors: branch=${flow.targets.joinToString { it.toString() }}, predecessors=${onTrue.predecessors()}"
                }
            }
            is Return -> {
                assert(bb.successors().isEmpty()) { "Block $bb has predecessors ${bb.index}." }
            }
        }

        validateInstructions(bb)
    }

    private fun validatePhi(phi: Phi, bb: Block) {
        for ((use, incoming) in phi.operands().zip(phi.incoming())) {
            if (use !is ValueInstruction) {
                continue
            }
            val actual = creation.get(use).block
            assert(dominatorTree.dominates(actual, incoming)) { "Inconsistent phi instruction $phi: value defined in $incoming, used in $actual " }
        }

        val incomings = phi.incoming()
        val predecessors = bb.predecessors()
        if (predecessors.size != incomings.size) {
            val message = StringBuilder().append("Inconsistent phi instruction: incoming blocks and predecessors are not equal. incoming=")
            incomings.joinTo(message)
            message.append(" predecessors=")
            predecessors.joinTo(message)

            throw ValidateSSAErrorException(message.toString())
        }
    }

    /** Check whether definition dominates to usage. */
    private fun validateDefUse(instruction: Instruction, bb: Block) {
        for (use in instruction.operands()) {
            if (use !is ValueInstruction) {
                continue
            }
            val definedIn = creation.get(use).block
            val isDefDominatesUse = dominatorTree.dominates(definedIn, bb)
            assert(isDefDominatesUse) { "Definition doesn't dominate to usage: value defined in $definedIn, but used in $bb" }
        }
    }

    private fun validateTypes(bb: Block) {
        for (instruction in bb) {
            //Todo remove it
            if (instruction is ValueInstruction) {
                for (user in instruction.usedIn()) {
                    assert(user.operands().contains(instruction)) {
                        "should be inst='${instruction.dump()}'"
                    }
                }
            }

            when (instruction) {
                is Phi -> {
                    assert(Phi.isCorrect(instruction)) { "Inconsistent phi instruction '${instruction.dump()}': different types ${instruction.operands().map { it.type() }.joinToString()}" }
                }
                is Neg -> {
                    fun message() = "Neg instruction '${instruction.dump()}' must have the same type: destination=${instruction.type()} operand=${instruction.operand().type()}"
                    assert(Neg.isCorrect(instruction)) { message() }
                }
                is Not -> {
                    fun message() = "Not instruction '${instruction.dump()}' must have the same type: destination=${instruction.type()} operand=${instruction.operand().type()}"
                    assert(Not.isCorrect(instruction)) { message() }
                }
                is ArithmeticBinary -> {
                    fun message() = "Binary arithmetic instruction '${instruction.dump()}' requires all operands to be of the same type: a=${instruction.first().type()}, b=${instruction.second().type()}"
                    assert(ArithmeticBinary.isCorrect(instruction)) { message() }
                }
                is SignedIntCompare -> {
                    fun message() = "Compare instruction '${instruction.dump()}' requires all operands to be of the same type: a=${instruction.first().type()}, b=${instruction.second().type()}"
                    assert(SignedIntCompare.isCorrect(instruction)) { message() }
                }
                is Load -> {
                    fun message() = "Load instruction '${instruction.dump()}' requires all operands to be of the same type."
                    assert(Load.isCorrect(instruction)) { message() }
                }
                is Store -> {
                    fun message() = "Store instruction '${instruction.dump()}' requires all operands to be of the same type."
                    assert(Store.isCorrect(instruction)) { message() }
                }
                is Call -> {
                    fun message() = "Call instruction '${instruction.dump()}' has inconsistent return types."
                    assert(Callable.isCorrect(instruction)) { message() }
                }
                is Bitcast -> {
                    fun message() = "Cast instruction '${instruction.dump()}' has inconsistent types."
                    assert(Bitcast.isCorrect(instruction)) { message() }
                }
                is Select -> {
                    fun message() = "Select instruction '${instruction.dump()}' requires all operands to be of the same type."
                    assert(Select.isCorrect(instruction)) { message() }
                }
                is GetElementPtr -> {
                    fun message() = "Gep instruction '${instruction.dump()}' has inconsistent types."
                    assert(GetElementPtr.isCorrect(instruction)) { message() }
                }
                is BranchCond -> {
                    //TODO("impl me")
                }
            }
        }
    }

    private fun validateControlFlow(bb: Block) {
        for (instruction in bb) {
            when (instruction) {
                is Phi -> validatePhi(instruction, bb)
                is Callable -> {
                    assert(prototypes.contains(instruction.prototype())) {
                        "Called undefined function: prototype=${instruction.prototype()}"
                    }
                    validateDefUse(instruction, bb)
                }
                else -> validateDefUse(instruction, bb)
            }
        }
    }

    private fun validateInstructions(bb: Block) {
        validateControlFlow(bb)
        validateTypes(bb)
    }

    companion object {
        fun run(module: Module): Module {
            val prototypes = module.prototypes
            module.functions.forEach { data ->
                VerifySSA(data, prototypes).pass()
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