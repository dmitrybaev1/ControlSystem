import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub

fun main(args: Array<String>) {
    val jedisPool = JedisPool()
    jedisPool.resource.use {
        it.connect()
        it.psubscribe(object : JedisPubSub(){
            override fun onPMessage(pattern: String?, channel: String?, message: String?) {
                when(channel!!){
                    "server.findEmployee" -> {
                        jedisPool.resource.use { pub ->
                            pub.connect()
                            pub.publish("db.findEmployee",message)
                        }
                    }
                    "server.writePass" -> {
                        jedisPool.resource.use { pub ->
                            pub.connect()
                            pub.publish("db.writePass",message)
                        }
                    }
                    "server.checkCredentials" -> {
                        jedisPool.resource.use { pub ->
                            pub.connect()
                            pub.publish("db.checkCredentials",message)
                        }
                    }
                    "server.sendEmployee" -> {
                        jedisPool.resource.use { pub ->
                            pub.connect()
                            pub.publish("gate.sendEmployee",message)
                        }
                    }
                    "server.sendStatus" -> {
                        jedisPool.resource.use { pub ->
                            pub.connect()
                            pub.publish("gate.sendStatus",message)
                        }
                    }
                    "server.sendPasses" -> {
                        jedisPool.resource.use { pub ->
                            pub.connect()
                            pub.publish("gate.sendPasses",message)
                        }
                    }
                }
            }
        },"server.*")
    }
}