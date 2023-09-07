package startup

import ir.read.ModuleReader
import ir.utils.DumpModule
import java.io.File

fun main(args: Array<String>) {
    val text = File(args[0]).readText()
    val module = ModuleReader(text).read()
    //println(DumpModule.apply(module))
}