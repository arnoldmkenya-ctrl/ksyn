/*
 * Copyright 2011 Phil Burk, Mobileer Inc
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

import com.softsynth.ksyn.data.FloatSample
import com.softsynth.ksyn.ports.UnitInputPort

/**
 * A GrainFarm that uses a FloatSample as source material. Use a ramp to move
 * smoothly through the sample by connecting to the position port.
 *
 * <pre><code>
 *    synth.add(sampleGrainFarm = SampleGrainFarm())
 *    sampleGrainFarm.setSample(sample)
 *    // Use a ramp to move smoothly within the file.
 *    synth.add(ramp = ContinuousRamp())
 *    ramp.output.connect(sampleGrainFarm.position)
 * </code></pre>
 *
 * @author Phil Burk (C) 2011 Mobileer Inc
 */
class SampleGrainFarm : GrainFarm() {
    private var sample: FloatSample? = null
    val position: UnitInputPort
    val positionRange: UnitInputPort

    init {
        position = UnitInputPort("Position", 0.0)
        addPort(position)
        positionRange = UnitInputPort("PositionRange", 0.0)
        addPort(positionRange)
    }

    override fun allocate(numGrains: Int) {
        val grainArray = Array(numGrains) {
            Grain(SampleGrainSource(), RaisedCosineEnvelope())
        }
        setGrainArray(grainArray)
    }

    override fun setupGrain(grain: Grain, i: Int) {
        val sampleGrainSource = grain.source as SampleGrainSource
        sample?.let { sampleGrainSource.setSample(it) }
        sampleGrainSource.setPosition(position.getValues()[i].toDouble())
        sampleGrainSource.setPositionRange(positionRange.getValues()[i].toDouble())
        super.setupGrain(grain, i)
    }

    fun setSample(sample: FloatSample) {
        this.sample = sample
    }
}
