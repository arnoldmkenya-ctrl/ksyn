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
import com.softsynth.ksyn.toSample

/**
 * Simple triangle wave oscillator.
 *
 * @author Phil Burk (C) 2009 Mobileer Inc
 */
class TriangleOscillator : UnitOscillator() {

    init {
        phase.set(0, -0.5)
    }

    override fun generate() {
        val frequencies = frequency.getValues()
        val amplitudes = amplitude.getValues()
        val outputs = output.getValues()

        // Variables have a single value.
        var currentPhase: Double = phase.getValue(0).toDouble()

        for (i in 0..<Synthesizer.FRAMES_PER_BLOCK) {
            /* Generate sawtooth phasor to provide phase for triangle generation. */
            val phaseIncrement = convertFrequencyToPhaseIncrement(frequencies[i].toDouble())
            currentPhase = incrementWrapPhase(currentPhase, phaseIncrement)

            /* Map phase to triangle waveform. */
            /* 0 - 0.999 => 0.5-p => +0.5 - -0.5 */
            /* -1.0 - 0.0 => 0.5+p => -0.5 - +0.5 */
            val triangle = if (currentPhase >= 0.0) (0.5 - currentPhase) else (0.5 + currentPhase)

            outputs[i] = (triangle * 2.0 * amplitudes[i].toDouble()).toSample()
        }

        // Value needs to be saved for next time.
        phase.set(0, currentPhase)
    }
}
