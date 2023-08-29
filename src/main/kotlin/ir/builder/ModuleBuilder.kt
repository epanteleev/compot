package ir.builder

import ir.*
import ir.Function
import ir.Module
import ir.pass.ana.VerifySSA
import ir.utils.TypeCheck

class ActivePoint(var function: Function, var bb: BasicBlock, var allocatedLabel: Int, var value: Int)

class ModuleBuilder {
    private val functions = hashMapOf<Function, FunctionData>()
    private val externFunctions = mutableSetOf<ExternFunction>()
    private var activePoint: ActivePoint? = null
    private var nextFunction: Int = 0

    private fun allocateBlock(): BasicBlock {
        val ap = activePointUnsafe()
        ap.allocatedLabel += 1

        val bb = BasicBlock.empty(BlockViewer(ap.allocatedLabel))
        findFunction(ap.function).blocks.putBlock(bb)
        return bb
    }

    private fun allocateValue(): Int {
        val ap = activePointUnsafe()
        val value = ap.value

        ap.value = value + 1
        return value
    }

    private fun withOutput(f: (Int) -> ValueInstruction): Value {
        val activePoint = activePointUnsafe()
        val value       = allocateValue()
        val instruction = f(value)

        activePoint.bb.append(instruction)
        return instruction
    }

    private fun activeFunction(): Function = activePointUnsafe().function

    private fun activePointUnsafe(): ActivePoint {
        if (activePoint == null) {
            throw ModuleException("Invalid active point.")
        }

        return activePoint as ActivePoint
    }

    private fun findBlock(label: Label): BasicBlock {
        val bb = when (label) {
            is BasicBlock -> label
            is BlockViewer -> findFunction(activeFunction()).blocks.findBlock(label)
            else -> throw ModuleException("Internal error")
        }
        return bb
    }

    private fun insert(instruction: Instruction) {
        activePointUnsafe().bb.append(instruction)
    }

    fun findFunction(function: Function): FunctionData {
        return functions[function] ?: throw ModuleException("Cannot find function: $function")
    }

    fun switchLabel(label: Label) {
        val ap = activePointUnsafe()
        val bb = findBlock(label)
        activePoint = ActivePoint(ap.function, bb, ap.allocatedLabel, ap.value)
    }

    fun createFunction(name: String, returnType: Type, argumentTypes: List<Type>): Function {
        val argumentValues = {
            argumentTypes.mapTo(arrayListOf()) {
                ArgumentValue(allocateValue(), it)
            }
        }
        return createFunction(name, returnType, argumentTypes, argumentValues)
    }

    fun createFunction(name: String, returnType: Type, argumentTypes: List<Type>, argumentValues: () -> List<ArgumentValue>): Function {
        val functionIndex = nextFunction
        nextFunction += 1
        val function = Function(functionIndex, name, returnType)
        val startBB = BasicBlock.empty(Label.entry)
        activePoint = ActivePoint(function, startBB, 0, 0)

        val data = FunctionData.create(name, returnType, argumentTypes, argumentValues())
        data.blocks.putBlock(startBB)
        val bb = activePoint!!.bb

        functions[function] = data

        switchLabel(bb)
        return function
    }

    fun createExternFunction(name: String, returnType: Type, arguments: List<Type>): ExternFunction {
        val extern = ExternFunction(name, returnType, arguments)
        externFunctions.add(extern)
        return extern
    }

    fun createLabel(): Label = allocateBlock()

    fun argument(index: Int): Value = findFunction(activeFunction()).arguments()[index]

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
        return withOutput { it: Int -> IntCompare(it, a, pred, b) }
    }

    fun load(ptr: Value): Value {
        return withOutput { it: Int -> Load(it, ptr) }
    }

    fun store(ptr: Value, value: Value) {
        val ptrType   = ptr.type()
        val valueType = value.type()

        if (ptrType.dereferenceOrNull() != valueType) {
            throw ModuleException("Operands have bad types: ptr=${ptr.type()}, value=${value.type()}")
        }

        insert(Store(ptr, value))
    }

    fun call(func: AnyFunction, args: ArrayList<Value>): Value {
        return withOutput { it: Int -> Call(it, func.type(), func, args) }
    }

    fun branch(target: Label) {
        insert(Branch(findBlock(target)))
    }

    fun branchCond(value: Value, onTrue: Label, onFalse: Label) {
        insert(BranchCond(value, findBlock(onTrue), findBlock(onFalse)))
    }

    fun stackAlloc(ty: Type, size: Long): Value {
        return withOutput { it: Int -> StackAlloc(it, ty, size) }
    }

    fun ret(value: Value) {
        insert(Return(value))
    }

    fun gep(source: Value, index: Value): Value {
        if (!index.type().isArithmetic()) {
            throw ModuleException("Index must be arithmetic type: index=${index.type()}")
        }
        if (source.type().dereferenceOrNull() == null) {
            throw ModuleException("Source must be pointer: source=${source.type()}")
        }

        return withOutput { it: Int -> GetElementPtr(it, source.type(), source, index) }
    }

    fun cast(value: Value, ty: Type, cast: CastType): Value {
        return withOutput { it: Int -> Cast(it, ty, cast, value) }
    }

    fun select(cond: Value, onTrue: Value, onFalse: Value): Value {
        if (onTrue.type() !== onFalse.type()) {
            throw ModuleException("Operands have different types: onTrue=${onTrue.type()}, onFalse=${onFalse.type()}")
        }

        return withOutput { it: Int -> Select(it, onTrue.type(), cond, onTrue, onFalse) }
    }

    fun phi(incoming: ArrayList<Value>, labels: List<Label>): Value {
        val phi = withOutput { it: Int -> Phi(it, incoming[0].type(), labels, incoming) }

        if (!TypeCheck.checkPhi(phi as Phi)) {
            throw ModuleException("Operands have different types: labels=$labels")
        }

        return phi
    }

    fun build(): Module {
        return VerifySSA.run(Module(functions, externFunctions))
    }

    companion object {
        fun create() : ModuleBuilder {
            return ModuleBuilder()
        }
    }
}