package com.softsynth.ksyn
import kotlin.math.sin
import kotlin.math.PI

class Oscillator {
    fun getSine(phase: Double): Double {
        return sin(phase * 2 * PI)
    }
}
