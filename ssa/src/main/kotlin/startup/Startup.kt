package startup

import java.io.File
import ir.read.ModuleReader


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("<ir-file>.ir")
        return
    }

    val text = File(args[0]).readText()
    val module = ModuleReader(text).read()

    Driver.output(args[0], module)
}