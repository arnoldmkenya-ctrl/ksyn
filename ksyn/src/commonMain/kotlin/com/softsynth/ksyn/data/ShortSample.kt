/*
 * Copyright 2010 Phil Burk, Mobileer Inc
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

package com.softsynth.ksyn.data

/**
 * Store multi-channel short audio data in an interleaved buffer.
 *
 * @author Phil Burk (C) 2010 Mobileer Inc
 */
open class ShortSample() : AudioSampleData() {
    lateinit var buffer: ShortArray
        private set

    constructor(numFrames: Int, channelsPerFrame: Int) : this() {
        allocate(numFrames, channelsPerFrame)
    }

    constructor(data: ShortArray) : this() {
        allocate(data.size, 1)
        write(data)
    }

    constructor(data: ShortArray, channelsPerFrame: Int) : this() {
        allocate(data.size / channelsPerFrame, channelsPerFrame)
        write(data)
    }

    override fun allocate(numFrames: Int, channelsPerFrame: Int) {
        buffer = ShortArray(numFrames * channelsPerFrame)
        this.maxFrames = numFrames
        this.setNumFrames(numFrames)
        this.setChannelsPerFrame(channelsPerFrame)
    }

    /**
     * Note that in a stereo sample, a frame has two values.
     * 
     * @param startFrame index of frame in the sample
     * @param data data to be written
     * @param startIndex index of first value in array
     * @param numFrames
     */
    fun write(startFrame: Int, data: ShortArray, startIndex: Int, numFrames: Int) {
        val numSamplesToWrite = numFrames * channelsPerFrame
        val firstSampleIndexToWrite = startFrame * channelsPerFrame
        data.copyInto(buffer, firstSampleIndexToWrite, startIndex, startIndex + numSamplesToWrite)
    }

    /**
     * Note that in a stereo sample, a frame has two values.
     * 
     * @param startFrame index of frame in the sample
     * @param data array to receive the data from the sample
     * @param startIndex index of first location in array to start filling
     * @param numFrames
     */
    fun read(startFrame: Int, data: ShortArray, startIndex: Int, numFrames: Int) {
        val numSamplesToRead = numFrames * channelsPerFrame
        val firstSampleIndexToRead = startFrame * channelsPerFrame
        buffer.copyInto(data, startIndex, firstSampleIndexToRead, firstSampleIndexToRead + numSamplesToRead)
    }

    fun write(data: ShortArray) {
        write(0, data, 0, data.size)
    }

    fun read(data: ShortArray) {
        read(0, data, 0, data.size)
    }

    fun readShort(index: Int): Short {
        return buffer[index]
    }

    fun writeShort(index: Int, value: Short) {
        buffer[index] = value
    }

    override fun readSample(index: Int): AudioSample {
        return buffer[index].toFloat() / 32768.0f // Basic conversion, matching max bounds roughly
    }

    override fun writeSample(index: Int, value: AudioSample) {
        var clipped = value
        if (clipped > 0.9999695f) clipped = 0.9999695f
        else if (clipped < -1.0f) clipped = -1.0f
        buffer[index] = (clipped * 32768.0f).toInt().toShort()
    }
}
