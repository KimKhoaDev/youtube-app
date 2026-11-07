package com.youtube.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class Ic { Home, Shorts, Subs, You, Search, Bell, Cast, Like, Share, Download, Save, Play, Pause, Back, More, Clock, Settings, Full, Close, Check }

@Composable
fun Icon(ic: Ic, color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / 24f
        val st = Stroke(2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        fun path(block: Path.() -> Unit) = Path().apply { block() }
        fun DrawScope.line(a: Offset, b: Offset) = drawLine(color, a, b, 2f * u, StrokeCap.Round)
        when (ic) {
            Ic.Home -> drawPath(path { moveTo(4 * u, 11 * u); lineTo(12 * u, 4 * u); lineTo(20 * u, 11 * u); lineTo(20 * u, 20 * u); lineTo(14.5f * u, 20 * u); lineTo(14.5f * u, 14 * u); lineTo(9.5f * u, 14 * u); lineTo(9.5f * u, 20 * u); lineTo(4 * u, 20 * u); close() }, color, style = st)
            Ic.Shorts -> {
                drawRoundRect(color, p(6f, 3f), Size(12 * u, 18 * u), CornerRadius(5 * u), style = st)
                drawPath(path { moveTo(10.5f * u, 9 * u); lineTo(10.5f * u, 15 * u); lineTo(15 * u, 12 * u); close() }, color)
            }
            Ic.Subs -> {
                line(p(6f, 4f), p(18f, 4f)); line(p(4f, 8f), p(20f, 8f))
                drawRoundRect(color, p(3f, 11f), Size(18 * u, 10 * u), CornerRadius(3 * u), style = st)
                drawPath(path { moveTo(10.5f * u, 13.5f * u); lineTo(10.5f * u, 18.5f * u); lineTo(15 * u, 16 * u); close() }, color)
            }
            Ic.You -> {
                drawCircle(color, 4.5f * u, p(12f, 8f), style = st)
                drawPath(path { moveTo(4 * u, 21 * u); quadraticTo(12 * u, 9 * u, 20 * u, 21 * u) }, color, style = st)
            }
            Ic.Search -> { drawCircle(color, 6.5f * u, p(10.5f, 10.5f), style = st); line(p(15.5f, 15.5f), p(20.5f, 20.5f)) }
            Ic.Bell -> {
                drawPath(path { moveTo(5 * u, 17 * u); quadraticTo(7 * u, 15 * u, 7 * u, 10 * u); cubicTo(7 * u, 4 * u, 17 * u, 4 * u, 17 * u, 10 * u); quadraticTo(17 * u, 15 * u, 19 * u, 17 * u); close() }, color, style = st)
                line(p(10f, 20.5f), p(14f, 20.5f))
            }
            Ic.Cast -> {
                drawPath(path { moveTo(3 * u, 8 * u); lineTo(3 * u, 6 * u); quadraticTo(3 * u, 5 * u, 4 * u, 5 * u); lineTo(20 * u, 5 * u); quadraticTo(21 * u, 5 * u, 21 * u, 6 * u); lineTo(21 * u, 18 * u); quadraticTo(21 * u, 19 * u, 20 * u, 19 * u); lineTo(14 * u, 19 * u) }, color, style = st)
                drawPath(path { moveTo(3 * u, 12 * u); quadraticTo(10 * u, 12 * u, 10 * u, 19 * u) }, color, style = st)
                drawPath(path { moveTo(3 * u, 16 * u); quadraticTo(6 * u, 16 * u, 6 * u, 19 * u) }, color, style = st)
            }
            Ic.Like -> {
                drawRoundRect(color, p(3f, 10f), Size(4 * u, 10 * u), CornerRadius(1 * u), style = st)
                drawPath(path { moveTo(7 * u, 10 * u); lineTo(11 * u, 3.5f * u); quadraticTo(14 * u, 3.5f * u, 13.5f * u, 8 * u); lineTo(19 * u, 8 * u); quadraticTo(21 * u, 8.5f * u, 20.5f * u, 11 * u); lineTo(19 * u, 18 * u); quadraticTo(18.5f * u, 20 * u, 16.5f * u, 20 * u); lineTo(7 * u, 20 * u) }, color, style = st)
            }
            Ic.Share -> drawPath(path { moveTo(14 * u, 4 * u); lineTo(21 * u, 11 * u); lineTo(14 * u, 18 * u); lineTo(14 * u, 14 * u); quadraticTo(6 * u, 14 * u, 3 * u, 20 * u); quadraticTo(4 * u, 10 * u, 14 * u, 8 * u); close() }, color, style = st)
            Ic.Download -> { line(p(12f, 4f), p(12f, 15f)); line(p(7f, 11f), p(12f, 16f)); line(p(17f, 11f), p(12f, 16f)); line(p(5f, 20f), p(19f, 20f)) }
            Ic.Save -> { line(p(4f, 7f), p(15f, 7f)); line(p(4f, 12f), p(15f, 12f)); line(p(4f, 17f), p(10f, 17f)); line(p(18f, 13f), p(18f, 21f)); line(p(14f, 17f), p(22f, 17f)) }
            Ic.Play -> drawPath(path { moveTo(8 * u, 5 * u); lineTo(8 * u, 19 * u); lineTo(19.5f * u, 12 * u); close() }, color)
            Ic.Pause -> { drawRoundRect(color, p(6f, 5f), Size(4 * u, 14 * u), CornerRadius(1.5f * u)); drawRoundRect(color, p(14f, 5f), Size(4 * u, 14 * u), CornerRadius(1.5f * u)) }
            Ic.Back -> { line(p(15f, 4f), p(7f, 12f)); line(p(7f, 12f), p(15f, 20f)) }
            Ic.Clock -> { drawCircle(color, 8.5f * u, p(12f, 12f), style = st); line(p(12f, 7f), p(12f, 12f)); line(p(12f, 12f), p(16f, 14f)) }
            Ic.Settings -> {
                drawCircle(color, 3.2f * u, p(12f, 12f), style = st)
                for (k in 0 until 8) { val a = k * Math.PI / 4; line(p(12f + 6.5f * Math.cos(a).toFloat(), 12f + 6.5f * Math.sin(a).toFloat()), p(12f + 9f * Math.cos(a).toFloat(), 12f + 9f * Math.sin(a).toFloat())) }
                drawCircle(color, 6.5f * u, p(12f, 12f), style = st)
            }
            Ic.Full -> { line(p(4f, 9f), p(4f, 4f)); line(p(4f, 4f), p(9f, 4f)); line(p(15f, 4f), p(20f, 4f)); line(p(20f, 4f), p(20f, 9f)); line(p(4f, 15f), p(4f, 20f)); line(p(4f, 20f), p(9f, 20f)); line(p(15f, 20f), p(20f, 20f)); line(p(20f, 20f), p(20f, 15f)) }
            Ic.Close -> { line(p(6f, 6f), p(18f, 18f)); line(p(18f, 6f), p(6f, 18f)) }
            Ic.Check -> { line(p(5f, 12.5f), p(10f, 17.5f)); line(p(10f, 17.5f), p(19f, 7f)) }
            Ic.More -> listOf(6f, 12f, 18f).forEach { drawCircle(color, 1.8f * u, p(12f, it)) }
        }
    }
}
