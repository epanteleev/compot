package parser.nodes

import tokenizer.Position
import tokenizer.tokens.Identifier
import tokenizer.tokens.Keyword

class NodeFabric {
    fun newEmptyStatement(position: Position): EmptyStatement {
        return EmptyStatement(position)
    }

    fun newGotoStatement(id: Identifier): GotoStatement {
        return GotoStatement(id)
    }

    fun newLabeledStatement(label: Identifier, stmt: Statement): LabeledStatement {
        return LabeledStatement(label, stmt)
    }

    fun newContinueStatement(continueKeyword: Keyword): ContinueStatement {
        return ContinueStatement(continueKeyword)
    }

    fun newBreakStatement(breakKeyword: Keyword): BreakStatement {
        return BreakStatement(breakKeyword)
    }

}