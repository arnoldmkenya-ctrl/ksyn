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

package com.softsynth.ksyn.ports

import com.softsynth.ksyn.data.FloatSample
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSequentialData {

    private val data1 = floatArrayOf(
        0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f
    )

    private val data2 = floatArrayOf(
        20.0f, 19.0f, 18.0f, 17.0f, 16.0f, 15.0f, 14.0f, 13.0f, 12.0f, 11.0f
    )

    @Test
    fun testCrossfade() {
        val sample1 = FloatSample(data1)
        val sample2 = FloatSample(data2)
        val xfade = SequentialDataCrossfade()
        xfade.setup(sample1, 4, 3, sample2, 1, 6)

        for (i in 0 until 3) {
            val factor = i / 3.0
            val value = ((1.0 - factor) * data1[i + 4]) + (factor * data2[i + 1])
            assertEquals(value, xfade.readDouble(i), 0.00001, "crossfade $i")
        }
        for (i in 3 until 6) {
            assertEquals(sample2.readDouble(i + 1), xfade.readDouble(i), 0.00001, "crossfade $i")
        }
    }
}
