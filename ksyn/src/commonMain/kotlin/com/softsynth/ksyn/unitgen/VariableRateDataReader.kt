/*
 * Copyright 2010 Phil Burk, Mobileer Inc
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

import com.softsynth.ksyn.ports.UnitInputPort

/**
 * Extends SequentialDataReader with a playback rate port.
 *
 * @author Phil Burk (C) 2009 Mobileer Inc
 */
abstract class VariableRateDataReader : SequentialDataReader() {
    /** A scaler for playback rate. Nominally 1.0. */
    var rate: UnitInputPort

    init {
        rate = UnitInputPort("Rate", 1.0)
        addPort(rate)
    }
}
