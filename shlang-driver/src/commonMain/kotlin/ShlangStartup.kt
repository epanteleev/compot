import startup.*
import driver.*


fun main(args: Array<String>) {
    val cli = CCLIParser.parse(args) ?: return
    ShlangDriver(cli).run()
}