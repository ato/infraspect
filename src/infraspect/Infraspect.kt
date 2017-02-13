package infraspect

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.skife.jdbi.v2.DBI

private fun openDatabase(): DBI {
    val dbConfig = HikariConfig()
    dbConfig.jdbcUrl = System.getenv("DB_URL") ?: "jdbc:mysql://localhost/infraspect"
    dbConfig.username = System.getenv("DB_USER") ?: "infraspect"
    dbConfig.password = System.getenv("DB_PASSWORD") ?: "infraspect"
    dbConfig.maximumPoolSize = System.getenv("DB_POOL_MAX")?.toInt() ?: 1
    val dataSource = HikariDataSource(dbConfig)
    return DBI(dataSource)
}

fun main(args: Array<String>) {
    val dbi = openDatabase()
    val dao = dbi.onDemand(DAO::class.java)
    System.out.println("Hello, world.")
}

