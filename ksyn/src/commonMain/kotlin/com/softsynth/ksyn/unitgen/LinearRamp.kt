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

package com.softsynth.ksyn.unitgen

import com.softsynth.ksyn.Synthesizer
import com.softsynth.ksyn.ports.UnitInputPort
import com.softsynth.ksyn.ports.UnitVariablePort
import com.softsynth.ksyn.toSample

/**
 * Output approaches Input linearly.
 * <P>
 * When you change the value of the input port, the ramp will start changing from its current output
 * value toward the value of input. An internal phase value will go from 0.0 to 1.0 at a rate
 * controlled by time. When the internal phase reaches 1.0, the output will equal input.
 * <P>
 *
 * @author (C) 1997 Phil Burk, SoftSynth.com
 */
class LinearRamp : UnitFilter() {
    /** Time in seconds to get to the input value. */
    val time = UnitInputPort("Time")
    val current = UnitVariablePort("Current")

    private var source: Double = 0.0
    private var phase: Double = 0.0
    private var target: Double = 0.0
    private var timeHeld: Double = 0.0
    private var rate: Double = 1.0

    init {
        addPort(time)
        addPort(current)
    }

    override fun generate() {
        val outputs = output.getValues()
        val currentInput = input.getValues()[0].toDouble()
        var currentValue = current.getValue(0).toDouble()

        // If input has changed, start new segment.
        // Equality check is OK because we set them exactly equal below.
        if (currentInput != target) {
            source = currentValue
            phase = 0.0
            target = currentInput
        }

        if (currentValue == target) {
            // at end of ramp
            for (i in 0 until Synthesizer.FRAMES_PER_BLOCK) {
                outputs[i] = currentValue.toSample()
            }
        } else {
            // in middle of ramp
            val currentTime = time.getValues()[0].toDouble()
            // Has time changed?
            if (currentTime != timeHeld) {
                rate = convertTimeToRate(currentTime)
                timeHeld = currentTime
            }

            for (i in 0 until Synthesizer.FRAMES_PER_BLOCK) {
                if (phase < 1.0) {
                    /* Interpolate current. */
                    currentValue = source + (phase * (target - source))
                    phase += rate
                } else {
                    currentValue = target
                }
                outputs[i] = currentValue.toSample()
            }
        }

        current.set(0, currentValue.toFloat())
    }
}
