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

class WAVEFileParser : AudioFileParser() {

    companion object {
        const val WAVE_FORMAT_PCM: Short = 1
        const val WAVE_FORMAT_IEEE_FLOAT: Short = 3
        const val WAVE_FORMAT_EXTENSIBLE: Short = (-2).toShort() // 0xFFFE

        val KSDATAFORMAT_SUBTYPE_IEEE_FLOAT = byteArrayOf(
            3, 0, 0, 0, 0, 0, 16, 0, -128, 0, 0, -86, 0, 56, -101, 113
        )
        val KSDATAFORMAT_SUBTYPE_PCM = byteArrayOf(
            1, 0, 0, 0, 0, 0, 16, 0, -128, 0, 0, -86, 0, 56, -101, 113
        )

        val WAVE_ID = ('W'.code shl 24) or ('A'.code shl 16) or ('V'.code shl 8) or 'E'.code
        val FMT_ID = ('f'.code shl 24) or ('m'.code shl 16) or ('t'.code shl 8) or ' '.code
        val DATA_ID = ('d'.code shl 24) or ('a'.code shl 16) or ('t'.code shl 8) or 'a'.code
        val CUE_ID = ('c'.code shl 24) or ('u'.code shl 16) or ('e'.code shl 8) or ' '.code
        val FACT_ID = ('f'.code shl 24) or ('a'.code shl 16) or ('c'.code shl 8) or 't'.code
        val SMPL_ID = ('s'.code shl 24) or ('m'.code shl 16) or ('p'.code shl 8) or 'l'.code
        val LTXT_ID = ('l'.code shl 24) or ('t'.code shl 16) or ('x'.code shl 8) or 't'.code
        val LABL_ID = ('l'.code shl 24) or ('a'.code shl 16) or ('b'.code shl 8) or 'l'.code
    }

    private var samplesPerBlock = 0
    private var blockAlign = 0
    private var numFactSamples = 0
    private var format: Short = 0

    override fun finish(): FloatSample {
        val data = byteData ?: throw Exception("No data found in audio sample.")
        val floatData = FloatArray(numFrames * samplesPerFrame)

        when (bitsPerSample) {
            16 -> SampleLoader.decodeLittleI16ToF32(data, 0, data.size, floatData, 0)
            24 -> SampleLoader.decodeLittleI24ToF32(data, 0, data.size, floatData, 0)
            32 -> {
                when (format) {
                    WAVE_FORMAT_IEEE_FLOAT -> SampleLoader.decodeLittleF32ToF32(data, 0, data.size, floatData, 0)
                    WAVE_FORMAT_PCM -> SampleLoader.decodeLittleI32ToF32(data, 0, data.size, floatData, 0)
                    else -> throw Exception("WAV: Unsupported format = $format")
                }
            }
            else -> throw Exception("WAV: Unsupported bitsPerSample = $bitsPerSample")
        }

        return makeSample(floatData)
    }

    private fun parseCueChunk(parser: IFFParser, ckSize: Int) {
        val numCuePoints = parser.readIntLittle()
        if ((ckSize - 4) != (6 * 4 * numCuePoints)) {
            throw Exception("Cue chunk too short!")
        }
        for (i in 0 until numCuePoints) {
            val dwName = parser.readIntLittle() /* dwName */
            val position = parser.readIntLittle() // dwPosition
            parser.skip((3 * 4).toLong()) // fccChunk, dwChunkStart, dwBlockStart
            val sampleOffset = parser.readIntLittle() // dwPosition
            val cuePoint = findOrCreateCuePoint(dwName)
            cuePoint.position = position
        }
    }

    private fun parseLablChunk(parser: IFFParser, ckSize: Int) {
        val dwName = parser.readIntLittle()
        val textLength = (ckSize - 4) - 1 // don't read NUL terminator
        val text = parseString(parser, textLength)
        val cuePoint = findOrCreateCuePoint(dwName)
        cuePoint.name = text
    }

    private fun parseLtxtChunk(parser: IFFParser, ckSize: Int) {
        val dwName = parser.readIntLittle()
        val dwSampleLength = parser.readIntLittle()
        parser.skip((4 + (4 * 2)).toLong()) // purpose through codepage
        val textLength = (ckSize - ((4 * 4) + (4 * 2))) - 1 // don't read NUL terminator
        if (textLength > 0) {
            val text = parseString(parser, textLength)
            val cuePoint = findOrCreateCuePoint(dwName)
            cuePoint.comment = text
        }
    }

    private fun parseFmtChunk(parser: IFFParser, ckSize: Int) {
        format = parser.readShortLittle()
        samplesPerFrame = parser.readShortLittle()
        frameRate = parser.readIntLittle().toDouble()
        parser.readIntLittle() /* skip dwAvgBytesPerSec */
        blockAlign = parser.readShortLittle().toInt() and 0xFFFF
        bitsPerSample = parser.readShortLittle().toInt() and 0xFFFF
        bytesPerFrame = blockAlign
        bytesPerSample = bytesPerFrame / samplesPerFrame
        samplesPerBlock = (8 * blockAlign) / bitsPerSample

        if (format == WAVE_FORMAT_EXTENSIBLE) {
            val extraSize = parser.readShortLittle()
            val validBitsPerSample = parser.readShortLittle()
            val channelMask = parser.readIntLittle()
            val guid = parser.readByteArray(16)

            if (guid.contentEquals(KSDATAFORMAT_SUBTYPE_IEEE_FLOAT)) {
                format = WAVE_FORMAT_IEEE_FLOAT
            } else if (guid.contentEquals(KSDATAFORMAT_SUBTYPE_PCM)) {
                format = WAVE_FORMAT_PCM
            }
        }

        if (format != WAVE_FORMAT_PCM && format != WAVE_FORMAT_IEEE_FLOAT) {
            throw Exception("Only WAVE_FORMAT_PCM and WAVE_FORMAT_IEEE_FLOAT supported. format = $format")
        }
        if (bitsPerSample != 16 && bitsPerSample != 24 && bitsPerSample != 32) {
            throw Exception("Only 16 and 24 bit PCM or 32-bit float WAV files supported. width = $bitsPerSample")
        }
    }

    private fun convertByteToFrame(byteOffset: Int): Int {
        if (blockAlign == 0) {
            throw Exception("WAV file has bytesPerBlock = zero")
        }
        if (samplesPerFrame.toInt() == 0) {
            throw Exception("WAV file has samplesPerFrame = zero")
        }
        return (samplesPerBlock * byteOffset) / (samplesPerFrame * blockAlign)
    }

    private fun calculateNumFrames(numBytes: Int): Int {
        return if (numFactSamples > 0) {
            numFactSamples
        } else {
            convertByteToFrame(numBytes)
        }
    }

    private fun readFraction(parser: IFFParser): Double {
        val maxFraction = 0x0FFFFFFFFL
        val fraction = parser.readIntLittle().toLong() and maxFraction
        return fraction.toDouble() / maxFraction.toDouble()
    }

    private fun parseSmplChunk(parser: IFFParser, ckSize: Int) {
        parser.readIntLittle() // Manufacturer
        parser.readIntLittle() // Product
        parser.readIntLittle() // Sample Period
        val unityNote = parser.readIntLittle()
        val pitchFraction = readFraction(parser)
        originalPitch = unityNote + pitchFraction

        parser.readIntLittle() // SMPTE Format
        parser.readIntLittle() // SMPTE Offset
        val numLoops = parser.readIntLittle()
        parser.readIntLittle() // Sampler Data

        var lastCueID = Int.MAX_VALUE
        for (i in 0 until numLoops) {
            val cueID = parser.readIntLittle()
            parser.readIntLittle() // type
            val loopStartPosition = parser.readIntLittle()
            val loopEndPosition = parser.readIntLittle() + 1
            val endFraction = readFraction(parser)
            parser.readIntLittle() // playCount

            if (cueID < lastCueID) {
                sustainBegin = loopStartPosition
                sustainEnd = loopEndPosition
                lastCueID = cueID
            }
        }
    }

    private fun parseFactChunk(parser: IFFParser, ckSize: Int) {
        numFactSamples = parser.readIntLittle()
    }

    private fun parseDataChunk(parser: IFFParser, ckSize: Int) {
        dataPosition = parser.offset
        if (ifLoadData) {
            byteData = parser.readByteArray(ckSize)
            val numRead = byteData?.size ?: 0
            if (numRead != ckSize) {
                throw Exception("WAV data chunk too short! Read $numRead instead of $ckSize")
            }
        } else {
            parser.skip(ckSize.toLong())
        }
        numFrames = calculateNumFrames(ckSize)
    }

    override fun handleForm(parser: IFFParser, ckID: Int, ckSize: Int, type: Int) {
        if (ckID == IFFParser.RIFF_ID && type != WAVE_ID) {
            throw Exception("Bad WAV form type = ${IFFParser.IDToString(type)}")
        }
    }

    override fun handleChunk(parser: IFFParser, ckID: Int, ckSize: Int) {
        when (ckID) {
            FMT_ID -> parseFmtChunk(parser, ckSize)
            DATA_ID -> parseDataChunk(parser, ckSize)
            CUE_ID -> parseCueChunk(parser, ckSize)
            FACT_ID -> parseFactChunk(parser, ckSize)
            SMPL_ID -> parseSmplChunk(parser, ckSize)
            LABL_ID -> parseLablChunk(parser, ckSize)
            LTXT_ID -> parseLtxtChunk(parser, ckSize)
            else -> {}
        }
    }
}
