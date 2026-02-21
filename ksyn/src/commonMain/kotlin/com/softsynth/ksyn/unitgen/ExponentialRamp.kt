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
import com.softsynth.ksyn.ports.UnitInputPort
import com.softsynth.ksyn.ports.UnitVariablePort
import com.softsynth.ksyn.toSample
import kotlin.math.max
import kotlin.math.pow

/**
 * Output approaches Input exponentially and will reach it in the specified time.
 *
 * @author Phil Burk (C) 2010 Mobileer Inc
 */
class ExponentialRamp : UnitFilter() {
    val time = UnitInputPort("Time")
    val current = UnitVariablePort("Current", 1.0f)

    private var target: Double = 0.0
    private var timeHeld: Double = 0.0
    private var scaler: Double = 1.0
    private val MIN_VALUE = 0.00001

    init {
        addPort(time)
        input.setup(MIN_VALUE, 1.0, 1.0)
        addPort(current)
    }

    override fun generate() {
        val outputs = output.getValues()
        var currentInput = input.getValues()[0].toDouble()
        val currentTime = time.getValues()[0].toDouble()
        var currentValue = current.getValue(0).toDouble()

        // Clip input values to prevent illegal values close to zero or negative.
        currentInput = max(MIN_VALUE, currentInput)
        currentValue = max(MIN_VALUE, currentValue)

        if (currentTime != timeHeld) {
            scaler = convertTimeToExponentialScaler(currentTime, currentValue, currentInput)
            timeHeld = currentTime
        }

        // If input has changed, start new segment.
        // Equality check is OK because we set them exactly equal below.
        if (currentInput != target) {
            scaler = convertTimeToExponentialScaler(currentTime, currentValue, currentInput)
            target = currentInput
        }

        if (currentValue < target) {
            // Going up.
            for (i in 0 until Synthesizer.FRAMES_PER_BLOCK) {
                currentValue *= scaler
                if (currentValue > target) {
                    currentValue = target
                    scaler = 1.0
                }
                outputs[i] = currentValue.toSample()
            }
        } else if (currentValue > target) {
            // Going down.
            for (i in 0 until Synthesizer.FRAMES_PER_BLOCK) {
                currentValue *= scaler
                if (currentValue < target) {
                    currentValue = target
                    scaler = 1.0
                }
                outputs[i] = currentValue.toSample()
            }

        } else if (currentValue == target) {
            for (i in 0 until Synthesizer.FRAMES_PER_BLOCK) {
                outputs[i] = target.toSample()
            }
        }

        current.set(0, currentValue.toFloat())
    }

    private fun convertTimeToExponentialScaler(duration: Double, source: Double, target: Double): Double {
        val numFrames = duration * (synthesisEngine?.frameRate?.toDouble() ?: 44100.0)
        return (target / source).pow(1.0 / numFrames)
    }
}
