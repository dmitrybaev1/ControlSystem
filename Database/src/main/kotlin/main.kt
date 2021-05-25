import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub
import java.util.Date
/*fun main(args: Array<String>) {
    val jedis = Jedis("localhost",6379,300)
    val huedis = Jedis("localhost",6379,300)
    jedis.connect()
    huedis.connect()
    jedis.psubscribe(object : JedisPubSub() {
        override fun onPMessage(pattern: String?,channel: String?, message: String?) {
            //super.onMessage(channel, message)
            println(pattern+channel+message)
            huedis.publish("gate.sendStatus","h")
        }
    },"db.*")
}*/
fun main(args: Array<String>) {
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    val jedisPub = Jedis("localhost",6379,500)
    val jedisSub = Jedis("localhost",6379,500)
    try{
        jedisPub.connect()
        jedisSub.connect()
    }
    catch (e : Exception){
        print(e.stackTraceToString())
    }
    Db.jedis = jedisPub
    Db.register(Executor(username = "user",password = "user",admin = false))
    Db.register(Executor(username = "admin",password = "admin",admin = true))
    Db.setEmployee(Employee(name = " Pushkin Alexander",position = "Writer",cardId = "802B490A6F4C04"))
    jedisSub.psubscribe(object : JedisPubSub() {
        override fun onPMessage(pattern: String?,channel: String?, message: String?) {
            when(channel!!){
                "db.findEmployee" -> {
                    val deviceId = message!!.split('`')[0]
                    val cardId = message!!.split('`')[1]
                    coroutineScope.launch {
                        val employee = Db.getEmployeeByCardId(cardId)!!
                        val messageToSend = deviceId+ "`" + employee.id.toString() + "," + employee.name + "," + employee.position
                        jedisPub.publish("gate.sendEmployee", messageToSend)
                    }
                }
                "db.writePass" -> {
                    val deviceId = message!!.split('`')[0]
                    val pass = message!!.split('`')[1]
                    coroutineScope.launch {
                        val s = pass.split(',')
                        val employee_id = s[0].toInt()
                        val approved = s[1].toBoolean()
                        Db.setPass(employee_id, Date(), approved)
                        getPasses(jedisPub,deviceId)
                    }
                }
                "db.checkCredentials" -> {
                    val deviceId = message!!.split('`')[0]
                    val credentials = message!!.split('`')[1]
                    coroutineScope.launch {
                        val s = credentials.split(',')
                        val userName = s[0]
                        val password = s[1]
                        var isAdmin: Boolean = false
                        if(s.size == 3 && s[2]=="admin")
                            isAdmin = true
                        if(Db.auth(userName, password,isAdmin)) {
                            val messageToSend = "$deviceId`ok"
                            jedisPub.publish("gate.sendStatus", messageToSend)
                        }
                        else{
                            val messageToSend = "$deviceId`fail"
                            jedisPub.publish("gate.sendStatus", messageToSend)
                        }
                    }
                }
            }
        }
    },"db.*")
}

private fun getPasses(jedisPub: Jedis, deviceId: String){
    val passes = Db.getPasses()
    if(!passes.isNullOrEmpty()) {
        var arr = ""
        for (i in passes.indices) {
            arr = if(i==passes.size-1)
                arr + passes[i].employeeId.toString() + "," + passes[i].dateTimeMsk.toString() + "," + passes[i].approved.toString()
            else
                arr + passes[i].employeeId.toString() + "," + passes[i].dateTimeMsk.toString() + "," + passes[i].approved.toString()+";"
        }
        val message = arr
        jedisPub.publish("gate.sendPasses","$deviceId`$message")
    }
    else {
        val message = ""
        jedisPub.publish("gate.sendPasses", "$deviceId`nothing")
    }
}