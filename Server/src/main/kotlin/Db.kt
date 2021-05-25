
import org.hibernate.SessionFactory
import org.hibernate.boot.registry.StandardServiceRegistryBuilder

import org.hibernate.boot.MetadataSources
import java.io.FileWriter

import java.util.*
import javax.persistence.NoResultException
import kotlin.Exception


object Db {
    val logWriter = FileWriter("log",true)
    init{
        setUp()
    }
    var sessionFactory: SessionFactory? = null

    private fun setUp() {
        // A SessionFactory is set up once for an application!
        val registry = StandardServiceRegistryBuilder()
            .configure() // configures settings from hibernate.cfg.xml
            .build()
        try {
            sessionFactory = MetadataSources(registry).buildMetadata().buildSessionFactory()
        } catch (e: Exception) {
            // The registry would be destroyed by the SessionFactory, but we had trouble building the SessionFactory
            // so destroy it manually.
            StandardServiceRegistryBuilder.destroy(registry)
            println(e.stackTraceToString())
        }
    }
    fun register(executor: Executor){
        val session = sessionFactory!!.openSession()
        session!!.beginTransaction()
        try {
            session.save(executor)
            session.transaction?.commit()
        }
        catch (e: Exception){
            session.transaction.rollback()
            println("${Date()}: insert executor error")
            logWriter.write("${Date()}: insert executor error")
            println(e.stackTraceToString())
        }
        session.close()
    }
    fun auth(userName: String, password: String): Boolean {
        val session = sessionFactory!!.openSession()
        val query = session!!.createQuery("FROM Executor WHERE username = :userName AND password = :password")
        query.setParameter("userName", userName)
        query.setParameter("password", password)
        session.beginTransaction()
        try {
            val executor = query.uniqueResult() as Executor
            session.transaction.commit()
            return true
        }

        catch (e: Exception){
            session.transaction.rollback()
            println("${Date()}: no user was found")
            logWriter.write("${Date()}: no user was found")
            println(e.stackTraceToString())
        }
        session.close()
        return false
    }
    fun setEmployee(employee: Employee){
        val session = sessionFactory!!.openSession()
        session!!.beginTransaction()
        try{
            session.save(employee)
            session.transaction.commit()
        }
        catch (e: Exception){
            session.transaction.rollback()
            println("${Date()}: error: insert employee")
            logWriter.write("${Date()}: error: insert employee")
            println(e.stackTraceToString())
        }
        session.close()
    }
    fun getEmployeeByCardId(cardId: String): Employee?{
        val session = sessionFactory!!.openSession()
        val query = session!!.createQuery("FROM Employee WHERE cardId=:cardId")
        query.setParameter("cardId",cardId)
        session.beginTransaction()
        try{
            val employee: Employee = query.uniqueResult() as Employee
            session.transaction.commit()
            return employee
        }
        catch (e : Exception){
            println("${Date()}: no employee was found")
            logWriter.write("${Date()}: no employee was found")
            println(e.stackTraceToString())
        }
        session.close()
        return null
    }
    fun getPasses(): List<PassRequest>{
        val session = sessionFactory!!.openSession()
        val query = session!!.createQuery("FROM PassRequest")
        session.beginTransaction()
        try {
            val passes: List<PassRequest> = query.list() as List<PassRequest>
            session.transaction.commit()
            if(passes.isNullOrEmpty())
                return emptyList()
            return passes
        }
        catch (e : Exception){
            session.transaction.rollback()
            println("${Date()}: passes was not found")
            logWriter.write("${Date()}: passes was not found")
            println(e.stackTraceToString())
        }
        session.close()
        return emptyList()
    }
    fun setPass(employeeId: Int, dateTimeMsk: Date, approved: Boolean){
        val session = sessionFactory!!.openSession()
        val query = session.createQuery("FROM Employee WHERE id = :employeeId")
        query.setParameter("employeeId",employeeId)
        session.beginTransaction()
        try {
            val employee: Employee = query.uniqueResult() as Employee
            session.save(PassRequest(employeeId = employee, dateTimeMsk = dateTimeMsk, approved = approved))
            session.transaction.commit()
        }
        catch (e : Exception){
            session.transaction.rollback()
            println("${Date()}: error: insert pass")
            logWriter.write("${Date()}: error: insert pass")
            println(e.stackTraceToString())
        }
        session.close()
    }
   /* fun test(): Executor{
        val session = sessionFactory!!.openSession()
        session.beginTransaction()
        val executor: Executor = session.load(Executor::class,1) as Executor
        session.transaction.commit()
        return executor
    }*/
}