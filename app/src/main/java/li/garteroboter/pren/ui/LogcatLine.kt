package li.garteroboter.pren.ui

/**
 * A item representing a single logging line in Logcat.
 */
data class LogcatLine(val content: String) {
    override fun toString(): String = content
}