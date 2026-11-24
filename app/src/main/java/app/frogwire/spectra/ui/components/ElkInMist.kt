package app.frogwire.spectra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.random.Random

private val Fog = Color(0xFF3A3658)
private val Pine = Color(0xFF1C192C)
private val Page = Color(0xFF0F0E15)

/**
 * Home-screen scene: the full crystal elk standing far off in a misty pine forest.
 * Its legs dissolve into the ground fog, and thin wisps drift slowly across it.
 * [glow] tints the light behind it (violet normally, deeper violet in Incognito+).
 */
@Composable
fun ElkInMist(modifier: Modifier = Modifier, glow: Color = Color(0xFF6D4AFF)) {
    val drift by rememberInfiniteTransition(label = "fog").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    val glowPulse by rememberInfiniteTransition(label = "crystal").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_200), RepeatMode.Reverse),
        label = "pulse"
    )
    val pines = remember { pineRows() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Light behind the elk
        drawCircle(
            brush = Brush.radialGradient(
                listOf(glow.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(w / 2, h * 0.5f),
                radius = maxOf(w, h) * 0.5f
            ),
            radius = maxOf(w, h) * 0.5f,
            center = Offset(w / 2, h * 0.5f)
        )

        // Distant pines, two rows, softer further back
        pines.forEach { (row, trees) ->
            val color = Pine.copy(alpha = if (row == 0) 0.28f else 0.45f)
            trees.forEach { (x, baseY, height) ->
                val tw = height * h * 0.32f
                val path = Path().apply {
                    moveTo(x * w, baseY * h)
                    lineTo(x * w + tw / 2, (baseY - height) * h)
                    lineTo(x * w + tw, baseY * h)
                    close()
                }
                drawPath(path, color)
            }
        }
        fogBand(centerX = 0.5f, centerY = 0.67f, width = 1.8f, height = 0.09f, alpha = 0.45f)

        drawElk(w, h, glowPulse)

        // Wisps drifting through the body, then the ground fog over the legs
        fogBand(0.30f + drift * 0.05f, 0.55f, 1.0f, 0.04f, 0.32f)
        fogBand(0.78f - drift * 0.05f, 0.605f, 1.0f, 0.035f, 0.28f)
        fogBand(0.5f + drift * 0.02f, 0.70f, 1.9f, 0.08f, 0.55f)
        fogBand(0.5f, 0.76f, 1.9f, 0.13f, 0.8f)
        fogBand(0.5f, 0.85f, 2.0f, 0.2f, 0.9f)

        // Fade into the page at the bottom
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Page), startY = h * 0.84f, endY = h),
            topLeft = Offset(0f, h * 0.84f)
        )
    }
}

/** Soft horizontal mist: a radial gradient squashed into a wide ellipse. */
private fun DrawScope.fogBand(centerX: Float, centerY: Float, width: Float, height: Float, alpha: Float) {
    val c = Offset(size.width * centerX, size.height * centerY)
    val r = size.width * width / 2
    scale(scaleX = 1f, scaleY = (size.height * height) / (size.width * width), pivot = c) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to Fog.copy(alpha = alpha),
                0.6f to Fog.copy(alpha = alpha * 0.5f),
                1f to Color.Transparent,
                center = c,
                radius = r
            ),
            radius = r,
            center = c
        )
    }
}

/** The elk, slightly hazed, with its lower legs fading out into the mist. */
private fun DrawScope.drawElk(w: Float, h: Float, glow: Float) {
    val s = w * 0.0026f
    val ox = w / 2 - 100f * s
    val oy = h * 0.74f - 162f * s
    val bounds = Rect(0f, 0f, w, h)

    drawIntoCanvas { it.saveLayer(bounds, Paint()) }
    val path = Path()
    for (i in ELK_COLORS.indices) {
        val k = i * 6
        path.reset()
        path.moveTo(ox + ELK_TRIANGLES[k] * s, oy + ELK_TRIANGLES[k + 1] * s)
        path.lineTo(ox + ELK_TRIANGLES[k + 2] * s, oy + ELK_TRIANGLES[k + 3] * s)
        path.lineTo(ox + ELK_TRIANGLES[k + 4] * s, oy + ELK_TRIANGLES[k + 5] * s)
        path.close()
        drawPath(path, Color(ELK_COLORS[i]))
    }
    drawCrystalLight(ox, oy, s, glow)
    // Keep 80% of the elk above the lower legs, dissolving to nothing at the hooves
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.8f),
            1f to Color.Transparent,
            startY = oy + 136f * s,
            endY = oy + 162f * s
        ),
        size = bounds.size,
        blendMode = BlendMode.DstIn
    )
    drawIntoCanvas { it.restore() }
}

private val LightHalo = Color(0xFF6FD6FF)
private val LightCore = Color(0xFFB8F2FF)

/**
 * Light inside the crystal: soft cyan along a few facet edges (brightest mid-seam) and
 * small four-point sparkles at the antler tips. [glow] (0..1) breathes slowly.
 */
private fun DrawScope.drawCrystalLight(ox: Float, oy: Float, s: Float, glow: Float) {
    for (i in 0 until ELK_SEAMS.size / 4) {
        val a = Offset(ox + ELK_SEAMS[i * 4] * s, oy + ELK_SEAMS[i * 4 + 1] * s)
        val b = Offset(ox + ELK_SEAMS[i * 4 + 2] * s, oy + ELK_SEAMS[i * 4 + 3] * s)
        // Halo: a few widening strokes at low alpha (works without blur support)
        for ((width, alpha) in listOf(6f to 0.10f, 3.5f to 0.16f, 2f to 0.24f)) {
            drawLine(
                brush = Brush.linearGradient(listOf(Color.Transparent, LightHalo.copy(alpha = alpha * glow), Color.Transparent), a, b),
                start = a, end = b, strokeWidth = s * width
            )
        }
        drawLine(
            brush = Brush.linearGradient(listOf(Color.Transparent, LightCore.copy(alpha = 0.7f * glow), Color.Transparent), a, b),
            start = a, end = b, strokeWidth = s
        )
    }
    val star = Path()
    for (i in 0 until ELK_GLINTS.size / 2) {
        val c = Offset(ox + ELK_GLINTS[i * 2] * s, oy + ELK_GLINTS[i * 2 + 1] * s)
        val haloRadius = s * 6f
        drawCircle(
            brush = Brush.radialGradient(listOf(LightHalo.copy(alpha = 0.45f * glow), Color.Transparent), c, haloRadius),
            radius = haloRadius,
            center = c
        )
        val l = s * 3.2f * (0.85f + 0.15f * glow)
        val q = s * 0.55f
        star.reset()
        star.moveTo(c.x, c.y - l); star.lineTo(c.x + q, c.y - q); star.lineTo(c.x + l, c.y)
        star.lineTo(c.x + q, c.y + q); star.lineTo(c.x, c.y + l); star.lineTo(c.x - q, c.y + q)
        star.lineTo(c.x - l, c.y); star.lineTo(c.x - q, c.y - q); star.close()
        drawPath(star, LightCore.copy(alpha = 0.6f + 0.35f * glow))
    }
}

/** Fixed pine layout (same every time): row index to (x, baseY, height) as fractions. */
private fun pineRows(): List<Pair<Int, List<Triple<Float, Float, Float>>>> {
    val random = Random(7)
    return listOf(0.66f to (0.16f to 0.30f), 0.72f to (0.10f to 0.20f)).mapIndexed { row, (base, range) ->
        val trees = ArrayList<Triple<Float, Float, Float>>()
        var x = -0.02f
        while (x < 1.02f) {
            val height = range.first + random.nextFloat() * (range.second - range.first)
            trees += Triple(x, base + (random.nextFloat() - 0.5f) * 0.01f, height)
            // trees overlap: step by part of their width (width = 0.32 * height, relative to scene height)
            x += height * 0.32f * (0.45f + random.nextFloat() * 0.35f) * 1.0f
        }
        row to trees
    }
}

// Generated from the elk design (side view, mid-stride, facing left; units: hooves at y = 162); back to front.
internal val ELK_TRIANGLES = floatArrayOf(
    82.51f, 90.31f, 93.49f, 89.69f, 92.5f, 126f,
    82.51f, 90.31f, 92.5f, 126f, 87.5f, 126f,
    87.5f, 126f, 92.5f, 126f, 89.75f, 162.1f,
    87.5f, 126f, 89.75f, 162.1f, 86.25f, 161.9f,
    123.1f, 89.15f, 136.9f, 86.85f, 138.5f, 123.93f,
    123.1f, 89.15f, 138.5f, 123.93f, 133.5f, 124.07f,
    133.5f, 124.07f, 138.5f, 123.93f, 133.74f, 162.18f,
    133.5f, 124.07f, 133.74f, 162.18f, 130.26f, 161.82f,
    69.1f, 28.24f, 62.9f, 23.76f, 76.51f, 6.08f,
    69.1f, 28.24f, 76.51f, 6.08f, 81.49f, 9.92f,
    81.49f, 9.92f, 76.51f, 6.08f, 91.13f, -10.4f,
    81.49f, 9.92f, 91.13f, -10.4f, 94.87f, -7.6f,
    94.87f, -7.6f, 91.13f, -10.4f, 105.11f, -28.61f,
    94.87f, -7.6f, 105.11f, -28.61f, 106.89f, -27.39f,
    70.49f, 17.32f, 67.51f, 20.68f, 59.2f, 12.09f,
    70.49f, 17.32f, 59.2f, 12.09f, 60.8f, 9.91f,
    60.8f, 9.91f, 59.2f, 12.09f, 53.8f, 8.4f,
    60.8f, 9.91f, 53.8f, 8.4f, 54.2f, 7.6f,
    81.9f, 3.79f, 78.1f, 6.21f, 71.81f, -5.37f,
    81.9f, 3.79f, 71.81f, -5.37f, 74.19f, -6.63f,
    74.19f, -6.63f, 71.81f, -5.37f, 69.58f, -13.84f,
    74.19f, -6.63f, 69.58f, -13.84f, 70.42f, -14.16f,
    91.15f, -4.66f, 86.85f, -3.34f, 83.68f, -16.7f,
    91.15f, -4.66f, 83.68f, -16.7f, 86.32f, -17.3f,
    86.32f, -17.3f, 83.68f, -16.7f, 83.55f, -25.95f,
    86.32f, -17.3f, 83.55f, -25.95f, 84.45f, -26.05f,
    103.16f, -20.07f, 100.84f, -23.93f, 111.15f, -29.05f,
    103.16f, -20.07f, 111.15f, -29.05f, 112.85f, -26.95f,
    112.85f, -26.95f, 111.15f, -29.05f, 117.66f, -35.29f,
    112.85f, -26.95f, 117.66f, -35.29f, 118.34f, -34.71f,
    76f, 36f, 92f, 56f, 72f, 60f,
    72f, 60f, 92f, 56f, 92f, 80f,
    72f, 60f, 92f, 80f, 66f, 92f,
    58f, 54f, 72f, 60f, 66f, 92f,
    58f, 54f, 66f, 92f, 55f, 66f,
    92f, 80f, 80f, 98f, 66f, 92f,
    92f, 56f, 126f, 60f, 92f, 80f,
    92f, 80f, 126f, 60f, 110f, 100f,
    92f, 80f, 110f, 100f, 80f, 98f,
    126f, 60f, 138f, 76f, 110f, 100f,
    126f, 60f, 150f, 62f, 138f, 76f,
    150f, 62f, 152f, 86f, 138f, 76f,
    138f, 76f, 152f, 86f, 140f, 95f,
    138f, 76f, 140f, 95f, 110f, 100f,
    146f, 59f, 154f, 67f, 160f, 47f,
    146f, 59f, 160f, 47f, 156f, 46f,
    151f, 63f, 155f, 61f, 160f, 50f,
    64f, 27f, 76f, 36f, 60f, 40f,
    60f, 40f, 76f, 36f, 72f, 60f,
    60f, 40f, 72f, 60f, 58f, 54f,
    49f, 43f, 60f, 40f, 58f, 54f,
    49f, 43f, 58f, 54f, 39f, 49f,
    64f, 27f, 56f, 27f, 60f, 40f,
    56f, 27f, 49f, 43f, 60f, 40f,
    56f, 27f, 46f, 35f, 49f, 43f,
    46f, 35f, 39f, 49f, 49f, 43f,
    46f, 35f, 35f, 45f, 39f, 49f,
    64f, 27f, 76f, 19f, 67f, 32f,
    35f, 45f, 38.5f, 42f, 37f, 47.5f,
    54f, 31f, 58f, 32f, 55f, 34f,
    65.54f, 85.32f, 78.46f, 86.68f, 70.47f, 124.39f,
    65.54f, 85.32f, 70.47f, 124.39f, 65.53f, 123.61f,
    65.53f, 123.61f, 70.47f, 124.39f, 61.86f, 162.39f,
    65.53f, 123.61f, 61.86f, 162.39f, 58.14f, 161.61f,
    133.65f, 83.59f, 150.35f, 80.41f, 152.75f, 123.93f,
    133.65f, 83.59f, 152.75f, 123.93f, 147.25f, 124.07f,
    147.25f, 124.07f, 152.75f, 123.93f, 145.88f, 162.3f,
    147.25f, 124.07f, 145.88f, 162.3f, 142.12f, 161.7f,
    65.17f, 26.83f, 56.83f, 25.17f, 61.6f, 5.15f,
    65.17f, 26.83f, 61.6f, 5.15f, 68.4f, 6.85f,
    68.4f, 6.85f, 61.6f, 5.15f, 68.49f, -14.67f,
    68.4f, 6.85f, 68.49f, -14.67f, 73.51f, -13.33f,
    73.51f, -13.33f, 68.49f, -14.67f, 74.83f, -35.28f,
    73.51f, -13.33f, 74.83f, -35.28f, 77.17f, -34.72f,
    63.47f, 16.98f, 60.53f, 21.02f, 50.22f, 12.28f,
    63.47f, 16.98f, 50.22f, 12.28f, 51.78f, 9.72f,
    51.78f, 9.72f, 50.22f, 12.28f, 43.82f, 8.41f,
    51.78f, 9.72f, 43.82f, 8.41f, 44.18f, 7.59f,
    66.77f, 1.23f, 63.23f, 4.77f, 53.88f, -6.01f,
    66.77f, 1.23f, 53.88f, -6.01f, 56.12f, -7.99f,
    56.12f, -7.99f, 53.88f, -6.01f, 49.63f, -13.74f,
    56.12f, -7.99f, 49.63f, -13.74f, 50.37f, -14.26f,
    71.16f, -9.26f, 66.84f, -6.74f, 60.65f, -19.36f,
    71.16f, -9.26f, 60.65f, -19.36f, 63.35f, -20.64f,
    63.35f, -20.64f, 60.65f, -19.36f, 58.57f, -28.86f,
    63.35f, -20.64f, 58.57f, -28.86f, 59.43f, -29.14f,
    75.77f, -25.23f, 72.23f, -28.77f, 81.87f, -36.99f,
    75.77f, -25.23f, 81.87f, -36.99f, 84.13f, -35.01f,
    84.13f, -35.01f, 81.87f, -36.99f, 87.63f, -43.26f,
    84.13f, -35.01f, 87.63f, -43.26f, 88.37f, -42.74f,
)

internal val ELK_COLORS = intArrayOf(
    0xFF3B3E84.toInt(), 0xFF3B3E84.toInt(), 0xFF373978.toInt(), 0xFF373978.toInt(), 0xFF375980.toInt(), 0xFF375980.toInt(),
    0xFF335174.toInt(), 0xFF335174.toInt(), 0xFF495B95.toInt(), 0xFF495B95.toInt(), 0xFF445D94.toInt(), 0xFF445D94.toInt(),
    0xFF496893.toInt(), 0xFF496893.toInt(), 0xFF424697.toInt(), 0xFF424697.toInt(), 0xFF424697.toInt(), 0xFF424697.toInt(),
    0xFF4E5996.toInt(), 0xFF4E5996.toInt(), 0xFF4E5996.toInt(), 0xFF4E5996.toInt(), 0xFF405694.toInt(), 0xFF405694.toInt(),
    0xFF405694.toInt(), 0xFF405694.toInt(), 0xFF4C6794.toInt(), 0xFF4C6794.toInt(), 0xFF4C6794.toInt(), 0xFF4C6794.toInt(),
    0xFF7189F7.toInt(), 0xFF686DF9.toInt(), 0xFF6A5CFC.toInt(), 0xFF6B56FD.toInt(), 0xFF6C50FE.toInt(), 0xFF6962FB.toInt(),
    0xFF7699F5.toInt(), 0xFF657FF6.toInt(), 0xFF686DF9.toInt(), 0xFF6291F3.toInt(), 0xFF72B3F1.toInt(), 0xFF609DF1.toInt(),
    0xFF6291F3.toInt(), 0xFF6485F5.toInt(), 0xFF6EB6F0.toInt(), 0xFF7FBAF2.toInt(), 0xF2D8ECFA.toInt(), 0xFF748CF7.toInt(),
    0xFF686DF9.toInt(), 0xFF6962FB.toInt(), 0xFF6A5BFC.toInt(), 0xFF6C53FD.toInt(), 0xFF778EF7.toInt(), 0xFF686BF9.toInt(),
    0xFF756EFB.toInt(), 0xFF6B56FD.toInt(), 0xFF735AFE.toInt(), 0xE6609DF1.toInt(), 0xFF2A2738.toInt(), 0xFF0F0E15.toInt(),
    0xFF6A5CFC.toInt(), 0xFF6A5CFC.toInt(), 0xFF6B56FD.toInt(), 0xFF6B56FD.toInt(), 0xFF68A2F2.toInt(), 0xFF68A2F2.toInt(),
    0xFF6291F3.toInt(), 0xFF6291F3.toInt(), 0xFF7491F6.toInt(), 0xFF7491F6.toInt(), 0xFF6A96F4.toInt(), 0xFF6A96F4.toInt(),
    0xFF73A8F3.toInt(), 0xFF73A8F3.toInt(), 0xFF686DF9.toInt(), 0xFF686DF9.toInt(), 0xFF686DF9.toInt(), 0xFF686DF9.toInt(),
    0xFF7C8FF8.toInt(), 0xFF7C8FF8.toInt(), 0xFF7C8FF8.toInt(), 0xFF7C8FF8.toInt(), 0xFF638AF4.toInt(), 0xFF638AF4.toInt(),
    0xFF638AF4.toInt(), 0xFF638AF4.toInt(), 0xFF79A7F4.toInt(), 0xFF79A7F4.toInt(), 0xFF79A7F4.toInt(), 0xFF79A7F4.toInt(),
)

/** Facet edges that glow: x1, y1, x2, y2 per seam. */
internal val ELK_SEAMS = floatArrayOf(
    92f, 56f, 92f, 80f, 92f, 80f, 110f, 100f, 126f, 60f, 138f, 76f, 138f, 76f, 140f, 95f, 56f, 27f, 49f, 43f, 76f, 36f, 72f, 60f
)

/** Antler tips that sparkle: x, y per glint. */
internal val ELK_GLINTS = floatArrayOf(
    76f, -35f, 88f, -43f, 59f, -29f, 50f, -14f, 44f, 8f, 106f, -28f, 118f, -35f, 84f, -26f
)
