package sema

import parser.nodes.*
import kotlin.collections.*


class StatementAnalysisResult(private val state: Map<Statement, StmtInfo>) {
    fun isReachable(statement: Statement): Boolean {
        return state[statement]!!.before == StmState.REACHABLE
    }

    fun isUnreachable(statement: Statement): Boolean {
        val s = state[statement]!!.before
        return s == StmState.EXITED || s == StmState.TERMINATED
    }

    fun isTerminator(statement: Statement): Boolean {
        val s = state[statement]!!.after
        return s == StmState.EXITED || s == StmState.TERMINATED
    }

    override fun toString(): String = buildString {
        append("Statement Analysis Result:\n")
        for ((statement, state) in state) {
            append("  |${statement.begin()}| ${statement.accept(StmName)}: $state\n")
        }
    }
}