import ir.read.ModuleReader
import startup.*


fun main(args: Array<String>) {
    val cli = CliParser.parse(args) ?: return
    val module = ModuleReader.read(cli.inputs().first().filename)
    OptDriver.compile(cli, module)
}