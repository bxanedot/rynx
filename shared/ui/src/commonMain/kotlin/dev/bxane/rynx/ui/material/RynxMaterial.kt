@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)
package dev.bxane.rynx.ui.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bxane.rynx.design.*
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.glass.*

internal val LocalRynxAppearance=staticCompositionLocalOf { RynxAppearance() }
internal val LocalRynxBackdrop=staticCompositionLocalOf { HazeState() }
internal val LocalRynxMaterialLayer=staticCompositionLocalOf { 1f }
internal val LocalRynxMaterialDepth=staticCompositionLocalOf { 0 }
internal val LocalRynxReducedMotion=staticCompositionLocalOf { false }
internal val LocalRynxRenderingPressure=staticCompositionLocalOf { false }
internal val LocalRynxDarkSurface=staticCompositionLocalOf { false }
internal val RynxAccent=Color(0xFFFA3658)

@Composable internal fun Modifier.rynxBackdropSource(level:Float):Modifier =
    if(LocalRynxAppearance.current.policy().backdrop) hazeSource(LocalRynxBackdrop.current,zIndex=level) else this

/** One input registry per window; nested surfaces do not allocate another full-strength blur. */
@Composable internal fun Modifier.rynxMaterial(
    radius:Dp=20.dp,selected:Boolean=false,interaction:MutableInteractionSource?=null,
    amount:Float=1f,shadow:Boolean=false,
):Modifier {
    val appearance=LocalRynxAppearance.current.normalized()
    val pressure=LocalRynxRenderingPressure.current
    val policy=appearance.policy(pressure)
    val dark=LocalRynxDarkSurface.current || appearance.darkTheme || appearance.material==RynxMaterialMode.Amoled
    val depth=LocalRynxMaterialDepth.current
    val layer=LocalRynxMaterialLayer.current
    val state=LocalRynxBackdrop.current
    val shape=RoundedCornerShape(radius)
    val base=if(appearance.material==RynxMaterialMode.Amoled)Color.Black else if(dark)Color(0xFF242B36) else Color(policy.surfaceArgb)
    val targetTint=if(selected)RynxAccent else base
    val animatedTint by animateColorAsState(targetTint,tween(if(LocalRynxReducedMotion.current)0 else RynxMotionTokens.ColorMillis))
    val selectedTint=if(appearance.material==RynxMaterialMode.Amoled)Color.Black else animatedTint
    val strength=amount.coerceIn(0f,1f)
    val style=remember(appearance,shape,dark,selected,depth,strength,pressure,selectedTint) {
        GlassStyle.regular.then {
            shape(shape)
            backgroundColor(Color.Transparent)
            contrast(.04f)
            whitePoint(if(dark)-.10f else .08f)
            tint(selectedTint.copy(alpha=((1f-appearance.transparency)*.60f+appearance.tintStrength*.26f)*strength))
            optics(blurRadius=(appearance.blurStrength*30f*strength).dp,
                refractionStrength=appearance.refractionIntensity*.85f*strength,
                refractionDisplacement=12.dp,depth=.65f,refractionDetailIntensity=0f)
            specularIntensity(appearance.highlightIntensity*.65f*strength)
            ambientResponse(appearance.borderBrightness*.22f)
            edgeShadow(Color.Black.copy(alpha=.12f*appearance.shadowIntensity))
            edgeSoftness(.8.dp)
            chromaMultiplier(if(appearance.dynamicColor)1f+appearance.vibrancy*.18f else 1f)
            hovered { lightingIntensity(.25f) }
            focused { lightingIntensity(.35f) }
            pressed {lightingIntensity(.65f);refractionMultiplier(1.04f)}
            interactionPositionAnimationSpec(spring(stiffness=RynxMotionTokens.PressStiffness,dampingRatio=RynxMotionTokens.PressDamping))
        }
    }
    var result=this
    if(shadow && policy.shadow>0f) result=result.shadow((10f*policy.shadow).dp,shape,clip=false)
    result=result.clip(shape)
    result=when {
        policy.backdrop && depth==0 && strength>.01f -> result.hazeGlass(
            input=HazeInput.Sources(state,selection=HazeSourceSelection.Behind.where {it.zIndex<layer}),
            style=style,interactionSource=interaction,
            performanceMode=if(pressure)HazePerformanceMode.Performance else HazePerformanceMode.Balanced,
        )
        policy.backdrop -> result.background(selectedTint.copy(alpha=(if(selected).20f else .09f)*strength))
        else -> result.background(if(selected && appearance.material!=RynxMaterialMode.Amoled)RynxAccent.copy(alpha=.18f).compositeOver(base) else base)
    }
    val border=when(appearance.material) {
        RynxMaterialMode.LiquidGlass -> Color.White.copy(alpha=appearance.borderBrightness*.30f*strength)
        RynxMaterialMode.Solid -> if(dark)Color.White.copy(alpha=.08f) else Color.Black.copy(alpha=.035f)
        RynxMaterialMode.Amoled -> Color.White.copy(alpha=if(selected).22f else .08f)
    }
    return result.border(.7.dp,border,shape)
}

@Composable internal fun RynxSurface(modifier:Modifier=Modifier,radius:Dp=20.dp,selected:Boolean=false,amount:Float=1f,
    shadow:Boolean=false,content:@Composable BoxScope.()->Unit) {
    val depth=LocalRynxMaterialDepth.current
    Box(modifier.rynxMaterial(radius,selected,amount=amount,shadow=shadow)) {
        CompositionLocalProvider(LocalRynxMaterialDepth provides depth+1) {content()}
    }
}

@Composable internal fun Modifier.rynxPressFeedback(interaction:MutableInteractionSource,enabled:Boolean=true):Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val reduced=LocalRynxReducedMotion.current
    val intensity=LocalRynxAppearance.current.motionIntensity
    val target=if(pressed && enabled && !reduced)1f-(1f-RynxMotionTokens.PressScale)*intensity else 1f
    val scale by animateFloatAsState(target,if(reduced)snap() else spring(stiffness=RynxMotionTokens.PressStiffness,dampingRatio=RynxMotionTokens.PressDamping))
    return hoverable(interaction,enabled).graphicsLayer {
        scaleX=if(reduced)1f else scale;scaleY=scaleX
        alpha=if(enabled)1f else .45f
    }.then(if(focused || hovered) Modifier.border(1.dp,RynxAccent.copy(alpha=if(focused).65f else .18f),RoundedCornerShape(20.dp)) else Modifier)
}

@Composable internal fun RynxButton(label:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,selected:Boolean=false) {
    val interaction=remember {MutableInteractionSource()}
    val dark=LocalRynxDarkSurface.current || LocalRynxAppearance.current.darkTheme || LocalRynxAppearance.current.material==RynxMaterialMode.Amoled
    Box(modifier.heightIn(min=44.dp).rynxPressFeedback(interaction,enabled)
        .rynxMaterial(18.dp,selected,interaction)
        .clickable(interactionSource=interaction,indication=null,enabled=enabled,role=Role.Button,onClick=onClick)
        .semantics {contentDescription=label;if(!enabled)disabled()}.padding(horizontal=16.dp,vertical=11.dp),contentAlignment=Alignment.Center) {
        BasicText(label,style=TextStyle(color=if(dark)Color(0xFFF6F6FA) else Color(0xFF26262B),fontSize=14.sp,fontWeight=FontWeight.SemiBold))
    }
}
