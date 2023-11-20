package org.dslul.openboard.objects

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity

object CameraMisc {
    private const val cameraFace: String = "cameraFace"
    const val flashConst: String = "flash"
    var fileUri: Uri? = null
    const val uri: String = "uri"
    const val typeGallery: String = "typeGallery"

    fun Context.setFlash(flash: Boolean) {
        val sharedPreferences =
            getSharedPreferences(flashConst, AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(flashConst, flash)
        editor.apply()
    }

    fun Context.getFlash(): Boolean {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences(flashConst, AppCompatActivity.MODE_PRIVATE)
        return sharedPreferences.getBoolean(flashConst, false)
    }
    fun Context.setCameraFace(boolean: Boolean) {
        val sharedPreferences = getSharedPreferences(
            cameraFace,
            AppCompatActivity.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        editor.putBoolean(cameraFace, boolean)
        editor.apply()
    }

    fun Context.getCameraFace(): Boolean {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences(
                cameraFace,
                AppCompatActivity.MODE_PRIVATE
            )
        return sharedPreferences.getBoolean(cameraFace, true)
    }

}