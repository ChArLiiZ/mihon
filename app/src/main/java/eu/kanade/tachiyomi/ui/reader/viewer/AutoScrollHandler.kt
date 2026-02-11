package eu.kanade.tachiyomi.ui.reader.viewer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Handles automatic page turning / scrolling in the reader.
 *
 * In Pager mode, it turns pages at a fixed interval.
 * In Webtoon mode, it continuously scrolls at a fixed speed.
 */
class AutoScrollHandler(
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    var isRunning: Boolean = false
        private set

    /**
     * Start auto page turning for Pager viewers.
     * Calls [onPageTurn] every [intervalSeconds] seconds.
     */
    fun startPageMode(intervalSeconds: Int, onPageTurn: () -> Unit) {
        stop()
        isRunning = true
        job = scope.launch {
            while (isActive) {
                delay(intervalSeconds * 1000L)
                onPageTurn()
            }
        }
    }

    /**
     * Start continuous scrolling for Webtoon viewers.
     * Calls [onScroll] with the scroll distance every ~30ms.
     */
    fun startScrollMode(scrollDistancePx: Int, onScroll: (Int) -> Unit) {
        stop()
        isRunning = true
        job = scope.launch {
            while (isActive) {
                delay(30L)
                onScroll(scrollDistancePx)
            }
        }
    }

    /**
     * Stop the auto scrolling/page turning.
     */
    fun stop() {
        job?.cancel()
        job = null
        isRunning = false
    }
}
