package com.ktmb.pts.utilities

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.FileOutputStream

object AzureTTS {

    private const val SpeechSubscriptionKey = "da6992ee73014008940ad35cc17400e9"
    private const val SpeechRegion = "southeastasia"
    private val mediaPlayer = MediaPlayer()
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun createSpeechSynthesizer(): SpeechSynthesizer {
        val speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion)
        speechConfig.speechSynthesisVoiceName = "ms-MY-OsmanNeural"
        return SpeechSynthesizer(speechConfig)
    }

    fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }

    fun stopSpeech() {
        mediaPlayer.stop()
    }

    fun speech(context: Context, text: String) {
        doAsync {
            val file = if (getAudioFile(context, text) != null) {
                getAudioFile(context, text)
            } else {
                val speechSynthesizer = createSpeechSynthesizer()
                val result = speechSynthesizer.SpeakText(text)
                LogManager.log("${result.resultId}: ${result.reason}", "AzureTTS")
                saveAudioFile(context, result.audioData, text)
            }

            val uri: Uri = Uri.fromFile(file)
            mediaPlayer.setAudioAttributes(audioAttributes)
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepare()
            mediaPlayer.start()
        }
    }

    private fun getAudioFile(context: Context, text: String): File? {
        val fileName = text.replace(" ", "_").toLowerCase()
        val dirPath = "${context.cacheDir}/audio/$fileName.mp3"
        val file = File(dirPath)

        return return if (file.exists()) {
            file
        } else {
            null
        }
    }

    private fun saveAudioFile(context: Context, audioByte: ByteArray, text: String): File {
        val fileName = text.replace(" ", "_").toLowerCase()
        val dirPath = "${context.cacheDir}/audio/$fileName.mp3"
        val file = File(dirPath)

        return if (file.exists()) {
            file
        } else {
            val tempFile = File.createTempFile(fileName, "mp3", context.cacheDir)
            val stream = FileOutputStream(tempFile)
            stream.write(audioByte)
            stream.close()
            LogManager.log("Saving new audio file: $dirPath", "AzureTTS")
            tempFile
        }
    }

}