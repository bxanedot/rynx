package dev.bxane.rynx.playback.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioDspTest {
    @Test fun flatPipelinePreservesPcmSamplesExactly() {
        val input=floatArrayOf(0f, .25f, -.5f, .999f, -1f, .125f)
        val expected=input.copyOf()
        RynxAudioDspRuntime(AudioDspSettings(),48_000,2).process(input,3)
        assertTrue(input.contentEquals(expected))
    }

    @Test fun pureModeBypassesEveryEnhancement() {
        val settings=AudioDspSettings(
            pureMode=true,
            equalizerEnabled=true,
            equalizerBands=listOf(AudioEqualizerBand(1_000f,12f)),
            bassDb=12f,
            trebleDb=12f,
            stereoWidth=2f,
            crossfeed=1f,
            compressorEnabled=true,
            limiterEnabled=true,
        )
        val input=floatArrayOf(.5f,-.5f,.2f,-.2f)
        val expected=input.copyOf()
        RynxAudioDspRuntime(settings,48_000,2).process(input,2)
        assertTrue(input.contentEquals(expected))
    }

    @Test fun peakingBandProducesItsConfiguredCenterGain() {
        val settings=AudioDspSettings(
            equalizerEnabled=true,
            autoHeadroom=false,
            equalizerBands=listOf(AudioEqualizerBand(1_000f,6f,q=1f)),
        )
        val input=sine(1_000.0,48_000,24_000)
        val before=rms(input,2_000)
        RynxAudioDspRuntime(settings,48_000,1).process(input,input.size)
        val after=rms(input,2_000)
        val gainDb=20.0*kotlin.math.log10(after/before)
        assertTrue(gainDb in 5.7..6.3,"Expected ~6 dB at the band center, got $gainDb dB")
    }

    @Test fun equalizerKeepsFrameCountAndProtectsPeakCeiling() {
        val runtime=RynxAudioDspRuntime(AudioDspSettings(
            equalizerEnabled=true,
            equalizerBands=listOf(AudioEqualizerBand(1_000f,12f)),
            limiterEnabled=true,
            limiterCeiling=.8f,
        ),48_000,2)
        val samples=FloatArray(4096){if(it%2==0).98f else -.98f}
        runtime.process(samples,samples.size/2)
        assertEquals(4096,samples.size)
        assertTrue(samples.all { it.isFinite() && kotlin.math.abs(it)<=.80001f })
    }

    @Test fun layoutFactoriesProduceTenAndThirtyOneEditableBands() {
        assertEquals(10,AudioDspSettings.forLayout(AudioEqualizerLayout.TEN_BAND).equalizerBands.size)
        assertEquals(31,AudioDspSettings.forLayout(AudioEqualizerLayout.THIRTY_ONE_BAND).equalizerBands.size)
    }

    @Test fun compressorReducesAboveThresholdAndKeepsStereoImageLinked() {
        val settings=AudioDspSettings(
            compressorEnabled=true,
            compressorThresholdDb=-18f,
            compressorRatio=4f,
            compressorAttackMs=2f,
            compressorReleaseMs=80f,
            autoHeadroom=false,
        )
        val input=FloatArray(48_000*2) { index ->
            val frame=index/2
            val channel=if(index%2==0)1f else .5f
            (sin(2.0*PI*1_000*frame/48_000)*.7*channel).toFloat()
        }
        val beforeLeft=rms(input.filterIndexed { index, _ -> index%2==0 }.toFloatArray(),2_000)
        val beforeRatio=beforeLeft/rms(input.filterIndexed { index, _ -> index%2==1 }.toFloatArray(),2_000)
        RynxAudioDspRuntime(settings,48_000,2).process(input,input.size/2)
        val left=input.filterIndexed { index, _ -> index%2==0 }.toFloatArray()
        val right=input.filterIndexed { index, _ -> index%2==1 }.toFloatArray()
        val afterLeft=rms(left,2_000)
        assertTrue(afterLeft < beforeLeft*.8,"Compressor should reduce the above-threshold signal")
        assertEquals(beforeRatio,afterLeft/rms(right,2_000),.02,"Stereo-linked detection should preserve image balance")
    }

    private fun sine(frequency:Double,sampleRate:Int,count:Int)=FloatArray(count){index->
        sin(2.0*PI*frequency*index/sampleRate).toFloat()*.4f
    }

    private fun rms(samples:FloatArray,skip:Int):Double {
        var sum=0.0
        for(index in skip until samples.size)sum+=samples[index]*samples[index]
        return kotlin.math.sqrt(sum/(samples.size-skip))
    }
}
