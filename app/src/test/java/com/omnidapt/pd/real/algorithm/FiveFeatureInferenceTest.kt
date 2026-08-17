package com.omnidapt.pd.real.algorithm

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiveFeatureInferenceTest {
    @Test
    fun spectralFeaturesMatchPythonScipyGoldenWindow() {
        val channel0 = DoubleArray(256) { n ->
            120 * sin(2 * PI * 17.3 * n / 256) +
                55 * sin(2 * PI * 30.7 * n / 256) +
                20 * sin(2 * PI * 84.4 * n / 256) +
                ((n % 17) - 8) * 0.3
        }
        val channel1 = DoubleArray(256) { n ->
            100 * sin(2 * PI * 17.3 * n / 256 + 0.2) +
                40 * sin(2 * PI * 30.7 * n / 256) +
                25 * sin(2 * PI * 84.4 * n / 256) +
                ((n % 13) - 6) * 0.25
        }
        val actual = extractRawSpectralFeatures(arrayOf(channel0, channel1))
        val python = doubleArrayOf(
            -15.043041870986032,
            0.47113866844026975,
            54.43544985605868,
            67.49910642666524,
            58.65854459393043,
        )
        actual.zip(python).forEach { (kotlin, expected) ->
            assertTrue("kotlin=$kotlin python=$expected", abs(kotlin - expected) < 1e-6)
        }
    }

    @Test
    fun gmmProbabilitiesMatchPythonGoldenVector() {
        val actual = inferState(
            doubleArrayOf(0.30, 0.40, 1.25, 0.45, 0.20),
            GmmModel.default(),
        )
        val expected = mapOf(
            "OFF-Rest" to 0.5786080216401956,
            "OFF-Move" to 0.1636099687081118,
            "ON-Rest" to 0.2227524371834246,
            "ON-Move" to 0.03502957246826779,
        )
        expected.forEach { (label, probability) ->
            assertEquals(probability, actual.probabilities.getValue(label), 1e-9)
        }
        assertEquals("OFF-Rest", actual.topState)
        assertEquals(-2.612572117114624, actual.logLikelihood, 1e-9)
        assertFalse(actual.rejected)
    }
}
