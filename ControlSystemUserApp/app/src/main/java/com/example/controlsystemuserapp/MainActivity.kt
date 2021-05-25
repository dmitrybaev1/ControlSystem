package com.example.controlsystemuserapp

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence


class MainActivity : AppCompatActivity() {
    private lateinit var attachTextView: TextView
    private lateinit var form: LinearLayout
    private lateinit var nameTextView: TextView
    private lateinit var positionTextView: TextView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button
    private lateinit var cancelButton: Button

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var intentFilter: IntentFilter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var techListArray: Array<Array<String>>
    private lateinit var readedCardCode: String

    private val coroutineContext = CoroutineScope(Dispatchers.IO)

    private val qos = 2
    private val broker = "tcp://192.168.0.100:1883"
    private val clientId = "ACSChecker"
    private val persistence = MemoryPersistence()
    private val mqttClient = MqttClient(broker,clientId,persistence)

    private var employeeId: Int? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        attachTextView = findViewById(R.id.attach)
        form = findViewById(R.id.form)
        nameTextView = findViewById(R.id.name)
        positionTextView = findViewById(R.id.position)
        acceptButton = findViewById(R.id.accept)
        rejectButton = findViewById(R.id.reject)
        cancelButton = findViewById(R.id.cancel)
        form.visibility = View.GONE
        attachTextView.visibility = View.VISIBLE

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if(nfcAdapter!=null){
            pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
            intentFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            techListArray = arrayOf(arrayOf(MifareClassic::class.java.name), arrayOf(NdefFormatable::class.java.name),
                arrayOf(NfcA::class.java.name))
        }
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
                if(topic == "checker/deviceUser/employeeInfo")
                    runOnUiThread {
                        attachTextView.visibility = View.GONE
                        form.visibility = View.VISIBLE
                        if(message.toString().isNotEmpty()) {
                            val s = message.toString().split(',')
                            employeeId = s[0].toInt()
                            nameTextView.text = s[1]
                            positionTextView.text = s[2]
                        }
                        else
                            Toast.makeText(this@MainActivity,"Not found",Toast.LENGTH_LONG).show()
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
    fun accept(view: View?){
        coroutineContext.launch {
            try {
                val message = MqttMessage((employeeId.toString() + "," + "true").toByteArray())
                message.qos = qos
                mqttClient.publish("checker/deviceUser/writePass", message)
            } catch (e: MqttException) {
                Log.e("mqtt_error", e.stackTraceToString())
            }
        }
        attachTextView.visibility = View.VISIBLE
        form.visibility = View.GONE
    }
    fun reject(view: View?){
        coroutineContext.launch {
            try {
                val message = MqttMessage((employeeId.toString() + "," + "false").toByteArray())
                message.qos = qos
                mqttClient.publish("checker/deviceUser/writePass", message)
            } catch (e: MqttException) {
                Log.e("mqtt_error", e.stackTraceToString())
            }
        }
        attachTextView.visibility = View.VISIBLE
        form.visibility = View.GONE
    }
    fun cancel(view: View?){
        attachTextView.visibility = View.VISIBLE
        form.visibility = View.GONE
    }
    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, arrayOf(intentFilter), techListArray);
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent?.action) {
            readedCardCode = bin2hex((intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as Tag?)!!.id)
            //Toast.makeText(this,readedCardCode,Toast.LENGTH_LONG).show()
            coroutineContext.launch {
                try {
                    val message = MqttMessage((readedCardCode).toByteArray())
                    message.qos = qos
                    mqttClient.publish("checker/deviceUser/findEmployee", message)
                }
                catch (e: MqttException){
                    Log.e("mqtt_error",e.stackTraceToString())
                }
            }

        }
    }
    private fun bin2hex(bytes: ByteArray): String {
        val HEX_ARRAY = "0123456789ABCDEF"
        val hexChars = CharArray((if (bytes.size > 7) 7 else bytes.size) * 2)
        var iSrc = bytes.size - 1
        var iRes = 0
        while (iSrc >= 0 && iRes <= 6) {
            val srcByte: Int = bytes[iSrc].toInt() and 0xFF
            hexChars[iRes * 2] = HEX_ARRAY[srcByte ushr 4]
            hexChars[iRes * 2 + 1] = HEX_ARRAY[srcByte and 0x0F]
            iSrc--
            iRes++
        }
        return String(hexChars)
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient.disconnect()
    }
}