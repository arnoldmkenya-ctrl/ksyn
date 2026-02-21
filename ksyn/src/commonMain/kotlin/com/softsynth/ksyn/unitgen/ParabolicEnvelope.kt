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
import com.softsynth.ksyn.ports.UnitOutputPort
import com.softsynth.ksyn.toSample

/**
 * ParabolicEnvelope unit. Output goes from zero to amplitude then back to zero in a parabolic arc.
 * <P>
 * Generate a short parabolic envelope that could be used for granular synthesis. The output starts
 * at zero, peaks at the value of amplitude then returns to zero. This unit has two states, IDLE and
 * RUNNING.
 * 
 * @author (C) 1997 Phil Burk, SoftSynth.com
 */
class ParabolicEnvelope : UnitGenerator() {

    /** Fastest repeat rate of envelope if it were continually retriggered in Hertz. */
    val frequency = UnitInputPort("Frequency", UnitOscillator.DEFAULT_FREQUENCY.toFloat())
    /** True value triggers envelope when in resting state. */
    val triggerInput = UnitInputPort("Input")
    val amplitude = UnitInputPort("Amplitude", UnitOscillator.DEFAULT_AMPLITUDE.toFloat())

    /** Trigger output when envelope started. */
    val triggerOutput = UnitOutputPort("TriggerOutput")
    /** Input trigger passed out if ignored for daisy chaining. */
    val triggerPass = UnitOutputPort("TriggerPass")
    val output = UnitOutputPort("Output")

    private var slope = 0.0
    private var curve = 0.0
    private var level = 0.0
    private var running = false

    init {
        addPort(triggerInput)
        addPort(frequency)
        addPort(amplitude)

        addPort(output)
        addPort(triggerOutput)
        addPort(triggerPass)
    }

    override fun generate() {
        val frequencies = frequency.getValues()
        val amplitudes = amplitude.getValues()
        val triggerInputs = triggerInput.getValues()
        val outputs = output.getValues()
        val triggerPasses = triggerPass.getValues()
        val triggerOutputs = triggerOutput.getValues()

        for (i in 0 until Synthesizer.FRAMES_PER_BLOCK) {
            if (!running) {
                if (triggerInputs[i] > 0) {
                    var freq = frequencies[i].toDouble() * (synthesisEngine?.getInverseNyquist()?.toDouble() ?: 0.0)
                    freq = if (freq > 1.0) 1.0 else (if (freq < -1.0) -1.0 else freq)
                    val ampl = amplitudes[i].toDouble()
                    val freq2 = freq * freq /* Square frequency. */
                    slope = 4.0 * ampl * (freq - freq2)
                    curve = -8.0 * ampl * freq2
                    level = 0.0
                    triggerOutputs[i] = UnitGenerator.TRUE.toFloat()
                    running = true
                } else {
                    triggerOutputs[i] = UnitGenerator.FALSE.toFloat()
                }
                triggerPasses[i] = UnitGenerator.FALSE.toFloat()
            } else { /* RUNNING */
                level += slope
                slope += curve
                if (level <= 0.0) {
                    level = 0.0
                    running = false
                    /* Autostop? - FIXME */
                }

                triggerOutputs[i] = UnitGenerator.FALSE.toFloat()
                triggerPasses[i] = triggerInputs[i].toFloat()
            }
            outputs[i] = level.toSample()
        }
    }
}
