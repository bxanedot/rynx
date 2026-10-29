package dev.bxane.rynx.android

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import dev.bxane.rynx.playback.audio.AudioDspSettings
import dev.bxane.rynx.playback.audio.AudioEqualizerBand
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.pow
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RynxMedia3AudioProcessorTest {
    @Test fun pureModePreservesPcm16BytesExactly() {
        val processor=RynxMedia3AudioProcessor(AudioDspSettings(
            pureMode=true,equalizerEnabled=true,equalizerBands=listOf(AudioEqualizerBand(1_000f,12f)),
        ))
        processor.configure(AudioProcessor.AudioFormat(48_000,2,C.ENCODING_PCM_16BIT))
        val input=ByteBuffer.allocateDirect(12).order(ByteOrder.LITTLE_ENDIAN).apply {
            listOf(0,32767,-32768,12345,-1,7).forEach {putShort(it.toShort())};flip()
        }
        val expected=ByteArray(input.remaining()).also {input.duplicate().get(it)}
        processor.queueInput(input)
        val output=processor.output
        val actual=ByteArray(output.remaining()).also(output::get)
        assertTrue(expected.contentEquals(actual))
        assertEquals(0,input.remaining())
        processor.reset()
    }

    @Test fun configuredPeakingBandChangesDecodedPcmSamples() {
        val processor=RynxMedia3AudioProcessor(AudioDspSettings(
            equalizerEnabled=true,autoHeadroom=false,
            equalizerBands=listOf(AudioEqualizerBand(1_000f,6f,q=1f)),
        ))
        processor.configure(AudioProcessor.AudioFormat(48_000,1,C.ENCODING_PCM_16BIT))
        val frames=24_000
        val input=ByteBuffer.allocateDirect(frames*2).order(ByteOrder.LITTLE_ENDIAN)
        var inputSquare=0.0
        for (frame in 0 until frames) {
            val value=(sin(2.0*PI*1_000*frame/48_000)*8_000).toInt().toShort()
            input.putShort(value)
            if(frame>=2_000)inputSquare+=(value/32768.0)*(value/32768.0)
        }
        input.flip()
        processor.queueInput(input)
        val output=processor.output.order(ByteOrder.LITTLE_ENDIAN)
        var outputSquare=0.0
        var measured=0
        var index=0
        while(output.remaining()>=2) {
            val sample=output.short/32768.0
            if(index>=2_000) {outputSquare+=sample*sample;measured++}
            index++
        }
        assertEquals(frames,index)
        val ratio=kotlin.math.sqrt((outputSquare/measured)/(inputSquare/measured))
        assertTrue("The Media3 processor should apply ~+6 dB at 1 kHz, measured ratio=$ratio",ratio in 1.8..2.2)
        processor.reset()
    }

    @Test fun activePcm24PipelineRetainsFormatAndAppliesGainWithoutTruncatingTo16Bit() {
        val processor=RynxMedia3AudioProcessor(AudioDspSettings(preampDb=-6f,autoHeadroom=false))
        val format=AudioProcessor.AudioFormat(48_000,1,C.ENCODING_PCM_24BIT)
        assertEquals(format,processor.configure(format))
        val input=ByteBuffer.allocateDirect(2_400*3).order(ByteOrder.LITTLE_ENDIAN)
        repeat(2_400) {put24(input,4_000_000)}
        input.flip()
        processor.queueInput(input)
        val output=processor.output.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(2_400*3,output.remaining())
        output.position(2_000*3)
        val positive=read24(output)
        assertTrue("Expected preserved 24-bit gain resolution, got $positive",positive in 1_990_000..2_020_000)
        processor.reset()
    }

    @Test fun activeFloatPcmPipelineAppliesGainAtThirtyTwoBitResolution() {
        val processor=RynxMedia3AudioProcessor(AudioDspSettings(preampDb=-6f,autoHeadroom=false))
        val format=AudioProcessor.AudioFormat(48_000,1,C.ENCODING_PCM_FLOAT)
        assertEquals(format,processor.configure(format))
        val input=ByteBuffer.allocateDirect(2_400*4).order(ByteOrder.LITTLE_ENDIAN)
        repeat(2_400) {input.putFloat(.5f)}
        input.flip()
        processor.queueInput(input)
        val output=processor.output.order(ByteOrder.LITTLE_ENDIAN)
        output.position(2_000*4)
        assertEquals(.5f*10f.pow(-6f/20f),output.float,1e-5f)
        processor.reset()
    }

    private fun put24(buffer:ByteBuffer,value:Int) {
        val encoded=value and 0x00ffffff
        buffer.put((encoded and 0xff).toByte())
        buffer.put(((encoded shr 8) and 0xff).toByte())
        buffer.put(((encoded shr 16) and 0xff).toByte())
    }

    private fun read24(buffer:ByteBuffer):Int {
        val value=(buffer.get().toInt() and 0xff) or
            ((buffer.get().toInt() and 0xff) shl 8) or
            ((buffer.get().toInt() and 0xff) shl 16)
        return if(value and 0x800000!=0)value or -0x1000000 else value
    }
}
