package net.nathcat.calendar.parser

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream

/**
 * Represents components of an ICS calendar
 */
public class ICSObject(
    val name: String,
    val properties: Map<String, ContentLine>,
    val children: Array<ICSObject>
) {
    companion object {
        /**
         * Parse the first occuring object of the specified type
         * @param type the class of the required type
         * @param lines the content lines of the ICS content
         */
        public fun parse(lines: ObjectInputStream): ICSObject {
            var line = lines.readObject() as ContentLine
            if (line.name != "BEGIN") throw IllegalArgumentException("First line must be BEGIN property.")

            return parseSub(lines, line.content)
        }

        private fun parseSub(lines: ObjectInputStream, name: String): ICSObject {
            val properties = mutableMapOf<String, ContentLine>()
            val children = arrayListOf<ICSObject>()

            while (true) {
                var line = lines.readObject() as ContentLine

                if (line.name == "BEGIN") {
                    children.add(parseSub(lines, line.content))
                }
                else if (line.name == "END") {
                    return ICSObject(name, properties, children.toTypedArray())
                }
                else {
                    properties.put(line.name, line)
                }
            }
        }

        /**
         * Read an ICS object from the provided file
         * @param filename The path to the target file
         * @returns an ICS object read from the file
         */
        public fun fromFile(filename: String): ICSObject {
            val lines = ContentLine.fromFile(filename)

            var baos = ByteArrayOutputStream()
            var oos = ObjectOutputStream(baos)
            for (line in lines) {
                oos.writeObject(line)
            }
            
            return parse(ObjectInputStream(ByteArrayInputStream(baos.toByteArray())))
        }
    }

    override public fun toString(): String {
        return name + " -> " + properties.keys.joinToString(", ")
    }
}
