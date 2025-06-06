package net.nathcat.calendar.parser

import java.lang.StringBuilder
import java.util.Scanner
import java.io.FileInputStream
import java.io.Serializable

public class ContentLine : Serializable {
    companion object {
        private val NAME_SECTION = 0
        private val PARAM_NAME_SECTION = 1
        private val PARAM_VALUE_SECTION = 2
        private val CONTENT_SECTION = 3
        private val DELIMITER_SECTION = 4

        /** 
         * Parse a content line from its' string.
         * @param line An unfolded content line string
         * @returns A parsed content line 
         */
        fun parse(line: String): ContentLine {
            var sb = StringBuilder()
            var name: String = ""
            var param_names = arrayListOf<String>()
            var param_values = arrayListOf<String>()
            var content: String = ""
            var section = NAME_SECTION
            var delim_return = NAME_SECTION
            
            for (char in line) {
                when (section) {
                    
                    NAME_SECTION -> { 
                        if (char == ':') {
                            name = sb.toString()
                            sb = StringBuilder()
                            section = CONTENT_SECTION
                        }
                        else if (char == ';') {
                            name = sb.toString()
                            sb = StringBuilder()
                            section = PARAM_NAME_SECTION
                        }
                        else {
                            sb.append(char)
                        }
                    }

                    PARAM_NAME_SECTION -> {
                        if (char == '=') {
                            param_names.add(sb.toString())
                            sb = StringBuilder()
                            section = PARAM_VALUE_SECTION
                        }
                        else {
                            sb.append(char)
                        }
                    }

                    PARAM_VALUE_SECTION -> {
                        if (char == ';') {
                            param_values.add(sb.toString())
                            sb = StringBuilder()
                            section = PARAM_NAME_SECTION
                        }
                        else if (char == ':') {
                            param_values.add(sb.toString())
                            sb = StringBuilder()
                            section = CONTENT_SECTION
                        }
                        else if (char == "\""[0]) {
                            section = DELIMITER_SECTION
                            delim_return = PARAM_VALUE_SECTION
                        }
                        else {
                            sb.append(char)
                        }
                    }

                    CONTENT_SECTION -> {
                        sb.append(char)
                    }

                    DELIMITER_SECTION -> {
                        if (char == "\""[0] /* Sorry, just for syntax highlighting because vscode is being dumb */) {
                            section = delim_return
                        }
                        else {
                            sb.append(char)
                        }
                    }

                    else -> { throw IllegalStateException("Invalid parser section: $section")}
                }
            }

            content = sb.toString()
            

            if (name == "") throw IllegalArgumentException("Name is empty")
            if (content == "") throw IllegalArgumentException("Content is empty")

            return ContentLine(
                    name,
                    param_names.toTypedArray(),
                    param_values.toTypedArray(),
                    content
            )
        }

        /**
         * Unfold all content lines in the given string
         * @param content String containing all content lines
         * @returns A string containing all unfolded content lines, separated by CRLF
         */
        fun unfoldAll(content: String): Array<String> {
            val lines = content.split("\r\n").toMutableList()
            lines.removeLast()
            var result = arrayListOf<String>()
            var currentLine = ""
            var expectedWhitespace = 0
            var expectedWsp = 'x'

            for (line in lines) {
                
                var position = 0
                while (position < expectedWhitespace) {
                    if (line[position] != ' ' && line[position] != '\t') {
                        result.add(currentLine)
                        currentLine = ""
                        expectedWhitespace = 0
                        break
                    }
                    else {
                        if (expectedWsp == 'x') {
                            expectedWsp = line[position]
                        }
                        else if (line[position] != expectedWsp) {
                            result.add(currentLine)
                            currentLine = ""
                            expectedWhitespace = 0
                            break
                        }
                    }

                    position++
                }

                currentLine += line.subSequence(position, line.length).toString()
                expectedWhitespace++
            }

            result.add(currentLine)

            return result.toTypedArray()
        }

        /**
         * Read all the content lines from a provided ICS file
         * @param filename The path to the ICS file
         * @returns An array containing all content lines in the ICS file
         */
        fun fromFile(filename: String): Array<ContentLine> {
            val inStream = FileInputStream(filename)
            val content = String(inStream.readAllBytes(), Charsets.UTF_8)
            val lines = unfoldAll(content)
            
            var result = arrayListOf<ContentLine>()
        
            for (line in lines) {
                result.add(parse(line))
            }

            return result.toTypedArray()
        }
    }

    public val name: String
    public val content: String
    public val params: Map<String, String>

    private constructor(
            name: String,
            paramNames: Array<String>,
            paramValues: Array<String>,
            content: String
    ) {
        this.name = name
        this.content = content
        this.params = mutableMapOf()

        for (i in 0..(paramNames.size - 1)) {
            this.params.put(paramNames[i], paramValues[i])
        }
    }

    override public fun toString(): String {
        return "Name: $name\n" +
                params.keys.joinToString("\t") +
                "\n" +
                params.values.joinToString("\t") +
                "\nContent = $content"
    }
}
