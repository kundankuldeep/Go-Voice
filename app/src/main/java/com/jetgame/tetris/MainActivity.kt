package com.jetgame.tetris

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jetgame.tetris.logic.*
import com.jetgame.tetris.speechTotext.RecognitionCallback
import com.jetgame.tetris.speechTotext.RecognitionStatus
import com.jetgame.tetris.speechTotext.SpeechService
import com.jetgame.tetris.ui.GameBody
import com.jetgame.tetris.ui.GameScreen
import com.jetgame.tetris.ui.PreviewGamescreen
import com.jetgame.tetris.ui.combinedClickable
import com.jetgame.tetris.ui.theme.ComposetetrisTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity(), RecognitionCallback {

    companion object {
        private const val ACTIVATION_KEYWORD = "Start"
        private const val RECORD_AUDIO_REQUEST_CODE = 101
    }

    private val variableForListening = true
    private val TAG = "Recognition"
    var mViewModel: GameViewModel? = null

//    private val recognitionManager: ContinuousSpeechRecognizer by lazy {
//        ContinuousSpeechRecognizer(
//            this, activationKeyword = ACTIVATION_KEYWORD, callback = this,
//            variableForListening = variableForListening
//        )
//    }

    private val recognitionManager: SpeechService by lazy {
        SpeechService(context = this, callback = this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtil.transparentStatusBar(this)
        SoundUtil.init(this)

        recognitionManager.createRecognizer()

        checkSpeechPermission()

        setContent {
            ComposetetrisTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    mViewModel = viewModel<GameViewModel>()
                    val viewState = mViewModel?.viewState?.value ?: return@Surface


                    LaunchedEffect(key1 = Unit) {
                        while (isActive) {
                            delay(650L - 55 * (viewState.level - 1))
                            mViewModel?.dispatch(Action.GameTick)
                        }
                    }

                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(key1 = Unit) {
                        val observer = object : DefaultLifecycleObserver {
                            override fun onResume(owner: LifecycleOwner) {
                                mViewModel?.dispatch(Action.Resume)
                            }

                            override fun onPause(owner: LifecycleOwner) {
                                mViewModel?.dispatch(Action.Pause)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }


                    GameBody(combinedClickable(
                        onMove = { direction: Direction ->
                            if (direction == Direction.Up) {
                                mViewModel?.dispatch(Action.Drop)
                            } else mViewModel?.dispatch(Action.Move(direction))
                        },
                        onRotate = {
                            mViewModel?.dispatch(Action.Rotate)
                        },
                        onRestart = {
                            mViewModel?.dispatch(Action.Reset)
                        },
                        onPause = {
                            if (mViewModel?.viewState?.value?.isRuning == true) {
                                mViewModel?.dispatch(Action.Pause)
                            } else {
                                mViewModel?.dispatch(Action.Resume)
                            }
                        },
                        onMute = {
                            mViewModel?.dispatch(Action.Mute)
                        }
                    )) {
                        GameScreen(
                            Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        setTimer()
    }

    private fun setTimer() {
        val timer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.i(TAG, "onTick: $millisUntilFinished")
            }

            override fun onFinish() {
                Log.i(TAG, "onFinish: ")
                recognitionManager.stopRecognition()
                recognitionManager.startRecognition()
                setTimer()
            }
        }
        timer.start()
    }

    private fun checkSpeechPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_REQUEST_CODE
            )
        }
    }


    override fun onDestroy() {
        recognitionManager.destroyRecognizer()
        super.onDestroy()
        SoundUtil.release()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startRecognition()
        }
    }

    override fun onPause() {
        stopRecognition()
        super.onPause()
    }

    private fun startRecognition() {
        recognitionManager.startRecognition()
    }

    private fun stopRecognition() {
        recognitionManager.stopRecognition()
    }

    private fun getErrorText(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
        SpeechRecognizer.ERROR_SERVER -> "Error from server"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
        else -> "Didn't understand, please try again."
    }

    override fun onPrepared(status: RecognitionStatus) {
        when (status) {
            RecognitionStatus.SUCCESS -> {
                Log.i("Recognition", "onPrepared: Success")
            }
            RecognitionStatus.UNAVAILABLE -> {
                Log.i("Recognition", "onPrepared: Failure or unavailable")
                AlertDialog.Builder(this)
                    .setTitle("Speech Recognizer unavailable")
                    .setMessage("Your device does not support Speech Recognition. Sorry!")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    override fun onBeginningOfSpeech() {
        Log.i(TAG, "onBeginningOfSpeech: ")
    }

    override fun onKeywordDetected() {
        Log.i(TAG, "onKeywordDetected: ")
    }

    override fun onReadyForSpeech(params: Bundle) {
        Log.i(TAG, "onReadyForSpeech: ")
    }

    override fun onBufferReceived(buffer: ByteArray) {
        Log.i(TAG, "onBufferReceived: ")
    }

    override fun onRmsChanged(rmsdB: Float) {

    }

    override fun onPartialResults(results: List<String>) {
//        val text = results.joinToString(separator = ",").lowercase()
        val text = results[results.size - 1].lowercase()
        handleGame(text)
        Log.i(TAG, "onPartialResults: $text")
    }

    var isStart = false
    var isPause = false


    private fun handleGame(currentText: String) {
        when {
            currentText.contains("start") -> {
                if (!isStart) {
                    mViewModel?.dispatch(Action.Reset)
                    isStart = true
                }
            }
            currentText.contains("pause") -> {
                if (isStart) {
                    mViewModel?.dispatch(Action.Pause)
                    isStart = false
                    isPause = true
                }
            }
            currentText.contains("resume") -> {
                if (isPause) {
                    mViewModel?.dispatch(Action.Resume)
                    isStart = true
                    isPause = false
                }
            }
            currentText.contains("up") -> {
                if (isStart) {
                    mViewModel?.dispatch(Action.Drop)
                }
            }
            currentText.contains("down") -> {
                if (isStart) {
                    mViewModel?.dispatch(Action.Move(Direction.Down))
                }
            }
            currentText.contains("left") -> {
                if (isStart) {
                    mViewModel?.dispatch(Action.Move(Direction.Left))
                }
            }
            currentText.contains("right") || currentText.contains("write") || currentText.contains("light") -> {
                if (isStart) {
                    mViewModel?.dispatch(Action.Move(Direction.Right))
                }
            }
            currentText.contains("rotate") -> {
                if (isStart) {
                    mViewModel?.dispatch(Action.Rotate)
                }
            }
            currentText.contains("reset") -> {
                if (isStart) {
                    mViewModel?.dispatch(Action.Reset)
                    isStart = false
                }
            }
        }
    }

    override fun onResults(results: List<String>, scores: FloatArray?) {
        val text = results.joinToString(separator = "\n")
        Log.i("Recognition", "onResults : $text")
    }

    override fun onError(errorCode: Int) {
        val errorMessage = getErrorText(errorCode)
        Log.i(TAG, "onError: $errorMessage")
    }

    override fun onEvent(eventType: Int, params: Bundle) {
        Log.i(TAG, "onEvent: ")
    }

    override fun onEndOfSpeech() {
        Log.i(TAG, "onEndOfSpeech: ")
        recognitionManager.startRecognition()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RECORD_AUDIO_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startRecognition()
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposetetrisTheme {
        GameBody {
            PreviewGamescreen(Modifier.fillMaxSize())
        }
    }
}