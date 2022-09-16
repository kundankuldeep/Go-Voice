package com.jetgame.tetris.speechTotext

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.getSystemService
import java.util.*


class SpeechService(
    private val context: Context,
    private val callback: RecognitionCallback? = null,
) : RecognitionListener {

    private val TAG = "speech 1"
    private var isActivated: Boolean = false
    private val speech: SpeechRecognizer by lazy { SpeechRecognizer.createSpeechRecognizer(context) }
    private val audioManager: AudioManager? = context.getSystemService()

    private val recognizerIntent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
    }

    fun createRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speech.setRecognitionListener(this)
            callback?.onPrepared(RecognitionStatus.SUCCESS)
        } else {
            callback?.onPrepared(RecognitionStatus.UNAVAILABLE)
        }
    }

    fun destroyRecognizer() {
        speech.destroy()
    }

    fun startRecognition() {
        speech.startListening(recognizerIntent)
    }

    fun stopRecognition() {
        speech.stopListening()
    }

    override fun onReadyForSpeech(p0: Bundle?) {
        Log.i(TAG, "onReadyForSpeech: ")
    }

    override fun onBeginningOfSpeech() {
        Log.i(TAG, "onBeginningOfSpeech: ")

    }

    override fun onRmsChanged(p0: Float) {

    }

    override fun onBufferReceived(p0: ByteArray?) {
        Log.i(TAG, "onBufferReceived: ")
    }

    override fun onEndOfSpeech() {

    }

    override fun onError(p0: Int) {
        Log.i(TAG, "onError: $p0")
    }

    override fun onResults(p0: Bundle?) {
        Log.i(TAG, "onResults: ")
        val matches = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()){
            callback?.onPartialResults(matches)
        }
    }

    override fun onPartialResults(p0: Bundle?) {
        val matches = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()){
            callback?.onPartialResults(matches)
        }
    }

    override fun onEvent(p0: Int, p1: Bundle?) {
        Log.i(TAG, "onEvent: ")
    }

}