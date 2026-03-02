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

package com.softsynth.ksyn.unitgen

import com.softsynth.ksyn.Synthesizer
import com.softsynth.ksyn.ports.UnitInputPort

/**
 * Simple stereo sample writer. Write two samples per audio frame with no interpolation. This can be
 * used to record audio or to build delay lines.
 *
 * Note that you must call start() on this unit because it does not have an output for pulling data.
 *
 * @see FixedRateMonoWriter
 * @author Phil Burk (C) 2009 Mobileer Inc
 */
class FixedRateStereoWriter : SequentialDataWriter() {
    init {
        input = UnitInputPort(2, "Input")
        addPort(input)
        dataQueue.numChannels = 2
    }

    override fun generate() {
        val input0s = input.getValues(0)
        val input1s = input.getValues(1)

        for (i in 0 until Synthesizer.FRAMES_PER_BLOCK) {
            if (dataQueue.hasMore()) {
                dataQueue.beginFrame(framePeriod)
                dataQueue.writeCurrentChannelDouble(0, input0s[i])
                dataQueue.writeCurrentChannelDouble(1, input1s[i])
                dataQueue.endFrame()
            } else {
                if (dataQueue.testAndClearAutoStop()) {
                    autoStop()
                }
            }
            dataQueue.firePendingCallbacks()
        }
    }
}
