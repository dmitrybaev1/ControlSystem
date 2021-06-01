import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*

fun main(args: Array<String>) {
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    val mqttClient = MqttClient("tcp://localhost:1883","ACSClient")
    mqttClient.connect()
    val channelSend = Channel<MqttElement>()
    val channelsReceive = arrayOfNulls<Channel<String>>(1000)
    for(i in channelsReceive.indices)
        channelsReceive[i] = Channel()
    repeat(1000){
        coroutineScope.launch {
            channelSend.send(MqttElement("checker/client$it/checkCredentials","user,user"))
            while(true) {
                val textResponse = channelsReceive[it]!!.receive()
                println(textResponse)
            }
        }
    }

    coroutineScope.launch {
        while(true){
            val mqttElement = channelSend.receive()
            val mqttMessage = MqttMessage(mqttElement.message.toByteArray())
            mqttMessage.qos = 0
            mqttClient.publish(mqttElement.topic,mqttMessage)
        }
    }
    coroutineScope.launch {
        mqttClient.setCallback(object : MqttCallback{
            override fun connectionLost(cause: Throwable?) {

            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                when{
                    topic!!.contains(Regex.fromLiteral("status")) -> {
                        val deviceId = topic.split('/')[1]
                        if(message.toString()=="ok")
                            coroutineScope.launch {
                                channelSend.send(MqttElement("checker/$deviceId/findEmployee", "802B490A6F4C04"))
                                channelsReceive[getIndexFromDeviceId(deviceId)]!!.send("$deviceId authorized")
                            }
                    }
                    topic.contains(Regex.fromLiteral("employeeInfo")) -> {
                        val deviceId = topic.split('/')[1]
                        coroutineScope.launch {
                            val s = message.toString().split(',')
                            channelSend.send(MqttElement("checker/$deviceId/writePass", s[0]+",true"))
                            channelsReceive[getIndexFromDeviceId(deviceId)]!!.send("$deviceId: received employee Name by cardId: "+s[1])
                        }
                    }
                    topic.contains(Regex.fromLiteral("writePass")) -> {
                        val deviceId = topic.split('/')[1]
                        coroutineScope.launch {
                            channelsReceive[getIndexFromDeviceId(deviceId)]!!.send("$deviceId: Pass has been written")
                        }
                    }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
        mqttClient.subscribe("checker/#")
    }
}
private fun getIndexFromDeviceId(deviceId: String): Int = deviceId.filter { it.isDigit() }.toInt()
