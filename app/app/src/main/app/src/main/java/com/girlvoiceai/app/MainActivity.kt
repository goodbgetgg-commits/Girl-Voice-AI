package com.girlvoiceai.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var audioFile: File? = null
    private var isRecording = false

    private lateinit var statusText: TextView
    private lateinit var recordButton: Button
    private lateinit var playButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioFile = File(cacheDir, "voice_recording.m4a")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }

        val title = TextView(this).apply {
            text = "Girl Voice AI"
            textSize = 30f
            gravity = android.view.Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "مرحباً بك! سجّل صوتك للبدء."
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 40, 0, 40)
        }

        recordButton = Button(this).apply {
            text = "🎙️ بدء تسجيل الصوت"
            setOnClickListener {
                if (isRecording) {
                    stopRecording()
                } else {
                    checkPermissionAndRecord()
                }
            }
        }

        playButton = Button(this).apply {
            text = "▶️ الاستماع إلى التسجيل"
            isEnabled = false
            setOnClickListener {
                playRecording()
            }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(recordButton)
        layout.addView(playButton)

        setContentView(layout)
    }

    private fun checkPermissionAndRecord() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        } else {
            startRecording()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        } else {
            Toast.makeText(
                this,
                "يجب السماح باستخدام الميكروفون",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun startRecording() {
        try {
            audioFile?.delete()

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            recordButton.text = "⏹️ إيقاف التسجيل"
            playButton.isEnabled = false
            statusText.text = "🔴 جارٍ تسجيل الصوت..."

        } catch (e: Exception) {
            releaseRecorder()
            isRecording = false
            statusText.text = "تعذّر بدء التسجيل"
            Toast.makeText(
                this,
                "حدث خطأ أثناء التسجيل",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
            statusText.text = "اكتمل التسجيل بنجاح"
            playButton.isEnabled = audioFile?.exists() == true
        } catch (e: Exception) {
            statusText.text = "التسجيل قصير جداً أو لم يكتمل"
            audioFile?.delete()
        } finally {
            releaseRecorder()
            isRecording = false
            recordButton.text = "🎙️ تسجيل صوت جديد"
        }
    }

    private fun playRecording() {
        try {
            player?.release()

            player = MediaPlayer().apply {
                setDataSource(audioFile!!.absolutePath)
                setOnCompletionListener {
                    it.release()
                    player = null
                    statusText.text = "انتهى تشغيل التسجيل"
                }
                prepare()
                start()
            }

            statusText.text = "▶️ جارٍ تشغيل التسجيل..."

        } catch (e: Exception) {
            player?.release()
            player = null
            statusText.text = "تعذّر تشغيل التسجيل"
        }
    }

    private fun releaseRecorder() {
        recorder?.release()
        recorder = null
    }

    override fun onDestroy() {
        if (isRecording) {
            try {
                recorder?.stop()
            } catch (_: Exception) {
            }
        }

        releaseRecorder()
        player?.release()
        player = null

        super.onDestroy()
    }
}
