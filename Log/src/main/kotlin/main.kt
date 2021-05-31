import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub
import java.io.FileWriter
import java.util.*
import kotlin.Exception

fun main(args: Array<String>) {
    val jedis = Jedis("localhost",6379,500)
    try{
        jedis.connect()
    }
    catch (e : Exception){
        print(e.stackTraceToString())
    }
    jedis.subscribe(object : JedisPubSub(){
        override fun onMessage(channel: String?, message: String?) {
            if(channel!! == "log"){
                println(message!!)
                val logWriter = FileWriter("log",true)
                logWriter.write(message)
                logWriter.close()
            }
        }
    },"log")
}