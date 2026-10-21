package com.blackstack.beatbox

object TTMLParser {
    private val P_TAG_REGEX = Regex("""<p\s+begin="([^"]+)"[^>]*>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)

    data class TTMLNode(val timestampMs: Long, val text: String)

    fun parseTTML(ttmlContent: String): List<TTMLNode> {
        val nodes = mutableListOf<TTMLNode>()
        try {
            val matches = P_TAG_REGEX.findAll(ttmlContent)
            for (match in matches) {
                val timeStr = match.groupValues[1]
                val rawText = match.groupValues[2]
                    .replace(Regex("<[^>]*>"), "")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .trim()

                val timeMs = parseTimeString(timeStr)
                if (timeMs >= 0 && rawText.isNotBlank()) {
                    nodes.add(TTMLNode(timeMs, rawText))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return nodes.sortedBy { it.timestampMs }
    }

    private fun parseTimeString(timeStr: String): Long {
        return try {
            if (timeStr.endsWith("s")) {
                val seconds = timeStr.removeSuffix("s").toFloatOrNull() ?: return -1L
                return (seconds * 1000).toLong()
            }
            val parts = timeStr.split(":")
            if (parts.size == 3) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val secParts = parts[2].split(".")
                val seconds = secParts[0].toLong()
                val millis = if (secParts.size > 1) {
                    secParts[1].padEnd(3, '0').take(3).toLong()
                } else 0L
                (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
            } else if (parts.size == 2) {
                val minutes = parts[0].toLong()
                val secParts = parts[1].split(".")
                val seconds = secParts[0].toLong()
                val millis = if (secParts.size > 1) {
                    secParts[1].padEnd(3, '0').take(3).toLong()
                } else 0L
                (minutes * 60 + seconds) * 1000 + millis
            } else {
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }

    fun toLRC(nodes: List<TTMLNode>): String {
        val builder = StringBuilder()
        nodes.forEach { node ->
            val totalSeconds = node.timestampMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val centiseconds = (node.timestampMs % 1000) / 10
            val timestamp = "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
            builder.appendLine("[$timestamp]${node.text}")
        }
        return builder.toString()
    }
}
