package sema

import parser.nodes.*
import parser.nodes.visitors.StatementVisitor

enum class StmState {
    REACHABLE,
    EXITED,
    LOOP_TERMINATED,
}

class SwitchInfo(val cases: MutableList<SwitchItem>, val states: MutableList<CaseInfo> = arrayListOf()) {
    fun isTerminator(): Boolean {
        val default = states.find { it.caseStatement is DefaultStatement }
        if (default == null) {
            return false
        }

        return states.all { it.after == StmState.EXITED }
    }
}

class StatementAnalysisResult(private val state: Map<Statement, StmState>, private val switchInfo: Map<SwitchStatement, SwitchInfo>) {
    fun isReachable(statement: Statement): Boolean {
        return state[statement]!! == StmState.REACHABLE
    }

    fun isUnreachable(statement: Statement): Boolean {
        val s = state[statement]!!
        return s == StmState.EXITED || s == StmState.LOOP_TERMINATED
    }

    fun isExited(statement: Statement): Boolean {
        return state[statement]!! == StmState.EXITED
    }


    fun switchItems(switch: SwitchStatement): List<SwitchItem> {
        return switchInfo[switch]?.cases ?: emptyList()
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

    fun peekTopLoop(): Statement {
        val loop = stack.lastOrNull { it is WhileStatement || it is DoWhileStatement || it is ForStatement }
        if (loop == null) {
            throw IllegalStateException("No loop statement on the stack")
        }

        return loop
    }

    fun top(): Statement {
        if (stack.isEmpty()) {
            throw IllegalStateException("Stack is empty, cannot peek")
        }
        return stack[stack.size - 1]
    }

    fun<T> scoped(statement: Statement, closure: (Statement) -> T): T {
        push(statement)
        val ret = closure(statement)
        pop()
        return ret
    }
}

class LoopInfo(val breaks: MutableList<BreakStatement> = arrayListOf(), val continues: MutableList<ContinueStatement> = arrayListOf())
class CaseInfo(val caseStatement: SwitchItem, val before: StmState, var after: StmState)

class StatementAnalysis(): StatementVisitor<StmState> {
    private val state = hashMapOf<Statement, StmState>()
    private val stack = StmtStack()
    private val switchInfo = hashMapOf<SwitchStatement, SwitchInfo>()
    private val loopInfo = hashMapOf<Statement, LoopInfo>()

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
        val initial = union(StmState.REACHABLE)
        state[gotoStatement] = initial
        val label = gotoStatement.label() ?:
            throw IllegalStateException("Goto statement without label: ${gotoStatement.name()}")

        val labelState = state[label]
        if (currentState == StmState.REACHABLE && (labelState == null || labelState == StmState.EXITED || labelState == StmState.LOOP_TERMINATED)) {
            state[label] = StmState.REACHABLE
            label.accept(this)
        }

        currentState = if (initial == StmState.EXITED) {
            StmState.EXITED
        } else {
            StmState.LOOP_TERMINATED
        }

        return currentState
    }

    override fun visit(continueStatement: ContinueStatement): StmState {
        val loop = stack.peekTopLoop()
        loopInfo[loop]!!.continues.add(continueStatement)

        val initial = union(StmState.REACHABLE)
        state[continueStatement] = initial
        currentState = if (initial == StmState.EXITED) {
            StmState.EXITED
        } else {
            StmState.LOOP_TERMINATED
        }

        return currentState
    }

    override fun visit(breakStatement: BreakStatement): StmState {
        val loop = stack.top()
        if (loop is WhileStatement || loop is DoWhileStatement || loop is ForStatement) {
            loopInfo[loop]!!.breaks.add(breakStatement)
        } else if (loop is CaseStatement || loop is DefaultStatement) {
            val switch = stack.peekTopSwitch()
            switchInfo[switch]!!.states.last().after = StmState.LOOP_TERMINATED
        }

        val initial = union(StmState.REACHABLE)
        state[breakStatement] = initial
        currentState = if (initial == StmState.EXITED) {
            StmState.EXITED
        } else {
            StmState.LOOP_TERMINATED
        }

        return currentState
    }

    private fun propagateFallThrough(info: SwitchInfo, state: StmState) {
        for ((idx, st) in info.states.reversed().withIndex()) {
            if (idx == 0) {
                continue
            }

            if (st.after == StmState.REACHABLE) {
                st.after = state
            } else {
                break
            }
        }
    }

    private fun handleSwitchItem(switchItem: SwitchItem): StmState {
        val before = currentState
        val top = stack.peekTopSwitch()
        currentState = state[top]!!

        val info = switchInfo[top]!!

        val lastItem = info.states.lastOrNull()
        val initial = union(StmState.REACHABLE)
        state[switchItem] = initial
        info.cases.add(switchItem)
        val caseInfo = CaseInfo(switchItem, initial, initial)
        info.states.add(caseInfo)

        if (lastItem != null) {
            propagateFallThrough(info, before)
        }

        val default = stack.scoped(switchItem) {
            switchItem.stmt().accept(this)
        }

        currentState = default
        caseInfo.after = when (caseInfo.after) {
            StmState.REACHABLE -> default
            StmState.EXITED -> StmState.EXITED
            StmState.LOOP_TERMINATED -> StmState.LOOP_TERMINATED
        }
        propagateFallThrough(info, default)

        return currentState
    }

    override fun visit(defaultStatement: DefaultStatement): StmState = handleSwitchItem(defaultStatement)
    override fun visit(caseStatement: CaseStatement): StmState = handleSwitchItem(caseStatement)

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
            StmState.REACHABLE -> initial
            StmState.EXITED -> when (elseState) {
                StmState.REACHABLE -> initial
                StmState.EXITED -> StmState.EXITED
                StmState.LOOP_TERMINATED -> StmState.LOOP_TERMINATED
            }
            StmState.LOOP_TERMINATED -> when (elseState) {
                StmState.REACHABLE -> initial
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
        val loop = LoopInfo()
        loopInfo[doWhileStatement] = loop

        stack.scoped(doWhileStatement) {
            currentState = doWhileStatement.body.accept(this)
        }

        val isNoBreaksAndCont = loop.breaks.isEmpty() && loop.continues.isEmpty()
        if (isNoBreaksAndCont && currentState == StmState.EXITED) {
            return StmState.EXITED
        }
        val isAllExited = loop.breaks.all { state[it] == StmState.EXITED } && loop.continues.all { state[it] == StmState.EXITED }
        if (isAllExited && currentState == StmState.EXITED) {
            return StmState.EXITED
        }

        currentState = initial
        return currentState
    }

    override fun visit(whileStatement: WhileStatement): StmState {
        val initial = union(StmState.REACHABLE)
        state[whileStatement] = initial
        val loop = LoopInfo()
        loopInfo[whileStatement] = loop

        stack.scoped(whileStatement) {
            whileStatement.body.accept(this)
            currentState = initial
        }

        val isNoBreaksAndCont = loop.breaks.isEmpty() && loop.continues.isEmpty()
        if (isNoBreaksAndCont && currentState == StmState.EXITED) {
            return StmState.EXITED
        }
        val isAllExited = loop.breaks.all { state[it] == StmState.EXITED } && loop.continues.all { state[it] == StmState.EXITED }
        if (isAllExited && currentState == StmState.EXITED) {
            return StmState.EXITED
        }

        currentState = initial
        return currentState
    }

    override fun visit(forStatement: ForStatement): StmState {
        val initial = union(StmState.REACHABLE)
        state[forStatement] = initial
        val loop = LoopInfo()
        loopInfo[forStatement] = loop
        stack.scoped(forStatement) {
            when (val init = forStatement.init) {
                is ForInitDeclaration, is ForInitEmpty -> {}
                is ForInitExpression -> init.expression.accept(this)
            }
            currentState = initial
            forStatement.body.accept(this)
            currentState = initial
        }

        val isNoBreaksAndCont = loop.breaks.isEmpty() && loop.continues.isEmpty()
        if (isNoBreaksAndCont && currentState == StmState.EXITED) {
            return StmState.EXITED
        }
        val isAllExited = loop.breaks.all { state[it] == StmState.EXITED } && loop.continues.all { state[it] == StmState.EXITED }
        if (isAllExited && currentState == StmState.EXITED) {
            return StmState.EXITED
        }

        currentState = initial
        return currentState
    }

    override fun visit(switchStatement: SwitchStatement): StmState {
        val initial = union(StmState.REACHABLE)
        state[switchStatement] = initial

        val sInfo = SwitchInfo(mutableListOf())
        switchInfo[switchStatement] = sInfo
        stack.scoped(switchStatement) {
            currentState = switchStatement.body.accept(this)
        }

        val lastItem = sInfo.states.lastOrNull()
        if (lastItem != null) {
            propagateFallThrough(sInfo, currentState)
        }

        val info = switchInfo[switchStatement] ?: throw IllegalStateException("Switch statement info not found")
        if (info.isTerminator()) {
            currentState = StmState.EXITED
            return currentState
        }

        currentState = initial
        return currentState
    }

    companion object {
        fun analyze(function: FunctionNode): StatementAnalysisResult {
            val analysis = StatementAnalysis()
            function.body.accept(analysis)
            return StatementAnalysisResult(analysis.state, analysis.switchInfo)
        }
    }
}