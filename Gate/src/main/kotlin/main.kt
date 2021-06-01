import redis.clients.jedis.Jedis

fun main(args: Array<String>) {
    Mqtt("tcp://localhost:1883","ACSServer", 0).start()
}