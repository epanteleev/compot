package sema

import parser.nodes.*
import kotlin.collections.*


class StatementAnalysisResult(private val state: Map<Statement, StmState>) {
    fun isReachable(statement: Statement): Boolean {
        return state[statement]!! == StmState.REACHABLE
    }

    fun isUnreachable(statement: Statement): Boolean {
        val s = state[statement]!!
        return s == StmState.EXITED || s == StmState.TERMINATED
    }

    fun isExited(statement: Statement): Boolean {
        return state[statement]!! == StmState.EXITED
    }

    override fun toString(): String = buildString {
        append("Statement Analysis Result:\n")
        for ((statement, state) in state) {
            append("  |${statement.begin()}| ${statement.accept(StmName)}: $state\n")
        }
    }
}