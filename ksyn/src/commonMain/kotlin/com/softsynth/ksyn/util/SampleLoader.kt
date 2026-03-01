/*
 * Copyright 2011 Phil Burk, Mobileer Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.softsynth.ksyn.util

import com.softsynth.ksyn.data.FloatSample
import com.softsynth.ksyn.util.soundfile.AIFFFileParser
import com.softsynth.ksyn.util.soundfile.AudioFileParser
import com.softsynth.ksyn.util.soundfile.IFFParser
import com.softsynth.ksyn.util.soundfile.WAVEFileParser
import kotlinx.io.Buffer
import kotlinx.io.Source

/**
 * Load a FloatSample from multiplatform sources.
 *
 * @author Phil Burk (C) 2011 Mobileer Inc, ported to Kotlin MPP 2024
 */
object SampleLoader {

    /**
     * Load a FloatSample from a raw byte array (e.g., read asynchronously using Compose resources).
     */
    fun loadFloatSample(byteArray: ByteArray): FloatSample {
        val buffer = Buffer()
        buffer.write(byteArray)
        return loadFloatSample(buffer)
    }

    /**
     * Load a FloatSample from a kotlinx.io.Source.
     */
    fun loadFloatSample(source: Source): FloatSample {
        val parser = IFFParser(source)
        parser.readHead()
        
        val fileParser: AudioFileParser = when {
            parser.isRIFF() -> WAVEFileParser()
            parser.isIFF() -> AIFFFileParser()
            else -> throw Exception("Unsupported audio file type.")
        }
        return fileParser.load(parser)
    }

    /**
     * Decode 24 bit samples from a BigEndian byte array into a float array. The samples will be
     * normalized into the range -1.0 to +1.0.
     */
    fun decodeBigI24ToF32(audioBytes: ByteArray, offset: Int, numBytes: Int, data: FloatArray, outputOffset: Int) {
        val lastByte = offset + numBytes
        var byteCursor = offset
        var floatCursor = outputOffset
        while (byteCursor < lastByte) {
            val hi = audioBytes[byteCursor++].toInt() and 0x00FF
            val mid = audioBytes[byteCursor++].toInt() and 0x00FF
            val lo = audioBytes[byteCursor++].toInt() and 0x00FF
            val value = (hi shl 24) or (mid shl 16) or (lo shl 8)
            data[floatCursor++] = value * (1.0f / Int.MAX_VALUE)
        }
    }

    fun decodeBigI16ToF32(audioBytes: ByteArray, offset: Int, numBytes: Int, data: FloatArray, outputOffset: Int) {
        val lastByte = offset + numBytes
        var byteCursor = offset
        var floatCursor = outputOffset
        while (byteCursor < lastByte) {
            val hi = audioBytes[byteCursor++].toInt() and 0x00FF
            val lo = audioBytes[byteCursor++].toInt() and 0x00FF
            val value = ((hi shl 8) or lo).toShort()
            data[floatCursor++] = value * (1.0f / 32768)
        }
    }

    fun decodeBigF32ToF32(audioBytes: ByteArray, offset: Int, numBytes: Int, data: FloatArray, outputOffset: Int) {
        val lastByte = offset + numBytes
        var byteCursor = offset
        var floatCursor = outputOffset
        while (byteCursor < lastByte) {
            var bits = audioBytes[byteCursor++].toInt() and 0x00FF
            bits = (bits shl 8) or (audioBytes[byteCursor++].toInt() and 0x00FF)
            bits = (bits shl 8) or (audioBytes[byteCursor++].toInt() and 0x00FF)
            bits = (bits shl 8) or (audioBytes[byteCursor++].toInt() and 0x00FF)
            data[floatCursor++] = Float.fromBits(bits)
        }
    }

    fun decodeBigI32ToF32(audioBytes: ByteArray, offset: Int, numBytes: Int, data: FloatArray, outputOffset: Int) {
        val lastByte = offset + numBytes
        var byteCursor = offset
        var floatCursor = outputOffset
        while (byteCursor < lastByte) {
            var value = audioBytes[byteCursor++].toInt() // MSB
            value = (value shl 8) or (audioBytes[byteCursor++].toInt() and 0x00FF)
            value = (value shl 8) or (audioBytes[byteCursor++].toInt() and 0x00FF)
            value = (value shl 8) or (audioBytes[byteCursor++].toInt() and 0x00FF)
            data[floatCursor++] = value * (1.0f / Int.MAX_VALUE)
        }
    }

    fun decodeLittleF32ToF32(audioBytes: ByteArray, offset: Int, numBytes: Int, data: FloatArray, outputOffset: Int) {
        val lastByte = offset + numBytes
        var byteCursor = offset
        var floatCursor = outputOffset
        while (byteCursor < lastByte) {
            var bits = audioBytes[byteCursor++].toInt() and 0x00FF // LSB
            bits += (audioBytes[byteCursor++].toInt() and 0x00FF) shl 8
            bits += (audioBytes[byteCursor++].toInt() and 0x00FF) shl 16
            bits += audioBytes[byteCursor++].toInt() shl 24
            data[floatCursor++] = Float.fromBits(bits)
        }
    }

    fun decodeLittleI32ToF32(audioBytes: ByteArray, offset: Int, numBytes: Int, data: FloatArray, outputOffset: Int) {
        val lastByte = offset + numBytes
        var byteCursor = offset
        var floatCursor = outputOffset
        while (byteCursor < lastByte) {
            var value = audioBytes[byteCursor++].toInt() and 0x00FF
            value += (audioBytes[byteCursor++].toInt() and 0x00FF) shl 8
            value += (audioBytes[byteCursor++].toInt() and 0x00FF) shl 16
            value += audioBytes[byteCursor++].toInt() shl 24
            data[floatCursor++] = value * (1.0f / Int.MAX_VALUE)
        }
    }

    fun decodeLittleI24ToF32(audioBytes: ByteArray, offset: Int, numBytes: Int, data: FloatArray, outputOffset: Int) {
        val lastByte = offset + numBytes
        var byteCursor = offset
        var floatCursor = outputOffset
        while (byteCursor < lastByte) {
            val lo = audioBytes[byteCursor++].toInt() and 0x00FF
            val mid = audioBytes[byteCursor++].toInt() and 0x00FF
            val hi = audioBytes[byteCursor++].toInt() and 0x00FF
            val value = (hi shl 24) or (mid shl 16) or (lo shl 8)
            data[floatCursor++] = value * (1.0f / Int.MAX_VALUE)
        }
    }

    fun decodeLittleI16ToF32(audioBytes: ByteArray, offset: Int, numBytes: Int, data: FloatArray, outputOffset: Int) {
        val lastByte = offset + numBytes
        var byteCursor = offset
        var floatCursor = outputOffset
        while (byteCursor < lastByte) {
            val lo = audioBytes[byteCursor++].toInt() and 0x00FF
            val hi = audioBytes[byteCursor++].toInt() and 0x00FF
            val value = ((hi shl 8) or lo).toShort()
            val sample = value * (1.0f / 32768)
            data[floatCursor++] = sample
        }
    }
}
