package net.nathcat.calendar

import java.io.*
import java.nio.file.Path
import java.util.Date

internal data class Resource(
        public val uri: String,
        public val content: ByteArray,
        public val mime: String
) {
    companion object {
        /**
         * Obtain a resource from a URI and webroot
         * @param webroot The base path from which the web server provides resources
         * @param uri The URI of the requested resource
         * @returns The requested resource, or null if it does not exist or is invalid
         */
        internal fun fromUri(webroot: String, uri: String): Resource? {
            return fromPath(Path.of(webroot, uri).toString())
        }

        /**
         * Obtain a resource from a file path
         * @param p The file path to fetch from
         * @returns The resource at the given location
         */
        internal fun fromPath(p: String): Resource? {
            var content: ByteArray
            var mime: String
            var path = Path.of(p)

            if (path.toFile().isDirectory && path.toFile().exists()) {
                path = Path.of(path.toString(), "index.html")
            }

            try {
                var fis = FileInputStream(path.toString())
                content = fis.readAllBytes()
            } catch (e: FileNotFoundException) {
                return null
            }

            var m = Regex("^.*\\.(?<ext>.*)$").matchEntire(path.toFile().name)
            if (m != null) {
                mime =
                        extensionToMIME(
                                if (m.groups["ext"] == null) null else m.groups["ext"]!!.value
                        )

            } else {
                return null
            }

            return Resource(path.toString(), content, mime)
        }
    }
}
