import launcher.assertStdio
import kotlin.test.Test

class OptTests {

    @Test
    fun testOpt() {
        assertStdio("./opt_ir/bubble_sort", listOf(), "1\n")
    }
}