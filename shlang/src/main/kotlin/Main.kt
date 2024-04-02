
import gen.IRGen
import parser.ProgramParser
import tokenizer.CTokenizer


fun main(args: Array<String>) {
    val src = """
        int main(int a) {
            return a;
        }
    """.trimIndent()

    val tokens = CTokenizer.apply(src)
    val program = ProgramParser(tokens).program()
    println(program)
    println(IRGen.apply(program))
}