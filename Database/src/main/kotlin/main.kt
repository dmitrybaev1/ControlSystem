import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import java.util.Date

fun main(args: Array<String>) {
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    //val jedisPub = Jedis("localhost",6379,500)
    //val jedisSub = Jedis("localhost",6379,500)
    val jedisPool = JedisPool()
    /*try{
        jedisPub.connect()
        jedisSub.connect()
    }
    catch (e : Exception){
        print(e.stackTraceToString())
    }*/
    //Db.jedis = jedisPub
    Db.register(Executor(username = "user",password = "user",admin = false))
    Db.register(Executor(username = "admin",password = "admin",admin = true))
    Db.setEmployee(Employee(name = " Pushkin Alexander",position = "Writer",cardId = "802B490A6F4C04"))
    Db.jedisPool = jedisPool
    jedisPool.resource.use {
        it.connect()
        it.psubscribe(object : JedisPubSub() {
            override fun onPMessage(pattern: String?,channel: String?, message: String?) {
                when(channel!!){
                    "db.findEmployee" -> {
                        val deviceId = message!!.split('`')[0]
                        val cardId = message!!.split('`')[1]
                        coroutineScope.launch {
                            val employee = Db.getEmployeeByCardId(cardId)!!
                            val messageToSend = deviceId+ "`" + employee.id.toString() + "," + employee.name + "," + employee.position
                            jedisPool.resource.use { pub ->
                                pub.connect()
                                pub.publish("server.sendEmployee", messageToSend)
                            }
                            //jedisPub.publish("gate.sendEmployee", messageToSend)
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
                            getPasses(jedisPool,deviceId)
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
                                jedisPool.resource.use { pub ->
                                    pub.connect()
                                    pub.publish("server.sendStatus", messageToSend)
                                }
                                //jedisPub.publish("gate.sendStatus", messageToSend)
                            }
                            else{
                                val messageToSend = "$deviceId`fail"
                                jedisPool.resource.use { pub ->
                                    pub.connect()
                                    pub.publish("server.sendStatus", messageToSend)
                                }
                                //jedisPub.publish("gate.sendStatus", messageToSend)
                            }
                        }
                    }
                }
            }
        },"db.*")
    }

}

private fun getPasses(jedisPool: JedisPool, deviceId: String){
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
        jedisPool.resource.use { pub ->
            pub.connect()
            pub.publish("server.sendPasses", "$deviceId`$message")
        }
        //jedisPub.publish("gate.sendPasses","$deviceId`$message")
    }
    else {
        val message = ""
        jedisPool.resource.use { pub ->
            pub.connect()
            pub.publish("server.sendPasses", "$deviceId`nothing")
        }

        //jedisPub.publish("gate.sendPasses", "$deviceId`nothing")
    }
}