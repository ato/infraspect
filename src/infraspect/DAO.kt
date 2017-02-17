package infraspect

import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.BindBean
import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.sqlobject.mixins.Transactional

interface DAO : Transactional<DAO> {
    @SqlUpdate
    fun insertJvm(@BindBean jvm: JvmReport)

    @SqlUpdate
    fun insertFile(@BindBean file: FileReport)

    @SqlUpdate
    fun insertSocket(@BindBean socket: SocketReport)

    @SqlUpdate
    fun deleteJvm(@Bind("host") host: String, @Bind("pid") pid: Int)

}