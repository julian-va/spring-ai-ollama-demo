package jva.cloud.infrastructure.utils

/**
 * Utilities for formatting long model responses.
 */
object ResponseUtils {

    /**
     * Normalizes and cleans the text of the full response.
     * - Unescapes common escaped sequences (e.g. "\\n", "\\r", "\\t") when
     *   the input appears to contain escaped control sequences rather than real
     *   control characters. This handles cases like "\\n10" becoming a newline + "10".
     * - Normalizes line endings to "\n".
     * - Trims trailing spaces on each line.
     * - Collapses multiple consecutive blank lines to a maximum of two.
     * - Ensures a space after list markers/bullets if missing.
     * - Trims leading and trailing whitespace.
     */
    fun formatFullResponse(raw: String): String {
        var s = raw

        // Detect escaped sequences and decide whether to unescape them.
        val hasEscapedSequences = s.contains("\\n") || s.contains("\\r") || s.contains("\\t")
        val realNewlineCount = s.count { it == '\n' }
        val escapedSeqCount = Regex("\\\\[nrt]").findAll(s).count()

        // Unescape when there are escaped sequences and either there are no real newlines
        // or there are more escaped sequences than real newlines (heuristic).
        if (hasEscapedSequences && (realNewlineCount == 0 || escapedSeqCount > realNewlineCount)) {
            s = s.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t")
        }

        // Normalize line endings (convert CRLF and lone CR to LF)
        s = s.replace("\r\n", "\n").replace("\r", "\n")
        // Remove trailing spaces per line
        s = s.lines().joinToString("\n") { it.trimEnd() }
        // Replace multiple blank lines with at most two
        s = s.replace(Regex("\n{3,}"), "\n\n")
        // Ensure a space after bullets/asterisks if missing
        s = s.replace(Regex("(?m)^([*\\-\\+]\\s*)(\\S)"), "$1$2")
        // Trim start/end
        return s.trim()
    }
}
