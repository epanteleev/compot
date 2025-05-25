package sema

import parser.nodes.BreakStatement
import parser.nodes.ContinueStatement
import parser.nodes.DefaultStatement
import parser.nodes.SwitchItem


class SwitchInfo(val cases: MutableList<SwitchItem>, val states: MutableList<CaseInfo> = arrayListOf()) {
    fun isTerminator(): Boolean {
        val default = states.find { it.caseStatement is DefaultStatement }
        if (default == null) {
            return false
        }

        return states.all { it.after == StmState.EXITED }
    }
}


class LoopInfo(val breaks: MutableList<BreakStatement> = arrayListOf(), val continues: MutableList<ContinueStatement> = arrayListOf())
class CaseInfo(val caseStatement: SwitchItem, val before: StmState, var after: StmState)
