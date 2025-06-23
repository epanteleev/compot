package parser

import parser.nodes.*
import tokenizer.tokens.Identifier


class LabelResolver private constructor(private val labels: MutableMap<String, LabeledStatement>, private val gotos: ArrayList<GotoStatement>) {
    fun addLabel(label: LabeledStatement): LabeledStatement {
        labels[label.name()] = label
        return label
    }

    fun resolve(id: Identifier): LabeledStatement? {
        return labels[id.str()]
    }

    fun addGoto(goto: GotoStatement): GotoStatement {
        gotos.add(goto)
        return goto
    }

    fun resolveAll() {
        for (goto in gotos) {
            val label = goto.resolve(this) ?:
                throw IllegalStateException("Label ${goto.id.str()} not found")
            label.gotos().add(goto)
        }

        clear()
    }

    private fun clear() {
        labels.clear()
        gotos.clear()
    }

    companion object {
        fun default(): LabelResolver {
            return LabelResolver(hashMapOf(), arrayListOf())
        }
    }
}