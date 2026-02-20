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
 * Narrow impulse oscillator. An impulse is only one sample wide. It is useful for pinging filters
 * or generating an "impulse response".
 *
 * @author Phil Burk (C) 2009 Mobileer Inc
 */
class ImpulseOscillator : UnitOscillator() {

    override fun generate() {
        val frequencies = frequency.getValues()
        val amplitudes = amplitude.getValues()
        val outputs = output.getValues()

        // Variables have a single value.
        var currentPhase: Double = phase.getValue(0).toDouble()

        val inverseNyquist: Double = synthesisEngine?.getInverseNyquist()?.toDouble() ?: 0.0

        for (i in 0..<Synthesizer.FRAMES_PER_BLOCK) {
            /* Generate sawtooth phasor to provide phase for impulse generation. */
            val phaseIncrement = frequencies[i].toDouble() * inverseNyquist
            currentPhase += phaseIncrement

            val ampl = amplitudes[i].toDouble()
            var result = 0.0
            if (currentPhase >= 1.0) {
                currentPhase -= 2.0
                result = ampl
            } else if (currentPhase < -1.0) {
                currentPhase += 2.0
                result = ampl
            }
            outputs[i] = result.toSample()
        }

        // Value needs to be saved for next time.
        phase.set(0, currentPhase)
    }
}
