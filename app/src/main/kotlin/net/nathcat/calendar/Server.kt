package net.nathcat.calendar

import org.json.simple.JSONObject
import com.sun.net.httpserver.*
import java.net.InetSocketAddress
import javax.net.ssl.SSLContext
import java.util.concurrent.Executors
import java.io.FileInputStream

/**
 * Front end server for CalendarCat
 * 
 * @author Nathan Baines
 */
internal class FrontEndServer(
    private val usingSSL: Boolean,
    private val serverConfig: JSONObject,
    private val sslConfig: JSONObject?
) {
    companion object {
        internal var instance: FrontEndServer? = null

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
        instance = this

        if (serverConfig.get("port") == null || serverConfig.get("webroot") == null) {
            error("Missing either port or webroot field in config file!")
        }

        var server: HttpServer

        if (usingSSL) {
            server = HttpsServer.create(InetSocketAddress(Math.toIntExact(serverConfig.get("port") as Long)), 0)
            var provider = LetsEncryptProvider(sslConfig!!)
            var sslContext = provider.getContext()
            server.setHttpsConfigurator(SSLConfigurator(sslContext!!))
        }
        else {
            server = HttpServer.create(InetSocketAddress(Math.toIntExact(serverConfig.get("port") as Long)), 0)
        }
    
        server.setExecutor(Executors.newCachedThreadPool())

        val webroot = serverConfig.get("webroot") as String

        server.createContext("/static", StaticHandler(webroot))
        server.createContext("/", PageHandler(webroot))

        println("Front end is ready to accept HTTP${if (usingSSL) "S" else ""} connections on port ${serverConfig.get("port")}.")
        server.start()
    }

    internal fun getSpecialCodeContent(code: Int): String {
        if (serverConfig.containsKey(code.toString())) {
            return String(FileInputStream(serverConfig.get(code.toString()) as String).readAllBytes())
        }

        return "<h1>${code}</h1><p>Your request hit an unexpected error, <a href=\"https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/${code}\">more information</a></p>"
    }
}

internal fun extensionToMIME(extension: String?): String {
    when(extension) {
        "html" -> return "text/html"
        "js" -> return "text/javascript"
        "css" -> return "text/css"
        
        else -> return "text/html"
    }
}