package com.softsynth.ksyn.util

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSampleLoader {

    @Test
    fun testWavFile() {
        // Locate Clarinet.wav in the composeResources directory relative to the ksyn framework workspace
        val localDir = System.getProperty("user.dir")
        
        // Since Gradle executes from the module directory, we navigate up to root then into demo
        val file = File(localDir, "../demo/src/commonMain/composeResources/files/Clarinet.wav")
        if (!file.exists()) {
            println("Skipping test: " + file.absolutePath + " not found.")
            return
        }

        val rawBytes = file.readBytes()
        val sample = SampleLoader.loadFloatSample(rawBytes)

        assertEquals(26752, sample.numFrames, "Number of frames in Clarinet.wav should match expected 26752")
    }
}
