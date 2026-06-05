package com.infinity.ai.circle

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.infinity.ai.ui.theme.InfinityTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * CircleLearnActivity
 *
 * Transparent full-screen activity.
 *
 * Flow:
 *   1. Retrieve screenshot from InfinityOverlayService.pendingScreenshot
 *   2. Show RegionSelectionView (classic Android View — drawn on top of screenshot)
 *   3. On region confirmed → ViewModel.processRegion() → OCR
 *   4. On OCR done → swap to Compose bottom sheet (action sheet + result)
 *   5. On dismiss → finish()
 */
class CircleLearnActivity : ComponentActivity() {

    companion object { private const val TAG = "CircleLearnActivity" }

    private val vm: CircleLearnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screenshot = InfinityOverlayService.pendingScreenshot
        if (screenshot == null || screenshot.width <= 1) {
            Log.e(TAG, "No screenshot available")
            showErrorAndFinish("Could not capture screen. Please try again.")
            return
        }

        showSelectionView(screenshot)
    }

    // ── Phase 4: Region selection ──────────────────────────────────────────────

    private fun showSelectionView(screenshot: Bitmap) {
        val selectionView = RegionSelectionView(this).apply {
            setScreenshot(screenshot)
            onCancel = { finish() }
            onRegionSelected = { region ->
                Log.i(TAG, "Region selected: $region")
                // Remove selection view, show Compose UI
                setContentView(FrameLayout(this@CircleLearnActivity))
                vm.processRegion(screenshot, region)
                showComposeBottomSheet()
            }
        }
        setContentView(selectionView)
    }

    // ── Phase 7+: Compose bottom sheet ────────────────────────────────────────

    private fun showComposeBottomSheet() {
        setContent {
            // Use the app's existing InfinityTheme — no new theme created
            InfinityTheme(darkTheme = true) {
                CircleLearnBottomSheetHost(
                    vm        = vm,
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun showErrorAndFinish(msg: String) {
        setContent {
            InfinityTheme(darkTheme = true) {
                CircleErrorScreen(message = msg, onDismiss = { finish() })
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Don't finish on pause — user may background briefly
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release screenshot bitmap — InfinityOverlayService will set a new one next capture
        InfinityOverlayService.pendingScreenshot?.recycle()
        InfinityOverlayService.pendingScreenshot = null
    }
}
