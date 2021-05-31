
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hibernate.SessionFactory
import org.hibernate.boot.registry.StandardServiceRegistryBuilder

import org.hibernate.boot.MetadataSources
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import java.io.FileWriter

import java.util.*
import javax.persistence.NoResultException
import kotlin.Exception


object Db {
    init {
        setUp()
    }
    var sessionFactory: SessionFactory? = null
    var jedisPool: JedisPool? = null
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
            jedisPool!!.resource.use {
                it.connect()
                it.publish("log", e.stackTraceToString())
            }
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
            jedisPool!!.resource.use {
                it.connect()
                it.publish("log", "${Date()}: " + e.stackTraceToString())
            }
        }
        session.close()
    }
    fun auth(userName: String, password: String, isAdmin: Boolean): Boolean {
        val session = sessionFactory!!.openSession()
        val query = session!!.createQuery("FROM Executor WHERE username = :userName AND password = :password " +
                "AND admin = :isAdmin")
        query.setParameter("userName", userName)
        query.setParameter("password", password)
        query.setParameter("isAdmin",isAdmin)
        session.beginTransaction()
        try {
            val executor = query.uniqueResult() as Executor
            session.transaction.commit()
            return true
        }

        catch (e: Exception){
            session.transaction.rollback()
            println("${Date()}: no user was found or no permissions")
            jedisPool!!.resource.use {
                it.connect()
                it.publish("log","${Date()}: "+e.stackTraceToString())
            }
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
            jedisPool!!.resource.use {
                it.connect()
                it.publish("log","${Date()}: "+e.stackTraceToString())
            }
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
            jedisPool!!.resource.use {
                it.connect()
                it.publish("log", e.stackTraceToString())
            }
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
            jedisPool!!.resource.use {
                it.connect()
                it.publish("log", e.stackTraceToString())
            }
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
            jedisPool!!.resource.use {
                it.connect()
                it.publish("log", e.stackTraceToString())
            }
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