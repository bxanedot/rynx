package dev.bxane.rynx.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

/** Original geometric vectors. No Apple SF Symbols artwork is bundled. */
enum class Glyph { Play, Pause, Next, Previous, Search, Add, Library, Queue, Close, Note, Sun, Moon, Repeat }

@Composable
fun RynxGlyph(glyph: Glyph, tint: Color, modifier: Modifier = Modifier) {
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
                Glyph.Search -> { drawCircle(tint,7f,Offset(10f,10f),style=Stroke(1.8f));line(15f,15f,21f,21f) }
                Glyph.Add -> { line(12f,4f,12f,20f);line(4f,12f,20f,12f) }
                Glyph.Close -> { line(6f,6f,18f,18f);line(18f,6f,6f,18f) }
                Glyph.Queue -> { for(y in listOf(6f,12f,18f)) { drawCircle(tint,1.2f,Offset(3f,y));line(8f,y,21f,y) } }
                Glyph.Library -> { for(x in listOf(4f,10f,17f)) line(x,4f,x,20f);line(17f,4f,21f,19f) }
                Glyph.Note -> { line(10f,17f,10f,5f);line(10f,5f,20f,3f);line(20f,3f,20f,15f);drawOval(tint,Offset(3f,16f),androidx.compose.ui.geometry.Size(7f,5f));drawOval(tint,Offset(13f,14f),androidx.compose.ui.geometry.Size(7f,5f)) }
                Glyph.Sun -> { drawCircle(tint,4f,Offset(12f,12f),style=Stroke(1.8f));for(i in 0..7) { val r=i*kotlin.math.PI.toFloat()/4;line(12+kotlin.math.cos(r)*8,12+kotlin.math.sin(r)*8,12+kotlin.math.cos(r)*10,12+kotlin.math.sin(r)*10) } }
                Glyph.Moon -> drawPath(Path().apply{moveTo(17f,3f);cubicTo(1f,0f,0f,22f,17f,21f);cubicTo(8f,16f,8f,8f,17f,3f);close()},tint)
                Glyph.Repeat -> {line(5f,7f,19f,7f);line(19f,7f,16f,4f);line(19f,7f,16f,10f);line(19f,17f,5f,17f);line(5f,17f,8f,14f);line(5f,17f,8f,20f);line(3f,8f,3f,13f);line(21f,11f,21f,16f)}
            }
        }
    }
}
