package net.nathcat.calendar

import com.sun.net.httpserver.*

/**
 * Handles a request to a page which requires authentication
 */
public class AuthenticatedPageHandler(
    private val webroot: String
) : HttpHandler {
    public override fun handle(t: HttpExchange) {

    }
}