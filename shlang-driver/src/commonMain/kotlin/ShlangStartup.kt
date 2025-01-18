import startup.*


fun main(args: Array<String>) {
    val cli = ShlangCommandLineParser.parse(args) ?: return
    ShlangDriver(cli).run()
}