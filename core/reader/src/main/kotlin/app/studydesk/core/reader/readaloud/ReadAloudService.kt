package app.studydesk.core.reader.readaloud

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.studydesk.core.reader.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Keeps read aloud going with the screen off, with Pause / Resume / Stop in the notification. */
@AndroidEntryPoint
class ReadAloudService : Service() {
    @Inject lateinit var controller: ReadAloudController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> controller.pause()
            ACTION_RESUME -> controller.resume()
            ACTION_STOP -> {
                controller.stop()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type)
        return START_NOT_STICKY
    }

    private fun notification(): android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, getString(R.string.read_aloud_channel), NotificationManager.IMPORTANCE_LOW),
            )
        }
        val state = controller.state.value
        val paused = state.status == ReadAloudState.Status.Paused
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentTitle(getString(R.string.read_aloud_notification_title))
            .setContentText(state.title)
            .setContentIntent(open)
            .setOngoing(!paused)
            .setSilent(true)
            .addAction(
                0,
                getString(if (paused) R.string.read_aloud_resume else R.string.read_aloud_pause),
                action(if (paused) ACTION_RESUME else ACTION_PAUSE, 1),
            )
            .addAction(0, getString(R.string.read_aloud_stop), action(ACTION_STOP, 2))
            .build()
    }

    private fun action(action: String, code: Int) = PendingIntent.getService(
        this,
        code,
        Intent(this, ReadAloudService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val CHANNEL = "read_aloud"
        private const val NOTIFICATION_ID = 41
        private const val ACTION_PAUSE = "pause"
        private const val ACTION_RESUME = "resume"
        private const val ACTION_STOP = "stop"

        /** Redraws the notification after a pause or resume from inside the app. */
        fun refresh(context: Context) {
            context.startService(Intent(context, ReadAloudService::class.java))
        }
    }
}
