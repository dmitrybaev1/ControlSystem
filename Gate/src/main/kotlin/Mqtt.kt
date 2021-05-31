import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisPubSub
import java.util.*

class Mqtt(broker: String,clientId: String,var qos: Int) {
    private var mqttClient: MqttClient
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    //private val jedisSub: Jedis
    //private val jedisPub: Jedis
    private val jedisPool: JedisPool
    init{
        if(qos>2)
            qos=2
        if(qos<0)
            qos=0
        if(qos==1||qos==2)
            mqttClient = MqttClient(broker,clientId, MemoryPersistence())
        else
            mqttClient = MqttClient(broker,clientId)
        //jedisSub = Jedis("localhost",6379,500)
        //jedisPub = Jedis("localhost",6379,500)
        jedisPool = JedisPool()
    }
    fun start(){

        /*try{
            jedisSub.connect()
            jedisPub.connect()
        }
        catch (e : Exception){
            print(e.stackTraceToString())
        }*/

        mqttClient.setCallback(object : MqttCallback{
            override fun connectionLost(cause: Throwable?) {
                try {
                    mqttClient.connect()
                }
                catch (e: MqttException){
                    println(e.stackTraceToString())
                    coroutineScope.launch {
                        jedisPool.resource.use {
                            it.connect()
                            it.publish("log","${Date()}: "+e.stackTraceToString())
                        }
                        //jedisPub.publish("log","${Date()}: "+e.stackTraceToString())
                    }
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                when{
                    topic!!.contains(Regex.fromLiteral("findEmployee")) -> findEmployee(message.toString(),topic)
                    topic!!.contains(Regex.fromLiteral("writePass")) -> writePass(message.toString(),topic)
                    topic!!.contains(Regex.fromLiteral("checkCredentials")) -> checkCredentials(message.toString(),topic)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }

        })
        try {
            mqttClient.connect()
            mqttClient.subscribe("checker/#")
        }
        catch (e: MqttException){
            coroutineScope.launch {
                jedisPool.resource.use {
                    it.connect()
                    it.publish("log","${Date()}: "+e.stackTraceToString())
                }
                //jedisPub.publish("log","${Date()}: "+e.stackTraceToString())
            }
        }
        jedisPool.resource.use {
            it.connect()
            it.psubscribe(object : JedisPubSub(){
                override fun onPMessage(pattern: String?,channel: String?, message: String?) {
                    when (channel!!) {
                        "gate.sendPasses" -> coroutineScope.launch {
                            try {
                                val deviceId = message!!.split('`')[0]
                                val m = message.split('`')[1]
                                val mqttMessage = MqttMessage(m.toByteArray())
                                mqttMessage.qos = qos
                                mqttClient.publish("checker/passes", mqttMessage)
                            } catch (e: Exception) {
                                println(e.stackTraceToString())
                                jedisPool.resource.use { pub ->
                                    pub.connect()
                                    pub.publish("log", "${Date()}" + e.stackTraceToString())
                                }
                                //jedisPub.publish("log", "${Date()}" + e.stackTraceToString())
                            }
                        }
                        "gate.sendEmployee" -> coroutineScope.launch {
                            try {
                                val deviceId = message!!.split('`')[0]
                                val m = message.split('`')[1]
                                val mqttMessage = MqttMessage(m.toByteArray())
                                mqttMessage.qos = qos
                                mqttClient.publish("checker/$deviceId/employeeInfo", mqttMessage)
                            } catch (e: Exception) {
                                println(e.stackTraceToString())
                                jedisPool.resource.use { pub ->
                                    pub.connect()
                                    pub.publish("log", "${Date()}" + e.stackTraceToString())
                                }
                            }
                        }
                        "gate.sendStatus" -> coroutineScope.launch {
                            try {
                                val deviceId = message!!.split('`')[0]
                                val m = message.split('`')[1]
                                val mqttMessage = MqttMessage(m.toByteArray())
                                mqttMessage.qos = qos
                                mqttClient.publish("checker/$deviceId/status", mqttMessage)
                            } catch (e: Exception) {
                                println(e.stackTraceToString())
                                jedisPool.resource.use { pub ->
                                    pub.connect()
                                    pub.publish("log", "${Date()}" + e.stackTraceToString())
                                }
                            }
                        }
                    }
                }
            },"gate.*")
        }

    }
    private fun extractDeviceIdFromTopic(topic: String): String = topic.split("/")[1]
    private fun findEmployee(cardId: String,mqttTopic: String){
        coroutineScope.launch {
            val deviceId = extractDeviceIdFromTopic(mqttTopic)
            jedisPool.resource.use { pub ->
                pub.connect()
                pub.publish("server.findEmployee", "$deviceId`$cardId")
            }
            //jedisPub.publish("db.findEmployee", "$deviceId`$cardId")
        }
    }
    private fun writePass(passData: String,mqttTopic: String){
        coroutineScope.launch {
            val deviceId = extractDeviceIdFromTopic(mqttTopic)
            jedisPool.resource.use { pub ->
                pub.connect()
                pub.publish("server.writePass","$deviceId`$passData")
            }
            //jedisPub.publish("db.writePass","$deviceId`$passData")
        }
    }

    private fun checkCredentials(credentials: String,mqttTopic: String){
        coroutineScope.launch {
            val deviceId = extractDeviceIdFromTopic(mqttTopic)
            jedisPool.resource.use { pub ->
                pub.connect()
                pub.publish("server.checkCredentials","$deviceId`$credentials")
            }
            //jedisPub.publish("db.checkCredentials","$deviceId`$credentials")
        }
    }
}