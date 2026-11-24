import gui.boolControl
import gui.doubleControl
import gui.intControl
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.div
import kotlin.math.*
import org.openrndr.color.mix
import org.openrndr.draw.BlendMode

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

class HypotWaves : Visual("Hypot Waves") {

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

class DriftOrchid : Visual("Drift Orchid") {
    var pointCount = 10000
    var speed = PI / 120 * 60  // sketch advanced t by PI/120 per frame at 60 fps
    var alpha = 96.0           // the `96` in stroke(w, 96)
    var ring = 60.0            // the `60` in 60*cos(c), the sideways swing
    var depth = 29.0           // the `29` that scales d into the vertical extent

    override fun Element.controls() {
        intControl("Points", 1000, 50000, ::pointCount)
        doubleControl("Speed", 0.0, 10.0, ::speed)
        doubleControl("Alpha", 1.0, 255.0, ::alpha)
        doubleControl("Ring", 0.0, 150.0, ::ring)
        doubleControl("Depth", 5.0, 60.0, ::depth)
    }

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        val time = t * speed

        drawer.points {
            for (i in pointCount - 1 downTo 0) {
                // The sketch used y = i/345 over 10000 points (y in 0..~29).
                // Rescale so the shape stays the same at any point count.
                val y = i.toDouble() * (10000.0 / pointCount) / 345.0

                // JS `y^8` is a bitwise XOR on the integer part of y, not a power
                val yXor = (y.toInt() xor 8).toDouble()

                // default-params from the original, in order.
                // cos(i - t/4) uses the real integer index: it acts as a
                // per-point pseudo-random phase, which spreads the points out.
                val base = if (y < 11) 6 + sin(yXor) * 6 else y / 5 + cos(y / 2)
                val k = base * cos(i - time / 4)
                val e = y / 7 - 13
                val d = hypot(k, e) + sin(e / 4 + time) / 2

                // q = radial distance; c = angle, assigned inside the x
                // expression in the sketch and reused by y
                val q = y * k / d * (3 + sin(d * 2 + y / 2 - time * 4))
                val c = d / 2 + 1 - time / 2

                val px = q + ring * cos(c) + cx - 200.0        // +200 -> + cx - 200.0
                val py = q * sin(c) + d * depth - 170.0 + cy - 200.0

                fill = ColorRGBa(1.0, 1.0, 1.0, alpha / 255.0)
                point(px, py)
            }
        }
    }
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// ---------------------------------------------------------------------------
// SHARED PALETTE (E4312B red, 149954 green, FFFFFF white; black comes from the host)
// White sits in the middle on purpose: green -> red directly would turn brown.
// ---------------------------------------------------------------------------
private val RED = ColorRGBa.fromHex("E4312B")
private val GREEN = ColorRGBa.fromHex("149954")
private val WHITE = ColorRGBa.fromHex("FFFFFF")

/** Maps v in 0..1 onto green -> white -> red. */
private fun palette(v: Double): ColorRGBa {
    val x = v.coerceIn(0.0, 1.0)
    return if (x < 0.5) mix(GREEN, WHITE, x * 2.0)
    else mix(WHITE, RED, (x - 0.5) * 2.0)
}

// ===========================================================================
// 1. MOIRE BLOOM  (grid x 100..299, y 99..299 step 2 -> about 200 x 101 points)
// ===========================================================================
class MoireBloom : Visual("Moire Bloom") {
    var pointCount = 20200
    var speed = PI / 90 * 60   // sketch advanced t by PI/90 per frame at 60 fps
    var alpha = 96.0           // the `96` in stroke(255, 96)
    var ripples = 9.0          // the `9` in sin(d*9): ripple bands per ring

    override fun Element.controls() {
        intControl("Points", 1000, 50000, ::pointCount)
        doubleControl("Speed", 0.0, 10.0, ::speed)
        doubleControl("Alpha", 1.0, 255.0, ::alpha)
        doubleControl("Ripples", 1.0, 20.0, ::ripples)
    }

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        val time = t * speed

        // Additive blending: overlapping translucent points add light -> neon glow
        val oldBlend = drawer.drawStyle.blendMode
        drawer.drawStyle.blendMode = BlendMode.ADD

        // The grid is twice as dense in x as in y (like the sketch's step of 2),
        // so cols = sqrt(2 * count) and rows = count / cols.
        val cols = sqrt(pointCount * 2.0).toInt().coerceAtLeast(2)
        val rows = (pointCount / cols).coerceAtLeast(2)

        drawer.points {
            for (i in pointCount - 1 downTo 0) {
                // Split the single index into a column and a row of the grid,
                // then map them onto the sketch's x = 100..299, y = 99..299
                val x = 100.0 + (i % cols) * (199.0 / (cols - 1))
                val y = 99.0 + (i / cols) * (200.0 / (rows - 1))

                // default-params from the original, in order.
                // k, e: grid re-centered around 0 (roughly -12.5..+12.5)
                val k = x / 8 - 25
                val e = y / 8 - 25

                // o: distance from the center, divided by a "breathing" denominator.
                // The k*e term warps the rings into a saddle; `- time` animates them.
                val o = hypot(k, e) / (4 + sin(k * e / 49 - time))

                // d: cosine of the warped distance -> concentric bands in -1..1
                val d = cos(o)

                // q: horizontal radius. k/cos(e) explodes near cos(e)=0, which makes
                // the long streaks (this is in the original). sin(d*ripples) carves
                // ripple bands; x/3 adds a base radius that grows with x.
                val q = k / cos(e) * sin(d * ripples) + x / 3

                // c: angle. Each ring rotates differently; time/8 spins the whole picture
                val c = o + time / 8

                // Polar -> Cartesian. The y radius (y/2+q)/2 squashes it into an ellipse.
                val px = q * cos(c) + cx - 200.0              // +200 -> + cx - 200.0
                val py = (y / 2 + q) / 2 * sin(c) + cy - 200.0

                // d (-1..1) -> 0..1 -> palette
                fill = palette((d + 1) / 2).opacify(alpha / 255.0)
                point(px, py)
            }
        }

        drawer.drawStyle.blendMode = oldBlend
    }
}

// ===========================================================================
// 2. TIDAL LANTERN  (grid x 100..299, y 99..299 step 2)
// ===========================================================================
class TidalLantern : Visual("Tidal Lantern") {
    var pointCount = 20200
    var speed = PI / 60 * 60   // sketch advanced t by PI/60 per frame at 60 fps
    var alpha = 96.0           // the `96` in stroke(255, 96)
    var ripples = 9.0          // the `9` in cos(d*9): ripple bands per ring
    var swell = 5.0            // the `5` in d = 5*cos(o): strength of the radial swell

    override fun Element.controls() {
        intControl("Points", 1000, 50000, ::pointCount)
        doubleControl("Speed", 0.0, 10.0, ::speed)
        doubleControl("Alpha", 1.0, 255.0, ::alpha)
        doubleControl("Ripples", 1.0, 20.0, ::ripples)
        doubleControl("Swell", 1.0, 10.0, ::swell)
    }

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        val time = t * speed

        val oldBlend = drawer.drawStyle.blendMode
        drawer.drawStyle.blendMode = BlendMode.ADD

        // Twice as dense in x as in y, as in the sketch
        val cols = sqrt(pointCount * 2.0).toInt().coerceAtLeast(2)
        val rows = (pointCount / cols).coerceAtLeast(2)

        drawer.points {
            for (i in pointCount - 1 downTo 0) {
                val x = 100.0 + (i % cols) * (199.0 / (cols - 1))   // 100..299
                val y = 99.0 + (i / cols) * (200.0 / (rows - 1))    // 99..299

                // default-params from the original, in order.
                // k, e: grid re-centered around 0 (roughly -12.5..+12.5)
                val k = x / 8 - 25
                val e = y / 8 - 25

                // o: distance from the center, slowed by /3.5. Constant on circles.
                val o = hypot(k, e) / 3.5

                // d: cosine of the distance scaled to -swell..+swell; drives the rings
                val d = swell * cos(o)

                // c: angle, assigned inside the x expression in the sketch and reused by y.
                // (d+o)/4 rotates each ring differently; -time/8 spins the picture.
                val c = (d + o) / 4 - time / 8

                // x radius: atan(3*cos(...)) is a squashed wave (about -1.25..+1.25)
                // that snaps between two levels, so the ripples look like sharp bands.
                // Times k it grows toward the left/right edges; x/2 is the base radius.
                val rx = k * atan(3 * cos(d * ripples)) + x / 2

                // y radius: the only place time enters the radius, so the rings "breathe"
                val ry = k * d * cos(o - d + time) + y / 2

                val px = rx * cos(c) + cx - 200.0   // +200 -> + cx - 200.0
                val py = ry * sin(c) + cy - 200.0

                // d (-swell..+swell) -> 0..1 -> palette
                fill = palette((d / swell + 1) / 2).opacify(alpha / 255.0)
                point(px, py)
            }
        }

        drawer.drawStyle.blendMode = oldBlend
    }
}

// ===========================================================================
// 3. NEON CURTAIN  (grid x 100..299, y 100..299 step 1 -> 200 x 200)
// ===========================================================================
class NeonCurtain : Visual("Neon Curtain") {
    var pointCount = 40000
    var speed = PI / 120 * 60  // sketch advanced t by PI/120 per frame at 60 fps
    var alpha = 66.0           // the `66` in stroke(255, 66)
    var swell = 5.0            // the `5` in d = 5*cos(...): strength of the ring swell
    var ripples = 9.0          // the `9` in atan(9*cos(...)): sharpness of the row waves

    override fun Element.controls() {
        intControl("Points", 1000, 50000, ::pointCount)
        doubleControl("Speed", 0.0, 10.0, ::speed)
        doubleControl("Alpha", 1.0, 255.0, ::alpha)
        doubleControl("Swell", 1.0, 10.0, ::swell)
        doubleControl("Ripples", 1.0, 30.0, ::ripples)
    }

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        val time = t * speed

        val oldBlend = drawer.drawStyle.blendMode
        drawer.drawStyle.blendMode = BlendMode.ADD

        // Square grid: n x n points
        val n = sqrt(pointCount.toDouble()).toInt().coerceAtLeast(2)

        drawer.points {
            for (i in pointCount - 1 downTo 0) {
                val x = 100.0 + (i % n) * (199.0 / (n - 1))   // 100..299
                val y = 100.0 + (i / n) * (199.0 / (n - 1))   // 100..299

                // default-params from the original, in order.
                val k = x / 8 - 25
                val e = y / 8 - 25

                // d: cosine of the distance from the center, scaled to -swell..+swell
                val d = swell * cos(hypot(k, e) / 3)

                // Row wave: depends only on e (the row), so all points in a row share it.
                // That is what creates the horizontal banding. atan makes it snap.
                val rowWave = atan(ripples * cos(e * 2 - time / 2))

                // Sideways push, scaled by k so the middle is calm and the edges swing
                val push = (d * k + k * 2 * rowWave) * sin(d * 2.5 - time)

                // /2 averages the grid x with the pushed x; +100 -> + cx - 200.0 + 100.0
                val px = (x + push) / 2 + cx - 100.0

                // Every term is built from d, so rings share a height pattern:
                // d*13 base height, a traveling ripple, and d*e which slants it by row
                val py = d * 13 + d * 3 * (3 + cos(d * 3 - time)) + d * e + cy   // +200 -> + cy

                fill = palette((d / swell + 1) / 2).opacify(alpha / 255.0)
                point(px, py)
            }
        }

        drawer.drawStyle.blendMode = oldBlend
    }
}

// ===========================================================================
// 4. SWAYING VEIL  (grid x 100..299, y 100..299 step 1 -> 200 x 200)
// ===========================================================================
class SwayingVeil : Visual("Swaying Veil") {
    var pointCount = 40000
    var speed = PI / 60 * 60   // sketch advanced t by PI/60 per frame at 60 fps
    var alpha = 66.0           // the `66` in stroke(255, 66)
    var swell = 5.0            // the `5` in d = 5*cos(...): strength of the ring swell
    var sway = 0.5             // the `1/2` in k/2*sin(y/3+t): side-to-side wave strength
    var flutter = 5.0          // the `5` in (d-2)*5*abs(cos(...)): vertical flutter

    override fun Element.controls() {
        intControl("Points", 1000, 50000, ::pointCount)
        doubleControl("Speed", 0.0, 10.0, ::speed)
        doubleControl("Alpha", 1.0, 255.0, ::alpha)
        doubleControl("Swell", 1.0, 10.0, ::swell)
        doubleControl("Sway", 0.0, 2.0, ::sway)
        doubleControl("Flutter", 0.0, 15.0, ::flutter)
    }

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        val time = t * speed

        val oldBlend = drawer.drawStyle.blendMode
        drawer.drawStyle.blendMode = BlendMode.ADD

        // Square grid: n x n points
        val n = sqrt(pointCount.toDouble()).toInt().coerceAtLeast(2)

        drawer.points {
            for (i in pointCount - 1 downTo 0) {
                val x = 100.0 + (i % n) * (199.0 / (n - 1))   // 100..299
                val y = 100.0 + (i / n) * (199.0 / (n - 1))   // 100..299

                // default-params from the original, in order.
                val k = x / 8 - 25
                val e = y / 8 - 25

                // d: cosine of the distance from the center, scaled to -swell..+swell
                val d = swell * cos(hypot(k, e) / 3)

                // Two sideways pushes: a ring-based one (depends on d) and a row-based
                // one (depends on y only), so each row sways at its own phase like a flag
                val push = d * k * sin(d * 2.5 - time) + k * sway * sin(y / 3 + time)

                // /2 averages grid x and pushed x; +100 -> + cx - 200.0 + 100.0
                val px = (x + push) / 2 + cx - 100.0

                // d*19 base height; abs(cos) folds the wave so it bounces instead of
                // swinging; (d-2) shifts which rings flutter up or down; d*e slants by row.
                // +215 -> + cy - 200.0 + 215.0 = + cy + 15.0
                val py = d * 19 + (d - 2) * flutter * abs(cos(d / 2 - time / 2)) +
                        d * e + cy + 15.0

                fill = palette((d / swell + 1) / 2).opacify(alpha / 255.0)
                point(px, py)
            }
        }

        drawer.drawStyle.blendMode = oldBlend
    }
}

// ===========================================================================
// 5. NEON THISTLE  (one path: x = i, y = i/41, 10000 points)
// ===========================================================================
class NeonThistle : Visual("Neon Thistle") {
    var pointCount = 10000
    var speed = PI / 240 * 60  // sketch advanced t by PI/240 per frame at 60 fps
    var alpha = 36.0           // the `36` in stroke(w, 36)
    var petals = 9.0           // the `9` in sin(atan2(k, e)*9): angular lobes
    var spin = 50.0            // the `50` in 50*cos(c): radius of the circular swing
    var lift = 45.0            // the `45` that scales d into vertical position

    override fun Element.controls() {
        intControl("Points", 1000, 50000, ::pointCount)
        doubleControl("Speed", 0.0, 10.0, ::speed)
        doubleControl("Alpha", 1.0, 255.0, ::alpha)
        doubleControl("Petals", 1.0, 20.0, ::petals)
        doubleControl("Spin", 0.0, 120.0, ::spin)
        doubleControl("Lift", 10.0, 90.0, ::lift)
    }

    override fun draw(drawer: Drawer, t: Double, cx: Double, cy: Double) {
        val time = t * speed

        val oldBlend = drawer.drawStyle.blendMode
        drawer.drawStyle.blendMode = BlendMode.ADD

        drawer.points {
            for (i in pointCount - 1 downTo 0) {
                // The sketch calls a(i, i/41): ONE parameter walking along a line.
                // Rescale so the shape stays the same at any point count.
                val x = i.toDouble() * (10000.0 / pointCount)   // 0..9999
                val y = x / 41                                  // 0..~244

                // default-params from the original, in order.
                // k: fast wave times slow wave -> swells and shrinks along the path
                val k = 5 * cos(x / 19) * cos(y / 30)
                val e = y / 8 - 12

                // d: squared distance of (k, e) from the origin (mag**2 = k*k + e*e),
                // scaled down, +2. Acts as the "depth" of each point (about 2..9).
                val d = (k * k + e * e) / 59 + 2

                // q: radius with three layers: a 9-lobed flower from atan2, a slow wave
                // along d, and a sideways stretch k/d that shrinks with depth (fake
                // perspective) and shimmers via the fast sin(... - 16t)
                val q = 4 * sin(atan2(k, e) * petals) +
                        9 * sin(d - time) -
                        k / d * (9 + sin(d * 9 - time * 16) * 3)

                // c: angle, assigned inside the x expression and reused by y.
                // d*d/7 twists each depth level differently.
                val c = d * d / 7 - time

                val px = q + spin * cos(c) + cx              // +200 -> + cx - 200.0 + 200.0
                val py = q * sin(c) + d * lift + cy - 209.0  // -9 -> + cy - 200.0 - 9.0

                // depth d (2..9) -> 0..1 -> palette: shallow = green, deep = red
                fill = palette((d - 2) / 7).opacify(alpha / 255.0)
                point(px, py)
            }
        }

        drawer.drawStyle.blendMode = oldBlend
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////