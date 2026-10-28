package dev.bxane.rynx.ui.material

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bxane.rynx.design.*
import dev.bxane.rynx.ui.*

@Composable internal fun AppearanceSheet(appearance:RynxAppearance,onChange:(RynxAppearance)->Unit,onClose:()->Unit) {
    PlayerBackHandler(true,onClose)
    val dark=appearance.darkTheme || appearance.material==RynxMaterialMode.Amoled
    val ink=if(dark)Color.White else Color(0xFF1A1B21)
    val muted=ink.copy(alpha=.62f)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.32f)).pointerInput(Unit) {
        awaitPointerEventScope {while(true)awaitPointerEvent().changes.forEach {it.consume()}}
    },contentAlignment=Alignment.BottomCenter) {
        CompositionLocalProvider(LocalRynxMaterialLayer provides 4f,LocalRynxDarkSurface provides dark) {
            RynxSurface(Modifier.fillMaxWidth().fillMaxHeight(.94f).semantics {paneTitle="Appearance"},radius=30.dp,shadow=true) {
                Column(Modifier.fillMaxSize().padding(horizontal=22.dp)) {
                    Row(Modifier.fillMaxWidth().height(66.dp),verticalAlignment=Alignment.CenterVertically) {
                        BasicText("Appearance",Modifier.weight(1f),style=TextStyle(color=ink,fontSize=25.sp,fontWeight=FontWeight.Bold))
                        RynxButton("Done",onClose)
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        BasicText("Interface Material",style=TextStyle(color=ink,fontSize=19.sp,fontWeight=FontWeight.SemiBold))
                        RynxSegments(RynxMaterialMode.entries.map {it.label to it.label},RynxMaterialMode.entries.indexOf(appearance.material),
                            {onChange(appearance.copy(material=RynxMaterialMode.entries[it]))})
                        MaterialPreview()
                        BasicText("Motion",style=TextStyle(color=ink,fontSize=19.sp,fontWeight=FontWeight.SemiBold))
                        RynxSegments(listOf("System" to "Follow system motion","Full" to "Full motion","Reduced" to "Reduced motion"),
                            RynxMotionPreference.entries.indexOf(appearance.motion),{onChange(appearance.copy(motion=RynxMotionPreference.entries[it]))})
                        AppearanceSlider("Motion intensity",appearance.motionIntensity,true){onChange(appearance.copy(motionIntensity=it))}
                        BasicText("Liquid Glass",style=TextStyle(color=ink,fontSize=19.sp,fontWeight=FontWeight.SemiBold))
                        val glass=appearance.material==RynxMaterialMode.LiquidGlass
                        val controls=listOf(
                            Triple("Transparency",appearance.transparency,{v:Float->appearance.copy(transparency=v)}),
                            Triple("Blur strength",appearance.blurStrength,{v:Float->appearance.copy(blurStrength=v)}),
                            Triple("Refraction intensity",appearance.refractionIntensity,{v:Float->appearance.copy(refractionIntensity=v)}),
                            Triple("Highlight intensity",appearance.highlightIntensity,{v:Float->appearance.copy(highlightIntensity=v)}),
                            Triple("Surface tint strength",appearance.tintStrength,{v:Float->appearance.copy(tintStrength=v)}),
                            Triple("Border brightness",appearance.borderBrightness,{v:Float->appearance.copy(borderBrightness=v)}),
                            Triple("Vibrancy",appearance.vibrancy,{v:Float->appearance.copy(vibrancy=v)}),
                            Triple("Shadow intensity",appearance.shadowIntensity,{v:Float->appearance.copy(shadowIntensity=v)}),
                        )
                        controls.forEach {(title,value,copy)->AppearanceSlider(title,value,glass){onChange(copy(it))}}
                        RynxGlassSwitch("Dynamic color adaptation",appearance.dynamicColor,glass){onChange(appearance.copy(dynamicColor=it))}
                        BasicText(if(glass)"Effects adapt to your device while keeping Liquid Glass selected." else "Your glass preferences are kept when you change materials.",style=TextStyle(color=muted,fontSize=13.sp))
                        BasicText("rynx · by bxane",style=TextStyle(color=muted,fontSize=13.sp))
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable private fun AppearanceSlider(label:String,value:Float,enabled:Boolean,onChange:(Float)->Unit) {
    val dark=LocalRynxAppearance.current.darkTheme || LocalRynxAppearance.current.material==RynxMaterialMode.Amoled
    Column {
        Row(Modifier.fillMaxWidth()) {
            BasicText(label,Modifier.weight(1f),style=TextStyle(color=(if(dark)Color.White else Color(0xFF1A1B21)).copy(alpha=if(enabled).85f else .4f),fontSize=14.sp))
            BasicText("${(value*100).toInt()}%",style=TextStyle(color=if(dark)Color.LightGray else Color.Gray,fontSize=12.sp))
        }
        CompositionLocalProvider(LocalRynxDarkSurface provides dark) {RynxSlider(value,label,enabled,onChange,Modifier.fillMaxWidth())}
    }
}

@Composable internal fun RynxGlassSwitch(label:String,checked:Boolean,enabled:Boolean=true,onChange:(Boolean)->Unit) {
    val interaction=remember {androidx.compose.foundation.interaction.MutableInteractionSource()}
    val reduced=LocalRynxReducedMotion.current
    val animated by androidx.compose.animation.core.animateFloatAsState(if(checked)1f else 0f,
        if(reduced)androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring(stiffness=RynxMotionTokens.SelectionStiffness,dampingRatio=RynxMotionTokens.SelectionDamping))
    val fraction=if(reduced){if(checked)1f else 0f}else animated
    Row(Modifier.fillMaxWidth().heightIn(min=52.dp).rynxPressFeedback(interaction,enabled)
        .clickable(enabled=enabled,role=Role.Switch,interactionSource=interaction,indication=null){onChange(!checked)}
        .semantics {contentDescription=label;toggleableState=if(checked)androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off;if(!enabled)disabled()},verticalAlignment=Alignment.CenterVertically) {
        val dark=LocalRynxDarkSurface.current
        BasicText(label,Modifier.weight(1f),style=TextStyle(color=if(dark)Color.White else Color(0xFF1A1B21),fontSize=14.sp))
        Box(Modifier.size(52.dp,32.dp).rynxMaterial(16.dp,checked,interaction)) {
            Box(Modifier.padding(3.dp).offset(x=(20*fraction).dp).size(26.dp).background(if(checked)RynxAccent else Color(0xFFBDBFC9),RoundedCornerShape(13.dp)))
        }
    }
}

@Composable private fun MaterialPreview() {
    val level=LocalRynxMaterialLayer.current
    Box(Modifier.fillMaxWidth().height(100.dp)) {
        val previewBackground=if(LocalRynxAppearance.current.material==RynxMaterialMode.Amoled)Brush.linearGradient(listOf(Color.Black,Color.Black))
            else Brush.linearGradient(listOf(Color(0xFF234B78),Color(0xFF9B497C),Color(0xFFEDB492)))
        Box(Modifier.fillMaxSize().rynxBackdropSource(level+.5f).background(previewBackground))
        CompositionLocalProvider(LocalRynxMaterialDepth provides 0,LocalRynxMaterialLayer provides level+1f) {
            RynxSurface(Modifier.align(Alignment.Center).size(210.dp,64.dp),24.dp) {
                BasicText("rynx",Modifier.align(Alignment.Center),style=TextStyle(color=if(LocalRynxAppearance.current.material==RynxMaterialMode.Amoled || LocalRynxAppearance.current.darkTheme)Color.White else Color(0xFF151722),fontSize=24.sp,fontWeight=FontWeight.SemiBold))
            }
        }
    }
}
