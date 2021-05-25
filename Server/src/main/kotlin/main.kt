import java.io.FileNotFoundException
import java.io.FileReader
import java.util.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val reader: FileReader
    Db.register(Executor(username = "admin",password = "admin",admin = true))
    Db.setEmployee(Employee(name = " Pushkin Alexander",position = "Writer",cardId = "802B490A6F4C04"))
    if(!args.isNullOrEmpty()) {
        try {
            reader = FileReader(args[0])
        } catch (e: FileNotFoundException) {
            println("error: configuration file not found")
            exitProcess(-1)
        }
        val lines = reader.readLines()
        val mutableMap = mutableMapOf<String,String>()
        for (line in lines) {
            val s = line.split(Regex("( = |= | =|=)"))
            if(s.size!=2){
                println("error: incorrect syntax of parameters")
                exitProcess(-1)
            }
            mutableMap[s[0]] = s[1]
        }
        if(mutableMap.size!=3||!(mutableMap.contains("host")&&mutableMap.contains("clientId")&&mutableMap.contains("qos"))){
            println("error: incorrect set of parameters")
            exitProcess(-1)
        }
        if(mutableMap["qos"]!!.toIntOrNull()==null){
            println("error: incorrect 'qos' value")
            exitProcess(-1)
        }
        Mqtt(mutableMap["host"]!!,mutableMap["clientId"]!!,mutableMap["qos"]!!.toInt()).start()
    }
    else{
        Mqtt("tcp://localhost:1883","ACSServer", 2).start()
    }

}