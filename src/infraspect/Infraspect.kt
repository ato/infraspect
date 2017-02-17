package infraspect

import com.google.gson.Gson
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.skife.jdbi.v2.DBI
import org.skife.jdbi.v2.TransactionStatus
import spark.Request
import spark.Response
import spark.Spark

data class Report(
        var host: String,
        var jvms: List<JvmReport>
)

data class JvmReport(
        var host: String,
        var pid: Int,
        var name: String?,
        var uid: Int,
        var heap_max: String?,
        var jvm_vendor: String?,
        var jvm_version: String?,
        var user: String,
        var files: List<FileReport>,
        var sockets: List<SocketReport>
)

data class FileReport(
        var host: String,
        var pid: Int,
        var path: String,
        var fd: Int
)

data class SocketReport(
        var host: String,
        var pid: Int,
        var fd: Int,
        var local_ip: String,
        var local_port: Int,
        var remote_ip: String,
        var remote_port: Int
)

class Infraspect(val dao: DAO) {
    fun acceptReport(report: Report) {
        for (jvm in report.jvms) {
            dao.inTransaction { dao: DAO, transactionStatus: TransactionStatus ->
                jvm.host = report.host
                dao.deleteJvm(jvm.host, jvm.pid)
                dao.insertJvm(jvm)
                for (file in jvm.files) {
                    file.host = jvm.host
                    file.pid = jvm.pid
                    dao.insertFile(file)
                }
                for (socket in jvm.sockets) {
                    socket.host = jvm.host
                    socket.pid = jvm.pid
                    dao.insertSocket(socket)
                }
            }
        }
    }
}

private fun parseDbConfig(): HikariConfig {
    val dbConfig = HikariConfig()
    dbConfig.jdbcUrl = System.getenv("DB_URL") ?: "jdbc:mysql://localhost/infraspect"
    dbConfig.username = System.getenv("DB_USER") ?: "infraspect"
    dbConfig.password = System.getenv("DB_PASSWORD") ?: "infraspect"
    dbConfig.maximumPoolSize = System.getenv("DB_POOL_MAX")?.toInt() ?: 1
    return dbConfig
}

fun main(args: Array<String>) {
    val dataSource = HikariDataSource(parseDbConfig())
    val dbi = DBI(dataSource)
    dbi.statementLocator = SqlLocator()
    val dao = dbi.onDemand(DAO::class.java)
    val infraspect = Infraspect(dao)

    val gson = Gson()

    Spark.post("/report") { request: Request, response: Response ->
        val report = gson.fromJson(request.body(), Report::class.java)
        infraspect.acceptReport(report)
        "OK"
    };
}

