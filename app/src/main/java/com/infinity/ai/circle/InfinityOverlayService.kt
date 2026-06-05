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
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.infinity.ai.MainActivity
import com.infinity.ai.ui.theme.InfinityTheme
import kotlinx.coroutines.*

/**
 * InfinityOverlayService
 *
 * Owns the entire Circle Learn flow via WindowManager overlays.
 * The host app is NEVER obscured by a full-screen Activity launch.
 *
 * Pipeline:
 *   Bubble tap
 *   → captureScreen()
 *   → attachSelectionOverlay()      RegionSelectionView on WindowManager
 *   → vm.processRegion()            OCR via existing pipeline
 *   → attachBottomSheetOverlay()    CircleLearnBottomSheetHost on WindowManager
 *   → onDismiss → removeAllOverlays(), restore bubble
 *
 * CircleLearnActivity is the fallback when SYSTEM_ALERT_WINDOW is not granted.
 */
class InfinityOverlayService : Service() {

    companion object {
        const val ACTION_START       = "com.infinity.ai.circle.START"
        const val ACTION_STOP        = "com.infinity.ai.circle.STOP"
        const val EXTRA_RESULT_CODE  = "result_code"
        const val EXTRA_RESULT_DATA  = "result_data"
        private const val NOTIF_ID   = 9001
        private const val CHANNEL_ID = "infinity_overlay"
        private const val TAG        = "InfinityOverlayService"

        /**
         * Fallback: populated before launching CircleLearnActivity when overlay
         * permission is unavailable. Not used in the normal overlay flow.
         */
        @Volatile var pendingScreenshot: Bitmap? = null
    }

    // ── WindowManager ──────────────────────────────────────────────────────────
    private lateinit var wm: WindowManager
    private val overlayType
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    // ── Overlay views ──────────────────────────────────────────────────────────
    private var bubble:           FloatingBubbleView? = null
    private var selectionOverlay: RegionSelectionView? = null
    private var sheetHost:        OverlayComposeHost? = null

    // ── MediaProjection ────────────────────────────────────────────────────────
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay:  VirtualDisplay?  = null
    private var imageReader:     ImageReader?     = null

    // ── ViewModel (service-scoped) ─────────────────────────────────────────────
    private val vmStore = ViewModelStore()
    private lateinit var vm: CircleLearnViewModel

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var screenWidth  = 0
    private var screenHeight = 0
    private var screenDpi    = 0
    private var currentScreenshot: Bitmap? = null

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        val dm = resources.displayMetrics
        screenWidth  = dm.widthPixels
        screenHeight = dm.heightPixels
        screenDpi    = dm.densityDpi

        vm = ViewModelProvider(
            vmStore,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[CircleLearnViewModel::class.java]
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIF_ID, buildNotification())
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                @Suppress("DEPRECATION")
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
        removeAllOverlays()
        teardownProjection()
        currentScreenshot?.recycle(); currentScreenshot = null
        vmStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Bubble ─────────────────────────────────────────────────────────────────

    private fun showBubble() {
        if (!OverlayPermissionHelper.hasOverlayPermission(this)) {
            Log.w(TAG, "No overlay permission — bubble not shown"); return
        }
        bubble = FloatingBubbleView(this) { onBubbleTapped() }
        bubble?.show()
    }

    private fun hideBubble() { bubble?.hide(); bubble = null }

    // ── Step 1: Bubble tapped ─────────────────────────────────────────────────

    private fun onBubbleTapped() {
        if (!OverlayPermissionHelper.hasOverlayPermission(this)) {
            fallbackToActivity(); return
        }
        // Hide bubble so it doesn't appear in the screenshot
        bubble?.hide()

        scope.launch {
            delay(120) // let bubble hide animation finish before capturing
            captureScreen { bitmap ->
                currentScreenshot?.recycle()
                currentScreenshot = bitmap
                attachSelectionOverlay(bitmap)
            }
        }
    }

    // ── Step 2: Selection overlay ─────────────────────────────────────────────

    private fun attachSelectionOverlay(screenshot: Bitmap) {
        val params = fullScreenParams(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val view = RegionSelectionView(this).apply {
            setScreenshot(screenshot)
            onCancel = {
                scope.launch(Dispatchers.Main) {
                    removeSelectionOverlay()
                    bubble?.show()
                }
            }
            onRegionSelected = { region ->
                scope.launch(Dispatchers.Main) {
                    removeSelectionOverlay()
                    vm.processRegion(screenshot, region)
                    attachBottomSheetOverlay()
                }
            }
        }

        selectionOverlay = view
        runCatching { wm.addView(view, params) }
            .onFailure { Log.e(TAG, "Failed to add selection overlay", it) }
    }

    private fun removeSelectionOverlay() {
        selectionOverlay?.let { runCatching { wm.removeView(it) } }
        selectionOverlay = null
    }

    // ── Step 3: Bottom-sheet overlay ──────────────────────────────────────────

    private fun attachBottomSheetOverlay() {
        val params = fullScreenParams(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val host = OverlayComposeHost(this) {
            InfinityTheme(darkTheme = true) {
                CircleLearnBottomSheetHost(
                    vm          = vm,
                    onDismiss   = {
                        scope.launch(Dispatchers.Main) {
                            removeBottomSheetOverlay()
                            vm.reset()
                            currentScreenshot?.recycle(); currentScreenshot = null
                            bubble?.show()
                        }
                    },
                    onOpenInApp = { route ->
                        scope.launch(Dispatchers.Main) {
                            removeAllOverlays()
                            vm.reset()
                            launchMainActivity(route)
                        }
                    }
                )
            }
        }

        sheetHost = host
        runCatching {
            wm.addView(host.view, params)
            host.start()
        }.onFailure { Log.e(TAG, "Failed to add bottom sheet overlay", it) }
    }

    private fun removeBottomSheetOverlay() {
        sheetHost?.let { it.stop(); runCatching { wm.removeView(it.view) } }
        sheetHost = null
    }

    private fun removeAllOverlays() {
        removeSelectionOverlay()
        removeBottomSheetOverlay()
    }

    // ── Fallback: launch CircleLearnActivity when overlay permission missing ───

    private fun fallbackToActivity() {
        Log.w(TAG, "Overlay permission unavailable — falling back to CircleLearnActivity")
        captureScreen { bitmap ->
            pendingScreenshot?.recycle()
            pendingScreenshot = bitmap
            val intent = Intent(this, CircleLearnActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    // ── Open Infinity app (explicit user request only) ────────────────────────

    private fun launchMainActivity(route: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (route != null) putExtra("route", route)
        }
        startActivity(intent)
    }

    // ── MediaProjection ────────────────────────────────────────────────────────

    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mgr.getMediaProjection(resultCode, data)
        Log.i(TAG, "MediaProjection ready")
    }

    private fun captureScreen(onCaptured: (Bitmap) -> Unit) {
        val mp = mediaProjection
        if (mp == null) {
            Log.e(TAG, "MediaProjection not available")
            vm.setError("Screen capture not available. Please restart Circle Learn.")
            attachBottomSheetOverlay()
            return
        }

        virtualDisplay?.release()
        imageReader?.close()

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mp.createVirtualDisplay(
            "CircleLearn", screenWidth, screenHeight, screenDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )

        scope.launch(Dispatchers.IO) {
            delay(300) // allow VirtualDisplay to render one frame
            val image = imageReader?.acquireLatestImage()
            if (image == null) {
                Log.e(TAG, "No image acquired")
                withContext(Dispatchers.Main) {
                    onCaptured(Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888))
                }
                return@launch
            }
            try {
                val planes      = image.planes
                val buffer      = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride   = planes[0].rowStride
                val rowPadding  = rowStride - pixelStride * screenWidth

                val bmp = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride, screenHeight, Bitmap.Config.ARGB_8888
                )
                bmp.copyPixelsFromBuffer(buffer)
                val cropped = Bitmap.createBitmap(bmp, 0, 0, screenWidth, screenHeight)
                if (cropped != bmp) bmp.recycle()

                withContext(Dispatchers.Main) { onCaptured(cropped) }
            } finally {
                image.close()
                virtualDisplay?.release(); virtualDisplay = null
                imageReader?.close();      imageReader    = null
            }
        }
    }

    private fun teardownProjection() {
        virtualDisplay?.release(); virtualDisplay = null
        imageReader?.close();      imageReader    = null
        mediaProjection?.stop();   mediaProjection = null
    }

    // ── WindowManager helpers ─────────────────────────────────────────────────

    private fun fullScreenParams(flags: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        0, 0,
        overlayType,
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    // ── Notification ───────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Infinity Circle Learn", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Circle Learn overlay service" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, InfinityOverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Infinity Circle Learn")
            .setContentText("Tap the ∞ bubble to circle anything and learn instantly")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPi)
            .build()
    }
}
