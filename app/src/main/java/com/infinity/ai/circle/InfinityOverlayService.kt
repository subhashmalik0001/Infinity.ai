package com.infinity.ai.circle

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import com.infinity.ai.MainActivity
import kotlinx.coroutines.*

/**
 * InfinityOverlayService
 *
 * Persistent foreground service that:
 * 1. Shows a notification (required for foreground services on API 26+)
 * 2. Attaches a floating bubble via FloatingBubbleView
 * 3. Manages MediaProjection lifecycle
 * 4. Captures the screen on demand and delivers the bitmap to CircleLearnActivity
 *
 * Lifecycle:
 *   startService(ACTION_START) → show bubble
 *   startService(ACTION_STOP)  → tear everything down
 *   bubble tap                 → capture screen → start CircleLearnActivity
 */
class InfinityOverlayService : Service() {

    companion object {
        const val ACTION_START            = "com.infinity.ai.circle.START"
        const val ACTION_STOP             = "com.infinity.ai.circle.STOP"
        const val EXTRA_RESULT_CODE       = "result_code"
        const val EXTRA_RESULT_DATA       = "result_data"
        private const val NOTIF_ID        = 9001
        private const val CHANNEL_ID     = "infinity_overlay"
        private const val TAG             = "InfinityOverlayService"

        // Shared screenshot accessible by CircleLearnActivity in the same process
        @Volatile var pendingScreenshot: Bitmap? = null
    }

    private var bubble       : FloatingBubbleView? = null
    private var mediaProjection: MediaProjection?  = null
    private var virtualDisplay : VirtualDisplay?   = null
    private var imageReader  : ImageReader?        = null
    private val scope        = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var screenWidth  = 0
    private var screenHeight = 0
    private var screenDpi    = 0

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val dm = resources.displayMetrics
        screenWidth  = dm.widthPixels
        screenHeight = dm.heightPixels
        screenDpi    = dm.densityDpi
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIF_ID, buildNotification())
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    setupMediaProjection(resultCode, resultData)
                }
                showBubble()
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        hideBubble()
        teardownProjection()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Bubble ─────────────────────────────────────────────────────────────────

    private fun showBubble() {
        if (!OverlayPermissionHelper.hasOverlayPermission(this)) {
            Log.w(TAG, "No overlay permission — bubble not shown")
            return
        }
        bubble = FloatingBubbleView(this) { onBubbleTapped() }
        bubble?.show()
        Log.i(TAG, "Bubble shown")
    }

    private fun hideBubble() {
        bubble?.hide()
        bubble = null
    }

    private fun onBubbleTapped() {
        Log.i(TAG, "Bubble tapped — capturing screen")
        captureScreen { bitmap ->
            pendingScreenshot = bitmap
            val intent = Intent(this, CircleLearnActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                         Intent.FLAG_ACTIVITY_CLEAR_TOP or
                         Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        }
    }

    // ── MediaProjection setup ──────────────────────────────────────────────────

    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mgr.getMediaProjection(resultCode, data)
        Log.i(TAG, "MediaProjection ready")
    }

    private fun captureScreen(onCaptured: (Bitmap) -> Unit) {
        val mp = mediaProjection
        if (mp == null) {
            Log.e(TAG, "MediaProjection not available")
            // Still launch the activity — it will show an error
            onCaptured(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
            return
        }

        // Clean up any previous reader
        virtualDisplay?.release()
        imageReader?.close()

        imageReader = ImageReader.newInstance(screenWidth, screenHeight,
            PixelFormat.RGBA_8888, 2)

        virtualDisplay = mp.createVirtualDisplay(
            "CircleLearn",
            screenWidth, screenHeight, screenDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )

        // Wait for the first image on IO thread
        scope.launch {
            delay(300) // give VirtualDisplay time to render one frame
            val image = imageReader?.acquireLatestImage()
            if (image == null) {
                Log.e(TAG, "No image acquired")
                withContext(Dispatchers.Main) {
                    onCaptured(Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888))
                }
                return@launch
            }
            try {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride  = planes[0].pixelStride
                val rowStride    = planes[0].rowStride
                val rowPadding   = rowStride - pixelStride * screenWidth

                val bmp = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight, Bitmap.Config.ARGB_8888
                )
                bmp.copyPixelsFromBuffer(buffer)

                // Crop to exact screen size (remove row padding)
                val cropped = Bitmap.createBitmap(bmp, 0, 0, screenWidth, screenHeight)
                if (cropped != bmp) bmp.recycle()

                Log.i(TAG, "Screen captured: ${cropped.width}x${cropped.height}")
                withContext(Dispatchers.Main) { onCaptured(cropped) }
            } finally {
                image.close()
                virtualDisplay?.release(); virtualDisplay = null
                imageReader?.close();      imageReader = null
            }
        }
    }

    private fun teardownProjection() {
        virtualDisplay?.release(); virtualDisplay = null
        imageReader?.close();      imageReader    = null
        mediaProjection?.stop();   mediaProjection = null
        pendingScreenshot?.recycle()
        pendingScreenshot = null
    }

    // ── Notification ───────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Infinity Circle Learn",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Circle Learn overlay service" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, InfinityOverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Infinity Circle Learn")
            .setContentText("Tap the bubble on any screen to learn instantly")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .build()
    }
}
