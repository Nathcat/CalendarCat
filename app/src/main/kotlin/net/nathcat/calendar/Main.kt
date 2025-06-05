package net.nathcat.calendar

import net.nathcat.calendar.parser.ContentLine

fun main() {
    val c = ContentLine.unfoldAll("DESCRIPTION;PARAM1=hello;PARAM2=world:this is a lo\r\n\tng description\r\n\t\t that exists on a long line.\r\n")
    println(ContentLine.parse(c[0]).toString())
}