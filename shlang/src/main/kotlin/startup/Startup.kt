package startup

fun main(args: Array<String>) {
    val cli = CCLIParser().parse(args) ?: return
    CDriver(cli).run()
}