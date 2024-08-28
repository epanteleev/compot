package ir.pass.analysis

import ir.value.LocalValue
import ir.types.Type
import ir.module.Module
import ir.instruction.*
import ir.instruction.lir.*
import ir.utils.CreationInfo
import ir.module.block.Block
import ir.module.block.Label
import ir.module.FunctionData
import ir.module.AnyFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor
import ir.pass.analysis.dominance.DominatorTreeFabric
import ir.pass.analysis.traverse.PreOrderFabric


class ValidateSSAErrorException(val module: Module, override val message: String): Exception(message) {
    override fun toString(): String {
        return "ValidateSSAErrorException: $message"
    }
}

class VerifySSA private constructor(private val module: Module, private val functionData: FunctionData,
                                    private val prototypes: List<AnyFunctionPrototype>): IRInstructionVisitor<Unit>() {
    private val dominatorTree by lazy { functionData.analysis(DominatorTreeFabric) }
    private val creation by lazy { CreationInfo.create(functionData) }
    private var bb = functionData.begin()
    private var exitBlocks = 0
    private val adjustStackFrame = arrayListOf<AdjustStackFrame>()

    private fun pass() {
        validateArguments()
        for (bb in functionData.analysis(PreOrderFabric)) {
            validateBlock(bb)
        }
        validateExitBlock()
    }

    private fun validateArguments() {
        for (arg in functionData.arguments()) {
            for (user in arg.usedIn()) {
                assert(user.containsOperand(arg)) {
                    "should be inst='${arg}', user='${user.dump()}', usedIn='${arg.usedIn()}'"
                }
            }
        }
    }

    private fun validateExitBlock() {
        assert(exitBlocks == 1) {
            "Allowed only one exit block, but found $exitBlocks blocks."
        }

        assert(adjustStackFrame.isEmpty()) {
            "Stack frame is not balanced: $adjustStackFrame"
        }
    }

    private fun checkReturn() {
        assert(bb.successors().isEmpty()) {
            "Exit block '$bb' has successors: ${bb.successors()}"
        }
        exitBlocks += 1
    }

    private fun validateBlock(block: Block) {
        if (block.equals(Label.entry)) {
            val predecessors = block.predecessors()
            assert(predecessors.isEmpty()) { "Begin block must not have predecessors: $predecessors" }
        }
        assert(!block.isEmpty()) { "Block '$block' must not be empty" }
        bb = block
        validateInstructions(block)
    }

    /** Check whether definition dominates to usage. */
    private fun validateDefUse(instruction: Instruction, block: Block) {
        instruction.operands { use ->
            if (use !is LocalValue) {
                return@operands
            }

            val definedIn = creation[use].block
            val isDefDominatesUse = dominatorTree.dominates(definedIn, block)
            assert(isDefDominatesUse) { "Definition doesn't dominate to usage: value defined in '$definedIn', but used in '$block'" }
        }
    }

    private fun validateInstructions(block: Block) {
        for (instruction in block) {
            if (instruction !is Phi) {
                validateDefUse(instruction, block)
            }

            if (instruction is LocalValue) {
                for (user in instruction.usedIn()) {
                    assert(user.containsOperand(instruction)) {
                        "should be inst='${instruction.dump()}', user='${user.dump()}', usedIn='${instruction.usedIn()}'"
                    }
                }
            }

            instruction.visit(this)
        }
    }

    override fun visit(alloc: Alloc) {
        assert(Alloc.typeCheck(alloc)) {
            "Instruction '${alloc.dump()}' has inconsistent types."
        }
    }

    override fun visit(generate: Generate) {
        assert(Generate.typeCheck(generate)) {
            "Instruction '${generate.dump()}' has inconsistent types."
        }
    }

    override fun visit(lea: Lea) {
        assert(Lea.typeCheck(lea)) {
            "Instruction '${lea.dump()}' has inconsistent types."
        }
    }

    override fun visit(binary: ArithmeticBinary) {
        assert(ArithmeticBinary.typeCheck(binary)) {
            "Instruction '${binary.dump()}' requires all operands to be of " +
                    "the same type: a=${binary.first().type()}, b=${binary.second().type()}"
        }
    }

    override fun visit(neg: Neg) {
        assert(Neg.typeCheck(neg)) {
            "Instruction '${neg.dump()}' must have the same types: destination=${neg.type()} operand=${neg.operand().type()}"
        }
    }

    override fun visit(not: Not) {
        assert(Not.typeCheck(not)) {
            "Instruction '${not.dump()}' must have the same types: destination=${not.type()} operand=${not.operand().type()}"
        }
    }

    override fun visit(branch: Branch) {
        val target = branch.target()
        val successors = bb.successors()

        assert(bb.successors().size == 1) {
            "Block '$bb' has other count of successors: successors=$successors"
        }
        assert(target == successors[0]) {
            "Block '$bb' has inconsistent successors: branch=${branch.targets.joinToString { it.toString() }}, successors='${successors}'"
        }
        assert(target.predecessors().contains(bb)) {
            "Block '$target' has inconsistent predecessors: branch=${branch.targets.joinToString { it.toString() }}, predecessors='${target.predecessors()}'"
        }
    }

    override fun visit(branchCond: BranchCond) {
        val onTrue  = branchCond.onTrue()
        val onFalse = branchCond.onFalse()
        val successors = bb.successors()

        assert(BranchCond.typeCheck(branchCond)) {
            "Inconsistent branch condition: '${branchCond.dump()}'"
        }
        assert(bb.successors().size == bb.last().targets().size) {
            "Block '$bb' has other count of successors: successors=$successors, terminate='${bb.last()}'"
        }
        assert(onTrue == successors[0]) {
            "Block '$bb' has inconsistent successors: branch='${branchCond.dump()}', successors='${successors}'"
        }
        assert(onFalse == successors[1]) {
            "Block '$bb' has inconsistent successors: branch='${branchCond.dump()}', successors='${successors}'"
        }
        assert(onTrue.predecessors().contains(bb)) {
            "Block '$onTrue' has inconsistent predecessors: branch='${branchCond.dump()}', predecessors='${onTrue.predecessors()}'"
        }
        assert(onFalse.predecessors().contains(bb)) {
            "Block '$onTrue' has inconsistent predecessors: branch='${branchCond.dump()}', predecessors='${onTrue.predecessors()}'"
        }
    }

    override fun visit(call: Call) {
        assert(Callable.typeCheck(call)) {
            "Call instruction '${call.dump()}' has inconsistent return types."
        }

        assert(prototypes.contains(call.prototype())) {
            "Called undefined function: prototype=${call.prototype()}"
        }
    }

    override fun visit(tupleCall: TupleCall) {
        assert(Callable.typeCheck(tupleCall)) {
            "Call instruction '${tupleCall.dump()}' has inconsistent return types."
        }

        assert(prototypes.contains(tupleCall.prototype())) {
            "Called undefined function: prototype=${tupleCall.prototype()}"
        }
    }

    override fun visit(flag2Int: Flag2Int) {
        assert(Flag2Int.typeCheck(flag2Int)) {
            "Instruction '${flag2Int.dump()}' has inconsistent types."
        }
    }

    override fun visit(bitcast: Bitcast) {
        assert(Bitcast.typeCheck(bitcast)) {
            "Instruction '${bitcast.dump()}' has inconsistent types."
        }
    }

    override fun visit(itofp: Int2Float) {
        assert(Int2Float.typeCheck(itofp)) {
            "Instruction '${itofp.dump()}' has inconsistent types."
        }
    }

    override fun visit(zext: ZeroExtend) {
        assert(ZeroExtend.typeCheck(zext)) {
            "Instruction '${zext.dump()}' has inconsistent types."
        }
    }

    override fun visit(sext: SignExtend) {
        assert(SignExtend.typeCheck(sext)) {
            "Instruction '${sext.dump()}' has inconsistent types."
        }
    }

    override fun visit(trunc: Truncate) {
        assert(Truncate.typeCheck(trunc)) {
            "Instruction '${trunc.dump()}' has inconsistent types."
        }
    }

    override fun visit(fptruncate: FpTruncate) {
        assert(FpTruncate.typeCheck(fptruncate)) {
            "Instruction '${fptruncate.dump()}' has inconsistent types."
        }
    }

    override fun visit(fpext: FpExtend) {
        assert(FpExtend.typeCheck(fpext)) {
            "Instruction '${fpext.dump()}' has inconsistent types."
        }
    }

    override fun visit(fptosi: FloatToInt) {
        assert(FloatToInt.typeCheck(fptosi)) {
            "Instruction '${fptosi.dump()}' has inconsistent types."
        }
    }

    override fun visit(copy: Copy) {
        assert(Copy.typeCheck(copy)) {
            "Instruction '${copy.dump()}' has inconsistent types."
        }
    }

    override fun visit(move: Move) {
        assert(Move.typeCheck(move)) {
            "Instruction '${move.dump()}' has inconsistent types."
        }
    }

    override fun visit(downStackFrame: DownStackFrame) {
        adjustStackFrame.add(downStackFrame)
    }

    override fun visit(gep: GetElementPtr) {
        assert(GetElementPtr.typeCheck(gep)) {
            "Instruction '${gep.dump()}' has inconsistent types."
        }
    }

    override fun visit(gfp: GetFieldPtr) {
        assert(GetFieldPtr.typeCheck(gfp)) {
            "Instruction '${gfp.dump()}' has inconsistent types."
        }
    }

    override fun visit(icmp: IntCompare) {
        assert(IntCompare.typeCheck(icmp)) {
            "Instruction '${icmp.dump()}' requires all operands to be of the same type: a=${icmp.first().type()}, b=${icmp.second().type()}"
        }
    }

    override fun visit(fcmp: FloatCompare) {
        assert(FloatCompare.typeCheck(fcmp)) {
            "Instruction '${fcmp.dump()}' requires all operands to be of the same type: a=${fcmp.first().type()}, b=${fcmp.second().type()}"
        }
    }

    override fun visit(load: Load) {
        assert(Load.typeCheck(load)) {
            "Instruction '${load.dump()}' requires all operands to be of the same type."
        }
    }

    override fun visit(phi: Phi) {
        assert(Phi.typeCheck(phi)) {
            val operands = buildString {
                phi.operands {
                    append(it.type())
                    append(" ")
                }
            }
            "Inconsistent phi instruction '${phi.dump()}': different types $operands"
        }

        phi.zip { incoming, use ->
            if (use !is LocalValue) {
                return@zip
            }
            val actual = creation[use].block
            assert(dominatorTree.dominates(actual, incoming)) {
                "Inconsistent phi instruction $phi: value defined in $incoming, used in $actual"
            }
        }

        val incoming     = phi.incoming()
        val predecessors = bb.predecessors()
        assert(predecessors.size == incoming.size) {
            "Inconsistent phi instruction: incoming blocks and predecessors are not equal. incoming=$incoming predecessors=$predecessors"
        }
    }

    override fun visit(returnValue: ReturnValue) {
        assert(ReturnValue.typeCheck(returnValue)) {
            "Inconsistent return value: '${returnValue.dump()}'"
        }

        val retType = functionData.prototype.returnType()
        assert(returnValue.type() == retType) {
            "Inconsistent return type: '${returnValue.dump()}', but expected '${retType}'"
        }

        checkReturn()
    }

    override fun visit(returnVoid: ReturnVoid) {
        val retType = functionData.prototype.returnType()
        assert(Type.Void == retType) {
            "Inconsistent return type: '${returnVoid.dump()}', but expected '${retType}'"
        }

        checkReturn()
    }

    override fun visit(indirectionCall: IndirectionCall) {
        assert(Callable.typeCheck(indirectionCall)) {
            "Call instruction '${indirectionCall.dump()}' has inconsistent return types."
        }
    }

    override fun visit(indirectionVoidCall: IndirectionVoidCall) {
        assert(Callable.typeCheck(indirectionVoidCall)) {
            "Call instruction '${indirectionVoidCall.dump()}' has inconsistent return types."
        }
    }

    override fun visit(select: Select) {
        assert(Select.typeCheck(select)) {
            "Instruction '${select.dump()}' requires all operands to be of the same type."
        }
    }

    override fun visit(store: Store) {
        assert(Store.typeCheck(store)) {
            "Instruction '${store.dump()}' requires all operands to be of the same type."
        }
    }

    override fun visit(upStackFrame: UpStackFrame) {
        val downStackFrame = adjustStackFrame.removeAt(adjustStackFrame.size - 1)
        val downCall = downStackFrame.call().prototype()
        val upCall = upStackFrame.call().prototype()
        assert(upCall == downCall) {
            "Inconsistent stack frame size: down='${downCall}' up='${upCall}'"
        }
    }

    override fun visit(voidCall: VoidCall) {
        assert(Callable.typeCheck(voidCall)) {
            "Call instruction '${voidCall.dump()}' has inconsistent return types."
        }

        assert(prototypes.contains(voidCall.prototype())) {
            "Called undefined function: prototype=${voidCall.prototype()}"
        }
    }

    override fun visit(int2ptr: Int2Pointer) {
        assert(Int2Pointer.typeCheck(int2ptr)) {
            "Instruction '${int2ptr.dump()}' has inconsistent types."
        }
    }

    override fun visit(ptr2Int: Pointer2Int) {
        assert(Pointer2Int.typeCheck(ptr2Int)) {
            "Instruction '${ptr2Int.dump()}' has inconsistent types."
        }
    }

    override fun visit(copy: IndexedLoad) {
        assert(IndexedLoad.typeCheck(copy)) {
            "Instruction '${copy.dump()}' has inconsistent types."
        }
    }

    override fun visit(move: MoveByIndex) {
        assert(MoveByIndex.typeCheck(move)) {
            "Instruction '${move.dump()}' has inconsistent types."
        }
    }

    override fun visit(store: StoreOnStack) {
        assert(StoreOnStack.typeCheck(store)) {
            "Instruction '${store.dump()}' has inconsistent types."
        }
    }

    override fun visit(loadst: LoadFromStack) {
        assert(LoadFromStack.typeCheck(loadst)) {
            "Instruction '${loadst.dump()}' has inconsistent types."
        }
    }

    override fun visit(leaStack: LeaStack) {
        assert(LeaStack.typeCheck(leaStack)) {
            "Instruction '${leaStack.dump()}' has inconsistent types."
        }
    }

    override fun visit(binary: TupleDiv) {
        assert(TupleDiv.typeCheck(binary)) {
            "Instruction '${binary.dump()}' requires all operands to be of the same type: a=${binary.first().type()}, b=${binary.second().type()}"
        }

        val projSet = hashSetOf<Int>()
        for (proj in binary.usedIn()) {
            assert(proj is Projection) {
                "Operand '$proj' must be a '${Projection.NAME}'"
            }
            proj as Projection
            val unchanged = projSet.add(proj.index())
            assert(unchanged) {
                "Projection '$proj' is duplicated in '${binary.dump()}'"
            }
        }
    }

    override fun visit(memcpy: Memcpy) {
        assert(memcpy.destination() != memcpy.source()) {
            "Inconsistent memcpy instruction: destination=${memcpy.destination()}, source=${memcpy.source()}"
        }

        assert(Memcpy.typeCheck(memcpy)) {
            "Instruction '${memcpy.dump()}' has inconsistent types."
        }
    }

    override fun visit(proj: Projection) {
        assert(Projection.typeCheck(proj)) {
            "Instruction '${proj.dump()}' has inconsistent types."
        }
    }

    override fun visit(switch: Switch) {
        assert(Switch.typeCheck(switch)) {
            "Instruction '${switch.dump()}' has inconsistent types."
        }
    }

    private fun assert(condition: Boolean, message: () -> String) {
        if (!condition) {
            throw ValidateSSAErrorException(module, message())
        }
    }

    companion object {
        fun run(module: Module): Module {
            val prototypes = module.prototypes
            module.functions.values.forEach { data ->
                VerifySSA(module, data, prototypes).pass()
            }

            return module
        }
    }
}