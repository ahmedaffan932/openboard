package org.dslul.openboard.translator.pro.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.dslul.openboard.objects.CameraMisc
import org.dslul.openboard.translator.pro.FragmentsDashboardActivity
import org.dslul.openboard.translator.pro.OCRActivity
import org.dslul.openboard.translator.pro.TranslateActivity
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage

/**
 * Tap target of the pinned notification's four tiles ([PinnedNoti]): opens the tool at once - no
 * splash, no interstitial, no app-open ad - the way a quick-access card should.
 *
 * - Translate / Voice: the Translate screen, in the app's task, without its entry interstitial.
 *   Voice starts speech input at once.
 * - Camera: the dashboard's camera tab (reused when the dashboard is alive). Camera is a premium
 *   feature there, so a locked user meets the dashboard's own upgrade / rewarded-ad offer.
 * - File: the system photo picker, hosted here; the picked image goes to [OCRActivity] - the app's
 *   gallery translation. Gallery translation is part of the camera feature, so a locked user is
 *   sent to the camera tab's offer instead.
 *
 * Transparent, in a task of its own (`taskAffinity=""`, out of Recents) so a repeat tap never
 * resurfaces the app's task by accident; excluded from the resume app-open ad (App). It finishes
 * in onCreate except while it hosts the picker.
 */
class QuickAccessActivity : AppCompatActivity() {

    /**
     * One action (and request code) per tile keeps the four PendingIntents apart - an extra alone
     * would not. The system finds this activity's earlier task by its root component whatever the
     * action, and would merely bring it back (a stale picker, no tile action) - which is why
     * [pendingIntent] clears it on every tap.
     */
    enum class Tile(val action: String, val requestCode: Int, val analyticsName: String) {
        TRANSLATE("org.dslul.openboard.translator.pro.pinned.TRANSLATE", 1240, "translate"),
        VOICE("org.dslul.openboard.translator.pro.pinned.VOICE", 1241, "voice"),
        CAMERA("org.dslul.openboard.translator.pro.pinned.CAMERA", 1242, "camera"),
        FILE("org.dslul.openboard.translator.pro.pinned.FILE", 1243, "file"),
    }

    companion object {
        /** A tile tap this recent is what brought the app up: the on-open reminder stays away. */
        private const val TAP_WINDOW_MS = 5_000L

        @Volatile
        private var lastTapAt = 0L

        /** Request codes 1240-1243: clear of 1234 itself and the router's 4000+ band. */
        fun pendingIntent(context: Context, tile: Tile): PendingIntent {
            val intent = Intent(context, QuickAccessActivity::class.java)
                .setAction(tile.action)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            return PendingIntent.getActivity(
                context,
                tile.requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** True right after a tile tap - read by App when the process comes to the foreground. */
        fun tappedRecently(): Boolean =
            lastTapAt != 0L && SystemClock.elapsedRealtime() - lastTapAt < TAP_WINDOW_MS
    }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                CameraMisc.fileUri = uri
                // The picker's read grant belongs to this activity and ends when it finishes, so
                // OCRActivity gets its own through the data + flag (it falls back to intent.data).
                val ocr = Intent(this, OCRActivity::class.java)
                    .setData(uri)
                    .putExtra(CameraMisc.typeGallery, true)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(ocr)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()

        val tile = Tile.entries.firstOrNull { it.action == intent.action }
        if (tile == null) {
            finish()
            return
        }
        // Recreated (process death while the picker was up): the result is delivered to
        // pickImage again, so only the picker host stays; nothing is launched a second time.
        if (savedInstanceState != null) {
            if (tile != Tile.FILE) finish()
            return
        }

        lastTapAt = SystemClock.elapsedRealtime()
        NotiAnalytics.entryOpen(this, NotiAnalytics.SOURCE_PINNED_NOTI, tile.analyticsName)

        when (tile) {
            Tile.TRANSLATE, Tile.VOICE -> {
                startActivity(
                    Intent(this, TranslateActivity::class.java)
                        .putExtra(TranslateActivity.EXTRA_SKIP_ENTRY_AD, true)
                        .putExtra(TranslateActivity.EXTRA_START_VOICE, tile == Tile.VOICE)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                finish()
            }

            Tile.CAMERA -> {
                openCameraTab()
                finish()
            }

            Tile.FILE -> {
                if (cameraLocked()) {
                    openCameraTab()
                    finish()
                    return
                }
                // Stays up to receive the result; being excluded from the resume ad covers the return.
                try {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    finish()
                }
            }
        }
    }

    /** The dashboard's own rule for its camera tab (FragmentsDashboardActivity.canOpenLockedPremiumItem). */
    private fun cameraLocked(): Boolean = Misc.isProScreenEnabled && !Misc.getPurchasedStatus(this)

    private fun openCameraTab() {
        startActivity(
            NotiTargets.dashboard(this, FragmentsDashboardActivity.TAB_CAMERA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
