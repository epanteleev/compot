package gen

import ir.module.block.Label
import ir.module.builder.impl.FunctionDataBuilder
import ir.value.IntegerConstant


class StmtStack {
    private val stack = mutableListOf<StmtInfo>()

    private inline fun<T: StmtInfo> push(stmtInfo: T): T {
        stack.add(stmtInfo)
        return stmtInfo
    }

    fun<T: StmtInfo> scoped(stmtInfo: T, closure: (T) -> Unit) {
        push(stmtInfo)
        closure(stmtInfo)
        pop()
    }

    private fun pop() {
        stack.removeAt(stack.size - 1)
    }

    fun top(): StmtInfo {
        return stack[stack.size - 1]
    }

    fun topLoop(): LoopStmtInfo? {
        return stack.findLast { it is LoopStmtInfo } as LoopStmtInfo?
    }

    fun topSwitchOrLoop(): StmtInfo? {
        return stack.findLast { it is LoopStmtInfo || it is SwitchStmtInfo }
    }
}

sealed class StmtInfo {
    private var exitBB: Label? = null

    fun exit(): Label? {
        return exitBB
    }

    fun resolveExit(ir: FunctionDataBuilder): Label {
        if (exitBB == null) {
            exitBB = ir.createLabel()
        }

        return exitBB as Label
    }
}

class SwitchStmtInfo(val default: Label, val table: MutableList<Label>, val values: MutableList<IntegerConstant>) : StmtInfo()

class LoopStmtInfo : StmtInfo() {
    private var conditionBB: Label? = null

    fun resolveCondition(ir: FunctionDataBuilder): Label {
        if (conditionBB == null) {
            conditionBB = ir.createLabel()
        }

        return conditionBB as Label
    }
}