package com.dwicao.dextra.browser

import android.view.Choreographer

data class FrameMetrics(
    val frameCount: Int,
    val jankCount: Int,
    val averageFrameTimeMs: Float,
)

class PerformanceMonitor {
    private val choreographer = Choreographer.getInstance()
    private var running = false
    private var lastFrameNanos = 0L
    private var frameCount = 0
    private var jankCount = 0
    private var totalFrameNanos = 0L
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            if (lastFrameNanos != 0L) {
                val elapsed = frameTimeNanos - lastFrameNanos
                frameCount++
                totalFrameNanos += elapsed
                if (elapsed > JANK_THRESHOLD_NANOS) jankCount++
            }
            lastFrameNanos = frameTimeNanos
            choreographer.postFrameCallback(this)
        }
    }

    fun start() {
        if (running) return
        running = true
        lastFrameNanos = 0L
        choreographer.postFrameCallback(frameCallback)
    }

    fun stop() {
        running = false
        choreographer.removeFrameCallback(frameCallback)
    }

    @Synchronized
    fun snapshot(): FrameMetrics = FrameMetrics(
        frameCount = frameCount,
        jankCount = jankCount,
        averageFrameTimeMs = if (frameCount == 0) 0f else totalFrameNanos / frameCount / 1_000_000f,
    )

    private companion object {
        const val JANK_THRESHOLD_NANOS = 24_000_000L
    }
}
