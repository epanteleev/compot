package gen

import ir.module.block.Label


class StmtStack {
    private val stack = mutableListOf<StmtInfo>()

    fun push(stmtInfo: StmtInfo): StmtInfo {
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

class SwitchStmtInfo(val exitBB: Label) : StmtInfo()

class LoopStmtInfo(val continueBB: Label, val exitBB: Label) : StmtInfo()

class IfStmtInfo(val elseBB: Label, val exitBB: Label) : StmtInfo()