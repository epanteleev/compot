package startup


fun main(args: Array<String>) {
    val cli = CliParser().parse(args) ?: return
    Driver(cli).run()
}