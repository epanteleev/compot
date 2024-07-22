package gen

import ir.module.block.Label


class StmtStack {
    private val stack = mutableListOf<StmtInfo>()

    fun push(stmtInfo: StmtInfo) {
        stack.add(stmtInfo)
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
}

abstract class StmtInfo {
    abstract val exitBB: Label
}

class SwitchStmtInfo(override val exitBB: Label) : StmtInfo()

class LoopStmtInfo(val continueBB: Label, override val exitBB: Label) : StmtInfo()