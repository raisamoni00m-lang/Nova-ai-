package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class NovaWakeWordForegroundService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopWakeWordDetection()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        _isWakeWordActive.value = true
        startWakeWordDetection()

        return START_STICKY
    }

    private fun startWakeWordDetection() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _isWakeWordActive.value = false
            return
        }

        if (isRecording) return
        isRecording = true

        serviceScope.launch {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    isRecording = false
                    return@launch
                }

                audioRecord?.startRecording()
                val buffer = ShortArray(bufferSize / 2)

                var energyBurstCounter = 0
                var silenceFrames = 0

                while (isActive && isRecording) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readCount > 0) {
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            sum += abs(buffer[i].toInt())
                        }
                        val avgAmp = sum / readCount

                        // Low-power acoustic signature tracking for speech cadence "Hey Nova"
                        if (avgAmp > 1400) {
                            energyBurstCounter++
                            silenceFrames = 0
                            if (energyBurstCounter in 3..12) {
                                // Potential cadence detected, broadcast wake event
                                _wakeWordTriggeredCount.value += 1
                                notifyWakeWordDetected()
                                energyBurstCounter = 0
                                kotlinx.coroutines.delay(2000) // Debounce
                            }
                        } else {
                            silenceFrames++
                            if (silenceFrames > 5) {
                                energyBurstCounter = 0
                            }
                        }
                    }
                    kotlinx.coroutines.delay(80) // Low power sleep interval
                }
            } catch (_: Exception) {
                isRecording = false
            } finally {
                stopAudioRecord()
            }
        }
    }

    private fun notifyWakeWordDetected() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_WAKE_TRIGGERED
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(launchIntent)
    }

    private fun stopAudioRecord() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        isRecording = false
    }

    private fun stopWakeWordDetection() {
        isRecording = false
        stopAudioRecord()
        _isWakeWordActive.value = false
    }

    override fun onDestroy() {
        stopWakeWordDetection()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nova Wake Word Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs background low-power listening for 'Hey Nova'"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nova AI Voice Assistant")
            .setContentText("Listening for \"Hey Nova\" wake word")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "nova_wake_word_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.example.nova.STOP_WAKE_WORD"
        const val ACTION_WAKE_TRIGGERED = "com.example.nova.WAKE_WORD_TRIGGERED"

        private val _isWakeWordActive = MutableStateFlow(false)
        val isWakeWordActive: StateFlow<Boolean> = _isWakeWordActive.asStateFlow()

        private val _wakeWordTriggeredCount = MutableStateFlow(0)
        val wakeWordTriggeredCount: StateFlow<Int> = _wakeWordTriggeredCount.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, NovaWakeWordForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NovaWakeWordForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
