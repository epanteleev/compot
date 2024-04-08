import tokenizer.CTokenizer
import preprocess.Preprocesssor
import org.junit.jupiter.api.Test
import kotlin.test.Ignore


class PreprocessorTest {
    @Ignore
    fun test1() {
        val tokens = CTokenizer.apply("#define HEAD 34\n HEAD")
        val p = Preprocesssor(tokens).preprocess()
        println(p)
    }
}