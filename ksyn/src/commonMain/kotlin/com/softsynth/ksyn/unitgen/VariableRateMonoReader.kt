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
class VariableRateMonoReader : VariableRateDataReader() {
    private var phase: Double = 0.0 // ranges from 0.0 to 1.0
    private var baseIncrement: Double = 1.0
    private var source: AudioSample = 0.0f
    private var current: AudioSample = 0.0f
    private var target: AudioSample = 0.0f
    private var starved: Boolean = true
    private var ranout: Boolean = false

    init {
        output = UnitOutputPort("Output")
        addPort(output!!)
    }

    override fun generate() {
        val amplitudes = amplitude.getValues()
        val rates = rate.getValues()
        val outputs = output!!.getValues()

        for (i in 0 until Synthesizer.FRAMES_PER_BLOCK) {
            // Decrement phase and advance through queued data until phase back in range.
            if (phase >= 1.0) {
                while (phase >= 1.0) {
                    source = target
                    phase -= 1.0
                    baseIncrement = advanceToNextFrame()
                }
            } else if ((i == 0) && (starved || !dataQueue.isTargetValid)) {
                // A starved condition can only be cured at the beginning of a block.
                source = current
                target = current
                phase = 0.0
                baseIncrement = advanceToNextFrame()
            }

            // Interpolate along line segment.
            current = ((target - source) * phase.toFloat()) + source
            outputs[i] = (current * amplitudes[i].toDouble()).toSample()

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
        // Fire callbacks before getting next value because we already got the target value previously.
        dataQueue.firePendingCallbacks()
        if (dataQueue.hasMore()) {
            starved = false
            target = dataQueue.readNextMonoDouble(synthesisEngine!!.framePeriod)

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
