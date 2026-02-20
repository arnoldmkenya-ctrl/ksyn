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
import com.softsynth.ksyn.toSample

/**
 * Simple pulse wave oscillator.
 *
 * @author Phil Burk (C) 2009 Mobileer Inc
 */
class PulseOscillator : UnitOscillator() {

    /**
     * Pulse width varies from -1.0 to +1.0. At 0.0 the pulse is actually a square wave.
     */
    val width = UnitInputPort("Width")

    init {
        addPort(width)
    }

    override fun generate() {
        val frequencies = frequency.getValues()
        val amplitudes = amplitude.getValues()
        val widths = width.getValues()
        val outputs = output.getValues()

        // Variables have a single value.
        var currentPhase: Double = phase.getValue(0).toDouble()

        for (i in 0..<Synthesizer.FRAMES_PER_BLOCK) {
            // Generate sawtooth phaser to provide phase for pulse generation.
            val phaseIncrement = convertFrequencyToPhaseIncrement(frequencies[i].toDouble())
            currentPhase = incrementWrapPhase(currentPhase, phaseIncrement)
            val ampl = amplitudes[i].toDouble()
            // Either full negative or positive amplitude.
            outputs[i] = (if (currentPhase < widths[i].toDouble()) -ampl else ampl).toSample()
        }

        // Value needs to be saved for next time.
        phase.set(0, currentPhase)
    }
}
