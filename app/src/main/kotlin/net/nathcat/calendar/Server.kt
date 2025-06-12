package net.nathcat.calendar

import org.json.simple.JSONObject
import com.sun.net.httpserver.*
import java.net.InetSocketAddress
import javax.net.ssl.SSLContext
import java.util.concurrent.Executors

/**
 * Front end server for CalendarCat
 * 
 * @author Nathan Baines
 */
internal class FrontEndServer(
    private val usingSSL: Boolean,
    private val serverConfig: JSONObject,
    private val sslConfig: JSONObject
) {
    companion object {
        private class SSLConfigurator(sslContext: SSLContext) : HttpsConfigurator(sslContext) {
            public override fun configure(params: HttpsParameters) {
                try {
                    var engine = sslContext.createSSLEngine()
                    params.setNeedClientAuth(false)
                    params.setCipherSuites(engine.getEnabledCipherSuites())
                    params.setProtocols(engine.getEnabledProtocols())
                    var ssl = sslContext.getSupportedSSLParameters()
                    params.setSSLParameters(ssl)
                } catch (e: Exception) {
                    e.printStackTrace()
                    error("Failed to create HTTPS port.")
                }
            }
        }
    }

    public fun start() {
        if (serverConfig.get("port") == null) {
            error("Missing port field in config file!")
        }

        var server: HttpServer

        if (usingSSL) {
            server = HttpsServer.create(InetSocketAddress(Math.toIntExact(serverConfig.get("port") as Long)), 0)
            var provider = LetsEncryptProvider(sslConfig)
            var sslContext = provider.getContext()
            server.setHttpsConfigurator(SSLConfigurator(sslContext!!))
        }
        else {
            server = HttpServer.create(InetSocketAddress(Math.toIntExact(serverConfig.get("port") as Long)), 0)
        }
    
        server.setExecutor(Executors.newCachedThreadPool())

        println("Front end is ready to accept HTTP${usingSSL ? "S" : ""} connections on port ${serverConfig.get("port")}.")
        server.start()
    }
}