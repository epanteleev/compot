package sema

import parser.nodes.*
import parser.nodes.visitors.StatementVisitor
import kotlin.collections.get


enum class StmState {
    REACHABLE,
    EXITED,
    TERMINATED,
}

class StatementAnalysis(): StatementVisitor<StmState> {
    private val state = hashMapOf<Statement, StmtInfo>()
    private val stack = StmtStack()
    private var currentState: StmState = StmState.REACHABLE
    private var lastSeenStmt: Statement? = null

    private fun lastSeen(statement: Statement, fn: (Statement?) -> StmState): StmState {
        val current = lastSeenStmt
        lastSeenStmt = statement
        return fn(current)
    }

    override fun visit(emptyStatement: EmptyStatement): StmState {
        state[emptyStatement] = SomeStatementInfo(currentState, currentState, emptyStatement)
        return currentState
    }

    override fun visit(exprStatement: ExprStatement): StmState {
        state[exprStatement] = SomeStatementInfo(currentState, currentState, exprStatement)
        return currentState
    }

    private fun gotoLabel(labeledStatement: LabeledStatement) {
        val labelState = state[labeledStatement]
        if (labelState?.before == StmState.REACHABLE) {
            return
        }

        val initial = LabeledStatementInfo(currentState, currentState, labeledStatement, false)
        state[labeledStatement] = initial
        val after = labeledStatement.stmt.accept(this)
        initial.after = after
    }

    private fun isFallthroughGoto(prev: Statement?, labeledStatement: LabeledStatement): Boolean {
        if (prev == null) {
            return true
        }

        val prevState = state[prev]
        if (prev is GotoStatement && prev.name() == labeledStatement.name() && prevState?.before == StmState.REACHABLE) {
            return true
        }
        return prevState?.after == StmState.REACHABLE
    }

    override fun visit(labeledStatement: LabeledStatement): StmState = lastSeen(labeledStatement) { prev ->
        val labelState = state[labeledStatement] as? LabeledStatementInfo
        val isFallThrough = isFallthroughGoto(prev, labeledStatement)
        if (labelState != null && labelState.before == StmState.REACHABLE) {
            labelState.fallthrough = isFallThrough
            currentState = labelState.after

            return@lastSeen currentState
        }

        val initial = LabeledStatementInfo(currentState, currentState, labeledStatement, isFallThrough)
        state[labeledStatement] = initial
        val after = labeledStatement.stmt.accept(this)
        currentState = after
        initial.after = currentState
        return@lastSeen currentState
    }

    private fun isLabelUnreachable(label: LabeledStatement): Boolean {
        val labelState = state[label]
        if (labelState == null) {
            return true
        }

        return labelState.before == StmState.EXITED || labelState.before == StmState.TERMINATED
    }

    override fun visit(gotoStatement: GotoStatement): StmState = lastSeen(gotoStatement) {
        val initial = SomeStatementInfo(currentState, StmState.TERMINATED, gotoStatement)
        state[gotoStatement] = initial
        val label = gotoStatement.label() ?:
            throw IllegalStateException("Goto statement without label: ${gotoStatement.name()}")

        if (currentState == StmState.REACHABLE && isLabelUnreachable(label)) {
            gotoLabel(label)
        }

        currentState = if (initial.before == StmState.EXITED) {
            StmState.EXITED
        } else {
            StmState.TERMINATED
        }
        initial.after = currentState
        return@lastSeen currentState
    }

    override fun visit(continueStatement: ContinueStatement): StmState = lastSeen(continueStatement)  {
        val loop = stack.peekTopLoop()
        loop.continues.add(continueStatement)

        val initial = SomeStatementInfo(currentState, StmState.TERMINATED, continueStatement)
        state[continueStatement] = initial
        currentState = if (currentState == StmState.EXITED) {
            StmState.EXITED
        } else {
            StmState.TERMINATED
        }

        initial.after = currentState
        return@lastSeen currentState
    }

    override fun visit(breakStatement: BreakStatement): StmState = lastSeen(breakStatement) {
        when (val loop = stack.top()) {
            is LoopInfo -> {
                loop.breaks.add(breakStatement)
            }
            is SwitchItemInfo -> {
                val switch = stack.peekTopSwitch()
                switch.states.last().after = StmState.TERMINATED
            }
            else -> {}
        }

        val stmtState = SomeStatementInfo(currentState, StmState.TERMINATED, breakStatement)
        state[breakStatement] = stmtState
        currentState = if (currentState == StmState.EXITED) {
            StmState.EXITED
        } else {
            StmState.TERMINATED
        }
        stmtState.after = currentState
        return@lastSeen currentState
    }

    private fun propagateFallThrough(info: SwitchInfo, state: StmState, skipLast: Boolean) {
        for ((idx, st) in info.states.reversed().withIndex()) {
            if (idx == 0 && skipLast) {
                continue
            }

            if (st.after == StmState.REACHABLE) {
                st.after = state
            } else {
                break
            }
        }
    }

    private fun handleSwitchItem(switchItem: SwitchItem): StmState = lastSeen(switchItem) {
        val before = currentState
        val info = stack.peekTopSwitch()
        val switchState = state[info.stmt()]!!
        currentState = switchState.before

        val caseInfo = SwitchItemInfo(switchItem, currentState, currentState)
        state[switchItem] = caseInfo
        info.cases.add(switchItem)
        info.states.add(caseInfo)

        propagateFallThrough(info, before, true)

        val default = stack.scoped(caseInfo) {
            switchItem.stmt().accept(this)
        }

        currentState = default
        caseInfo.after = when (caseInfo.after) {
            StmState.REACHABLE -> default
            StmState.EXITED -> StmState.EXITED
            StmState.TERMINATED -> StmState.TERMINATED
        }
        propagateFallThrough(info, default, true)

        return@lastSeen currentState //TODO return caseInfo.after???
    }

    override fun visit(defaultStatement: DefaultStatement): StmState = handleSwitchItem(defaultStatement)
    override fun visit(caseStatement: CaseStatement): StmState = handleSwitchItem(caseStatement)

    override fun visit(returnStatement: ReturnStatement): StmState = lastSeen(returnStatement) {
        val initial = SomeStatementInfo(currentState, StmState.EXITED, returnStatement)
        state[returnStatement] = initial
        currentState = StmState.EXITED
        return@lastSeen currentState
    }

    private fun handleCompoundStatement(compoundStatement: CompoundStatement): StmState {
        val stmtInfo = SomeStatementInfo(currentState, currentState, compoundStatement)
        state[compoundStatement] = stmtInfo
        for (item in compoundStatement.statements) {
            currentState = when (item) {
                is CompoundStmtDeclaration -> currentState
                is CompoundStmtStatement -> item.statement.accept(this)
            }
        }

        stmtInfo.after = currentState
        return currentState
    }

    override fun visit(compoundStatement: CompoundStatement): StmState = lastSeen(compoundStatement) {
        return@lastSeen handleCompoundStatement(compoundStatement)
    }

    override fun visit(ifElseStatement: IfElseStatement): StmState = lastSeen(ifElseStatement) {
        val initial = SomeStatementInfo(currentState, currentState, ifElseStatement)
        state[ifElseStatement] = initial

        val thenState = ifElseStatement.then.accept(this)
        currentState = initial.before

        val elseState = ifElseStatement.elseNode.accept(this)
        currentState = when (thenState) {
            StmState.REACHABLE -> initial.before
            StmState.EXITED -> when (elseState) {
                StmState.REACHABLE -> initial.before
                StmState.EXITED -> StmState.EXITED
                StmState.TERMINATED -> StmState.TERMINATED
            }
            StmState.TERMINATED -> when (elseState) {
                StmState.REACHABLE -> initial.before
                StmState.EXITED -> StmState.TERMINATED
                StmState.TERMINATED -> StmState.TERMINATED
            }
        }

        initial.after = currentState
        return@lastSeen currentState
    }

    override fun visit(ifStatement: IfStatement): StmState = lastSeen(ifStatement) {
        val stmtInfo = SomeStatementInfo(currentState, currentState, ifStatement)
        state[ifStatement] = stmtInfo

        val thenState = ifStatement.then.accept(this)
        if (thenState == StmState.EXITED || thenState == StmState.TERMINATED) {
            currentState = stmtInfo.before
        }

        stmtInfo.after = currentState
        return@lastSeen currentState
    }

    private fun finalLoopState(loopInfo: LoopInfo): StmState {
        val isNoBreaksAndCont = loopInfo.breaks.isEmpty() && loopInfo.continues.isEmpty()
        if (isNoBreaksAndCont && currentState == StmState.EXITED) {
            return StmState.EXITED
        }
        val isAllExited = loopInfo.breaks.all { state[it]?.before == StmState.EXITED }
                && loopInfo.continues.all { state[it]?.before == StmState.EXITED }
        if (isAllExited && currentState == StmState.EXITED) {
            return StmState.EXITED
        }

        currentState = loopInfo.before
        return loopInfo.before
    }

    override fun visit(doWhileStatement: DoWhileStatement): StmState = lastSeen(doWhileStatement) {
        val loopInfo = LoopInfo(currentState, currentState, doWhileStatement)
        state[doWhileStatement] = loopInfo

        stack.scoped(loopInfo) {
            currentState = doWhileStatement.body.accept(this)
        }

        val stateAfter = finalLoopState(loopInfo)
        loopInfo.after = stateAfter
        return@lastSeen stateAfter
    }

    override fun visit(whileStatement: WhileStatement): StmState = lastSeen(whileStatement) {
        val loopInfo = LoopInfo(currentState, currentState, whileStatement)
        state[whileStatement] = loopInfo

        stack.scoped(loopInfo) {
            whileStatement.body.accept(this)
            currentState = loopInfo.before
        }

        val stateAfter = finalLoopState(loopInfo)
        loopInfo.after = stateAfter
        return@lastSeen stateAfter
    }

    override fun visit(forStatement: ForStatement): StmState = lastSeen(forStatement) {
        val loop = LoopInfo(currentState, currentState, forStatement)
        state[forStatement] = loop
        stack.scoped(loop) {
            when (val init = forStatement.init) {
                is ForInitDeclaration, is ForInitEmpty -> {}
                is ForInitExpression -> init.expression.accept(this)
            }
            currentState = loop.before
            forStatement.body.accept(this)
            currentState = loop.before
        }

        return@lastSeen finalLoopState(loop)
    }

    override fun visit(switchStatement: SwitchStatement): StmState = lastSeen(switchStatement) {
        val info = SwitchInfo(currentState, currentState, switchStatement, mutableListOf())
        state[switchStatement] = info

        stack.scoped(info) {
            currentState = switchStatement.body.accept(this)
        }

        propagateFallThrough(info, currentState, false)
        if (info.isTerminator()) {
            currentState = StmState.EXITED
            return@lastSeen currentState
        } else {
            currentState = info.before
        }

        info.after = currentState
        return@lastSeen currentState
    }

    companion object {
        fun analyze(function: FunctionNode): StatementAnalysisResult {
            val analysis = StatementAnalysis()
            analysis.handleCompoundStatement(function.body)
            //function.body.accept(analysis)
            return StatementAnalysisResult(analysis.state)
        }
    }
}