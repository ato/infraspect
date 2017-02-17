package infraspect

import org.skife.jdbi.v2.StatementContext
import org.skife.jdbi.v2.tweak.StatementLocator
import java.util.*

class SqlLocator : StatementLocator {
    val statements = HashMap<String, String>()

    init {
        javaClass.getResourceAsStream("dao.sql").bufferedReader().use { rdr ->
            var name = "anonymous"
            val buffer = StringBuilder()
            rdr.forEachLine { line ->
                if (line.startsWith("#region ")) {
                    name = line.substring("#region ".length)
                } else if (line.startsWith("#endregion")) {
                    statements.put(name, buffer.toString())
                    buffer.setLength(0)
                } else {
                    buffer.append(line)
                }
            }
        }
    }

    override fun locate(name: String, context: StatementContext): String {
        val statement = statements[name]
        if (statement == null) {
            throw RuntimeException("statement not found in dao.sql: " + name)
        }
        return statement
    }
}