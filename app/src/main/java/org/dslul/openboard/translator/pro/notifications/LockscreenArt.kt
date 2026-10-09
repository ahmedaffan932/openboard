package org.dslul.openboard.translator.pro.notifications

import android.os.Build
import androidx.annotation.DrawableRes
import org.dslul.openboard.inputmethod.latin.R

/**
 * Art for the lock-screen notification (Figma TranslatorPro / "Lockscreen 1-3").
 *
 * The camera, voice and text creatives are the design file's own images, byte for byte
 * (lockscreen_hero_*.jpg, 420x187). Phrasebook has no design art, so it uses a bundled feature
 * photo (noti_photo_phrasebook, Unsplash License: unsplash.com/photos/Uk3t05ndSng, Annie Spratt).
 */
object LockscreenArt {

    /**
     * For the collapsed notification row - the view the lock screen shows - so it carries the
     * feature image rather than the logo (from API 31 Android already puts the app icon at the
     * left of that row). Square, corners pre-cut, cropped onto each subject.
     */
    @DrawableRes
    fun notificationThumb(target: String): Int = when (target) {
        NotiTargets.CAMERA -> R.drawable.lockscreen_thumb_camera
        NotiTargets.CONVERSATION -> R.drawable.lockscreen_thumb_voice
        NotiTargets.PHRASEBOOK -> R.drawable.lockscreen_thumb_phrasebook
        else -> R.drawable.lockscreen_thumb_text
    }

    /**
     * For the notification's expanded RemoteViews. From API 31 that layout clips the photo to a
     * rounded outline (layout-v31), so it takes the plain art. Below 31 RemoteViews cannot clip at
     * all, so the photo arrives with its corners already cut to transparent.
     */
    @DrawableRes
    fun notificationHero(target: String): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when (target) {
                NotiTargets.CAMERA -> R.drawable.lockscreen_hero_camera
                NotiTargets.CONVERSATION -> R.drawable.lockscreen_hero_voice
                NotiTargets.PHRASEBOOK -> R.drawable.noti_photo_phrasebook
                else -> R.drawable.lockscreen_hero_text
            }
        } else {
            when (target) {
                NotiTargets.CAMERA -> R.drawable.lockscreen_hero_camera_rounded
                NotiTargets.CONVERSATION -> R.drawable.lockscreen_hero_voice_rounded
                NotiTargets.PHRASEBOOK -> R.drawable.lockscreen_hero_phrasebook_rounded
                else -> R.drawable.lockscreen_hero_text_rounded
            }
        }
}
