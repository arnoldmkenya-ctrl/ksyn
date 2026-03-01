/*
 * Copyright 2009 Phil Burk, Mobileer Inc
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

import com.softsynth.ksyn.data.FloatSample
import com.softsynth.ksyn.util.SampleLoader

class AIFFFileParser : AudioFileParser() {

    companion object {
        const val SUPPORTED_FORMATS = "Only 16 and 24 bit PCM or 32-bit float AIF files supported."
        val AIFF_ID = ('A'.code shl 24) or ('I'.code shl 16) or ('F'.code shl 8) or 'F'.code
        val AIFC_ID = ('A'.code shl 24) or ('I'.code shl 16) or ('F'.code shl 8) or 'C'.code
        val COMM_ID = ('C'.code shl 24) or ('O'.code shl 16) or ('M'.code shl 8) or 'M'.code
        val SSND_ID = ('S'.code shl 24) or ('S'.code shl 16) or ('N'.code shl 8) or 'D'.code
        val MARK_ID = ('M'.code shl 24) or ('A'.code shl 16) or ('R'.code shl 8) or 'K'.code
        val INST_ID = ('I'.code shl 24) or ('N'.code shl 16) or ('S'.code shl 8) or 'T'.code
        val NONE_ID = ('N'.code shl 24) or ('O'.code shl 16) or ('N'.code shl 8) or 'E'.code
        val FL32_ID = ('F'.code shl 24) or ('L'.code shl 16) or ('3'.code shl 8) or '2'.code
        val FL32_ID_LC = ('f'.code shl 24) or ('l'.code shl 16) or ('3'.code shl 8) or '2'.code
    }

    private var sustainBeginID = -1
    private var sustainEndID = -1
    private var releaseBeginID = -1
    private var releaseEndID = -1
    private var typeFloat = false

    override fun finish(): FloatSample {
        setLoops()

        val data = byteData ?: throw Exception("No data found in audio sample.")
        val floatData = FloatArray(numFrames * samplesPerFrame)

        when (bitsPerSample) {
            16 -> SampleLoader.decodeBigI16ToF32(data, 0, data.size, floatData, 0)
            24 -> SampleLoader.decodeBigI24ToF32(data, 0, data.size, floatData, 0)
            32 -> {
                if (typeFloat) {
                    SampleLoader.decodeBigF32ToF32(data, 0, data.size, floatData, 0)
                } else {
                    SampleLoader.decodeBigI32ToF32(data, 0, data.size, floatData, 0)
                }
            }
            else -> throw Exception("$SUPPORTED_FORMATS size = $bitsPerSample")
        }

        return makeSample(floatData)
    }

    private fun read80BitFloat(parser: IFFParser): Double {
        val bytes = parser.readByteArray(10)
        val exp = ((bytes[0].toInt() and 0x3F) shl 8) or (bytes[1].toInt() and 0xFF)
        val mant = ((bytes[2].toInt() and 0xFF) shl 16) or ((bytes[3].toInt() and 0xFF) shl 8) or (bytes[4].toInt() and 0xFF)
        return mant / (1 shl (22 - exp)).toDouble()
    }

    private fun parseCOMMChunk(parser: IFFParser, ckSize: Int) {
        samplesPerFrame = parser.readShortBig()
        numFrames = parser.readIntBig()
        bitsPerSample = parser.readShortBig().toInt() and 0xFFFF
        frameRate = read80BitFloat(parser)

        if (ckSize > 18) {
            val format = parser.readIntBig()
            typeFloat = when (format) {
                FL32_ID, FL32_ID_LC -> true
                NONE_ID -> false
                else -> throw Exception("$SUPPORTED_FORMATS format ${IFFParser.IDToString(format)}")
            }
        }

        bytesPerSample = (bitsPerSample + 7) / 8
        bytesPerFrame = bytesPerSample * samplesPerFrame
    }

    private fun parseINSTChunk(parser: IFFParser, ckSize: Int) {
        val baseNote = parser.readByte()
        val detune = parser.readByte()
        originalPitch = baseNote + (0.01 * detune)

        val lowNote = parser.readByte()
        val highNote = parser.readByte()

        parser.skip(2) /* lo,hi velocity */
        val gain = parser.readShortBig()

        var playMode = parser.readShortBig() /* sustain */
        sustainBeginID = parser.readShortBig().toInt() and 0xFFFF
        sustainEndID = parser.readShortBig().toInt() and 0xFFFF

        playMode = parser.readShortBig() /* release */
        releaseBeginID = parser.readShortBig().toInt() and 0xFFFF
        releaseEndID = parser.readShortBig().toInt() and 0xFFFF
    }

    private fun setLoops() {
        cueMap[sustainBeginID]?.let { sustainBegin = it.position }
        cueMap[sustainEndID]?.let { sustainEnd = it.position }
    }

    private fun parseSSNDChunk(parser: IFFParser, ckSize: Int) {
        val offset = parser.readIntBig()
        parser.readIntBig() /* blocksize */
        parser.skip(offset.toLong())
        dataPosition = parser.offset

        val numBytes = ckSize - 8 - offset
        if (ifLoadData) {
            byteData = parser.readByteArray(numBytes)
            val numRead = byteData?.size ?: 0
            if (numRead != numBytes) {
                throw Exception("AIFF data chunk too short!")
            }
        } else {
            parser.skip(numBytes.toLong())
        }
    }

    private fun parseMARKChunk(parser: IFFParser, ckSize: Int) {
        val startOffset = parser.offset
        val numCuePoints = parser.readShortBig().toInt() and 0xFFFF

        for (i in 0 until numCuePoints) {
            val numInMark = parser.offset - startOffset
            if (numInMark >= ckSize) {
                break
            }

            val uniqueID = parser.readShortBig().toInt() and 0xFFFF
            val position = parser.readIntBig()
            val len = parser.readByte().toInt() and 0xFF
            val markerName = parseString(parser, len)
            if ((len and 1) == 0) {
                parser.skip(1) /* skip pad byte */
            }

            val cuePoint = findOrCreateCuePoint(uniqueID)
            cuePoint.position = position
            cuePoint.name = markerName
        }
    }

    override fun handleForm(parser: IFFParser, ckID: Int, ckSize: Int, type: Int) {
        if (ckID == IFFParser.FORM_ID && type != AIFF_ID && type != AIFC_ID) {
            throw Exception("Bad AIFF form type = ${IFFParser.IDToString(type)}")
        }
    }

    override fun handleChunk(parser: IFFParser, ckID: Int, ckSize: Int) {
        when (ckID) {
            COMM_ID -> parseCOMMChunk(parser, ckSize)
            SSND_ID -> parseSSNDChunk(parser, ckSize)
            MARK_ID -> parseMARKChunk(parser, ckSize)
            INST_ID -> parseINSTChunk(parser, ckSize)
            else -> {}
        }
    }
}
