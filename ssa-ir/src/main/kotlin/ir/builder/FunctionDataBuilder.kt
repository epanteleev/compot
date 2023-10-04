package ir.builder

import ir.*
import ir.utils.TypeCheck

class FunctionDataBuilder private constructor(
    private val prototype: FunctionPrototype,
    private var argumentValues: List<ArgumentValue>,
    private val blocks: BasicBlocks,
    private var value: Int
) {
    private var allocatedLabel: Int = 0
    private var bb: BasicBlock = blocks.begin()

    private fun allocateValue(): Int {
        val currentValue = value
        value += 1
        return currentValue
    }

    private fun allocateBlock(): BasicBlock {
        allocatedLabel += 1
        val bb = BasicBlock.empty(allocatedLabel)
        blocks.putBlock(bb)
        return bb
    }

    private fun withOutput(f: (Int) -> ValueInstruction): Value {
        val value = allocateValue()
        val instruction = f(value)

        bb.append(instruction)
        return instruction
    }

    private fun insert(instruction: Instruction) {
        bb.append(instruction)
    }

    fun begin(): BasicBlock {
        return blocks.begin()
    }

    fun build(): FunctionData {
        return FunctionData.create(prototype, blocks, argumentValues)
    }

    fun createLabel(): BasicBlock = allocateBlock()

    fun switchLabel(label: Label) {
        bb = blocks.findBlock(label)
    }

    fun argument(index: Int): ArgumentValue = argumentValues[index]

    fun arguments(): List<ArgumentValue> {
        return argumentValues
    }

    fun arithmeticUnary(op: ArithmeticUnaryOp, value: Value): Value {
        return withOutput { it: Int -> ArithmeticUnary(it, value.type(), op, value) }
    }

    fun arithmeticBinary(a: Value, op: ArithmeticBinaryOp, b: Value): Value {
        if (a.type() != b.type()) {
            throw ModuleException("Operands have different types: a=${a.type()}, b=${b.type()}")
        }

        return withOutput { it: Int -> ArithmeticBinary(it, a.type(), a, op, b) }
    }

    fun intCompare(a: Value, pred: IntPredicate, b: Value): Value {
        val cmp = withOutput { it: Int -> IntCompare(it, a, pred, b) }
        if (!TypeCheck.checkIntCompare(cmp as IntCompare)) {
            throw ModuleException("Operands have different types: a=${a.type()}, b=${b.type()}")
        }

        return cmp
    }

    fun load(ptr: Value): Value {
        val load = withOutput { it: Int -> Load(it, ptr) }
        if (!TypeCheck.checkLoad(load as Load)) {
            throw ModuleException("Inconsistent types: ${load.dump()}")
        }

        return load
    }

    fun store(ptr: Value, value: Value) {
        val store = Store(ptr, value)
        if (!TypeCheck.checkStore(store)) {
            throw ModuleException("Inconsistent types: ${store.dump()}")
        }

        insert(store)
    }

    fun call(func: AnyFunctionPrototype, args: ArrayList<Value>): Value {
        if (func.type() == Type.Void) {
            val call = VoidCall(func, args)
            insert(call)
            return Value.UNDEF
        }

        val call = withOutput { it: Int -> Call(it, func.type(), func, args) }
        if (!TypeCheck.checkCall(call as Call)) {
            throw ModuleException("Inconsistent types: ${call.dump()}")
        }

        return call
    }

    fun branch(target: Label) {
        insert(Branch(blocks.findBlock(target)))
    }

    fun branchCond(value: Value, onTrue: Label, onFalse: Label) {
        insert(BranchCond(value, blocks.findBlock(onTrue), blocks.findBlock(onFalse)))
    }

    fun stackAlloc(ty: Type, size: Long): Value {
        val alloc = withOutput { it: Int -> StackAlloc(it, ty, size) }
        if (!TypeCheck.checkAlloc(alloc as StackAlloc)) {
            throw ModuleException("Inconsistent types: ${alloc.dump()}")
        }

        return alloc
    }

    fun ret(value: Value) {
        insert(Return(value))
    }

    fun gep(source: Value, index: Value): Value {
        val gep = withOutput { it: Int -> GetElementPtr(it, source.type(), source, index) }
        if (!TypeCheck.checkGep(gep as GetElementPtr)) {
            throw ModuleException("Inconsistent types: ${gep.dump()}")
        }

        return gep
    }

    fun cast(value: Value, ty: Type, cast: CastType): Value {
        val castInst = withOutput { it: Int -> Cast(it, ty, cast, value) }
        if (!TypeCheck.checkCast(castInst as Cast)) {
            throw ModuleException("Inconsistent types: ${castInst.dump()}")
        }

        return castInst
    }

    fun select(cond: Value, onTrue: Value, onFalse: Value): Value {
        val selectInst = withOutput { it: Int -> Select(it, onTrue.type(), cond, onTrue, onFalse) }
        if (!TypeCheck.checkSelect(selectInst as Select)) {
            throw ModuleException("Inconsistent types: ${selectInst.dump()}")
        }

        return selectInst
    }

    fun phi(incoming: ArrayList<Value>, labels: ArrayList<BasicBlock>): Value {
        val phi = withOutput { it: Int -> Phi(it, incoming[0].type(), labels, incoming) }

        if (!TypeCheck.checkPhi(phi as Phi)) {
            throw ModuleException("Operands have different types: labels=$labels")
        }

        return phi
    }
    
    companion object {
        fun create(
            name: String,
            returnType: Type,
            arguments: List<Type>,
            argumentValues: List<ArgumentValue>
        ): FunctionDataBuilder {
            val prototype = FunctionPrototype(name, returnType, arguments)
            val maxIndex = if (argumentValues.isNotEmpty()) {
                argumentValues.maxBy { it.defined() }.defined()
            } else {
                -1
            }

            val startBB = BasicBlock.empty(Label.entry.index)
            val basicBlocks = BasicBlocks.create(startBB)

            val builder = FunctionDataBuilder(prototype, argumentValues, basicBlocks, maxIndex + 1)
            builder.switchLabel(startBB)
            return builder
        }

        fun create(name: String, returnType: Type, argumentTypes: List<Type>): FunctionDataBuilder {
            val argumentValues = argumentTypes.withIndex().mapTo(arrayListOf()) {
                ArgumentValue(it.index, it.value)
            }

            return create(name, returnType, argumentTypes, argumentValues)
        }
    }
}