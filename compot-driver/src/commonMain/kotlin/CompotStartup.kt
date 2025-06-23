import startup.*


fun main(args: Array<String>) {
    val cli = CompotCommandLineParser.parse(args) ?: return
    CompotDriver(cli).run()
}