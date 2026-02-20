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
import com.softsynth.ksyn.ports.UnitOutputPort

/**
 * <pre>
 * output = (inputA * inputB) + inputC
 * </pre>
 *
 * @author Phil Burk (C) 2009 Mobileer Inc
 * @version 016
 * @see Multiply
 * @see Add
 */
class MultiplyAdd : UnitGenerator() {
    val inputA = UnitInputPort("InputA")
    val inputB = UnitInputPort("InputB")
    val inputC = UnitInputPort("InputC")
    val output = UnitOutputPort("Output")

    init {
        addPort(inputA)
        addPort(inputB)
        addPort(inputC)
        addPort(output)
    }

    override fun generate() {
        val aValues = inputA.getValues()
        val bValues = inputB.getValues()
        val cValues = inputC.getValues()
        val outputs = output.getValues()
        
        for (i in 0..<Synthesizer.FRAMES_PER_BLOCK) {
            outputs[i] = (aValues[i] * bValues[i]) + cValues[i]
        }
    }
}
