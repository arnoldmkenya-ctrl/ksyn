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

import com.softsynth.ksyn.data.SequentialData

/**
 * An event that is passed to a UnitDataQueueCallback when the element in the queue is played.
 *
 * @author Phil Burk 2009 Mobileer Inc
 */
open class QueueDataEvent(val sourcePort: UnitDataQueuePort) {
    var sequentialData: SequentialData? = null
    var startFrame: Int = 0
    var numFrames: Int = 0

    /** Set how many times the block should be repeated after the first time. For example, if you set numLoops to zero the block will only be played once. If you set numLoops to one the block will be played twice. */
    var numLoops: Int = 0
    var loopsLeft: Int = 0

    /** Number of frames to cross fade from the previous block to this block. This can be used to avoid pops when making abrupt transitions. There must be frames available after the end of the previous block to use for crossfading. The crossfade is linear. */
    var crossFadeIn: Int = 0

    /** If true then this item will be skipped if other items are queued after it. This flag allows you to queue lots of small pieces of sound without making the queue very long. */
    var isSkipIfOthers: Boolean = false

    /** Stop the unit that contains this port after this command has finished. */
    var isAutoStop: Boolean = false

    /** If true then the queue will be cleared and this item will be started immediately. It is better to use this flag than to clear the queue from the application because there could be a gap before the next item is available. This is most useful when combined with setCrossFadeIn(). */
    var isImmediate: Boolean = false
}
