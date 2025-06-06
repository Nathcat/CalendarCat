package net.nathcat.calendar

import net.nathcat.calendar.parser.ICSObject

fun main() {
    val obj = ICSObject.fromFile("../test.ics")

    f(obj, 0)
}

fun f(o: ICSObject, c: Int) {
    for (i in 0..c-1) {
        print("\t")
    }
    
    println(o.toString())

    for (i in 0..o.children.size-1) {
        f(o.children[i], c + 1)
    }
}