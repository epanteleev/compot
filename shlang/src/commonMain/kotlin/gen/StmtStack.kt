package gen

import ir.module.block.Label
import ir.value.IntegerConstant
import ir.value.Value


class StmtStack {
    private val stack = mutableListOf<StmtInfo>()

    fun<T: StmtInfo> push(stmtInfo: T): T {
        stack.add(stmtInfo)
        return stmtInfo
    }

    fun pop() {
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

abstract class StmtInfo

class SwitchStmtInfo(val exitBB: Label, val switchValue: Value, val default: Label, val table: MutableList<Label>, val values: MutableList<IntegerConstant>) : StmtInfo()

class LoopStmtInfo(val continueBB: Label, val exitBB: Label) : StmtInfo()

class IfStmtInfo(val elseBB: Label, val exitBB: Label) : StmtInfo()