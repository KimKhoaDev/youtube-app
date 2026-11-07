package com.youtube.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import com.youtube.app.liquidglass.LiquidButton

object YT {
    val Red = Color(0xFFFF0033)
    @Composable fun text() = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F0F0F)
    @Composable fun sub() = if (isSystemInDarkTheme()) Color(0xFFAAAAAA) else Color(0xFF606060)
    @Composable fun bg() = if (isSystemInDarkTheme()) Color(0xFF0F0F0F) else Color.White
}

@Composable
fun Txt(text: String, color: Color, size: Int, bold: Boolean = false, maxLines: Int = Int.MAX_VALUE, modifier: Modifier = Modifier) =
    BasicText(
        text, modifier,
        style = TextStyle(color, size.sp, if (bold) FontWeight.SemiBold else FontWeight.Normal, lineHeight = (size * 1.3f).sp),
        maxLines = maxLines, overflow = TextOverflow.Ellipsis
    )

/** Round-ish icon button made from Kyant0's LiquidButton. */
@Composable
fun IconBtn(ic: Ic, backdrop: Backdrop, modifier: Modifier = Modifier, width: Dp = 52.dp, iconSize: Dp = 20.dp, tint: Color = Color.Unspecified, onClick: () -> Unit = {}) {
    LiquidButton(onClick, backdrop, modifier.width(width), tint = tint) { Icon(ic, YT.text(), iconSize) }
}

@Composable
fun Chip(label: String, selected: Boolean, backdrop: Backdrop, onClick: () -> Unit) {
    LiquidButton(onClick, backdrop, tint = if (selected) YT.Red else Color.Unspecified) {
        Txt(label, if (selected) Color.White else YT.text(), 14, bold = true, maxLines = 1)
    }
}

@Composable
fun Pill(backdrop: Backdrop, tint: Color = Color.Unspecified, onClick: () -> Unit = {}, content: @Composable RowScope.() -> Unit) =
    LiquidButton(onClick, backdrop, tint = tint, content = content)

/** Large glass surface (sheets/panels) using the same backdrop effects as Kyant0's components. */
@Composable
fun Modifier.panel(backdrop: Backdrop, radius: Dp = 28.dp): Modifier {
    val tint = if (isSystemInDarkTheme()) Color(0xFF1C1C1E).copy(0.6f) else Color.White.copy(0.6f)
    return drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedRectangle(radius) },
        effects = { vibrancy(); blur(10f.dp.toPx()); lens(16f.dp.toPx(), 32f.dp.toPx()) },
        onDrawSurface = { drawRect(tint) }
    )
}

fun clock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}
