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

/**
 * Write two samples per audio frame to an AudioOutputStream as interleaved samples.
 *
 * Note that you must call start() on this unit because it does not have an output for pulling data.
 *
 * @author Phil Burk (C) 2009 Mobileer Inc
 */
class StereoStreamWriter : UnitStreamWriter() {
    init {
        input = UnitInputPort(2, "Input")
        addPort(input)
    }

    override fun generate() {
        val leftInputs = input.getValues(0)
        val rightInputs = input.getValues(1)
        val output = outputStream ?: return
        for (i in 0 until Synthesizer.FRAMES_PER_BLOCK) {
            output.write(leftInputs[i])
            output.write(rightInputs[i])
        }
    }
}
