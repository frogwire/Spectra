package app.frogwire.spectra.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import app.frogwire.spectra.ui.theme.SpectraCyan
import app.frogwire.spectra.ui.theme.SpectraViolet

private const val MAX_TINES = 5

/**
 * The Spectra crystal elk: a low-poly, front-view head in flat violet and cyan facets,
 * with antlers made of growing crystal tines.
 *
 * [crystals] (0..5) is how many tines sprout on each antler. The privacy panel passes the
 * number of protections that are on, so a new crystal grows each time one switches on.
 */
@Composable
fun CrystalElk(
    modifier: Modifier = Modifier,
    crystals: Int = MAX_TINES
) {
    val growth = List(MAX_TINES) { i ->
        animateFloatAsState(
            targetValue = if (i < crystals) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "tine$i"
        ).value
    }

    Canvas(modifier = modifier) {
        val s = size.minDimension / 100f
        val ox = (size.width - 100f * s) / 2f
        val oy = (size.height - 100f * s) / 2f
        fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
        fun c(t: Float, alpha: Float = 1f) = lerp(SpectraViolet, SpectraCyan, t).copy(alpha = alpha)

        // Both antlers: x is mirrored for the right side
        for (side in listOf(-1f, 1f)) {
            fun x(v: Float) = if (side < 0) v else 100f - v

            // Main beam: a tall, tapering crystal shard
            tri(p(x(38f), 47f), p(x(44f), 46f), p(x(24f), 5f), c(if (side < 0) 0.15f else 0.85f))
            tri(p(x(41f), 46.5f), p(x(44f), 46f), p(x(24f), 5f), c(if (side < 0) 0.45f else 0.55f, 0.9f))

            // Tines along the beam, alternating outward and upward-inward
            for (i in 0 until MAX_TINES) {
                val g = growth[i]
                if (g <= 0.01f) continue
                val t = 0.2f + i * 0.16f
                val bx = 41f + (24f - 41f) * t
                val by = 46.5f + (5f - 46.5f) * t
                val outward = i % 2 == 0
                val dx = (if (outward) -11f else 7f) * g
                val dy = (if (outward) -6f else -9f) * g
                tri(
                    p(x(bx - 1.6f), by + 2.4f),
                    p(x(bx + 1.6f), by - 2.4f),
                    p(x(bx + dx), by + dy),
                    c(i / (MAX_TINES - 1f), 0.95f)
                )
            }

            // Ear
            tri(p(x(35f), 51f), p(x(15f), 45f), p(x(32f), 59f), c(if (side < 0) 0.2f else 0.8f, 0.85f))
            tri(p(x(34f), 52f), p(x(21f), 47f), p(x(32f), 57f), c(0.5f, 0.6f))
        }

        // Face facets
        tri(p(38f, 46f), p(62f, 46f), p(50f, 51f), c(0.5f))            // brow
        tri(p(38f, 46f), p(50f, 51f), p(32f, 58f), c(0.1f))            // left forehead
        tri(p(62f, 46f), p(50f, 51f), p(68f, 58f), c(0.9f))            // right forehead
        tri(p(32f, 58f), p(50f, 51f), p(44f, 74f), c(0.25f))           // left cheek
        tri(p(68f, 58f), p(50f, 51f), p(56f, 74f), c(0.75f))           // right cheek
        tri(p(50f, 51f), p(44f, 74f), p(56f, 74f), c(0.55f))           // bridge
        tri(p(32f, 58f), p(44f, 74f), p(43f, 87f), c(0.05f, 0.9f))     // left jaw
        tri(p(68f, 58f), p(56f, 74f), p(57f, 87f), c(0.95f, 0.9f))     // right jaw
        tri(p(44f, 74f), p(56f, 74f), p(43f, 87f), c(0.4f))            // muzzle
        tri(p(56f, 74f), p(57f, 87f), p(43f, 87f), c(0.65f))
        tri(p(43f, 87f), p(57f, 87f), p(50f, 94f), Color(0xFF2A2738))  // nose

        // Eyes: small dark facets
        tri(p(40f, 58f), p(45f, 60f), p(41f, 62f), Color(0xFF0F0E15))
        tri(p(60f, 58f), p(55f, 60f), p(59f, 62f), Color(0xFF0F0E15))
    }
}

private fun DrawScope.tri(a: Offset, b: Offset, c: Offset, color: Color) {
    val path = Path().apply {
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        close()
    }
    drawPath(path, color)
}
