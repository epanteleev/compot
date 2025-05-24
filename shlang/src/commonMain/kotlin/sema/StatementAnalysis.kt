package sema

import parser.nodes.BreakStatement
import parser.nodes.CaseStatement
import parser.nodes.CompoundStatement
import parser.nodes.CompoundStmtDeclaration
import parser.nodes.CompoundStmtStatement
import parser.nodes.ContinueStatement
import parser.nodes.DefaultStatement
import parser.nodes.DoWhileStatement
import parser.nodes.EmptyStatement
import parser.nodes.ExprStatement
import parser.nodes.ForStatement
import parser.nodes.FunctionNode
import parser.nodes.GotoStatement
import parser.nodes.IfElseStatement
import parser.nodes.IfStatement
import parser.nodes.LabeledStatement
import parser.nodes.ReturnStatement
import parser.nodes.Statement
import parser.nodes.SwitchStatement
import parser.nodes.WhileStatement
import parser.nodes.visitors.StatementVisitor

enum class StmState {
    REACHABLE,
    EXITED,
    LOOP_TERMINATED,
}

class StatementAnalysisResult(val state: Map<Statement, StmState>) {
    fun isReachable(statement: Statement): Boolean {
        return state[statement]!! == StmState.REACHABLE
    }

    fun isUnreachable(statement: Statement): Boolean {
        val s = state[statement]!!
        return s == StmState.EXITED || s == StmState.LOOP_TERMINATED
    }

    override fun toString(): String = buildString {
        append("Statement Analysis Result:\n")
        for ((statement, state) in state) {
            append("  |${statement.begin()}| ${statement.accept(StmName)}: $state\n")
        }
    }
}

private class StmtStack {
    private val stack = ArrayList<Statement>()

    fun push(statement: Statement) {
        stack.add(statement)
    }

    fun pop(): Statement {
        if (stack.isEmpty()) {
            throw IllegalStateException("Stack is empty, cannot pop")
        }
        return stack.removeAt(stack.size - 1)
    }

    fun peekTopSwitch(): SwitchStatement {
        val switch = stack.lastOrNull { it is SwitchStatement }
        if (switch == null) {
            throw IllegalStateException("No switch statement on the stack")
        }

        return switch as SwitchStatement
    }

    fun<T> scoped(statement: Statement, closure: (Statement) -> T): T {
        push(statement)
        val ret = closure(statement)
        pop()
        return ret
    }
}

class SwtichInfo(val cases: MutableList<CaseStatement>, val states: MutableList<StmState> = arrayListOf(), var defaultState: StmState? = null, var defaultStatement: DefaultStatement? = null)


class StatementAnalysis(): StatementVisitor<StmState> {
    private val state = hashMapOf<Statement, StmState>()
    private val stack = StmtStack()

    private val switchInfo = hashMapOf<SwitchStatement, SwtichInfo>()
    private var currentState: StmState = StmState.REACHABLE

    private fun union(state: StmState) : StmState {
        val st = when (currentState) {
            StmState.REACHABLE -> state
            StmState.EXITED -> StmState.EXITED
            StmState.LOOP_TERMINATED -> if (state == StmState.REACHABLE) StmState.LOOP_TERMINATED else state
        }

        currentState = st
        return st
    }

    override fun visit(emptyStatement: EmptyStatement): StmState {
        state[emptyStatement] = union(StmState.REACHABLE)
        return currentState
    }

    override fun visit(exprStatement: ExprStatement): StmState {
        state[exprStatement] = union(StmState.REACHABLE)
        return currentState
    }

    override fun visit(labeledStatement: LabeledStatement): StmState {
        if (state[labeledStatement] == StmState.REACHABLE) {
            currentState = StmState.REACHABLE
            return labeledStatement.stmt.accept(this)
        }

        state[labeledStatement] = union(StmState.REACHABLE)
        return labeledStatement.stmt.accept(this)
    }

    override fun visit(gotoStatement: GotoStatement): StmState {
        state[gotoStatement] = union(StmState.REACHABLE)
        val label = gotoStatement.label() ?:
            throw IllegalStateException("Goto statement without label: ${gotoStatement.name()}")

        val labelState = state[label]
        if (currentState == StmState.REACHABLE && (labelState == null || labelState == StmState.EXITED || labelState == StmState.LOOP_TERMINATED)) {
            state[label] = StmState.REACHABLE
            label.accept(this)
        }

        currentState = StmState.LOOP_TERMINATED
        return currentState
    }

    override fun visit(continueStatement: ContinueStatement): StmState {
        state[continueStatement] = union(StmState.REACHABLE)
        currentState = StmState.LOOP_TERMINATED
        return currentState
    }

    override fun visit(breakStatement: BreakStatement): StmState {
        state[breakStatement] = union(StmState.REACHABLE)
        currentState = StmState.LOOP_TERMINATED
        return currentState
    }

    override fun visit(defaultStatement: DefaultStatement): StmState {
        val stateBefore = currentState
        val top = stack.peekTopSwitch()
        currentState = state[top]!!

        val info = switchInfo[top]!!
        if (info.cases.isNotEmpty() && info.defaultStatement == null) {
            info.defaultState = stateBefore
        }
        info.defaultStatement = defaultStatement

        state[defaultStatement] = union(StmState.REACHABLE)
        return currentState
    }

    override fun visit(caseStatement: CaseStatement): StmState {
        val stateBefore = currentState
        val switchStatement = stack.peekTopSwitch()
        currentState = state[switchStatement]!!

        val info = switchInfo[switchStatement]!!
        if (info.cases.isNotEmpty() && info.defaultStatement == null) {
            info.states.add(stateBefore)
        }
        info.cases.add(caseStatement)

        state[caseStatement] = union(StmState.REACHABLE)
        val case = caseStatement.stmt.accept(this)
        currentState = case
        return case
    }

    override fun visit(returnStatement: ReturnStatement): StmState {
        state[returnStatement] = union(StmState.REACHABLE)
        currentState = StmState.EXITED
        return currentState
    }

    override fun visit(compoundStatement: CompoundStatement): StmState {
        state[compoundStatement] = union(StmState.REACHABLE)
        for (item in compoundStatement.statements) {
            currentState = when (item) {
                is CompoundStmtDeclaration -> currentState
                is CompoundStmtStatement -> item.statement.accept(this)
            }
        }

        return currentState
    }

    override fun visit(ifElseStatement: IfElseStatement): StmState {
        val initial = union(StmState.REACHABLE)
        state[ifElseStatement] = initial

        val thenState = ifElseStatement.then.accept(this)
        currentState = initial

        val elseState = ifElseStatement.elseNode.accept(this)
        currentState = when (thenState) {
            StmState.REACHABLE -> StmState.REACHABLE
            StmState.EXITED -> when (elseState) {
                StmState.REACHABLE -> StmState.REACHABLE
                StmState.EXITED -> StmState.EXITED
                StmState.LOOP_TERMINATED -> StmState.LOOP_TERMINATED
            }
            StmState.LOOP_TERMINATED -> when (elseState) {
                StmState.REACHABLE -> StmState.REACHABLE
                StmState.EXITED -> StmState.LOOP_TERMINATED
                StmState.LOOP_TERMINATED -> StmState.LOOP_TERMINATED
            }
        }

        return currentState
    }

    override fun visit(ifStatement: IfStatement): StmState {
        val initial = union(StmState.REACHABLE)
        state[ifStatement] = initial

        val thenState = ifStatement.then.accept(this)
        if (thenState == StmState.EXITED || thenState == StmState.LOOP_TERMINATED) {
            currentState = initial
        }

        return currentState
    }

    override fun visit(doWhileStatement: DoWhileStatement): StmState {
        val initial = union(StmState.REACHABLE)
        state[doWhileStatement] = initial
        stack.scoped(doWhileStatement) {
            val bodyState = doWhileStatement.body.accept(this)
            currentState = when (bodyState) {
                StmState.REACHABLE -> StmState.REACHABLE
                StmState.LOOP_TERMINATED -> initial
                StmState.EXITED -> StmState.EXITED
            }
        }

        return currentState
    }

    override fun visit(whileStatement: WhileStatement): StmState {
        val initial = union(StmState.REACHABLE)

        state[whileStatement] = initial
        stack.scoped(whileStatement) {
            val bodyState = whileStatement.body.accept(this)
            currentState = when (bodyState) {
                StmState.REACHABLE -> StmState.REACHABLE
                StmState.LOOP_TERMINATED -> initial
                StmState.EXITED -> StmState.EXITED
            }
        }


        return currentState
    }

    override fun visit(forStatement: ForStatement): StmState {
        val initial = union(StmState.REACHABLE)
        state[forStatement] = initial
        stack.scoped(forStatement) {
            val bodyState = forStatement.body.accept(this)
            currentState = when (bodyState) {
                StmState.REACHABLE -> StmState.REACHABLE
                StmState.LOOP_TERMINATED -> initial
                StmState.EXITED -> StmState.EXITED
            }
        }

        return currentState
    }

    override fun visit(switchStatement: SwitchStatement): StmState {
        val initial = union(StmState.REACHABLE)
        state[switchStatement] = initial

        val sInfo = SwtichInfo(mutableListOf())
        switchInfo[switchStatement] = sInfo
        stack.scoped(switchStatement) {
            currentState = switchStatement.body.accept(this)
        }

        if (sInfo.cases.isNotEmpty() && sInfo.defaultStatement == null) {
            sInfo.states.add(currentState)
        }

        val info = switchInfo[switchStatement] ?: throw IllegalStateException("Switch statement info not found")
        val switchStates = info.states.all { it == StmState.EXITED }
        if (switchStates) {
            currentState = if (info.defaultState != null) {
                info.defaultState!!
            } else {
                StmState.REACHABLE
            }
            return currentState
        }

        currentState = initial

        return currentState
    }

    companion object {
        fun analyze(function: FunctionNode): StatementAnalysisResult {
            val analysis = StatementAnalysis()
            function.body.accept(analysis)
            return StatementAnalysisResult(analysis.state)
        }
    }
}