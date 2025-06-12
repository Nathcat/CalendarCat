package net.nathcat.calendar

import com.sun.net.httpserver.*

/**
 * Handles a request to a page which does not require authentication
 */
public class PageHandler(
    private val webroot: String
) : HttpHandler {
    public override fun handle(t: HttpExchange) {

    }
}