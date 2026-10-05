import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import kotlin.math.*

abstract class Visual {
    @BooleanParameter("Enabled")
    var enabled: Boolean = false

    abstract fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double)
}

class FishFieldVisual : Visual() {
    @IntParameter("Number of Fish", 1, 10)
    var fishCount: Int = 3

    @DoubleParameter("Fish Spread", 0.0, 400.0)
    var fishSpread: Double = 0.0

    @DoubleParameter("Fish Scale", 0.2, 2.0)
    var fishScale: Double = 1.0

    @IntParameter("Point Count", 1000, 50000)
    var pointCount: Int = 20000

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        val fishAngleStep = 2 * PI / fishCount
        val fishCenters = (0 until fishCount).map { layer ->
            val angle = fishAngleStep * layer
            Pair(cx + cos(angle) * fishSpread, cy + sin(angle) * fishSpread)
        }

        drawer.points {
            for (i in pointCount - 1 downTo 0) {
                val y = i / 500.0
                val k = cos(y * 5) * if (y < 11) 21.0 else 11.0
                val e = y / 8.0 - 13.0
                val o = sqrt(k * k + e * e) / 6.0
                val safeK = if (abs(k) < 0.001) 0.001 else k
                val q = (k * 2.0 + 49.0 + cos(19.0 / safeK) +
                        k * cos(y / 2.0) * (1.0 + sin(o * 4.0 - e * 2.0 - t))) * fishScale

                val layer = i % fishCount
                val (fishCx, fishCy) = fishCenters[layer]
                val c = o / 1.5 - e / 5.0 - t / 8.0 + layer * 8.0

                val x = q * sin(c) + fishCx
                val yCoord = fishCy + q * cos(c) - 79.0 * fishScale * sin(c / 3.0)

                fill = ColorConfig.getColor(layer, i, t, x, yCoord, cx, cy)
                point(x, yCoord)
            }
        }
    }
}

class DisplacementGridVisual : Visual() {
    @IntParameter("Point Count", 1000, 100000)
    var pointCount: Int = 40000

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        drawer.points {
            for (i in pointCount - 1 downTo 0) {
                val x = (i % 200).toDouble()
                val y = i / 200.0

                val k = x / 8.0 - 12.5
                val e = y / 8.0 - 12.5
                val o = (k * k + e * e) / 169.0
                val d = 0.5 + 5.0 * cos(o)

                val px = x + d * k * sin(d * 2.0 + o + t) + e * cos(e + t) + cx - 100.0
                val py = o * 135.0 - y / 4.0 - d * 6.0 * cos(d * 3.0 + o * 9.0 + t) + cy - 75.0

                val gray = ((d * sin(k) * sin(t * 4.0 + e)).pow(2) * 400.0)
                    .coerceIn(0.0, 255.0) / 255.0
                fill = ColorRGBa(gray, gray, gray, 36.0 / 255.0)

                point(px, py)
            }
        }
    }
}

class WaveRippleVisual : Visual() {
    @IntParameter("Point Count", 1000, 50000)
    var pointCount: Int = 40000

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        drawer.points {
            for (i in pointCount - 1 downTo 0) {
                val x = (i % 400).toDouble()
                val y = i / 400.0

                val k = x / 16.0 - 12.5
                val d = -5.0 * abs(sin(k / 3.0) * sin(y / 24.0))

                val q = x / 4.0 - y / 3.0 + 60.0 +
                        (sin(t) + d * 3.0 + 3.0) * k * sin(d * 3.0 + t + sin(d))
                val c = d / 2.0 + t / 8.0

                val px = q * 0.7 * cos(c) + cx - 200.0
                val py = (q + y / 2.0 - d * 19.0) * 0.7 * sin(c) + cy - 200.0

                fill = ColorRGBa(1.0, 1.0, 1.0, 36.0 / 255.0)
                point(px, py)
            }
        }
    }
}