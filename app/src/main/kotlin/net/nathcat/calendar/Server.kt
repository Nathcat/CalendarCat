package net.nathcat.calendar

import org.json.simple.JSONObject
import com.sun.net.httpserver.*
import java.net.InetSocketAddress
import javax.net.ssl.SSLContext
import java.util.concurrent.Executors
import java.io.FileInputStream
import java.io.File

/**
 * Front end server for CalendarCat
 * 
 * @author Nathan Baines
 */
internal class FrontEndServer(
    private val usingSSL: Boolean,
    internal val serverConfig: JSONObject,
    private val sslConfig: JSONObject?,
    internal val debug: Boolean
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

    private val templates: MutableMap<String, String> = mutableMapOf<String, String>()

    public fun start() {
        instance = this

        if (serverConfig.get("port") == null || serverConfig.get("webroot") == null || serverConfig.get("templatesDir") == null) {
            error("Missing either port, webroot, or templatesDir field in config file!")
        }

        // Read the templates from the given templates directory
        var templatesDir = File(serverConfig.get("templatesDir") as String)
        if (!templatesDir.isDirectory || !templatesDir.exists()) error("templatesDir must be a directory!")
        
        println("Searching $templatesDir for templates...")
        val pattern = Regex("(?<name>.+)\\.html")
        for (file in templatesDir.listFiles()) {
            var m = pattern.matchEntire(file.name)
            if (m == null) continue
            else {
                println("Found template ${m.groups["name"]!!.value}")
                var content = String(FileInputStream(file).readAllBytes())
                templates.put(m.groups["name"]!!.value, content)
            }     
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
        server.createContext("/", PageHandler(webroot, mutableMapOf<String, Any>()))
        server.createContext("/app", AuthenticatedPageHandler(webroot))

        println("Front end is ready to accept HTTP${if (usingSSL) "S" else ""} connections on port ${serverConfig.get("port")}.")
        server.start()
    }

    internal fun getSpecialCodeContent(code: Int): Resource {
        if (serverConfig.containsKey(code.toString())) {
            return Resource.fromPath(serverConfig.get(code) as String)!!
        }

        return Resource(
            "/error",
            "<h1>${code}</h1><p>Your request hit an unexpected error, <a href=\"https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/${code}\">more information</a></p>".toByteArray(),
            "text/html"
        )
    }

    /**
     * Replace templates within the given html string with the provided templates
     * @param html The HTML string to search for templates
     * @returns The HTML string with all present templates replaced
     */
    internal fun replaceTemplates(html: String): String {
        var result = String(html.toCharArray())

        for (name in templates.keys) {
            var r = Regex("<$name\\s*\\/>")
            result = r.replace(result, templates[name]!!)
        }

        return result
    }

    /**
     * Replaces data templates with the corresponding supplied values in the data map.
     * @param html The string containing the templates
     * @param dataMap The map containing the key - value data
     * @returns A HTML with all data templates replaced. 
     */
    internal fun replaceData(html: String, dataMap: Map<String, Any>): String {
        var result = String(html.toCharArray())

        for (name in dataMap.keys) {
            var r = Regex("<<\\s*$name\\s*>>")
            result = r.replace(result, dataMap[name].toString())
        }
        
        var r = Regex("<<\\s*if\\s*\\((?<name>.+)\\)\\s*>>(?<sub>.*)<<\\s*end if\\s*>>")
        var M = r.findAll(result)

        for (m in M) {
            if (dataMap[m.groups["name"]!!.value] as Boolean) result = result.subSequence(0, m.range.first).toString() + m.groups["sub"]!!.value + result.subSequence(m.range.last + 1, result.length).toString()
            else result = result.subSequence(0, m.range.first).toString() + result.subSequence(m.range.last + 1, result.length).toString()
        }

        return result
    }
}

internal fun extensionToMIME(extension: String?): String {
    when(extension) {
        "html" -> return "text/html; charset=UTF-8"
        "js" -> return "text/javascript"
        "css" -> return "text/css"
        
        else -> return "text/html; charset=UTF-8"
    }
}