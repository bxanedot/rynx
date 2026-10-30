package dev.bxane.rynx.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bxane.rynx.design.RynxMaterialMode
import dev.bxane.rynx.playback.audio.AudioDspSettings
import dev.bxane.rynx.playback.audio.AudioEqualizerBand
import dev.bxane.rynx.playback.audio.AudioEqualizerLayout
import dev.bxane.rynx.playback.audio.AudioFilterShape
import dev.bxane.rynx.playback.audio.AudioSignalInfo
import dev.bxane.rynx.ui.material.*
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

@Composable internal fun AudioDspSheet(
    settings: AudioDspSettings,
    signalInfo: AudioSignalInfo,
    clippingCount: Long,
    onChange: (AudioDspSettings) -> Unit,
    onClose: () -> Unit,
) {
    PlayerBackHandler(true,onClose)
    val dark=LocalRynxAppearance.current.darkTheme || LocalRynxAppearance.current.material==RynxMaterialMode.Amoled
    val ink=if(dark)Color.White else Color(0xFF1A1B21)
    val muted=ink.copy(alpha=.62f)
    val disabled=settings.pureMode
    val update:(AudioDspSettings)->Unit={onChange(it.normalized())}
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.34f)).pointerInput(Unit) {
        awaitPointerEventScope {while(true)awaitPointerEvent().changes.forEach {it.consume()}}
    },contentAlignment=Alignment.BottomCenter) {
        CompositionLocalProvider(LocalRynxMaterialLayer provides 4f,LocalRynxDarkSurface provides dark) {
            RynxSurface(Modifier.fillMaxWidth().fillMaxHeight(.96f).semantics {paneTitle="Sound and EQ"},radius=30.dp,shadow=true) {
                Column(Modifier.fillMaxSize().padding(horizontal=22.dp)) {
                    Row(Modifier.fillMaxWidth().height(66.dp),verticalAlignment=Alignment.CenterVertically) {
                        BasicText("Sound and EQ",Modifier.weight(1f),style=TextStyle(color=ink,fontSize=25.sp,fontWeight=FontWeight.Bold))
                        RynxButton("Done",onClose)
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        BasicText("Playback mode",style=TextStyle(color=ink,fontSize=19.sp,fontWeight=FontWeight.SemiBold))
                        RynxGlassSwitch("Rynx Pure Mode",settings.pureMode) {enabled->update(settings.copy(pureMode=enabled))}
                        BasicText("Pure Mode bypasses Rynx effects. Android and Media3 still control decoding, mixing, resampling and output routing; exclusive or bit-perfect output cannot be confirmed.",style=TextStyle(color=muted,fontSize=13.sp))

                        BasicText("Equalizer",style=TextStyle(color=ink,fontSize=19.sp,fontWeight=FontWeight.SemiBold))
                        RynxGlassSwitch("Enable equalizer",settings.equalizerEnabled,enabled=!disabled) {enabled->update(settings.copy(equalizerEnabled=enabled))}
                        RynxSegments(
                            listOf("10 band" to "10-band equalizer","31 band" to "31-band equalizer","Parametric" to "Parametric equalizer"),
                            settings.equalizerLayout.ordinal,
                            {index->update(settings.withLayout(AudioEqualizerLayout.entries[index]))},
                        )
                        val eqActive=settings.equalizerEnabled && !disabled
                        if(settings.equalizerLayout==AudioEqualizerLayout.PARAMETRIC) {
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                BasicText("Filter bands",Modifier.weight(1f),style=TextStyle(color=ink,fontSize=14.sp,fontWeight=FontWeight.SemiBold))
                                RynxButton("Add filter",{
                                    if(settings.equalizerBands.size<12) {
                                        val frequencies=listOf(80f,250f,750f,1_500f,3_000f,6_000f,12_000f)
                                        val frequency=frequencies.getOrElse(settings.equalizerBands.size-4){1_000f}
                                        update(settings.copy(equalizerBands=settings.equalizerBands+AudioEqualizerBand(frequency)))
                                    }
                                },enabled=!disabled && settings.equalizerBands.size<12)
                            }
                            settings.equalizerBands.forEachIndexed {index,band->
                                ParametricBand(index,band,ink,muted,eqActive,
                                    onChange={changed->update(settings.copy(equalizerBands=settings.equalizerBands.toMutableList().also {it[index]=changed}))},
                                    onRemove={if(settings.equalizerBands.size>1)update(settings.copy(equalizerBands=settings.equalizerBands.toMutableList().also {it.removeAt(index)}))},
                                )
                            }
                        } else {
                            LazyColumn(Modifier.fillMaxWidth().heightIn(max=390.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                itemsIndexed(settings.equalizerBands,key={_,band->"eq:${band.frequencyHz}"}) {index,band->
                                    val frequency=frequencyLabel(band.frequencyHz)
                                    Column(Modifier.fillMaxWidth().rynxMaterial(12.dp,amount=.22f).padding(horizontal=12.dp,vertical=4.dp)) {
                                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                            BasicText(frequency,Modifier.weight(1f),style=TextStyle(color=ink,fontSize=13.sp,fontWeight=FontWeight.Medium))
                                            BasicText("${band.gainDb.toInt()} dB",style=TextStyle(color=muted,fontSize=12.sp))
                                        }
                                        RynxSlider(((band.gainDb+18f)/36f).coerceIn(0f,1f),"$frequency EQ gain",eqActive,{fraction->
                                            val bands=settings.equalizerBands.toMutableList()
                                            bands[index]=band.copy(gainDb=fraction*36f-18f)
                                            update(settings.copy(equalizerBands=bands))
                                        })
                                    }
                                }
                            }
                        }

                        BasicText("Tone and headroom",style=TextStyle(color=ink,fontSize=19.sp,fontWeight=FontWeight.SemiBold))
                        DspSlider("Bass",settings.bassDb,-12f..12f,"${settings.bassDb.toInt()} dB",!disabled){update(settings.copy(bassDb=it))}
                        DspSlider("Treble",settings.trebleDb,-12f..12f,"${settings.trebleDb.toInt()} dB",!disabled){update(settings.copy(trebleDb=it))}
                        DspSlider("Preamp",settings.preampDb,-24f..12f,"${settings.preampDb.toInt()} dB",!disabled){update(settings.copy(preampDb=it))}
                        RynxGlassSwitch("Automatic EQ headroom",settings.autoHeadroom,enabled=!disabled) {enabled->update(settings.copy(autoHeadroom=enabled))}

                        BasicText("Dynamic range",style=TextStyle(color=ink,fontSize=19.sp,fontWeight=FontWeight.SemiBold))
                        RynxGlassSwitch("Compressor",settings.compressorEnabled,enabled=!disabled) {enabled->update(settings.copy(compressorEnabled=enabled))}
                        if(settings.compressorEnabled) {
                            DspSlider("Threshold",settings.compressorThresholdDb,-60f..0f,"${settings.compressorThresholdDb.toInt()} dB",!disabled){update(settings.copy(compressorThresholdDb=it))}
                            DspSlider("Ratio",settings.compressorRatio,1f..20f,"${(settings.compressorRatio*10).toInt()/10f}:1",!disabled){update(settings.copy(compressorRatio=it))}
                            DspSlider("Attack",settings.compressorAttackMs,.1f..200f,"${(settings.compressorAttackMs*10).toInt()/10f} ms",!disabled){update(settings.copy(compressorAttackMs=it))}
                            DspSlider("Release",settings.compressorReleaseMs,5f..2_000f,"${settings.compressorReleaseMs.toInt()} ms",!disabled){update(settings.copy(compressorReleaseMs=it))}
                        }

                        BasicText("Stereo and protection",style=TextStyle(color=ink,fontSize=19.sp,fontWeight=FontWeight.SemiBold))
                        DspSlider("Stereo width",settings.stereoWidth,0f..2f,"${(settings.stereoWidth*100).toInt()}%",!disabled){update(settings.copy(stereoWidth=it))}
                        DspSlider("Stereo balance",settings.stereoBalance,-1f..1f,when {settings.stereoBalance<-.01f->"Left ${(abs(settings.stereoBalance)*100).toInt()}%";settings.stereoBalance>.01f->"Right ${(settings.stereoBalance*100).toInt()}%";else->"Center"},!disabled){update(settings.copy(stereoBalance=it))}
                        DspSlider("Headphone crossfeed",settings.crossfeed,0f..1f,"${(settings.crossfeed*100).toInt()}%",!disabled){update(settings.copy(crossfeed=it))}
                        RynxGlassSwitch("Inter-sample peak limiter",settings.limiterEnabled,enabled=!disabled) {enabled->update(settings.copy(limiterEnabled=enabled))}
                        if(settings.limiterEnabled) DspSlider("Limiter ceiling",settings.limiterCeiling,.5f..1f,"${(settings.limiterCeiling*100).toInt()}%",!disabled){update(settings.copy(limiterCeiling=it))}
                        RynxGlassSwitch("TPDF output dither",settings.ditherEnabled,enabled=!disabled) {enabled->update(settings.copy(ditherEnabled=enabled))}
                        BasicText("Optional low-level dither when Rynx converts processed PCM to 8-, 16- or 24-bit integer samples. Disabled in Pure Mode. It does not upgrade the original track or DAC precision.",style=TextStyle(color=muted,fontSize=13.sp))
                        RynxGlassSwitch("Skip long silences",settings.skipSilenceEnabled,enabled=!disabled) {enabled->update(settings.copy(skipSilenceEnabled=enabled))}
                        BasicText("Uses Media3 silence detection to shorten quiet passages during Android playback. Off by default to preserve musical pauses, and automatically bypassed in Pure Mode.",style=TextStyle(color=muted,fontSize=13.sp))
                        Presets(settings,ink,disabled,update)

                        BasicText("Signal and output",style=TextStyle(color=ink,fontSize=19.sp,fontWeight=FontWeight.SemiBold))
                        val sourceRate=signalInfo.sourceSampleRateHz?.let {"${it/1000f} kHz"} ?: "Not reported by source"
                        val sourceDepth=signalInfo.sourceBitDepth?.let {"$it-bit PCM"} ?: "Codec-managed / not exposed"
                        val sourceCodec=signalInfo.sourceEncoding ?: "Media3 decoded format"
                        BasicText("Media3 selected track format: $sourceRate · $sourceDepth · $sourceCodec",style=TextStyle(color=muted,fontSize=13.sp))
                        val dspRate=signalInfo.dspInputSampleRateHz?.let {"${it/1000f} kHz"} ?: "Not configured"
                        val dspDepth=signalInfo.dspInputBitDepth?.let {"$it-bit PCM"} ?: "Not reported"
                        val dspChannels=signalInfo.dspInputChannelCount?.let {"$it channel(s)"} ?: "Unknown channels"
                        val dspState=when(signalInfo.dspProcessingActive) {true->"Processing";false->"Bypassed";null->"Not configured"}
                        BasicText("Rynx DSP input: $dspRate · $dspDepth · $dspChannels · $dspState",style=TextStyle(color=muted,fontSize=13.sp))
                        BasicText("DSP input describes the PCM delivered to Rynx, not the decoded source quality or the physical audio output format.",style=TextStyle(color=muted,fontSize=13.sp))
                        val nativeRate=signalInfo.primaryOutputNativeRateHz?.let {"${it/1000f} kHz"} ?: "unavailable"
                        BasicText("Android primary output native-rate hint: $nativeRate. It may differ from the active Bluetooth or USB route.",style=TextStyle(color=muted,fontSize=13.sp))
                        if(signalInfo.availableOutputRoutes.isNotEmpty()) BasicText("Android reports these available routes: ${signalInfo.availableOutputRoutes.joinToString()}",style=TextStyle(color=muted,fontSize=13.sp))
                        BasicText("Active DAC bit depth, exclusive USB mode and bit-perfect delivery are not exposed or guaranteed by this Media3 path.",style=TextStyle(color=muted,fontSize=13.sp))
                        BasicText(if(clippingCount>0)"Clipping guard engaged on $clippingCount processed samples since app launch." else "No clipped PCM samples recorded since app launch.",style=TextStyle(color=if(clippingCount>0)Color(0xFFFF9E45) else muted,fontSize=13.sp))
                        RynxButton("Reset sound settings",{update(AudioDspSettings())})
                        BasicText("rynx · bxane · DSP runs locally on this device",style=TextStyle(color=muted,fontSize=13.sp))
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable private fun DspSlider(label:String,value:Float,range:ClosedFloatingPointRange<Float>,valueLabel:String,enabled:Boolean,onChange:(Float)->Unit) {
    val safe=if(value.isFinite())value.coerceIn(range.start,range.endInclusive) else range.start
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            BasicText(label,Modifier.weight(1f),style=TextStyle(color=if(LocalRynxDarkSurface.current)Color.White else Color(0xFF1A1B21),fontSize=14.sp))
            BasicText(valueLabel,style=TextStyle(color=if(LocalRynxDarkSurface.current)Color.LightGray else Color.Gray,fontSize=12.sp))
        }
        RynxSlider((safe-range.start)/(range.endInclusive-range.start),label,enabled,{fraction->onChange(range.start+fraction*(range.endInclusive-range.start))})
    }
}

@Composable private fun ParametricBand(index:Int,band:AudioEqualizerBand,ink:Color,muted:Color,enabled:Boolean,onChange:(AudioEqualizerBand)->Unit,onRemove:()->Unit) {
    Column(Modifier.fillMaxWidth().rynxMaterial(16.dp,amount=.24f).padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            BasicText("Filter ${index+1} · ${frequencyLabel(band.frequencyHz)}",Modifier.weight(1f),style=TextStyle(color=ink,fontSize=14.sp,fontWeight=FontWeight.SemiBold))
            RynxButton("Remove",onRemove,enabled=enabled)
        }
        RynxSegments(AudioFilterShape.entries.map {it.name.replace('_',' ') to "${it.name.replace('_',' ')} filter"},band.shape.ordinal,{onChange(band.copy(shape=AudioFilterShape.entries[it]))})
        DspSlider("Frequency",ln((band.frequencyHz.coerceAtLeast(20f)/20f).toDouble()).toFloat()/ln(1_000.0).toFloat(),0f..1f,frequencyLabel(band.frequencyHz),enabled){fraction->
            onChange(band.copy(frequencyHz=20f*1_000f.pow(fraction)))
        }
        if(band.shape!=AudioFilterShape.HIGH_PASS && band.shape!=AudioFilterShape.LOW_PASS)
            DspSlider("Gain",band.gainDb,-18f..18f,"${band.gainDb.toInt()} dB",enabled){onChange(band.copy(gainDb=it))}
        DspSlider("Q",band.q,.1f..12f,"${(band.q*10).toInt()/10f}",enabled){onChange(band.copy(q=it))}
    }
}

@Composable private fun Presets(settings:AudioDspSettings,ink:Color,enabled:Boolean,onChange:(AudioDspSettings)->Unit) {
    BasicText("Presets",style=TextStyle(color=ink,fontSize=19.sp,fontWeight=FontWeight.SemiBold))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("Flat","Bass","Vocal","Treble").forEach {preset->
            RynxButton(preset,{
                val bands=settings.equalizerBands.mapIndexed {index,band->
                    val frequency=band.frequencyHz
                    val gain=when(preset) {
                        "Bass"->when {frequency<50f->4f;frequency<125f->3f;frequency<250f->2f;else->0f}
                        "Vocal"->when {frequency in 500f..2_000f->3f;frequency in 250f..4_000f->1f;else->0f}
                        "Treble"->when {frequency>=8_000f->4f;frequency>=4_000f->2f;frequency>=2_000f->1f;else->0f}
                        else->0f
                    }
                    band.copy(gainDb=gain)
                }
                onChange(settings.copy(equalizerEnabled=true,equalizerBands=bands))
            },enabled=enabled)
        }
    }
}

private fun frequencyLabel(frequency:Float):String = if(frequency>=1_000f)"${(frequency/1_000f*10).toInt()/10f} kHz" else "${frequency.toInt()} Hz"
