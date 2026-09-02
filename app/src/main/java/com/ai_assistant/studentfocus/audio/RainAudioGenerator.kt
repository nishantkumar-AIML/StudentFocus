package com.ai_assistant.studentfocus.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import com.ai_assistant.studentfocus.R

class RainAudioGenerator {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentVolume = 0.5f

    fun start(context: Context) {
        if (isPlaying) return
        isPlaying = true

        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.calming_rain).apply {
                isLooping = true
                setVolume(currentVolume, currentVolume)
                
                // Slow down playback to 0.8x speed for a gentler, deeper sound
                try {
                    val params = PlaybackParams().apply {
                        speed = 0.8f
                    }
                    playbackParams = params
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                start()
            }
        } catch (e: Exception) {
            isPlaying = false
            e.printStackTrace()
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume
        try {
            mediaPlayer?.setVolume(volume, volume)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun stop() {
        isPlaying = false
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaPlayer = null
        }
    }
}
