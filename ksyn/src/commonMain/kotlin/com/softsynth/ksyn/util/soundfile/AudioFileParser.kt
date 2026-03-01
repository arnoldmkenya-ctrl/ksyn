/*
 * Copyright 2001 Phil Burk, Mobileer Inc
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
import com.softsynth.ksyn.data.SampleMarker
import kotlinx.io.readByteArray

/**
 * Base class for various types of audio specific file parsers.
 *
 * @author (C) 2001 Phil Burk, SoftSynth.com
 */
abstract class AudioFileParser : ChunkHandler {
    protected var parser: IFFParser? = null
    protected var byteData: ByteArray? = null
    var ifLoadData = true /* If true, load sound data into memory. */
    
    // Number of bytes from beginning of file where sound data resides.
    var dataPosition: Long = 0
        protected set
        
    protected var bitsPerSample = 0
    protected var bytesPerFrame = 0 // in the file
    protected var bytesPerSample = 0 // in the file
    protected val cueMap = HashMap<Int, SampleMarker>()
    protected var samplesPerFrame: Short = 0
    protected var frameRate = 0.0
    protected var numFrames = 0
    protected var originalPitch = 60.0
    protected var sustainBegin = -1
    protected var sustainEnd = -1

    /**
     * This can be read by another thread when load()ing a sample to determine how many bytes have
     * been read so far.
     */
    val numBytesRead: Long
        get() = parser?.offset ?: 0

    /**
     * This can be read by another thread when load()ing a sample to determine how many bytes need
     * to be read.
     */
    val fileSize: Long
        get() = parser?.fileSize ?: 0

    protected fun findOrCreateCuePoint(uniqueID: Int): SampleMarker {
        return cueMap.getOrPut(uniqueID) { SampleMarker() }
    }

    fun load(parser: IFFParser): FloatSample {
        this.parser = parser
        parser.parseAfterHead(this)
        return finish()
    }

    internal abstract fun finish(): FloatSample

    internal fun makeSample(floatData: FloatArray): FloatSample {
        val floatSample = FloatSample(floatData, samplesPerFrame.toInt())

        floatSample.setChannelsPerFrame(samplesPerFrame.toInt())
        floatSample.frameRate = frameRate
        floatSample.pitch = originalPitch

        if (sustainBegin >= 0) {
            floatSample.sustainBegin = sustainBegin
            floatSample.sustainEnd = sustainEnd
        }

        for (marker in cueMap.values) {
            floatSample.addMarker(marker)
        }

        if (floatSample.markerCount >= 2) {
            floatSample.sustainBegin = floatSample.getMarker(0)!!.position
            floatSample.sustainEnd = floatSample.getMarker(1)!!.position
        }
        return floatSample
    }

    protected fun parseString(parser: IFFParser, textLength: Int): String {
        val bar = parser.readByteArray(textLength)
        return bar.decodeToString()
    }
}
