package org.dslul.openboard.translator.pro.widget

import android.app.Dialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.DialogInterface
import android.graphics.Outline
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.DialogWidgetPromptBinding
import org.dslul.openboard.inputmethod.latin.databinding.ItemWidgetPromptStepBinding
import org.dslul.openboard.translator.pro.classes.App
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.Ads
import java.util.function.Consumer
import kotlin.math.min

/**
 * The "Add widget" card: a frosted-glass dialog over the dashboard that explains the three manual
 * steps (long-press the home screen, tap Widgets, drag the widget), with a shared-bottom native ad
 * as its bottom section. Shown once, by [WidgetPrompt.showIfDue].
 *
 * "Got it" opens the launcher's system pin dialog ([AppWidgetManager.requestPinAppWidget]) directly
 * (xtech gates that behind a rewarded ad; openboard does not). The glass is Android 12's window
 * background blur, which blurs only what is behind the card's own rounded bounds; without it
 * (API < 31, or blur switched off at runtime) the card falls back to an opaque grey that reads the
 * same.
 */
class WidgetPromptDialog : DialogFragment() {

    private var binding: DialogWidgetPromptBinding? = null
    private var requestPinAfterDismiss = false

    @RequiresApi(Build.VERSION_CODES.S)
    private var blurListener: Consumer<Boolean>? = null

    companion object {
        const val TAG = "widgetPrompt"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.WidgetPromptDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            // Back and the close button dismiss; a stray tap on the dim does not, since the ad
            // sits at the card's bottom edge.
            setCanceledOnTouchOutside(false)
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        requireActivity().setAppLanguage()
        return DialogWidgetPromptBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = binding ?: return
        val radius = resources.getDimension(R.dimen.widget_prompt_corner_radius)
        // Clips the native ad's bottom corners to the card. android:clipToOutline in XML only
        // takes effect from API 31; set in code it works everywhere.
        b.root.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) =
                outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
        b.root.clipToOutline = true

        bindStep(b.step1, 1, R.string.widget_prompt_step1_title, R.string.widget_prompt_step1_desc, R.drawable.ic_widget_step_touch)
        bindStep(b.step2, 2, R.string.widget_prompt_step2_title, R.string.widget_prompt_step2_desc, R.drawable.ic_widget_step_grid)
        bindStep(b.step3, 3, R.string.widget_prompt_step3_title, R.string.widget_prompt_step3_desc, R.drawable.ic_widget_step_drag)

        b.btnWidgetPromptClose.setOnClickListener { dismiss() }
        b.btnWidgetPromptGotIt.setOnClickListener {
            requestPinAfterDismiss = true
            dismissAllowingStateLoss()
        }

        Ads.loadAndShowSharedBottomNativeAd(requireActivity(), b.widgetPromptAd)
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        val metrics = resources.displayMetrics
        val margin = resources.getDimensionPixelSize(R.dimen.widget_prompt_side_margin)
        val maxWidth = resources.getDimensionPixelSize(R.dimen.widget_prompt_max_width)
        window.setLayout(min(metrics.widthPixels - 2 * margin, maxWidth), ViewGroup.LayoutParams.WRAP_CONTENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Called at once with the current state, then whenever it changes.
            val listener = Consumer<Boolean> { enabled -> applyGlass(window, enabled) }
            blurListener = listener
            window.windowManager.addCrossWindowBlurEnabledListener(listener)
        } else {
            applyGlass(window, blur = false)
        }
    }

    override fun onResume() {
        super.onResume()
        // The user followed the steps without tapping "Got it" and came back.
        if (WidgetPrompt.isWidgetOnHomeScreen(requireContext())) {
            dismissAllowingStateLoss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!requestPinAfterDismiss) return
        requestPinAfterDismiss = false
        // After the card's window is gone, so the launcher's dialog is not stacked under it.
        val activity = activity ?: return
        activity.window.decorView.post { requestPin(activity) }
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurListener?.let { dialog?.window?.windowManager?.removeCrossWindowBlurEnabledListener(it) }
            blurListener = null
        }
        super.onStop()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun requestPin(activity: FragmentActivity) {
        if (activity.isFinishing || activity.isDestroyed) return
        val manager = AppWidgetManager.getInstance(activity)
        // Not every launcher supports pinning. Skip (and don't arm resume-ad suppression) when it
        // doesn't, so a request that never hands off to a system screen can't leave the suppression
        // armed for a later, unrelated app return.
        if (!manager.isRequestPinAppWidgetSupported) return
        try {
            // The pin dialog is a system screen; don't pop a resume app-open ad when we come back.
            App.suppressNextResumeAd()
            manager.requestPinAppWidget(
                ComponentName(activity, TranslateWidgetProvider::class.java),
                null,
                null,
            )
        } catch (e: Exception) {
            // Some launchers reject the request; the card's steps still work there by hand. The
            // suppression window self-expires, so no cleanup is needed here.
            e.printStackTrace()
        }
    }

    private fun applyGlass(window: Window, blur: Boolean) {
        window.setBackgroundDrawableResource(
            if (blur) R.drawable.bg_widget_prompt_glass else R.drawable.bg_widget_prompt_solid
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(
                if (blur) resources.getDimensionPixelSize(R.dimen.widget_prompt_blur_radius) else 0
            )
        }
    }

    private fun bindStep(
        step: ItemWidgetPromptStepBinding,
        number: Int,
        @StringRes title: Int,
        @StringRes description: Int,
        @DrawableRes icon: Int,
    ) {
        // The app's own UI locale (Arabic gets Arabic-Indic digits), not the system one.
        step.widgetStepNumber.text = String.format(resources.configuration.locales[0], "%d", number)
        step.widgetStepTitle.setText(title)
        step.widgetStepDesc.setText(description)
        step.widgetStepIcon.setImageResource(icon)
    }
}
