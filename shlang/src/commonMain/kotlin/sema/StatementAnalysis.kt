package sema

import parser.nodes.*
import parser.nodes.visitors.StatementVisitor

enum class StmState {
    REACHABLE,
    EXITED,
    LOOP_TERMINATED,
}


class StatementAnalysis(): StatementVisitor<StmState> {
    private val state = hashMapOf<Statement, StmState>()
    private val stack = StmtStack()
    private val switchInfo = hashMapOf<SwitchStatement, SwitchInfo>()
    private val loopInfo = hashMapOf<Statement, LoopInfo>()

    private var currentState: StmState = StmState.REACHABLE

    override fun visit(emptyStatement: EmptyStatement): StmState {
        state[emptyStatement] = currentState
        return currentState
    }

    override fun visit(exprStatement: ExprStatement): StmState {
        state[exprStatement] = currentState
        return currentState
    }

    override fun visit(labeledStatement: LabeledStatement): StmState {
        if (state[labeledStatement] == StmState.REACHABLE) {
            currentState = StmState.REACHABLE
            return labeledStatement.stmt.accept(this)
        }

        state[labeledStatement] = currentState
        return labeledStatement.stmt.accept(this)
    }

    override fun visit(gotoStatement: GotoStatement): StmState {
        val initial = currentState
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

        val initial = currentState
        state[continueStatement] = initial
        currentState = if (initial == StmState.EXITED) {
            StmState.EXITED
        } else {
            StmState.LOOP_TERMINATED
        }

        return currentState
    }

    override fun visit(breakStatement: BreakStatement): StmState {
        when (val loop = stack.top()) {
            is WhileStatement, is DoWhileStatement, is ForStatement -> {
                loopInfo[loop]!!.breaks.add(breakStatement)
            }
            is CaseStatement, is DefaultStatement -> {
                val switch = stack.peekTopSwitch()
                switchInfo[switch]!!.states.last().after = StmState.LOOP_TERMINATED
            }
            else -> {}
        }

        state[breakStatement] = currentState
        currentState = if (currentState == StmState.EXITED) {
            StmState.EXITED
        } else {
            StmState.LOOP_TERMINATED
        }

        return currentState
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

    private fun handleSwitchItem(switchItem: SwitchItem): StmState {
        val before = currentState
        val top = stack.peekTopSwitch()
        currentState = state[top]!!

        val info = switchInfo[top]!!

        val initial = currentState
        state[switchItem] = initial
        info.cases.add(switchItem)
        val caseInfo = CaseInfo(switchItem, initial, initial)
        info.states.add(caseInfo)

        propagateFallThrough(info, before, true)

        val default = stack.scoped(switchItem) {
            switchItem.stmt().accept(this)
        }

        currentState = default
        caseInfo.after = when (caseInfo.after) {
            StmState.REACHABLE -> default
            StmState.EXITED -> StmState.EXITED
            StmState.LOOP_TERMINATED -> StmState.LOOP_TERMINATED
        }
        propagateFallThrough(info, default, true)

        return currentState
    }

    override fun visit(defaultStatement: DefaultStatement): StmState = handleSwitchItem(defaultStatement)
    override fun visit(caseStatement: CaseStatement): StmState = handleSwitchItem(caseStatement)

    override fun visit(returnStatement: ReturnStatement): StmState {
        state[returnStatement] = currentState
        currentState = StmState.EXITED
        return currentState
    }

    override fun visit(compoundStatement: CompoundStatement): StmState {
        state[compoundStatement] = currentState
        for (item in compoundStatement.statements) {
            currentState = when (item) {
                is CompoundStmtDeclaration -> currentState
                is CompoundStmtStatement -> item.statement.accept(this)
            }
        }

        return currentState
    }

    override fun visit(ifElseStatement: IfElseStatement): StmState {
        val initial = currentState
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
        val initial = currentState
        state[ifStatement] = initial

        val thenState = ifStatement.then.accept(this)
        if (thenState == StmState.EXITED || thenState == StmState.LOOP_TERMINATED) {
            currentState = initial
        }

        return currentState
    }

    private fun finalizeLoop(loopInfo: LoopInfo, loopState: StmState): StmState {
        val isNoBreaksAndCont = loopInfo.breaks.isEmpty() && loopInfo.continues.isEmpty()
        if (isNoBreaksAndCont && currentState == StmState.EXITED) {
            return StmState.EXITED
        }
        val isAllExited = loopInfo.breaks.all { state[it] == StmState.EXITED } && loopInfo.continues.all { state[it] == StmState.EXITED }
        if (isAllExited && currentState == StmState.EXITED) {
            return StmState.EXITED
        }

        currentState = loopState
        return loopState
    }

    override fun visit(doWhileStatement: DoWhileStatement): StmState {
        val initial = currentState
        state[doWhileStatement] = initial
        val loop = LoopInfo()
        loopInfo[doWhileStatement] = loop

        stack.scoped(doWhileStatement) {
            currentState = doWhileStatement.body.accept(this)
        }

        return finalizeLoop(loop, initial)
    }

    override fun visit(whileStatement: WhileStatement): StmState {
        val initial = currentState
        state[whileStatement] = initial
        val loop = LoopInfo()
        loopInfo[whileStatement] = loop

        stack.scoped(whileStatement) {
            whileStatement.body.accept(this)
            currentState = initial
        }

        return finalizeLoop(loop, initial)
    }

    override fun visit(forStatement: ForStatement): StmState {
        val initial = currentState
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

        return finalizeLoop(loop, initial)
    }

    override fun visit(switchStatement: SwitchStatement): StmState {
        val initial = currentState
        state[switchStatement] = initial

        val sInfo = SwitchInfo(mutableListOf())
        switchInfo[switchStatement] = sInfo
        stack.scoped(switchStatement) {
            currentState = switchStatement.body.accept(this)
        }

        propagateFallThrough(sInfo, currentState, false)

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