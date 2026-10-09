package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.dslul.openboard.inputmethod.latin.R

data class LockscreenContent(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("desc") val desc: String = "",
    @SerializedName("button") val button: String = "",
    @SerializedName("target") val target: String = NotiTargets.TRANSLATE
)

object NotiContentStore {

    /**
     * The lock-screen notification's rotation: the Remote Config JSON
     * override (`noti_lockscreen_content`) when present and valid, else the bundled localized
     * items. RC content is single-language by nature; the bundled items localize into every
     * supported locale.
     *
     * The camera, voice and text items read the Figma copy ("Lockscreen 1-3"); the text item keeps
     * the design's document art and CTA without its PDF promise - the app has no PDF translation.
     * Phrasebook, which the design has no frame for, keeps its own copy. Ids are stable, so the
     * persisted never-twice-in-a-row id always means the same item.
     */
    fun lockscreenContent(context: Context): List<LockscreenContent> =
        remoteOverride() ?: listOf(
            LockscreenContent(
                1,
                context.getString(R.string.noti_ls_card_title_camera),
                context.getString(R.string.noti_ls_card_desc_camera),
                context.getString(R.string.noti_ls_card_btn_try),
                NotiTargets.CAMERA
            ),
            LockscreenContent(
                2,
                context.getString(R.string.noti_ls_card_title_voice),
                context.getString(R.string.noti_ls_card_desc_voice),
                context.getString(R.string.noti_ls_card_btn_try),
                NotiTargets.CONVERSATION
            ),
            LockscreenContent(
                3,
                context.getString(R.string.noti_ls_title_3),
                context.getString(R.string.noti_ls_desc_3),
                context.getString(R.string.noti_ls_btn_3),
                NotiTargets.PHRASEBOOK
            ),
            LockscreenContent(
                5,
                context.getString(R.string.noti_ls_card_title_text),
                context.getString(R.string.noti_ls_card_desc_text),
                context.getString(R.string.noti_ls_card_btn_translate),
                NotiTargets.TRANSLATE
            )
        )

    private fun remoteOverride(): List<LockscreenContent>? {
        if (NotiConfig.lockscreenContentJson.isBlank()) return null
        return try {
            // Gson writes an explicit JSON null straight into these non-null Kotlin fields (it
            // bypasses the constructor defaults), so every field is normalised before anyone
            // calls a String method on it.
            @Suppress("USELESS_CAST", "USELESS_ELVIS")
            val parsed = Gson()
                .fromJson(NotiConfig.lockscreenContentJson, Array<LockscreenContent>::class.java)
                ?.filterNotNull()
                ?.map {
                    it.copy(
                        title = (it.title as String?).orEmpty(),
                        desc = (it.desc as String?).orEmpty(),
                        button = (it.button as String?).orEmpty(),
                        target = (it.target as String?) ?: NotiTargets.TRANSLATE
                    )
                }
                ?.filter { it.title.isNotBlank() && it.button.isNotBlank() }
            if (parsed.isNullOrEmpty()) return null
            // Missing/duplicate ids would void the no-consecutive-repeat rule
            // (all default to 0) - reassign positional ids when that happens.
            val ids = parsed.map { it.id }
            if (ids.any { it <= 0 } || ids.toSet().size != parsed.size) {
                parsed.mapIndexed { i, item -> item.copy(id = i + 1) }
            } else {
                parsed
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Spec rotation rule: random per slot, never the same content in two consecutive sends. */
    fun pickRandomExcluding(list: List<LockscreenContent>, lastId: Int): LockscreenContent {
        val candidates = list.filter { it.id != lastId }
        return candidates.ifEmpty { list }.random()
    }
}
