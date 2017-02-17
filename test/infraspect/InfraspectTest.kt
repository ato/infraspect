package infraspect

import com.google.gson.Gson
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.Test
import org.skife.jdbi.v2.DBI
import java.io.InputStreamReader


class InfraspectTest {

    @Test
    fun test() {
        val dbConfig = HikariConfig()
        dbConfig.jdbcUrl = "jdbc:h2:mem:test"
        dbConfig.maximumPoolSize = 1
        val dbi = DBI(HikariDataSource(dbConfig))
        dbi.statementLocator = SqlLocator()

        dbi.withHandle { h ->
            h.createScript("schema").execute()
        }

        val infraspect = Infraspect(dbi.onDemand(DAO::class.java))

        val gson = Gson()
        InputStreamReader(javaClass.getResourceAsStream("report.json")).use { reader ->
            val report = gson.fromJson(reader, Report::class.java)
            infraspect.acceptReport(report)
            infraspect.acceptReport(report)
        }
    }

}

