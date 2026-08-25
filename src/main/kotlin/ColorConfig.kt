/**
 * COLOR PALETTE CONFIGURATION
 **/
import org.openrndr.color.ColorRGBa
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

object ColorConfig {
    var mode: ColorMode = ColorMode.NEON

    var singleColor: ColorRGBa = ColorRGBa(0.55, 0.0, 0.0)
    val layerColors = listOf(
        ColorRGBa(0.9, 0.3, 0.4),
        ColorRGBa(0.3, 0.7, 0.9),
        ColorRGBa(0.9, 0.8, 0.3)
    )
    val pastelColors = listOf(
        ColorRGBa(1.0, 0.71, 0.76),
        ColorRGBa(0.68, 0.85, 0.90),
        ColorRGBa(0.85, 0.94, 0.68),
        ColorRGBa(0.96, 0.87, 0.70),
        ColorRGBa(0.93, 0.76, 0.93),
        ColorRGBa(1.0, 0.85, 0.73)
    )
    val neonColors = listOf(
        ColorRGBa(1.0, 0.0, 1.0),
        ColorRGBa(0.0, 1.0, 1.0),
        ColorRGBa(1.0, 1.0, 0.0),
        ColorRGBa(0.0, 1.0, 0.0),
        ColorRGBa(1.0, 0.2, 0.0),
        ColorRGBa(0.5, 0.0, 1.0)
    )

    fun getColor(layer: Int, i: Int, t: Double, x: Double, y: Double, cx: Double, cy: Double): ColorRGBa {
        return when (mode) {
            ColorMode.SINGLE -> singleColor
            ColorMode.MULTICOLOR -> layerColors[layer % layerColors.size]
            ColorMode.COLOR_SHIFT -> {
                val hue = (t * 0.3 + layer * 0.33 + i * 0.0001) % 1.0
                hsvToRgb(hue, 0.8, 1.0)
            }
            ColorMode.RAINBOW -> {
                val hue = (atan2(y - cy, x - cx) / (2 * PI) + 0.5 + t * 0.05 + layer * 0.15) % 1.0
                hsvToRgb(hue, 0.9, 1.0)
            }
            ColorMode.PASTEL -> {
                val idx = (layer + (i / 3333)) % pastelColors.size
                pastelColors[idx]
            }
            ColorMode.NEON -> {
                val hue = (t * 0.15 + layer * 0.17 + i * 0.00005) % 1.0
                val idx = (hue * neonColors.size).toInt() % neonColors.size
                neonColors[idx]
            }
        }
    }

    private fun hsvToRgb(h: Double, s: Double, v: Double): ColorRGBa {
        val c = v * s
        val x = c * (1 - abs((h * 6) % 2 - 1))
        val m = v - c
        val (r1, g1, b1) = when {
            h < 1.0/6 -> Triple(c, x, 0.0)
            h < 2.0/6 -> Triple(x, c, 0.0)
            h < 3.0/6 -> Triple(0.0, c, x)
            h < 4.0/6 -> Triple(0.0, x, c)
            h < 5.0/6 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        return ColorRGBa(r1 + m, g1 + m, b1 + m)
    }
}