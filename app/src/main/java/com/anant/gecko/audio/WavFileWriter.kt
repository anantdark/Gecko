package com.anant.gecko.audio

import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Minimal PCM WAV writer (mono, 16-bit, little-endian). */
class WavFileWriter(
    private val sampleRate: Int,
    private val file: File,
    private val channels: Int = 1,
    private val bitsPerSample: Int = 16,
) : Closeable {

    private val raf = RandomAccessFile(file, "rw")
    private var totalSampleBytesWritten = 0

    init {
        raf.setLength(0)
        raf.write(riffHeader(0))
    }

    @Throws(IOException::class)
    fun write(bytes: ByteArray, offset: Int, count: Int) {
        raf.write(bytes, offset, count)
        totalSampleBytesWritten += count
    }

    fun totalSampleBytesWritten(): Int = totalSampleBytesWritten

    @Throws(IOException::class)
    override fun close() {
        raf.seek(0)
        raf.write(riffHeader(totalSampleBytesWritten))
        raf.close()
    }

    private fun riffHeader(dataSize: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1) // PCM
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign)
        buffer.putShort(bitsPerSample.toShort())
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)
        return buffer.array()
    }
}
