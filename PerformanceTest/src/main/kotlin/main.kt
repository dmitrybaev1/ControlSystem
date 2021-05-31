import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

fun main(args: Array<String>) {
    val mqttClients = arrayOfNulls<MqttAsyncClient>(1000)
    for(i in mqttClients.indices) {
        val deviceId: String = "device$i"
        mqttClients[i] = MqttAsyncClient("tcp://localhost:1883", "ACSTest$i", MemoryPersistence())
        val mqttClient = mqttClients[i]!!
        try {
            mqttClient.setCallback(object : MqttCallback{
                override fun connectionLost(cause: Throwable?) {

                }
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    when(topic!!){
                        "checker/$deviceId/status" -> {
                            println("DeviceId:$deviceId Try to auth...")
                            if(message.toString()=="ok") {
                                println("DeviceId:$deviceId Auth Result:ok, start messaging...")
                                val message = MqttMessage("802B490A6F4C04".toByteArray())
                                message.qos = 2
                                mqttClient.publish("checker/$deviceId/findEmployee",message)
                            }
                            else
                                println("DeviceId:$deviceId Auth Result:fail, stopping...")
                        }
                        "checker/$deviceId/employeeInfo" -> {
                            println("DeviceId:$deviceId Requesting for Employee Info...")
                            val s = message.toString().split(',')
                            println("DeviceId:$deviceId Name of Employee:"+s[1])
                            val message = MqttMessage((s[0]+",true").toByteArray())
                            message.qos = 2
                            mqttClient.publish("checker/$deviceId/writePass",message)
                        }
                        "checker/$deviceId/writePass" -> {
                            println("DeviceId:$deviceId Pass has been written")
                        }
                    }
                }
                override fun deliveryComplete(token: IMqttDeliveryToken?) {

                }
            })
            mqttClient.connect(null,object : IMqttActionListener{
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    mqttClient.subscribe("checker/#",2,null, object : IMqttActionListener{
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            val message = MqttMessage("user,user".toByteArray())
                            message.qos = 2
                            mqttClient.publish("checker/$deviceId/checkCredentials", message)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {

                        }
                    })
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {

                }
            })

        } catch (e: MqttException) {
            println(e.stackTraceToString())
        }
    }
}