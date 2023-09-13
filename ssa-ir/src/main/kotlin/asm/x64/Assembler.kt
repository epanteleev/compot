package asm.x64

class Assembler {
    private val list = arrayListOf<ObjFunction>()

    fun mkFunction(name: String): ObjFunction {
        val fn = ObjFunction(name)
        list.add(fn)
        return fn
    }

    override fun toString(): String {
        val builder = StringBuilder()
        list.forEach {
            builder.append(".global ${it.name()}\n")
        }
        builder.append('\n')
        list.forEach {
            builder.append(it)
        }

        return builder.toString()
    }
}