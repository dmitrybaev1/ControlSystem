import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.FileWriter
import java.util.*

class Mqtt(broker: String,clientId: String,var qos: Int) {
    private var mqttClient: MqttClient
    val logWriter = FileWriter("log",true)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    init{
        if(qos>2)
            qos=2
        if(qos<0)
            qos=0
        if(qos==1||qos==2)
            mqttClient = MqttClient(broker,clientId, MemoryPersistence())
        else
            mqttClient = MqttClient(broker,clientId)
    }

    fun start(){
        mqttClient.setCallback(object : MqttCallback{
            override fun connectionLost(cause: Throwable?) {
                try {
                    mqttClient.connect()
                }
                catch (e: MqttException){
                    println(e.stackTraceToString())
                    logWriter.write("${Date()}: "+e.stackTraceToString())
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                when(topic!!){
                    "checker/findEmployee" -> findEmployee(message.toString())
                    "checker/writePass" -> writePass(message.toString())
                    "checker/checkCredentials" -> checkCredentials(message.toString())
                    "checker/checkCredentialsAdmin" -> checkCredentialsAdmin(message.toString())
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }

        })
        try {
            mqttClient.connect()
            mqttClient.subscribe("checker/+")
        }
        catch (e: MqttException){
            println(e.stackTraceToString())
            logWriter.write("${Date()}: "+e.stackTraceToString())
        }
    }
    private fun findEmployee(cardId: String){
        coroutineScope.launch {
            try {
                val employee = Db.getEmployeeByCardId(cardId)
                val message =
                    MqttMessage((employee!!.id.toString() + "," + employee.name + "," + employee.position).toByteArray())
                message.qos = qos
                mqttClient.publish("checker/employeeInfo", message)
            }
            catch (e : MqttException){
                println(e.stackTraceToString())
                logWriter.write("${Date()}: "+e.stackTraceToString())
            }
        }
    }
    private fun writePass(passData: String){
        coroutineScope.launch {
            val s = passData.split(',')
            val employee_id = s[0].toInt()
            val approved = s[1].toBoolean()
            Db.setPass(employee_id, Date(), approved)
            getPasses()
        }
    }
    private fun getPasses(){
        try {
            val passes = Db.getPasses()
            if(!passes.isNullOrEmpty()) {
                var arr = ""
                for (i in passes.indices) {
                    arr = if(i==passes.size-1)
                        arr + passes[i].employeeId.toString() + "," + passes[i].dateTimeMsk.toString() + "," + passes[i].approved.toString()
                    else
                        arr + passes[i].employeeId.toString() + "," + passes[i].dateTimeMsk.toString() + "," + passes[i].approved.toString()+";"
                }
                val message = MqttMessage(arr.toByteArray())
                mqttClient.publish("checker/passes",message)
            }
            else {
                val message = MqttMessage("".toByteArray())
                mqttClient.publish("checker/passes",message)
            }
        }
        catch (e : MqttException){
            println(e.stackTraceToString())
            logWriter.write("${Date()}: "+e.stackTraceToString())
        }
    }
    private fun checkCredentials(credentials: String){
        coroutineScope.launch {
            try {
                val s = credentials.split(',')
                val userName = s[0]
                val password = s[1]
                if(Db.auth(userName, password)) {
                    val message = MqttMessage("ok".toByteArray())
                    message.qos = qos
                    mqttClient.publish("checker/status", message)
                }
                else{
                    val message = MqttMessage("fail".toByteArray())
                    message.qos = qos
                    mqttClient.publish("checker/status", message)
                }
            }
            catch (e : MqttException){
                println(e.stackTraceToString())
                logWriter.write("${Date()}: "+e.stackTraceToString())
            }
        }
    }
    private fun checkCredentialsAdmin(credentials: String){
        coroutineScope.launch {
            try {
                val s = credentials.split(',')
                val userName = s[0]
                val password = s[1]
                if(Db.auth(userName, password)) {
                    val message = MqttMessage("ok".toByteArray())
                    message.qos = qos
                    mqttClient.publish("checker/statusAdmin", message)
                }
                else{
                    val message = MqttMessage("fail".toByteArray())
                    message.qos = qos
                    mqttClient.publish("checker/statusAdmin", message)
                }
            }
            catch (e : MqttException){
                println(e.stackTraceToString())
                logWriter.write("${Date()}: "+e.stackTraceToString())
            }
        }
    }
}