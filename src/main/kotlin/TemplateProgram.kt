import audioPlayer.PlaylistPlayer
import org.openrndr.Fullscreen
import org.openrndr.KEY_ESCAPE
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import kotlin.math.*




// ============================================
// MAIN APPLICATION
// ============================================
fun main() = application {
    configure {
//        width = 1080 / 2
//        height = 1920 / 2
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
//        display = displays[1]
        hideCursor = true
    }

    program {
        // Initialize playlist player and scan the folder
        val playlistPlayer = PlaylistPlayer(this)
        playlistPlayer.loadDirectory("data/audio/")


        var t = 0.0
        val pointCount = 20000

        keyboard.keyDown.listen { event ->
            when (event.key) {
                KEY_ESCAPE -> {
                    application.exit()
                }
                else -> {
                    when (event.name) {
                        "1" -> ColorConfig.mode = ColorMode.SINGLE
                        "2" -> ColorConfig.mode = ColorMode.MULTICOLOR
                        "3" -> ColorConfig.mode = ColorMode.COLOR_SHIFT
                        "4" -> ColorConfig.mode = ColorMode.RAINBOW
                        "5" -> ColorConfig.mode = ColorMode.PASTEL
                        "6" -> ColorConfig.mode = ColorMode.NEON
                        else -> ColorConfig.mode
                    }
                    println("Color mode: ${ColorConfig.mode}")
                }
            }
        }

        // TODO
//        extend(ScreenRecorder()) {
//            maximumDuration = 26.0 // stops recording after 26 seconds
//            frameRate = 60.0
//            multisample = BufferMultisample.SampleCount(8) // Anti-alias sharp vector lines
//            h264 {
//                constantRateFactor = 10
//            }
//        }
        extend(Screenshots()) {
            key = "s"
        }
        extend {
            // Keep playlist queue updated every frame
            playlistPlayer.update()

            val cx = width / 2.0
            val cy = height / 2.0

            // Set the color of the background
            val backGroundColor = ColorRGBa.fromHex("000003")
            drawer.clear(backGroundColor)

            t += PI / 45.0

            // ==========================================
            // BATCHED POINTS: All 20,000 in one GPU call
            // ==========================================
            drawer.points {
                for (i in 0 until pointCount) {
                    val y = i / 500.0
                    val k = cos(y * 5) * if (y < 11) 21.0 else 11.0
                    val e = y / 8.0 - 13.0
                    val o = sqrt(k * k + e * e) / 6.0
                    val safeK = if (abs(k) < 0.001) 0.001 else k
                    val q = k * 2.0 + 49.0 + cos(19.0 / safeK) +
                            k * cos(y / 2.0) * (1.0 + sin(o * 4.0 - e * 2.0 - t))
                    val layer = i % 3
                    val c = o / 1.5 - e / 5.0 - t / 8.0 + layer * 8.0
                    val x = q * sin(c) + cx
                    val yCoord = cy + q * cos(c) - 79.0 * sin(c / 3.0)

                    // Set per-point color
                    fill = ColorConfig.getColor(layer, i, t, x, yCoord, cx, cy)
                    // Add point to batch
                    point(x, yCoord)
                }
            }

            // Clean Up
            ended.listen {
                playlistPlayer.stop()
            }
        }
    }
}

