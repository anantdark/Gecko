package com.anant.gecko.audio

import android.os.SystemClock
import java.io.IOException
import java.util.LinkedList

/**
 * Ring buffer of PCM chunks. Not thread-safe across callers — all access must
 * happen on the audio thread.
 */
class AudioMemory {

    fun interface Consumer {
        @Throws(IOException::class)
        fun consume(array: ByteArray, offset: Int, count: Int): Int
    }

    data class Stats(
        val filled: Int,
        val total: Int,
        val estimation: Int,
        val overwriting: Boolean,
    )

    private val filled = LinkedList<ByteArray>()
    private val free = LinkedList<ByteArray>()

    private var fillingStartUptimeMillis = 0L
    private var filling = false
    private var currentWasFilled = false
    private var current: ByteArray? = null
    private var offset = 0

    @Synchronized
    fun allocate(sizeToEnsure: Long) {
        var currentSize = allocatedMemorySize
        while (currentSize < sizeToEnsure) {
            currentSize += CHUNK_SIZE
            free.addLast(ByteArray(CHUNK_SIZE))
        }
        while (free.isNotEmpty() && currentSize - CHUNK_SIZE >= sizeToEnsure) {
            currentSize -= CHUNK_SIZE
            free.removeLast()
        }
        while (filled.isNotEmpty() && currentSize - CHUNK_SIZE >= sizeToEnsure) {
            currentSize -= CHUNK_SIZE
            filled.removeFirst()
        }
        if (current != null && currentSize - CHUNK_SIZE >= sizeToEnsure) {
            current = null
            offset = 0
            currentWasFilled = false
        }
    }

    @get:Synchronized
    val allocatedMemorySize: Long
        get() = (free.size + filled.size + if (current == null) 0 else 1).toLong() * CHUNK_SIZE

    @Throws(IOException::class)
    fun read(skipBytes: Int, reader: Consumer) {
        var remaining = skipBytes
        synchronized(this) {
            val cur = current
            if (!filling && cur != null && currentWasFilled) {
                remaining -= skipAndFeed(remaining, cur, offset, cur.size - offset, reader)
            }
            for (arr in filled) {
                remaining -= skipAndFeed(remaining, arr, 0, arr.size, reader)
            }
            if (cur != null && offset > 0) {
                skipAndFeed(remaining, cur, 0, offset, reader)
            }
        }
    }

    fun countFilled(): Int {
        synchronized(this) {
            var sum = 0
            val cur = current
            if (!filling && cur != null && currentWasFilled) {
                sum += cur.size - offset
            }
            for (arr in filled) {
                sum += arr.size
            }
            if (cur != null && offset > 0) {
                sum += offset
            }
            return sum
        }
    }

    @Throws(IOException::class)
    fun fill(filler: Consumer) {
        val buffer: ByteArray
        val writeOffset: Int
        synchronized(this) {
            var cur = current
            if (cur == null) {
                cur = if (free.isEmpty()) {
                    if (filled.isEmpty()) return
                    currentWasFilled = true
                    filled.removeFirst()
                } else {
                    currentWasFilled = false
                    free.removeFirst()
                }
                current = cur
                offset = 0
            }
            buffer = cur
            writeOffset = offset
            filling = true
            fillingStartUptimeMillis = SystemClock.uptimeMillis()
        }

        val read = filler.consume(buffer, writeOffset, buffer.size - writeOffset)

        synchronized(this) {
            if (writeOffset + read >= buffer.size) {
                filled.addLast(buffer)
                current = null
                offset = 0
            } else {
                offset = writeOffset + read
            }
            filling = false
        }
    }

    @Synchronized
    fun getStats(fillRate: Int): Stats {
        val cur = current
        val filledBytes = filled.size * CHUNK_SIZE + when {
            cur == null -> 0
            currentWasFilled -> CHUNK_SIZE
            else -> offset
        }
        val totalBytes = (filled.size + free.size + if (cur == null) 0 else 1) * CHUNK_SIZE
        val estimation = if (filling) {
            ((SystemClock.uptimeMillis() - fillingStartUptimeMillis) * fillRate / 1000).toInt()
        } else {
            0
        }
        return Stats(filledBytes, totalBytes, estimation, currentWasFilled)
    }

    @Throws(IOException::class)
    private fun skipAndFeed(
        bytesToSkip: Int,
        arr: ByteArray,
        offset: Int,
        length: Int,
        consumer: Consumer,
    ): Int {
        if (bytesToSkip >= length) return length
        if (bytesToSkip > 0) {
            consumer.consume(arr, offset + bytesToSkip, length - bytesToSkip)
            return bytesToSkip
        }
        consumer.consume(arr, offset, length)
        return 0
    }

    companion object {
        /** ~5s of 48 kHz mono 16-bit — smaller than Echo's 20s chunks for snappier saves. */
        const val CHUNK_SIZE: Int = 480_000
    }
}
