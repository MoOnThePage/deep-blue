import audioPlayer.PlaylistPlayer
import gui.doubleControl
import org.openrndr.Fullscreen
import org.openrndr.KEY_ESCAPE
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.panel.ControlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.layout
import org.openrndr.panel.style.*
import org.openrndr.panel.styleSheet
import kotlin.math.*

// ============================================
// SETTINGS (shared, non-visual)
// ============================================
class Settings {
    var angularSpeed: Double = 45.0
    val backGroundColor: ColorRGBa = ColorRGBa.fromHex("000003")
}

// ============================================
// MAIN APPLICATION
// ============================================
fun main() = application {
    configure {
        width = 1080 / 2
        height = 1920 / 2
        windowResizable = true
//        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
//        display = displays[1]
        hideCursor = false
    }

    program {
        val settings = Settings()
        ColorConfig.mode = ColorMode.SINGLE

        // Visuals: add new ones here, the panel picks them up automatically
        val fishVisual = FishFieldVisual().apply { enabled = true }
        val displacementVisual = DisplacementGridVisual()
        val waveVisual = WaveRippleVisual()
        val hypotWaves = HypotWaves()
        val helixBloom = HelixBloom()
        val visuals = listOf(fishVisual, displacementVisual, waveVisual, hypotWaves, helixBloom)

        var isVisible = true
        var lastClickTime = 0.0
        val doubleClickThreshold = 0.25
        val cmGui = ControlManager().apply {
            styleSheet(has type "dropdown-button") {
                background = Color.RGBa(ColorRGBa.PINK)
                color = Color.RGBa(ColorRGBa.BLACK)
                width = 200.px
            }

            styleSheet(has type "slider") {
                color = Color.RGBa(ColorRGBa.PINK)
                width = 200.px
            }

            // Position the root layout container to the left side
            styleSheet(has type "x-root") {
                position = Position.ABSOLUTE
                left = 20.px
                top = 20.px
            }

            layout {
                // Global controls
                doubleControl("Angular speed", 5.0, 90.0, settings::angularSpeed, 0)

                dropdownButton {
                    label = "Color"
                    ColorMode.entries.forEach { mode ->
                        val entry = item {
                            label = mode.name
                            events.picked.listen {
                                ColorConfig.mode = mode
                            }
                        }
                        if (mode == ColorConfig.mode) value = entry
                    }
                }

                // One toggle + its own controls per visual
                visuals.forEach { it.buildPanel(this) }
            }
        }

        extend(cmGui)
        // Track double-clicks using buttonUp events
        mouse.buttonUp.listen {
            val currentTime = seconds
            if (currentTime - lastClickTime < doubleClickThreshold) {
                isVisible = !isVisible
                cmGui.enabled = isVisible
            }
            lastClickTime = currentTime
        }

//        val settings = object {
//            @IntParameter("Number of Fish", 1, 10)
//            var fishCount: Int = 3
//
//            @DoubleParameter("Fish Spread", 0.0, 400.0)
//            var fishSpread: Double = 0.0 // orbit radius spreading fish center
//
//            @DoubleParameter("Fish Scale", 0.2, 2.0)
//            var fishScale: Double = 1.0    // shrink each fish as spread grows
//
//            @DoubleParameter("Angular Speed", 5.0, 90.0)
//            var angularSpeed: Double = 45.0
//
//            @ColorParameter("background")
//            var backGroundColor = ColorRGBa.fromHex("000003")
//
//            @OptionParameter("Color Mode")
//            var colorMode = ColorMode.SINGLE
//
//            @IntParameter("Point Count", 0, 50000)
//            var pointCount: Int = 20000
//        }
//
//        gui.add(settings, "Settings")
//
//        extend(gui)

        // Initialize playlist player and scan the folder
        val playlistPlayer = PlaylistPlayer()
        playlistPlayer.loadDirectory("data/audio/")

        var t = 0.0

        keyboard.keyDown.listen { event ->
            when (event.key) {
                KEY_ESCAPE -> {
                    application.exit()
                }
                else -> {
                    when (event.name) {
                        // Audio controls
                        "n" -> playlistPlayer.nextTrack()
                        "p" -> playlistPlayer.previousTrack()
                    }
                }
            }
        }

        record() //
        extend(Screenshots()) {
            key = "s"
        }

        // Clean Up (registered once, not every frame)
        ended.listen {
            playlistPlayer.stop()
        }

        extend {
            playlistPlayer.update()

            val cx = width / 2.0
            val cy = height / 2.0

            drawer.clear(settings.backGroundColor)
            t += PI / settings.angularSpeed

            for (visual in visuals) {
                if (visual.enabled) {
                    visual.draw(drawer, t, cx, cy)
                }
            }
//            // Keep playlist queue updated every frame
//            playlistPlayer.update()
//            ColorConfig.mode = settings.colorMode
//
//            val cx = width / 2.0
//            val cy = height / 2.0
//
//            // Set the color of the background
//            drawer.clear(settings.backGroundColor)
//
//            t += PI / settings.angularSpeed // t += PI / 45.0
//
//            // Precompute each fish's own center ONCE per frame, not per point.
//            val fishAngleStep = 2 * PI / settings.fishCount
//            val fishCenters = (0 until settings.fishCount).map { layer ->
//                val angle = fishAngleStep * layer
//                Pair(
//                    cx + cos(angle) * settings.fishSpread,
//                    cy + sin(angle) * settings.fishSpread
//                )
//            }
//
//            // ==========================================
//            // BATCHED POINTS: All 20,000 in one GPU call
//            // ==========================================
//            drawer.points {
//                for (i in settings.pointCount - 1 downTo 0) {
//                    val y = i / 500.0
//                    val k = cos(y * 5) * if (y < 11) 21.0 else 11.0
//                    val e = y / 8.0 - 13.0
//                    val o = sqrt(k * k + e * e) / 6.0
//                    val safeK = if (abs(k) < 0.001) 0.001 else k
//                    val q = k * 2.0 + 49.0 + cos(19.0 / safeK) +
//                            k * cos(y / 2.0) * (1.0 + sin(o * 4.0 - e * 2.0 - t)) * settings.fishScale
//                    val layer = i % settings.fishCount
//                    val (fishCx, fishCy) = fishCenters[layer]
//                    val c = o / 1.5 - e / 5.0 - t / 8.0 + layer * 8.0
//                    val x = q * sin(c) + fishCx
//                    val yCoord = fishCy + q * cos(c) - 79.0 * settings.fishScale * sin(c / 3.0)
//
//                    // Set per-point color
//                    fill = ColorConfig.getColor(layer, i, t, x, yCoord, cx, cy)
//                    // Add point to batch
//                    point(x, yCoord)
//                }
//            }
        }
    }
}