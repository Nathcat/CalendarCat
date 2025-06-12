package net.nathcat.calendar

import net.nathcat.calendar.parser.ICSObject
import java.util.Arrays
import java.io.FileInputStream
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

fun main(args: Array<String>) {

    // Extract parameters from CLI args
    var usingSSL = true
    if (Arrays.stream(args).anyMatch("--no-ssl"::equals)) {
        usingSSL = false
    }

    var serverConfigPath = "Assets/Server_Config.json"
    var sslConfigPath = "Assets/SSL_Config.json"

    var serverPathR = Regex("^--server-config=(?<value>.*)$")
    var sslPathR = Regex("^--ssl-config=(?<value>.*)$")

    for (arg in args) {
        var m = serverPathR.matchEntire(arg)

        if (m != null) {
            serverConfigPath = m.groups["value"]!!.value
        }

        m = sslPathR.matchEntire(arg)
            
        if (m != null) {
            sslConfigPath = m.groups["value"]!!.value
        }
    }


    var serverConfig = readJSON(serverConfigPath)
    var sslConfig: JSONObject? = null
    if (usingSSL) sslConfig = readJSON(sslConfigPath)

    val server = FrontEndServer(usingSSL, serverConfig, sslConfig)
    server.start()
}


internal fun readJSON(path: String): JSONObject {
    var content = String(FileInputStream(path).readAllBytes())
    return JSONParser().parse(content) as JSONObject
}