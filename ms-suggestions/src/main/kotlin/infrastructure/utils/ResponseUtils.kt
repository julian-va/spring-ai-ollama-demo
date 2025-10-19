package jva.cloud.infrastructure.utils

/**
 * Utilities for formatting and normalizing long textual responses produced by models.
 *
 * The helper functions handle escaped control sequences, normalize line endings,
 * trim trailing spaces, collapse excessive blank lines and ensure consistent
 * spacing after list markers.
 */
object ResponseUtils {

    /**
     * Normalize and clean the provided raw model response.
     *
     * Behavior:
     * - Unescapes common escaped sequences ("\\n", "\\r", "\\t") when the
     *   input appears to contain escaped control sequences rather than actual
     *   control characters.
     * - Normalizes CRLF and CR line endings to LF ("\n").
     * - Trims trailing spaces on each line.
     * - Collapses multiple consecutive blank lines to a maximum of two.
     * - Ensures a space after list markers (e.g., "-", "*", "+") if missing.
     * - Trims leading and trailing whitespace.
     *
     * @param raw raw response text from the model
     * @return a cleaned and normalized string suitable for display or storage
     */
    fun formatFullResponse(raw: String): String {
        var s = raw

        val hasEscapedSequences = s.contains("\\n") || s.contains("\\r") || s.contains("\\t")
        val realNewlineCount = s.count { it == '\n' }
        val escapedSeqCount = Regex("\\\\[nrt]").findAll(s).count()

        if (hasEscapedSequences && (realNewlineCount == 0 || escapedSeqCount > realNewlineCount)) {
            s = s.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t")
        }

        s = s.replace("\r\n", "\n").replace("\r", "\n")
        s = s.lines().joinToString("\n") { it.trimEnd() }
        s = s.replace(Regex("\n{3,}"), "\n\n")
        s = s.replace(Regex("(?m)^([*\\-+]\\s*)(\\S)"), "$1$2")
        return s.trim()
    }
}
