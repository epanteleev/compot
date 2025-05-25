package sema

import parser.nodes.*


internal class StmtStack {
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