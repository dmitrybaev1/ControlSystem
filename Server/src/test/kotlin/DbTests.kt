import org.junit.jupiter.api.*
import java.util.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DbTests {
    @Test
    @Order(1)
    fun insert(){
        assertDoesNotThrow {
            Db.register(Executor(username = "user",password = "password",admin = false))
            val employee = Employee(name = "Baev Dmitry",position = "Developer",cardId = "804C4BA3524F6F")
            Db.setEmployee(employee)
            Db.setPass(employeeId = employee.id!!,dateTimeMsk = Date(),approved = true)
        }
    }
    @Test
    @Order(2)
    fun select(){
        assert(Db.auth("user","password"))
        assertDoesNotThrow {assert(Db.getEmployeeByCardId("804C4BA3524F6F")!!.name == "Baev Dmitry")}
        assertDoesNotThrow {assert(Db.getPasses().size==1&&Db.getPasses()[0].approved)}
    }
}