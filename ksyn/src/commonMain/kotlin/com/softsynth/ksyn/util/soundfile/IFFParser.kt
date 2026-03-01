/*
 * Copyright 1997 Phil Burk, Mobileer Inc
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

package com.softsynth.ksyn.util.soundfile

import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readIntLe
import kotlinx.io.readShortLe

/**
 * Parse Electronic Arts style IFF File. IFF is a file format that allows "chunks" of data to be
 * placed in a hierarchical file. It was designed by Jerry Morrison at Electronic Arts for the Amiga
 * computer and is now used extensively by Apple Computer and other companies. IFF is an open
 * standard.
 *
 * @author (C) 1997 Phil Burk, SoftSynth.com
 */
class IFFParser(val source: Source) {

    /**
     * Since IFF files use chunks with explicit size, it is important to keep track of how many
     * bytes have been read from the file. Can be used to report progress when loading samples.
     */
    var offset: Long = 0
        private set

    /**
     * Size of file based on outermost chunk size plus 8. Can be used to report progress when
     * loading samples.
     */
    var fileSize: Long = 0
        private set
    private var fileId: Int = 0

    companion object {
        val RIFF_ID = ('R'.code shl 24) or ('I'.code shl 16) or ('F'.code shl 8) or 'F'.code
        val LIST_ID = ('L'.code shl 24) or ('I'.code shl 16) or ('S'.code shl 8) or 'T'.code
        val FORM_ID = ('F'.code shl 24) or ('O'.code shl 16) or ('R'.code shl 8) or 'M'.code

        /** Convert a 4 character IFF ID to a String */
        fun IDToString(ID: Int): String {
            val bar = ByteArray(4)
            bar[0] = (ID shr 24).toByte()
            bar[1] = (ID shr 16).toByte()
            bar[2] = (ID shr 8).toByte()
            bar[3] = ID.toByte()
            return bar.decodeToString()
        }
    }

    /** @return Next byte from stream. Increment offset by 1. */
    fun readByte(): Byte {
        val b = source.readByte()
        offset += 1
        return b
    }

    /** Read 32 bit signed integer assuming Big Endian byte order. */
    fun readIntBig(): Int {
        val i = source.readInt()
        offset += 4
        return i
    }

    /** Read 32 bit signed integer assuming Little Endian byte order. */
    fun readIntLittle(): Int {
        val i = source.readIntLe()
        offset += 4
        return i
    }

    /** Read 16 bit signed short assuming Big Endian byte order. */
    fun readShortBig(): Short {
        val s = source.readShort()
        offset += 2
        return s
    }

    /** Read 16 bit signed short assuming Little Endian byte order. */
    fun readShortLittle(): Short {
        val s = source.readShortLe()
        offset += 2
        return s
    }

    fun readUShortLittle(): Int {
        return readShortLittle().toInt() and 0x0000FFFF
    }

    /** Read 32 bit signed int assuming IFF order. */
    fun readChunkSize(): Int {
        return if (isRIFF()) {
            readIntLittle()
        } else {
            readIntBig()
        }
    }

    /** @return Skip forward in stream and add numBytes to offset. */
    fun skip(numBytes: Long): Long {
        source.skip(numBytes)
        offset += numBytes
        return offset
    }

    /** @return Next byte array from stream. Increment offset by len. */
    fun readByteArray(length: Int): ByteArray {
        val array = source.readByteArray(length)
        offset += array.size
        return array
    }

    /** @return Next byte array from stream. Increment offset by len. */
    fun read(array: ByteArray): Int {
        val numRead = source.readAtMostTo(array)
        if (numRead > 0) {
            offset += numRead
        }
        return numRead
    }

    /**
     * Parse the stream after reading the first ID and pass the forms and chunks to the ChunkHandler
     */
    fun parseAfterHead(handler: ChunkHandler) {
        val numBytes = readChunkSize()
        fileSize = numBytes + 8L
        parseChunk(handler, fileId, numBytes)
    }

    /**
     * Parse the FORM and pass the chunks to the ChunkHandler The cursor should be positioned right
     * after the type field.
     */
    internal fun parseForm(handler: ChunkHandler, ID: Int, numBytesIn: Int, type: Int) {
        var numBytes = numBytesIn
        while (numBytes > 8) {
            val ckid = readIntBig()
            var size = readChunkSize()
            numBytes -= 8
            if (size < 0) {
                throw Exception("Bad IFF chunk Size: \${IDToString(ckid)} = 0x\${ckid.toString(16)}, Size = $size")
            }
            parseChunk(handler, ckid, size)
            if ((size and 1) == 1) size++ // even-up
            numBytes -= size
        }

        if (numBytes > 0) {
            skip(numBytes.toLong())
        }
    }

    /*
     * Parse one chunk from IFF file. After calling handler, make sure stream is positioned at end
     * of chunk.
     */
    internal fun parseChunk(handler: ChunkHandler, ckid: Int, numBytesIn: Int) {
        var numBytes = numBytesIn
        val startOffset = offset
        if (isForm(ckid)) {
            val type = readIntBig()
            handler.handleForm(this, ckid, numBytes - 4, type)
            val endOffset = offset
            val numRead = (endOffset - startOffset).toInt()
            if (numRead < numBytes) {
                parseForm(handler, ckid, (numBytes - numRead), type)
            }
        } else {
            handler.handleChunk(this, ckid, numBytes)
        }
        val endOffset = offset
        val numRead = (endOffset - startOffset).toInt()
        if ((numBytes and 1) == 1) numBytes++ // even-up
        if (numRead < numBytes) skip((numBytes - numRead).toLong())
    }

    fun readHead() {
        offset = 0
        fileId = readIntBig()
    }

    fun isRIFF(): Boolean {
        return fileId == RIFF_ID
    }

    fun isIFF(): Boolean {
        return fileId == FORM_ID
    }

    /**
     * Does the following chunk ID correspond to a container type like FORM?
     */
    fun isForm(ckid: Int): Boolean {
        return if (isRIFF()) {
            when (ckid) {
                LIST_ID, RIFF_ID -> true
                else -> false
            }
        } else {
            when (ckid) {
                LIST_ID, FORM_ID -> true
                else -> false
            }
        }
    }
}
