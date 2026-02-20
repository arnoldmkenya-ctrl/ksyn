/*
 * Copyright 2004 Phil Burk, Mobileer Inc
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

/**
 * SelectUnit unit. Select InputA or InputB based on value on Select port.
 *
 *<pre> `
 * output = ( select > 0.0 ) ? inputB : inputA;
` </pre> *
 *
 * @author (C) 2004-2009 Phil Burk, SoftSynth.com
 */
class Select : UnitGenerator() {
    val inputA = UnitInputPort("InputA")
    val inputB = UnitInputPort("InputB")
    val select = UnitInputPort("Select")
    val output = UnitOutputPort("Output")

    init {
        addPort(inputA)
        addPort(inputB)
        addPort(select)
        addPort(output)
    }

    override fun generate() {
        val inputAs = inputA.getValues()
        val inputBs = inputB.getValues()
        val selects = select.getValues()
        val outputs = output.getValues()

        for (i in 0..<Synthesizer.FRAMES_PER_BLOCK) {
            outputs[i] = if (selects[i] > UnitGenerator.FALSE) inputBs[i] else inputAs[i]
        }
    }
}
