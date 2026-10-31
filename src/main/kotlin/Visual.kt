import gui.boolControl
import gui.doubleControl
import gui.intControl
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.div
import kotlin.math.*

abstract class Visual(val name: String) {
    var enabled: Boolean = false

    abstract fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double)

    /** Override to add this visual's own sliders/toggles. */
    open fun Element.controls() {}

    /** Called once from main: one toggle (named after the visual) + its controls. */
    fun buildPanel(parent: Element) {
        parent.div("visual") {
            boolControl(name, this@Visual::enabled)
            controls()
        }
    }
}

class FishFieldVisual : Visual("Fish Field") {
    var fishCount = 3
    var fishSpread = 0.0
    var fishScale = 1.0
    var pointCount = 20000

    override fun Element.controls() {
        intControl("Fish count", 1, 10, ::fishCount)
        doubleControl("Fish spread", 0.0, 400.0, ::fishSpread, 0)
        doubleControl("Fish scale", 0.2, 2.0, ::fishScale)
        intControl("Points", 1000, 50000, ::pointCount)
    }

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        val count = fishCount.coerceAtLeast(1)
        val fishAngleStep = 2 * PI / count
        val fishCenters = (0 until count).map { layer ->
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

                val layer = i % count
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

class DisplacementGridVisual : Visual("Displacement Grid") {
    var pointCount = 40000

    override fun Element.controls() {
        intControl("Points", 1000, 100000, ::pointCount)
    }

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

class WaveRippleVisual : Visual("Wave Ripple") {
    var pointCount = 40000

    override fun Element.controls() {
        intControl("Points", 1000, 50000, ::pointCount)
    }

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

class HypotWaves : Visual("Hypo tWaves") {

    // 1. add a plain `var` for every knob you want tunable
    var pointCount = 10000

    override fun Element.controls() {
        intControl("Points", 1000, 50000, ::pointCount)
    }

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        drawer.points {
            for (i in pointCount downTo 1) {
                val x = i.toDouble()
                val y = i.toDouble() / 235.0

                val k = (4.0 + cos(x / 9.0 - t)) * cos(x / 30.0)
                val e = y / 7.0 - 13.0

                val d = hypot(k, e) + sin(y / 99.0 + t / 2.0) - 4.0
                val c = d - t

                val q = 3.0 * sin(k * 2.0) + sin(y / 29.0) * k * (9.0 + 2.0 * sin(cos(e) * 9.0 - d * 4.0 + t))

                // Adjusting center offset from hardcoded 200 to use cx/cy template variables
                val px = q + 40.0 * cos(c) + cx - 200.0
                val py = q * sin(c) + d * 35.0 + cy - 200.0

                fill = ColorRGBa(1.0, 1.0, 1.0, 96.0 / 255.0)
                point(px, py)
            }
        }
    }
}

class HelixBloom : Visual("Helix Bloom") {

    // 1. add a plain `var` for every knob you want tunable
    var pointCount = 20000
    var alpha = 36.0          // per-point opacity, 0..255 (sketch used 96 at 20k points)
    var speed = PI / 45 * 60  // sketch advanced t by PI/45 per frame at 60 fps
    var baseRadius = 79.0     // the `79` in q
    var stretch = 40.0        // the `+ 40` that elongates the y radius

    override fun Element.controls() {
        intControl("Points", 1000, 50000, ::pointCount)
        doubleControl("Alpha", 1.0, 255.0, ::alpha)
        doubleControl("Speed", 0.0, 10.0, ::speed)
        doubleControl("Base radius", 20.0, 150.0, ::baseRadius)
        doubleControl("Y stretch", 0.0, 120.0, ::stretch)
    }

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        val time = t * speed

        drawer.points {
            for (i in pointCount - 1 downTo 0) {
                // Sketch used y = i/99 over 20000 points (y in 0..~202).
                // Normalizing keeps the same shape at any point count;
                // more points just means a denser figure.
                val y = i.toDouble() / pointCount * (20000.0 / 99.0)

                // default-params from the original, in order
                val k = 8 * cos(y)
                val e = y / 8 - 12
                val d = hypot(k, e).pow(3) / 999 + 1

                val q = baseRadius - e * sin(k) +
                        k / d * (8 + 4 * sin(d * d - time + cos(e + time / 2)))

                val c = d / 2 + e / 99 * sin(time + d) - time / 8 + (i % 2) * 3

                // 200 was the sketch's canvas center x -> cx
                // 190 was center y (200) minus 10 -> cy - 10
                val px = q * sin(c) + cx
                val py = (q + stretch) * cos(c) + cy - 10.0

                fill = ColorRGBa(1.0, 1.0, 1.0, alpha / 255.0)
                point(px, py)
            }
        }
    }
}

class SilkLoom : Visual("Silk Loom") {
    var pointCount = 30000
    var speed = PI / 30 * 60   // sketch advanced t by PI/30 per frame at 60 fps
    var baseAlpha = 36.0       // the `36` in stroke(w, 36 + ...)
    var flicker = 3.0          // the `3` in 3/cos(t+e), the pulsing opacity
    var spread = 30.0          // the `30` that swings the strands sideways
    var height = 79.0          // the `79` that scales d into the vertical extent

    override fun Element.controls() {
        intControl("Points", 1000, 50000, ::pointCount)
        doubleControl("Speed", 0.0, 10.0, ::speed)
        doubleControl("Alpha", 1.0, 120.0, ::baseAlpha)
        doubleControl("Flicker", 0.0, 20.0, ::flicker)
        doubleControl("Spread", 0.0, 80.0, ::spread)
        doubleControl("Height", 20.0, 150.0, ::height)
    }

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        val time = t * speed

        drawer.points {
            for (i in pointCount - 1 downTo 0) {
                // The sketch used i = 0..29999 directly. Rescale so the shape
                // stays the same at any point count (more points = denser).
                val s = i.toDouble() * (30000.0 / pointCount)

                // default-params from the original, in order
                val k = 5 * cos(s / 49) * cos(s / 3690)
                val e = s / 984 - 12
                val d = (k * k + e * e) / 99 + 1   // mag(k, e)**2 = k² + e²

                // i % 3 uses the real integer index so the 3 interleaved
                // strands are preserved
                val m = (i % 3).toDouble()

                // c is assigned inside the x expression in the sketch,
                // and reused by y
                val c = d - time / 18 + m * 4

                val px = k * (4 + sin(d * 18 - time * 2 + m * 2)) -
                        5 * sin(atan2(k, e) * 9) +
                        spread * sin(c) + cx - 200.0   // +200 -> + cx - 200.0
                val py = 80 * sin(c - d) + d * height + cy - 200.0

                // stroke(w, 36 + 3/cos(t+e)): white, alpha can spike near
                // cos = 0, so clamp to 0..255 like p5 does
                val a = (baseAlpha + flicker / cos(time + e)).coerceIn(0.0, 255.0)
                fill = ColorRGBa(1.0, 1.0, 1.0, a / 255.0)
                point(px, py)
            }
        }
    }
}
