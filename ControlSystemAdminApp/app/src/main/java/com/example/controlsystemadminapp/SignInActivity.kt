package com.example.controlsystemadminapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import com.example.controlsystemadminapp.R

class SignInActivity : AppCompatActivity() {
    private lateinit var loginEditText: EditText
    private lateinit var passwordEditText: EditText

    private val qos = 2
    private val broker = "tcp://192.168.0.100:1883"
    private val clientId = "ACSAdmin"
    private val persistence = MemoryPersistence()
    private val mqttClient = MqttClient(broker,clientId,persistence)

    private val coroutineContext = CoroutineScope(Dispatchers.IO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)
        loginEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.e("lost",cause!!.stackTraceToString())
                /*try {
                    mqttClient.connect()
                } catch (e: MqttException) {
                    Log.e("mqtt_error", e.stackTraceToString())
                }*/
            }
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                if(topic == "checker/deviceAdmin/status")
                    runOnUiThread {
                        if(message.toString()=="ok") {
                            mqttClient.disconnect()
                            val intent = Intent(this@SignInActivity,MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        else
                            Toast.makeText(this@SignInActivity,"User not found or you don't have permissions",Toast.LENGTH_LONG).show()
                    }
            }
            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }

        })
        coroutineContext.launch {
            try {
                mqttClient.connect()
                mqttClient.subscribe("checker/#")
            } catch (e: MqttException) {
                Log.e("lost_on_first_connect", e.stackTraceToString())
            }
        }
    }
    fun signIn(view: View?){
        coroutineContext.launch {
            try {
                val message =
                    MqttMessage((loginEditText.text.toString() + "," + passwordEditText.text
                        .toString()+",admin").toByteArray())
                message.qos = qos
                mqttClient.publish("checker/deviceAdmin/checkCredentials", message)
            } catch (e: MqttException) {
                Log.e("mqtt_error", e.stackTraceToString())
            }
        }
    }
}