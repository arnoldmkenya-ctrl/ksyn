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

package com.softsynth.ksyn.unitgen

import com.softsynth.ksyn.Synthesizer
import com.softsynth.ksyn.AudioSample
import com.softsynth.ksyn.toSample
import com.softsynth.ksyn.ports.UnitOutputPort

/**
 * This reader can play any SequentialData and will interpolate between adjacent values. It can play
 * both envelopes and samples.
 *
 * @author Phil Burk (C) 2009 Mobileer Inc
 */
class VariableRateStereoReader : VariableRateDataReader() {
    private var phase: Double = 0.0
    private var baseIncrement: Double = 1.0
    private var source0: AudioSample = 0.0f
    private var current0: AudioSample = 0.0f
    private var target0: AudioSample = 0.0f
    private var source1: AudioSample = 0.0f
    private var current1: AudioSample = 0.0f
    private var target1: AudioSample = 0.0f
    private var starved: Boolean = true
    private var ranout: Boolean = false

    init {
        dataQueue.numChannels = 2
        output = UnitOutputPort(2, "Output")
        addPort(output!!)
    }

    override fun generate() {
        val amplitudes = amplitude.getValues()
        val rates = rate.getValues()
        val output0s = output!!.getValues(0)
        val output1s = output!!.getValues(1)

        for (i in 0 until Synthesizer.FRAMES_PER_BLOCK) {
            // Decrement phase and advance through queued data until phase back in range.
            if (phase >= 1.0) {
                while (phase >= 1.0) {
                    source0 = target0
                    source1 = target1
                    phase -= 1.0
                    baseIncrement = advanceToNextFrame()
                }
            } else if ((i == 0) && (starved || !dataQueue.isTargetValid)) {
                // A starved condition can only be cured at the beginning of a block.
                source0 = current0
                target0 = current0
                source1 = current1
                target1 = current1
                phase = 0.0
                baseIncrement = advanceToNextFrame()
            }

            // Interpolate along line segment.
            current0 = ((target0 - source0) * phase.toFloat()) + source0
            output0s[i] = (current0 * amplitudes[i].toDouble()).toSample()
            current1 = ((target1 - source1) * phase.toFloat()) + source1
            output1s[i] = (current1 * amplitudes[i].toDouble()).toSample()

            val phaseIncrement = baseIncrement * rates[i].toDouble()
            phase += limitPhaseIncrement(phaseIncrement)
        }

        if (ranout) {
            ranout = false
            if (dataQueue.testAndClearAutoStop()) {
                autoStop()
            }
        }
    }

    fun limitPhaseIncrement(phaseIncrement: Double): Double {
        return phaseIncrement
    }

    private fun advanceToNextFrame(): Double {
        dataQueue.firePendingCallbacks()
        if (dataQueue.hasMore()) {
            starved = false

            dataQueue.beginFrame(synthesisEngine!!.framePeriod)
            target0 = dataQueue.readCurrentChannelDouble(0)
            target1 = dataQueue.readCurrentChannelDouble(1)
            dataQueue.endFrame()

            // calculate phase increment;
            return synthesisEngine!!.framePeriod * dataQueue.normalizedRate
        } else {
            starved = true
            ranout = true
            phase = 0.0
            return 0.0
        }
    }
}
