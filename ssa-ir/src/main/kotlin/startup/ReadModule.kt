package startup

import java.io.File

fun main(args: Array<String>) {
    val text = File(args[0]).readText()
    //println(DumpModule.apply(module))
}