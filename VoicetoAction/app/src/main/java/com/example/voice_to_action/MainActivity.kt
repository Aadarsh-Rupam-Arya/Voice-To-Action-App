package com.example.voice_to_action

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var editText: EditText
    private lateinit var micButton: FloatingActionButton
    private lateinit var joinMeetButton: MaterialButton
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views with new Material Design components
        editText = findViewById(R.id.texts)
        micButton = findViewById(R.id.button)
        joinMeetButton = findViewById(R.id.joinMeetButton)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        setupSpeechRecognizer()
        setupClickListeners()
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                showSnackbar("Ready to listen")
            }

            override fun onBeginningOfSpeech() {
                editText.text.clear()
                editText.hint = "Listening..."
                micButton.setImageResource(R.drawable.ic_mic_offs)
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                updateMicButtonState(false)
            }

            override fun onError(error: Int) {
                updateMicButtonState(false)
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    else -> "Error occurred. Please try again"
                }
                showSnackbar(errorMessage)
            }

            override fun onResults(bundle: Bundle?) {
                updateMicButtonState(false)
                bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)?.let { text ->
                    editText.setText(text)
                    saveToNote(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun setupClickListeners() {
        micButton.setOnClickListener {
            if (isListening) {
                speechRecognizer.stopListening()
                updateMicButtonState(false)
            } else {
                checkPermissionsAndStartListening()
            }
        }

        joinMeetButton.setOnClickListener {
            openGoogleMeet()
        }
    }

    private fun updateMicButtonState(listening: Boolean) {
        isListening = listening
        micButton.setImageResource(
            if (listening) R.drawable.ic_mic_offs else R.drawable.ic_mics
        )
        editText.hint = if (listening) "Listening..." else "Tap microphone to start speaking"
    }

    private fun openGoogleMeet() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://meet.google.com/kzc-teqi-ayy"))
            startActivity(intent)
        } catch (e: Exception) {
            showSnackbar("Unable to open Google Meet")
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startListening()
            } else {
                showSnackbar("Microphone permission is required for speech recognition")
            }
        }

    private fun checkPermissionsAndStartListening() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startListening()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startListening() {
        updateMicButtonState(true)
        speechRecognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
    }

    private fun saveToNote(text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(intent, "Save to Notes"))
        } catch (e: Exception) {
            showSnackbar("Unable to save note")
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}