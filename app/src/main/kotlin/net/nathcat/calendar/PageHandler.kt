package net.nathcat.calendar

import com.sun.net.httpserver.*
import java.nio.file.Path
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Date

/**
 * Handles a request to a page which does not require authentication
 */
open internal class PageHandler(
    private val webroot: String,
    internal var dataMap: MutableMap<String, Any>
) : HttpHandler {
    public override fun handle(t: HttpExchange) {
        var code = 404
        var path = t.requestURI.path

        var resource = Resource.fromUri(webroot, t.requestURI.path)

        if (resource == null) {
            println("${Date().toString()}: Couldn't find requested file, ${path.toString()}")
            code = 404
        }
        else code = 200

        if (code != 200) {
            resource = FrontEndServer.instance!!.getSpecialCodeContent(code)
        }

        var content = FrontEndServer.instance!!.replaceTemplates(String(resource!!.content, Charsets.UTF_8)).toByteArray(Charsets.UTF_8)
        content = FrontEndServer.instance!!.replaceData(String(content, Charsets.UTF_8), dataMap).toByteArray(Charsets.UTF_8)

        println("${Date().toString()}: ${code} - ${t.getRequestURI().path} -> ${t.remoteAddress.hostString}")

        t.getResponseHeaders().set("Content-Type", resource.mime)
        t.sendResponseHeaders(code, content.size.toLong())
        var os = t.getResponseBody()
        os.write(content)
        os.close()
    }
}