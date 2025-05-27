package sema

import parser.nodes.*

sealed class StmtInfo(val before: StmState, internal var after: StmState) {
    abstract fun stmt(): Statement
}

class SwitchInfo(before: StmState, after: StmState, val switchStatement: SwitchStatement, internal val cases: MutableList<SwitchItem>, internal val states: MutableList<SwitchItemInfo> = arrayListOf()): StmtInfo(before, after) {
    override fun stmt(): Statement = switchStatement

    fun isTerminator(): Boolean {
        val default = states.find { it.caseStatement is DefaultStatement }
        if (default == null) {
            return false
        }

        return states.all { it.after == StmState.EXITED }
    }
}

class LoopInfo(before: StmState, after: StmState, val loop: Statement, val breaks: MutableList<BreakStatement> = arrayListOf(), val continues: MutableList<ContinueStatement> = arrayListOf()): StmtInfo(before, after) {
    override fun stmt(): Statement = loop
}

class SwitchItemInfo(val caseStatement: SwitchItem, before: StmState, after: StmState): StmtInfo(before, after) {
    override fun stmt(): Statement = caseStatement.stmt()
}

class SomeStatementInfo(before: StmState, after: StmState, val statement: Statement): StmtInfo(before, after) {
    override fun stmt(): Statement = statement
}