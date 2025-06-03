package sema


internal class StmtStack {
    private val stack = ArrayList<StmtInfo>()

    fun push(statement: StmtInfo) {
        stack.add(statement)
    }

    fun pop(): StmtInfo {
        if (stack.isEmpty()) {
            throw IllegalStateException("Stack is empty, cannot pop")
        }

        return stack.removeAt(stack.size - 1)
    }

    fun peekTopSwitch(): SwitchInfo {
        val switch = stack.lastOrNull { it is SwitchInfo }
        if (switch == null) {
            throw IllegalStateException("No switch statement on the stack")
        }

        return switch as SwitchInfo
    }

    fun peekTopLoop(): LoopInfo {
        val loop = stack.lastOrNull { it is LoopInfo }
        if (loop == null) {
            throw IllegalStateException("No loop statement on the stack")
        }

        return loop as LoopInfo
    }

    fun tryPeekTopLoop(): LoopInfo? {
        return stack.lastOrNull { it is LoopInfo } as? LoopInfo
    }

    fun top(): StmtInfo {
        if (stack.isEmpty()) {
            throw IllegalStateException("Stack is empty, cannot peek")
        }

        return stack[stack.size - 1]
    }

    fun<T> scoped(statement: StmtInfo, closure: (StmtInfo) -> T): T {
        push(statement)
        val ret = closure(statement)
        pop()
        return ret
    }
}