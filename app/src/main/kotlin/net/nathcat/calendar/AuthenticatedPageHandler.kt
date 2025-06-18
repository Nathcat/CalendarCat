package net.nathcat.calendar

import com.sun.net.httpserver.*
import net.nathcat.authcat.AuthCat
import java.util.Date

/**
 * Handles a request to a page which requires authentication
 */
internal class AuthenticatedPageHandler(
    private val webroot: String
) : PageHandler(webroot, mutableMapOf<String, Any>()) {
    public override fun handle(t: HttpExchange) {
        var cookieList = t.requestHeaders["Cookie"]

        if (cookieList == null) {
            var err = "${Date().toString()}: No cookies provided - ${t.requestURI.path}"
            println(err)
            var target = FrontEndServer.instance!!.serverConfig["authRedirect"]!! as String
            t.responseHeaders.set("Location", target)
            t.sendResponseHeaders(307, err.length.toLong())
            t.responseBody.write(err.toByteArray(Charsets.UTF_8))
            return
        }
    
        var token: String? = null
        val r = Regex("(?<name>.*)=(?<value>.*)")
        for (cookie in cookieList) {
            var m = r.matchEntire(cookie)
            if (m != null) {
                if (m.groups["name"]!!.value == "AuthCat-SSO") {
                    token = m.groups["value"]!!.value
                }
            }
        }

        if (FrontEndServer.instance!!.debug) {
            token = FrontEndServer.instance!!.serverConfig["debugAuthToken"] as String
            println("Using debug token!")
        }

        if (token == null) {
            var err = "${Date().toString()}: User did not provide an SSO token - ${t.requestURI.path}"
            println(err)
            var target = FrontEndServer.instance!!.serverConfig["authRedirect"]!! as String
            t.responseHeaders.set("Location", target)
            t.sendResponseHeaders(307, err.length.toLong())
            t.responseBody.write(err.toByteArray(Charsets.UTF_8))
            return
        }

        var result = AuthCat.loginWithCookie(token)
        if (!result.result) {
            var err = "${Date().toString()}: Failed to authenticate user with provided SSO token - ${t.requestURI.path}"
            println(err)
            var target = FrontEndServer.instance!!.serverConfig["authRedirect"]!! as String
            t.responseHeaders.set("Location", target)
            t.sendResponseHeaders(307, err.length.toLong())
            t.responseBody.write(err.toByteArray(Charsets.UTF_8))
            return
        }

        var pairs = mutableListOf<Pair<String, Any>>()
        

        super.dataMap = mutableMapOf<String, Any>()
        for (key in result.user.keys) {
            super.dataMap.put(key as String, result.user[key]!!)
        }

        super.handle(t)
    }
}