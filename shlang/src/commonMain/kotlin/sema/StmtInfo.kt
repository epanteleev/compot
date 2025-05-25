package sema

import parser.nodes.*

sealed class StmtInfo {
    abstract fun stmt(): Statement
}

class SwitchInfo(val switchStatement: SwitchStatement, val cases: MutableList<SwitchItem>, val states: MutableList<SwitchItemInfo> = arrayListOf()): StmtInfo() {
    override fun stmt(): Statement = switchStatement
    fun isTerminator(): Boolean {
        val default = states.find { it.caseStatement is DefaultStatement }
        if (default == null) {
            return false
        }

        return states.all { it.after == StmState.EXITED }
    }
}

class LoopInfo(val loop: Statement, val breaks: MutableList<BreakStatement> = arrayListOf(), val continues: MutableList<ContinueStatement> = arrayListOf()): StmtInfo() {
    override fun stmt(): Statement = loop
}

class SwitchItemInfo(val caseStatement: SwitchItem, val before: StmState, var after: StmState): StmtInfo() {
    override fun stmt(): Statement = caseStatement.stmt()
}
