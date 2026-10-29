package dev.bxane.rynx.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

/** Semantic glyph registry. Apple glyph names are resolved through GlyphStudio on Apple targets. */
enum class Glyph(val symbolName: String) {
    Play("play.fill"), Pause("pause.fill"), Next("forward.end.fill"), Previous("backward.end.fill"),
    MoveUp("arrow.up"), MoveDown("arrow.down"), Search("magnifyingglass"), Add("plus"), Edit("pencil"),
    Library("books.vertical"), Queue("list.bullet"), Close("xmark"), Note("music.note"),
    Sun("sun.max.fill"), Moon("moon.fill"), Repeat("repeat"), Heart("heart"), HeartFill("heart.fill"),
    Home("house"), Browse("safari"), Radio("dot.radiowaves.left.and.right"), Star("star"), StarFill("star.fill"),
    More("ellipsis"), SpeakerQuiet("speaker"), SpeakerLoud("speaker.wave.2.fill"), Output("airplayaudio"),
    Lyrics("quote.bubble"), Shuffle("shuffle"), Reorder("line.3.horizontal"), Equalizer("slider.horizontal.3"),
    Infinity("infinity"), AutoMix("waveform"), Appearance("gearshape"), ChevronRight("chevron.right"),
}

@Composable
internal expect fun drawGlyphStudioSymbol(glyph: Glyph, tint: Color, modifier: Modifier): Boolean

@Composable
fun RynxGlyph(glyph: Glyph, tint: Color, modifier: Modifier = Modifier) {
    if (drawGlyphStudioSymbol(glyph, tint, modifier)) return
    Canvas(modifier.size(22.dp)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(tint, Offset(x,y), Offset(x2,y2), 1.8f, StrokeCap.Round)
            fun triangle(left: Float, right: Float) = drawPath(Path().apply {
                moveTo(left, 5f); lineTo(right, 12f); lineTo(left, 19f); close()
            }, tint)
            when(glyph) {
                Glyph.Play -> triangle(7f, 19f)
                Glyph.Pause -> { drawRect(tint,Offset(6f,5f),androidx.compose.ui.geometry.Size(4f,14f)); drawRect(tint,Offset(14f,5f),androidx.compose.ui.geometry.Size(4f,14f)) }
                Glyph.Next -> { triangle(3f,12f); triangle(12f,21f) }
                Glyph.Previous -> { triangle(21f,12f); triangle(12f,3f) }
                Glyph.MoveUp -> { line(12f,20f,12f,5f);line(6f,11f,12f,5f);line(18f,11f,12f,5f) }
                Glyph.MoveDown -> { line(12f,4f,12f,19f);line(6f,13f,12f,19f);line(18f,13f,12f,19f) }
                Glyph.Search -> { drawCircle(tint,7f,Offset(10f,10f),style=Stroke(1.8f));line(15f,15f,21f,21f) }
                Glyph.Add -> { line(12f,4f,12f,20f);line(4f,12f,20f,12f) }
                Glyph.Edit -> { drawPath(Path().apply{moveTo(4f,17.5f);lineTo(15.8f,5.7f);lineTo(18.3f,8.2f);lineTo(6.5f,20f);lineTo(3.5f,20.5f);close()},tint,style=Stroke(1.7f,join=androidx.compose.ui.graphics.StrokeJoin.Round));line(14f,7.5f,16.5f,10f);line(3.5f,20.5f,9f,19f) }
                Glyph.Close -> { line(6f,6f,18f,18f);line(18f,6f,6f,18f) }
                Glyph.Queue -> { for(y in listOf(6f,12f,18f)) { drawCircle(tint,1.2f,Offset(3f,y));line(8f,y,21f,y) } }
                Glyph.Library -> { for(x in listOf(4f,10f,17f)) line(x,4f,x,20f);line(17f,4f,21f,19f) }
                Glyph.Note -> { line(10f,17f,10f,5f);line(10f,5f,20f,3f);line(20f,3f,20f,15f);drawOval(tint,Offset(3f,16f),androidx.compose.ui.geometry.Size(7f,5f));drawOval(tint,Offset(13f,14f),androidx.compose.ui.geometry.Size(7f,5f)) }
                Glyph.Sun -> { drawCircle(tint,4f,Offset(12f,12f),style=Stroke(1.8f));for(i in 0..7) { val r=i*kotlin.math.PI.toFloat()/4;line(12+kotlin.math.cos(r)*8,12+kotlin.math.sin(r)*8,12+kotlin.math.cos(r)*10,12+kotlin.math.sin(r)*10) } }
                Glyph.Moon -> drawPath(Path().apply{moveTo(17f,3f);cubicTo(1f,0f,0f,22f,17f,21f);cubicTo(8f,16f,8f,8f,17f,3f);close()},tint)
                Glyph.Repeat -> {line(5f,7f,19f,7f);line(19f,7f,16f,4f);line(19f,7f,16f,10f);line(19f,17f,5f,17f);line(5f,17f,8f,14f);line(5f,17f,8f,20f);line(3f,8f,3f,13f);line(21f,11f,21f,16f)}
                Glyph.Star, Glyph.StarFill -> {
                    val star=Path().apply {
                        for(i in 0..9) {
                            val angle=(-90+i*36)*kotlin.math.PI.toFloat()/180
                            val radius=if(i%2==0) 10f else 4.5f
                            val x=12+kotlin.math.cos(angle)*radius;val y=12+kotlin.math.sin(angle)*radius
                            if(i==0) moveTo(x,y) else lineTo(x,y)
                        };close()
                    }
                    drawPath(star,tint,style=if(glyph==Glyph.Star) Stroke(1.8f,join=androidx.compose.ui.graphics.StrokeJoin.Round) else Fill)
                }
                Glyph.Appearance -> { drawCircle(tint,9f,Offset(12f,12f),style=Stroke(1.6f));drawCircle(tint,4f,Offset(9f,9f));drawCircle(tint,2f,Offset(16f,15f),style=Stroke(1.6f)) }
                Glyph.More -> for(x in listOf(5f,12f,19f)) drawCircle(tint,1.7f,Offset(x,12f))
                Glyph.SpeakerQuiet, Glyph.SpeakerLoud -> {
                    drawPath(Path().apply {moveTo(3f,9f);lineTo(7f,9f);lineTo(12f,5f);lineTo(12f,19f);lineTo(7f,15f);lineTo(3f,15f);close()},tint)
                    if(glyph==Glyph.SpeakerLoud) {
                        drawArc(tint,-50f,100f,false,Offset(11f,7f),androidx.compose.ui.geometry.Size(7f,10f),style=Stroke(1.5f,cap=StrokeCap.Round))
                        drawArc(tint,-48f,96f,false,Offset(9f,3f),androidx.compose.ui.geometry.Size(14f,18f),style=Stroke(1.5f,cap=StrokeCap.Round))
                    }
                }
                Glyph.Output -> {
                    drawPath(Path().apply {moveTo(12f,13f);lineTo(18f,22f);lineTo(6f,22f);close()},tint)
                    for(radius in listOf(4f,7f,10f)) drawArc(tint,135f,270f,false,Offset(12-radius,11-radius),androidx.compose.ui.geometry.Size(radius*2,radius*2),style=Stroke(1.5f,cap=StrokeCap.Round))
                }
                Glyph.Lyrics -> {
                    drawPath(Path().apply {moveTo(5f,3f);lineTo(19f,3f);quadraticTo(22f,3f,22f,6f);lineTo(22f,15f);quadraticTo(22f,18f,19f,18f);lineTo(12f,18f);lineTo(7f,22f);lineTo(7f,18f);lineTo(5f,18f);quadraticTo(2f,18f,2f,15f);lineTo(2f,6f);quadraticTo(2f,3f,5f,3f);close()},tint,style=Stroke(1.8f,join=androidx.compose.ui.graphics.StrokeJoin.Round))
                    line(7f,8f,10f,8f);line(14f,8f,17f,8f);line(10f,8f,9f,12f);line(17f,8f,16f,12f)
                }
                Glyph.Shuffle -> {
                    drawPath(Path().apply {moveTo(3f,6f);lineTo(6f,6f);cubicTo(12f,6f,11f,18f,18f,18f);lineTo(21f,18f);moveTo(3f,18f);lineTo(6f,18f);cubicTo(12f,18f,11f,6f,18f,6f);lineTo(21f,6f)},tint,style=Stroke(1.8f,cap=StrokeCap.Round))
                    line(18f,3f,21f,6f);line(18f,9f,21f,6f);line(18f,15f,21f,18f);line(18f,21f,21f,18f)
                }
                Glyph.Reorder -> for(y in listOf(7f,12f,17f)) line(4f,y,20f,y)
                Glyph.ChevronRight -> { line(9f,5f,16f,12f);line(16f,12f,9f,19f) }
                Glyph.Equalizer -> {line(5f,4f,5f,20f);line(12f,4f,12f,20f);line(19f,4f,19f,20f);drawCircle(tint,2.3f,Offset(5f,9f));drawCircle(tint,2.3f,Offset(12f,15f));drawCircle(tint,2.3f,Offset(19f,7f))}
                Glyph.Infinity -> drawPath(Path().apply {moveTo(12f,12f);cubicTo(7f,2f,1f,6f,2f,12f);cubicTo(3f,19f,7f,21f,12f,12f);cubicTo(17f,3f,22f,5f,22f,12f);cubicTo(22f,19f,17f,22f,12f,12f)},tint,style=Stroke(1.8f,cap=StrokeCap.Round))
                Glyph.AutoMix -> {drawCircle(tint.copy(alpha=.4f),6f,Offset(8f,12f));drawCircle(tint,6f,Offset(16f,12f));drawCircle(Color.Transparent,2f,Offset(16f,12f));drawCircle(tint.copy(alpha=.45f),2f,Offset(16f,12f))}
                Glyph.Heart, Glyph.HeartFill -> {
                    val heart=Path().apply {
                        moveTo(12f,21f);cubicTo(10.1f,19.2f,3f,14.2f,3f,9.4f)
                        cubicTo(3f,5.4f,7.8f,3.3f,12f,7.7f);cubicTo(16.2f,3.3f,21f,5.4f,21f,9.4f)
                        cubicTo(21f,14.2f,13.9f,19.2f,12f,21f);close()
                    }
                    drawPath(heart,tint,style=if(glyph==Glyph.Heart) Stroke(1.8f,join=androidx.compose.ui.graphics.StrokeJoin.Round) else Fill)
                }
                Glyph.Home -> {
                    drawPath(Path().apply{moveTo(3f,10.5f);lineTo(12f,3f);lineTo(21f,10.5f)},tint,style=Stroke(1.8f,cap=StrokeCap.Round,join=androidx.compose.ui.graphics.StrokeJoin.Round))
                    line(5.5f,9f,5.5f,20f);line(18.5f,9f,18.5f,20f);line(5.5f,20f,18.5f,20f)
                    drawRect(tint,Offset(10f,14f),androidx.compose.ui.geometry.Size(4f,6f))
                }
                Glyph.Browse -> {
                    drawCircle(tint,8.5f,Offset(12f,12f),style=Stroke(1.8f))
                    drawPath(Path().apply{moveTo(15.8f,5.4f);lineTo(13.2f,13.2f);lineTo(5.3f,18f);lineTo(8f,10.1f);close()},tint)
                }
                Glyph.Radio -> {
                    drawCircle(tint,8f,Offset(12f,12f),style=Stroke(1.8f))
                    drawCircle(tint,4.6f,Offset(12f,12f),style=Stroke(1.8f))
                    drawCircle(tint,1.5f,Offset(12f,12f))
                    line(9f,4f,19f,4f)
                }
            }
        }
    }
}
