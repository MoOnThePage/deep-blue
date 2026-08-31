package audioPlayer

import ddf.minim.AudioPlayer
import ddf.minim.Minim
import org.openrndr.Program
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

// Play List of audio files
class PlaylistPlayer(private val program: Program) {
    private val minim: Minim = Minim(object {
        fun sketchPath(fileName: String): String = File(fileName).absolutePath

        fun createInput(fileName: String): InputStream? {
            val file = File(fileName)
            if (file.exists()) return FileInputStream(file)
            val resourcePath = if (fileName.startsWith("/")) fileName else "/$fileName"
            return PlaylistPlayer::class.java.getResourceAsStream(resourcePath)
        }
    })

    private val playlist = mutableListOf<File>() // List of audio files
    private var currentTrackIndex = 0 // Current track index

    var player: AudioPlayer? = null
        private set

    // Scan directory for audio files
    fun loadDirectory(directoryPath: String) {
        val dir = File(directoryPath)
        playlist.clear()
        if (dir.exists() && dir.isDirectory) {
            val audioFiles = dir.listFiles { _, name ->
                val lower = name.lowercase()
                lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg")
            }?.sortedBy { it.name } ?: emptyList()

            playlist.addAll(audioFiles)
            println("Found ${playlist.size} audio files in $directoryPath")
        } else {
            println("Audio directory not found: ${dir.absolutePath}")
        }

        if (playlist.isNotEmpty()) {
            currentTrackIndex = 0
            playTrack(currentTrackIndex)
        }
    }

    // Play the next track in the playlist
    private fun playTrack(index: Int) {
        player?.close() // Close previous audio stream
        val file = playlist[index]
        println("Now Playing [${index + 1}/${playlist.size}]: ${file.name}")

        player = minim.loadFile(file.absolutePath, 2048)
        player?.play()
    }

    /**
     * Must be called inside `extend { ... }` frame loop to check if track ended
     * and automatically advance to the next track in an infinite loop.
     */
    fun update() {
        if (playlist.isEmpty()) return

        val p = player ?: return
        // When track finishes playing
        if (!p.isPlaying && p.position() >= p.length() - 100) {
            nextTrack()
        }
    }

    fun nextTrack() {
        if (playlist.isEmpty()) return
        currentTrackIndex = (currentTrackIndex + 1) % playlist.size // Infinite loop back to 0
        playTrack(currentTrackIndex)
    }

    // Load an audio to play
    fun loadFile(path: String, bufferSize: Int = 2048) {
        player = minim.loadFile(path, bufferSize)
    }

    fun previousTrack() {
        if (playlist.isEmpty()) return
        currentTrackIndex = if (currentTrackIndex - 1 < 0) playlist.size - 1 else currentTrackIndex - 1
        playTrack(currentTrackIndex)
    }

    // cleaning up
    fun stop() {
        player?.close()
        minim.stop()
    }
}