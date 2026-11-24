package org.dslul.openboard.translator.pro.learn

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ItemLearnOptionBinding
import org.dslul.openboard.translator.pro.classes.Misc
import java.util.Locale

/** Small view helpers shared by the Learn screens. */
object LearnUi {

    fun dp(context: Context, value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics).toInt()

    /** The language's name in the app language, capitalised ("Spanish", "Español"…). */
    fun languageName(context: Context, code: String): String {
        val appLocale = Locale(Misc.getAppLanguage(context))
        val name = LearnSpeech.localeFor(code).getDisplayLanguage(appLocale).ifBlank { code }
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(appLocale) else it.toString() }
    }

    @DrawableRes
    fun flag(context: Context, code: String): Int = try {
        Misc.getFlag(context, code)
    } catch (e: Exception) {
        R.drawable.ic_ui_globe
    }

    fun unitTitle(context: Context, unit: Int): String {
        val id = context.resources.getIdentifier("learn_unit_$unit", "string", context.packageName)
        return if (id != 0) context.getString(id) else context.getString(R.string.learn_lesson_n, unit)
    }

    fun color(context: Context, res: Int) = ContextCompat.getColor(context, res)

    /** A styled TextView without an XML layout (word tiles, keys, match tiles, week days). */
    fun text(context: Context, style: Int, text: CharSequence): TextView =
        TextView(context, null, 0, style).apply { this.text = text }

    fun haptic(view: View, correct: Boolean, enabled: Boolean) {
        if (!enabled) return
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (correct) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
        } else {
            if (correct) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }

    /** Inflates an item_learn_option row into [parent] and returns its binding. */
    fun option(parent: ViewGroup): ItemLearnOptionBinding =
        ItemLearnOptionBinding.inflate(LayoutInflater.from(parent.context), parent, true)

    fun setOptionSelected(b: ItemLearnOptionBinding, selected: Boolean) {
        val ctx = b.root.context
        b.root.setBackgroundResource(if (selected) R.drawable.bg_learn_option_selected else R.drawable.bg_ui_card_r16_clickable)
        b.tvTitle.setTextColor(color(ctx, if (selected) R.color.home_accent else R.color.home_text_primary))
        b.ivRadio.setImageResource(if (selected) R.drawable.ui_radio_on else R.drawable.ui_radio_off)
    }

    fun flagInto(view: ImageView, code: String) {
        view.setImageResource(flag(view.context, code))
    }

    /**
     * The app's centred dialog card (bg_ui_dialog): icon, title, message, primary button and a
     * text button. Returns the dialog, already shown.
     */
    fun messageDialog(
        activity: Activity,
        @DrawableRes icon: Int,
        iconTint: Int,
        @DrawableRes iconBackground: Int,
        title: String,
        message: String,
        primary: String,
        secondary: String?,
        secondaryDanger: Boolean = false,
        onPrimary: () -> Unit,
        onSecondary: () -> Unit = {},
        extraButton: Pair<String, () -> Unit>? = null,
    ): Dialog {
        val dialog = Dialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_learn_message, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        view.findViewById<View>(R.id.flIcon).setBackgroundResource(iconBackground)
        view.findViewById<ImageView>(R.id.ivIcon).apply {
            setImageResource(icon)
            setColorFilter(color(activity, iconTint))
        }
        view.findViewById<TextView>(R.id.tvTitle).text = title
        view.findViewById<TextView>(R.id.tvMessage).text = message
        view.findViewById<TextView>(R.id.btnPrimary).apply {
            text = primary
            setOnClickListener { dialog.dismiss(); onPrimary() }
        }
        view.findViewById<TextView>(R.id.btnExtra).apply {
            if (extraButton == null) visibility = View.GONE else {
                text = extraButton.first
                setOnClickListener { dialog.dismiss(); extraButton.second() }
            }
        }
        view.findViewById<TextView>(R.id.btnSecondary).apply {
            if (secondary == null) visibility = View.GONE else {
                text = secondary
                if (secondaryDanger) setTextColor(color(activity, R.color.home_danger))
                setOnClickListener { dialog.dismiss(); onSecondary() }
            }
        }
        if (!activity.isFinishing && !activity.isDestroyed) dialog.show()
        return dialog
    }

    fun setVisible(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun horizontalRow(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }
}
