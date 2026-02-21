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
import com.softsynth.ksyn.ports.UnitVariablePort
import com.softsynth.ksyn.toSample

/**
 * A ramp whose function over time is continuous in value and in slope. Also called an "S curve".
 * 
 * @author Phil Burk (C) 2009 Mobileer Inc
 */
class ContinuousRamp : UnitFilter() {
    val current = UnitVariablePort("Current")
    /**
     * Time it takes to get from current value to input value when input is changed. Default value
     * is 1.0 seconds.
     */
    val time = UnitInputPort("Time", 1.0)
    
    private var previousInput: Double = Double.MIN_VALUE
    // Coefficients for cubic polynomial.
    private var a: Double = 0.0
    private var b: Double = 0.0
    private var d: Double = 0.0
    private var framesLeft: Int = 0

    init {
        addPort(time)
        addPort(current)
    }

    override fun generate() {
        val outputs = output.getValues()
        val inputs = input.getValues()
        val currentTime = time.getValues()[0].toDouble()
        var currentValue = current.getValue(0).toDouble()
        var inputValue = currentValue

        for (i in 0 until Synthesizer.FRAMES_PER_BLOCK) {
            inputValue = inputs[i].toDouble()
            var x: Double
            if (inputValue != previousInput) {
                var x = framesLeft.toDouble()
                val currentSlope = x * ((3.0 * a * x) + (2.0 * b))
                // Calculate coefficients.
                framesLeft = (((synthesisEngine?.frameRate ?: 44100.0).toDouble()) * currentTime).toInt()
                if (framesLeft < 1) {
                    framesLeft = 1
                }
                x = framesLeft.toDouble()
                // Calculate coefficients.
                d = inputValue
                val xsq = x * x
                b = ((3.0 * currentValue) - (currentSlope * x) - (3.0 * d)) / xsq
                a = (currentSlope - (2.0 * b * x)) / (3.0 * xsq)
                previousInput = inputValue
            }

            if (framesLeft > 0) {
                x = (--framesLeft).toDouble()
                // Cubic polynomial. c==0
                currentValue = (x * (x * ((x * a) + b))) + d
            }

            outputs[i] = currentValue.toSample()
        }

        current.value = currentValue.toSample()
    }
}
