package com.soundsing.app.audio

import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

/**
 * Thread-safe Circular Ring Buffer for variable audio delay (0ms to 500ms).
 * Optimized for 48,000 Hz, 16-bit Stereo PCM audio (4 bytes per stereo frame).
 */
class AudioRingBuffer(
    val sampleRate: Int = 48000,
    val channelCount: Int = 2,
    val maxDelayMs: Int = 500
) {
    // 1 frame = channelCount (2) * 2 bytes = 4 bytes (2 shorts)
    private val shortsPerFrame = channelCount

    // Capacity: enough for maxDelayMs + 200ms safety buffer
    private val maxFrames = ((sampleRate * (maxDelayMs + 200)) / 1000)
    private val bufferSizeShorts = maxFrames * shortsPerFrame
    private val buffer = ShortArray(bufferSizeShorts)

    private val lock = Any()
    private var writeHead = 0
    private var totalSamplesBuffered = 0
    private val currentDelayMs = AtomicInteger(0)

    /**
     * Set target delay in milliseconds (clamped between 0 and maxDelayMs).
     */
    fun setDelayMs(delayMs: Int) {
        val clamped = delayMs.coerceIn(0, maxDelayMs)
        currentDelayMs.set(clamped)
    }

    fun getDelayMs(): Int = currentDelayMs.get()

    /**
     * Clear buffer and reset pointers.
     */
    fun reset() {
        synchronized(lock) {
            buffer.fill(0)
            writeHead = 0
            totalSamplesBuffered = 0
        }
    }

    /**
     * Process audio samples:
     * - Writes incoming `input` samples into ring buffer.
     * - Fills `output` with audio delayed by `currentDelayMs`.
     * - Returns the number of shorts written to `output`.
     */
    fun process(input: ShortArray, output: ShortArray, numShorts: Int): Int {
        val delay = currentDelayMs.get()

        // Zero delay fast-path
        if (delay <= 0) {
            System.arraycopy(input, 0, output, 0, numShorts)
            synchronized(lock) {
                totalSamplesBuffered = 0
            }
            return numShorts
        }

        val targetDelayFrames = (sampleRate * delay) / 1000
        val targetDelayShorts = targetDelayFrames * shortsPerFrame

        synchronized(lock) {
            // Write input into circular buffer
            for (i in 0 until numShorts) {
                buffer[writeHead] = input[i]
                writeHead = (writeHead + 1) % bufferSizeShorts
            }
            totalSamplesBuffered = min(totalSamplesBuffered + numShorts, bufferSizeShorts)

            if (totalSamplesBuffered < targetDelayShorts) {
                // Buffer is still pre-buffering to achieve initial delay target
                output.fill(0, 0, numShorts)
            } else {
                // Calculate read position targetDelayShorts behind writeHead
                var readHead = (writeHead - targetDelayShorts)
                if (readHead < 0) {
                    readHead += bufferSizeShorts
                }

                // Read delayed samples
                for (i in 0 until numShorts) {
                    output[i] = buffer[readHead]
                    readHead = (readHead + 1) % bufferSizeShorts
                }
            }
        }

        return numShorts
    }
}
