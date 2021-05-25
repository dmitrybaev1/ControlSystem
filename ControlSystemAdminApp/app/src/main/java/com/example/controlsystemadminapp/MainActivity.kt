package com.example.controlsystemadminapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import com.example.controlsystemadminapp.R

class MainActivity : AppCompatActivity() {
    private lateinit var eventTextView: TextView
    private lateinit var eventsListView: RecyclerView

    private val coroutineContext = CoroutineScope(Dispatchers.IO)

    private val qos = 2
    private val broker = "tcp://192.168.0.100:1883"
    private val clientId = "ACSAdmin"
    private val persistence = MemoryPersistence()
    private val mqttClient = MqttClient(broker,clientId,persistence)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        eventTextView = findViewById(R.id.eventText)
        eventsListView = findViewById(R.id.eventsView)
        eventTextView.visibility = View.VISIBLE
        eventsListView.visibility = View.GONE
        mqttClient.setCallback(object : MqttCallback{
            override fun connectionLost(cause: Throwable?) {
                Log.e("con_lost",cause!!.stackTraceToString())
                try {
                    mqttClient.connect()
                } catch (e: MqttException) {
                    Log.e("mqtt_error_reconnect", e.stackTraceToString())
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                if(topic == "checker/passes")
                    runOnUiThread {
                        if(!message.toString().isNullOrEmpty()||message.toString()!="nothing") {
                            eventTextView.visibility = View.GONE
                            eventsListView.visibility = View.VISIBLE
                            val lines = message.toString().split(';')
                            val list = ArrayList<EventItem>()
                            for(l in lines){
                                val s = l.split(',')
                                list.add(EventItem(s[0],s[1],s[2]))
                            }
                            eventsListView.adapter = EventAdapter(list)
                            eventsListView.layoutManager = LinearLayoutManager(this@MainActivity,LinearLayoutManager.VERTICAL,false)
                        }
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
                Log.e("mqtt_error_first_con", e.stackTraceToString())
            }
        }
    }
}