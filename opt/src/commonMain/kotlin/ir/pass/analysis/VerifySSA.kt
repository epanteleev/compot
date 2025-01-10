package ir.pass.analysis

import ir.value.LocalValue
import ir.module.Module
import ir.instruction.*
import ir.instruction.lir.*
import ir.module.block.Block
import ir.module.block.Label
import ir.module.FunctionData
import ir.module.AnyFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor
import ir.pass.analysis.dominance.DominatorTreeFabric
import ir.pass.analysis.traverse.PreOrderFabric
import ir.types.AggregateType
import ir.types.VoidType
import ir.value.ArgumentValue
import ir.value.UsableValue
import ir.value.constant.BoolValue
import ir.value.Value


class ValidateSSAErrorException(val functionData: FunctionData, override val message: String): Exception(message) {
    override fun toString(): String {
        return "ValidateSSAErrorException: $message"
    }
}

class VerifySSA private constructor(private val functionData: FunctionData,
                                    private val prototypes: List<AnyFunctionPrototype>): IRInstructionVisitor<Unit>() {
    private val dominatorTree by lazy { functionData.analysis(DominatorTreeFabric) }
    private var bb = functionData.begin()
    private var exitBlocks = 0
    private val adjustStackFrame = arrayListOf<AdjustStackFrame>()

    private fun pass() {
        validateArguments()
        validateEdges()
        for (bb in functionData.analysis(PreOrderFabric)) {
            validateBlock(bb)
        }
        validateExitBlock()
    }

    private fun validateEdges() {
        for (bb in functionData) {
            if (bb.predecessors().isEmpty() && bb != Label.entry) {
                throw ValidateSSAErrorException(functionData, "Block '$bb' has no predecessors.")
            }
            for (succ in bb.successors()) {
                assert(succ.predecessors().contains(bb)) { "Block '$succ' doesn't have predecessor '$bb'" }
            }
        }
    }

    private fun validateArguments() {
        val prototype = functionData.prototype
        for ((idx, arg) in functionData.arguments().withIndex()) {
            for (user in arg.usedIn()) {
                assert(user.containsOperand(arg)) {
                    "should be inst='${arg}', user='${user.dump()}', usedIn='${arg.usedIn()}'"
                }
            }

            val byVal = prototype.byValue(idx) ?: continue
            assert(arg.attributes.contains(byVal)) {
                "Argument '${arg}' must have the same attributes as prototype argument '$byVal'"
            }
            assert(arg.type() is AggregateType) {
                "Argument '${arg}' must have a aggregate type, but found '${arg.type()}'"
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
        assert(block.lastOrNull() is TerminateInstruction) { "Block '$block' must have a terminator." }
        bb = block
        validateInstructions(block)
    }

    /** Check whether definition dominates to usage. */
    private fun validateDefUse(instruction: Instruction, block: Block) {
        instruction.operands { use ->
            if (use !is LocalValue) {
                return@operands
            }
            if (use is ArgumentValue) {
                return@operands
            }
            use as Instruction

            val definedIn = use.owner()
            val isDefDominatesUse = dominatorTree.dominates(definedIn, block)
            assert(isDefDominatesUse) { "Definition doesn't dominate to usage: value defined in '$definedIn', but used in '$block'" }
        }
    }

    private fun validatePredicate(user: Instruction, condition: Value) = when (condition) {
        is BoolValue -> {}
        is CompareInstruction -> {
            val prev = bb.idom(user)
            assert(prev is CompareInstruction) {
                "Previous instruction must be an comparison: '${prev?.dump()}'"
            }
            val cmp = prev as CompareInstruction
            assert(cmp === condition) {
                "Comparison operands must have the same type: '${cmp.dump()}'"
            }
        }
        else -> assert(false) { "Condition must be a boolean value: '$condition'" }
    }

    private fun validateInstructions(block: Block) {
        for (instruction in block) {
            if (instruction !is Phi) {
                validateDefUse(instruction, block)
            }

            if (instruction is UsableValue) {
                for (user in instruction.usedIn()) {
                    assert(user.containsOperand(instruction)) {
                        "should be inst='${instruction.dump()}', user='${user.dump()}', usedIn='${instruction.usedIn()}'"
                    }
                }
            }

            instruction.accept(this)
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

    override fun visit(add: Add) {
        assert(Add.typeCheck(add)) {
            "Instruction '${add.dump()}' requires all operands to be of the same type: a=${add.lhs().type()}, b=${add.rhs().type()}"
        }
    }

    override fun visit(and: And) {
        assert(And.typeCheck(and)) {
            "Instruction '${and.dump()}' requires all operands to be of the same type: a=${and.lhs().type()}, b=${and.rhs().type()}"
        }
    }

    override fun visit(mul: Mul) {
        assert(Mul.typeCheck(mul)) {
            "Instruction '${mul.dump()}' requires all operands to be of the same type: a=${mul.lhs().type()}, b=${mul.rhs().type()}"
        }
    }

    override fun visit(or: Or) {
        assert(Or.typeCheck(or)) {
            "Instruction '${or.dump()}' requires all operands to be of the same type: a=${or.lhs().type()}, b=${or.rhs().type()}"
        }
    }

    override fun visit(xor: Xor) {
        assert(Xor.typeCheck(xor)) {
            "Instruction '${xor.dump()}' requires all operands to be of the same type: a=${xor.lhs().type()}, b=${xor.rhs().type()}"
        }
    }

    override fun visit(fadd: Fxor) {
        assert(Fxor.typeCheck(fadd)) {
            "Instruction '${fadd.dump()}' requires all operands to be of the same type: a=${fadd.lhs().type()}, b=${fadd.rhs().type()}"
        }
    }

    override fun visit(div: Div) {
        assert(Div.typeCheck(div)) {
            "Instruction '${div.dump()}' requires all operands to be of the same type: a=${div.lhs().type()}, b=${div.rhs().type()}"
        }
    }

    override fun visit(sub: Sub) {
        assert(Sub.typeCheck(sub)) {
            "Instruction '${sub.dump()}' requires all operands to be of the same type: a=${sub.lhs().type()}, b=${sub.rhs().type()}"
        }
    }

    override fun visit(shr: Shr) {
        assert(Shr.typeCheck(shr)) {
            "Instruction '${shr.dump()}' requires all operands to be of the same type: a=${shr.lhs().type()}, b=${shr.rhs().type()}"
        }
    }

    override fun visit(shl: Shl) {
        assert(Shl.typeCheck(shl)) {
            "Instruction '${shl.dump()}' requires all operands to be of the same type: a=${shl.lhs().type()}, b=${shl.rhs().type()}"
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

        validatePredicate(branchCond, branchCond.condition())
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

        validatePredicate(flag2Int, flag2Int.value())
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

    override fun visit(utofp: Unsigned2Float) {
        assert(Unsigned2Float.typeCheck(utofp)) {
            "Instruction '${utofp.dump()}' has inconsistent types."
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

    override fun visit(fptosi: Float2Int) {
        assert(Float2Int.typeCheck(fptosi)) {
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
            if (use is ArgumentValue) {
                return@zip
            }
            use as Instruction
            val actual = use.owner()
            assert(dominatorTree.dominates(actual, incoming)) {
                "${functionData.prototype.shortDescription()}: inconsistent phi instruction $phi: value defined in $incoming, used in $actual"
            }
        }

        val incoming     = phi.incoming()
        val predecessors = bb.predecessors()
        assert(predecessors.size == incoming.size) {
            "Inconsistent phi instruction: incoming blocks and predecessors are not equal. incoming=$incoming predecessors=$predecessors"
        }
        incoming.forEach { inc ->
            assert(predecessors.contains(inc)) { //TODO strict order
                "Inconsistent phi instruction: incoming block $inc is not a predecessor of $bb"
            }
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
        assert(VoidType == retType) {
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

        validatePredicate(select, select.condition())
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

    override fun visit(indexedLoad: IndexedLoad) {
        assert(IndexedLoad.typeCheck(indexedLoad)) {
            "Instruction '${indexedLoad.dump()}' has inconsistent types."
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

    override fun visit(tupleDiv: TupleDiv) {
        assert(TupleDiv.typeCheck(tupleDiv)) {
            "Instruction '${tupleDiv.dump()}' requires all operands to be of the same type: a=${tupleDiv.first().type()}, b=${tupleDiv.second().type()}"
        }

        val projSet = hashSetOf<Int>()
        for (proj in tupleDiv.usedIn()) {
            assert(proj is Projection) {
                "Operand '$proj' must be a '${Projection.NAME}'"
            }
            proj as Projection
            val unchanged = projSet.add(proj.index())
            assert(unchanged) {
                "Projection '$proj' is duplicated in '${tupleDiv.dump()}'"
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

    override fun visit(tupleCall: IndirectionTupleCall) {
        assert(Callable.typeCheck(tupleCall)) {
            "Call instruction '${tupleCall.dump()}' has inconsistent return types."
        }
    }

    override fun visit(intrinsic: Intrinsic) {
        assert(Intrinsic.typeCheck(intrinsic)) {
            "Instruction '${intrinsic.dump()}' has inconsistent types."
        }
    }

    private fun assert(condition: Boolean, message: () -> String) {
        if (!condition) {
            throw ValidateSSAErrorException(functionData, message())
        }
    }

    companion object {
        fun run(module: Module): Module {
            val prototypes = module.prototypes
            for (data in module.functions.values) {
                try {
                    VerifySSA(data, prototypes).pass()
                } catch (e: ValidateSSAErrorException) {
                    throw e
                } catch (e: Exception) {
                    throw ValidateSSAErrorException(data, e.message + e.stackTraceToString())
                }
            }

            return module
        }
    }
}