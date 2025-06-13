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
internal class PageHandler(
    private val webroot: String
) : HttpHandler {
    public override fun handle(t: HttpExchange) {
        var path = Path.of(webroot, t.getRequestURI().path)
        var content: ByteArray = byteArrayOf()
        var code = 404
        var mime = "text/html"

        if (path.toFile().isDirectory && path.toFile().exists()) {
            path = Path.of(path.toString(), "index.html")
        }

        try {
            var fis = FileInputStream(path.toString())
            content = fis.readAllBytes()
            code = 200
        }
        catch (e: FileNotFoundException) {
            println("${Date().toString()}: Couldn't find requested file, ${path.toString()}")
        }

        var m = Regex("^.*\\.(?<ext>.*)$").matchEntire(path.toFile().name)
        if (m != null) {
            code = 200
            mime = extensionToMIME(if (m.groups["ext"] == null) null else m.groups["ext"]!!.value)
        }
        else {
            code = 500
            println("${Date().toString()}: Invalid target file name")
        }

        if (code != 200) {
            content = FrontEndServer.instance!!.getSpecialCodeContent(code).toByteArray()
        }

        content = FrontEndServer.instance!!.replaceTemplates(String(content, Charsets.UTF_8)).toByteArray(Charsets.UTF_8)

        println("${Date().toString()}: ${code} - ${t.getRequestURI().path} -> ${t.remoteAddress.hostString}")

        t.getResponseHeaders().set("Content-Type", mime)
        t.sendResponseHeaders(code, content.size.toLong())
        var os = t.getResponseBody()
        os.write(content)
        os.close()
    }
}