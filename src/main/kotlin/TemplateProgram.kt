import audioPlayer.PlaylistPlayer
import gui.doubleControl
import org.openrndr.KEY_ESCAPE
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
import org.openrndr.math.Vector2
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
// PLACEMENT (per-visual position and scale)
// ============================================
// x, y are FRACTIONS of the window size, measured from the window center:
//   0.0 = centered, -0.5 = left/top edge, +0.5 = right/bottom edge.
// Fractions (not pixels) keep placements correct when the window is resized or maximized.
class Placement(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var scale: Double = 1.0
)

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

        // Visuals: add new ones here, the panel picks them up automatically.
        // Order matters: later visuals are drawn on top, so they win ties when grabbing.
        val fishVisual = FishFieldVisual().apply { enabled = true }
        val displacementVisual = DisplacementGridVisual()
        val waveVisual = WaveRippleVisual()
        val hypotWaves = HypotWaves()
        val helixBloom = HelixBloom()
        val silkLoom = SilkLoom()
        val visuals = listOf(fishVisual, displacementVisual, waveVisual, hypotWaves, helixBloom, silkLoom)

        // One Placement per visual (position + scale)
        val placements = visuals.associateWith { Placement() }

        var t = 0.0

        // ----------------------------------------
        // Selection state
        // ----------------------------------------
        var selectedIndex = 0                       // visual whose options are shown
        val hidden = ElementClass("hidden")         // style class that hides a visual's option panel
        val panels = mutableListOf<Element>()       // one option container per visual
        val visualEntries = mutableListOf<Item>()   // dropdown entries, so code can change the selection
        var visualDropdown: DropdownButton? = null

        // Show only the selected visual's options and sync the dropdown.
        // Selecting NEVER enables or disables the visual.
        fun selectVisual(index: Int) {
            selectedIndex = index
            panels.forEachIndexed { i, panel ->
                if (i == index) panel.classes.remove(hidden) else panel.classes.add(hidden)
            }
            visualEntries.getOrNull(index)?.let { visualDropdown?.value = it }
        }

        // ----------------------------------------
        // Interaction state
        // ----------------------------------------
        var isVisible = true                 // is the control panel shown?
        var pressPos = Vector2.ZERO          // where the current press started
        var pressedOnPanel = false           // did the press start over the panel (sliders)?
        var grabbed = false                  // are we currently dragging a visual?
        var moved = false                    // did the pointer move past the threshold during this press?
        var grabOffset = Vector2.ZERO        // (visual anchor - pointer) at grab time, keeps the drag jump-free
        var lastTapTime = -10.0              // time of the previous tap, for double-tap detection

        val doubleTapTime = 0.3              // max seconds between two taps to count as a double tap
        val dragThreshold = 8.0              // pixels of movement before a press becomes a drag
        val panelStripWidth = 260.0          // left strip occupied by the panel (20px margin + 200px width + spare).
        // Presses inside it go to the sliders while the panel is open.

        // The design size the visuals were written for. The window may be bigger,
        // so everything is scaled by this factor.
        fun fitFactor(w: Double, h: Double) = min(w / 540.0, h / 960.0)

        // Window position of a visual's anchor (its placement point)
        fun anchorOf(p: Placement) =
            Vector2(width / 2.0 + p.x * width, height / 2.0 + p.y * height)

        // Apply one visual's placement transform to the drawer, then draw it.
        fun Drawer.drawPlaced(visual: Visual, p: Placement, w: Double, h: Double) {
            val cx = w / 2.0
            val cy = h / 2.0
            // 1. move the visual's center to its placement (fractions -> pixels)
            translate(cx + p.x * w, cy + p.y * h)
            // 2. scale around that center
            scale(p.scale * fitFactor(w, h))
            // 3. undo the visual's own hard-coded center (cx, cy)
            translate(-cx, -cy)
            visual.draw(this, t, cx, cy)
        }

        // Enabled visual whose anchor is closest to a window position.
        // Iterating from the end of the list means ties go to the visual drawn last (on top).
        fun nearestEnabled(pos: Vector2): Int? {
            var best: Int? = null
            var bestDist = Double.MAX_VALUE
            for (i in visuals.indices.reversed()) {
                if (!visuals[i].enabled) continue
                val d = anchorOf(placements.getValue(visuals[i])).distanceTo(pos)
                if (d < bestDist) {
                    bestDist = d
                    best = i
                }
            }
            return best
        }

        // ----------------------------------------
        // Control panel
        // ----------------------------------------
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

            // Hidden panels take no space in the layout
            styleSheet(has class_ "hidden") {
                display = Display.NONE
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

                // Visual selector. Choosing an entry only changes WHICH visual's options are shown.
                // It does NOT enable the visual. Dragging a visual on screen selects it too.
                visualDropdown = dropdownButton {
                    label = "Visual"
                    visuals.forEachIndexed { index, visual ->
                        val entry = item {
                            label = visual::class.simpleName ?: "Visual $index"
                            events.picked.listen { selectVisual(index) }
                        }
                        visualEntries.add(entry)
                        if (index == 0) value = entry
                    }
                }

                // Build every visual's controls once; only the first starts visible
                visuals.forEachIndexed { index, visual ->
                    val container = div {
                        if (index != 0) classes.add(hidden)
                        visual.buildPanel(this)

                        // Position is set by dragging, only scale stays a slider
                        val p = placements.getValue(visual)
                        doubleControl("Scale", 0.1, 3.0, p::scale, 2)
                    }
                    panels.add(container)
                }
            }
        }

        extend(cmGui)

        // ----------------------------------------
        // Mouse + touch input
        // (a finger arrives as the LEFT mouse button, so one code path covers both)
        // ----------------------------------------

        // Press: only record where it started. Nothing is grabbed yet, so a plain tap stays a tap.
        mouse.buttonDown.listen { e ->
            pressPos = e.position
            pressedOnPanel = isVisible && e.position.x < panelStripWidth
            grabbed = false
            moved = false
        }

        // Drag: once the pointer passes the threshold, grab the enabled visual nearest to the
        // press point (or the dropdown's selection if none is enabled), then move it.
        mouse.dragged.listen { e ->
            val pastThreshold = e.position.distanceTo(pressPos) > dragThreshold

            if (!grabbed && !pressedOnPanel && pastThreshold) {
                val idx = nearestEnabled(pressPos) ?: selectedIndex
                selectVisual(idx)                                   // panel + dropdown follow the grabbed visual
                val p = placements.getValue(visuals[idx])
                grabOffset = anchorOf(p) - pressPos                 // keeps the drag jump-free
                grabbed = true
            }

            if (grabbed) {
                val target = e.position + grabOffset
                val p = placements.getValue(visuals[selectedIndex])
                p.x = ((target.x - width / 2.0) / width).coerceIn(-1.0, 1.0)
                p.y = ((target.y - height / 2.0) / height).coerceIn(-1.0, 1.0)
            }
            if (pastThreshold) moved = true
        }

        // Release: a "tap" is a clean press that was not on the panel and never moved.
        // Two quick taps toggle the control panel.
        mouse.buttonUp.listen {
            if (!pressedOnPanel && !grabbed && !moved) {
                if (seconds - lastTapTime < doubleTapTime) {
                    isVisible = !isVisible
                    cmGui.enabled = isVisible
                    lastTapTime = -10.0
                } else {
                    lastTapTime = seconds
                }
            } else {
                lastTapTime = -10.0   // drags never count toward a double tap
            }
            grabbed = false
            moved = false
        }

        // Initialize playlist player and scan the folder
        val playlistPlayer = PlaylistPlayer()
        playlistPlayer.loadDirectory("data/audio/")

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

//        record(60.0)
        extend(Screenshots()) {
            key = "s"
        }

        // Clean Up (registered once, not every frame)
        ended.listen {
            playlistPlayer.stop()
        }

        // ----------------------------------------
        // Draw loop
        // ----------------------------------------
        extend {
            playlistPlayer.update()

            // Window size as plain numbers, read once per frame
            val w = width.toDouble()
            val h = height.toDouble()

            drawer.clear(settings.backGroundColor)
            t += PI / settings.angularSpeed

            for (visual in visuals) {
                if (visual.enabled) {
                    val p = placements.getValue(visual)
                    drawer.isolated {
                        drawPlaced(visual, p, w, h)
                    }
                }
            }
        }
    }
}