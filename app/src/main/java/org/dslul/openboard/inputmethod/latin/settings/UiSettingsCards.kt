package org.dslul.openboard.inputmethod.latin.settings

import android.app.Activity
import android.app.Fragment
import android.app.FragmentManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.graphics.drawable.ColorDrawable
import android.preference.PreferenceCategory
import android.view.View
import android.view.ViewGroup
import android.widget.HeaderViewListAdapter
import android.widget.ListAdapter
import android.widget.ListView
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc

/**
 * Keyboard settings cards (Figma TranslatorProZyro section 09): every run of preferences between
 * two categories is drawn as one rounded card with hairline dividers, the categories as labels
 * above. The framework preference list has no hook for that, so this wraps the list's adapter and
 * gives each row the right slice (single / top / middle / bottom) of the card.
 *
 * Item positions are unchanged by the wrapper, so PreferenceScreen's click handling (which reads
 * its own root adapter) keeps working.
 */
object UiSettingsCards {

    /**
     * Installs the cards on every preference list [activity] shows. Call before the activity's
     * super.onCreate so its first fragment is covered.
     */
    @JvmStatic
    fun attach(activity: Activity) {
        activity.fragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?
                ) {
                    (v.findViewById<View>(android.R.id.list) as? ListView)?.let { install(it) }
                }
            }, true)
    }

    /**
     * [base] with the app's own Dark theme switch (Misc.isNightModeOn) applied. These framework
     * PreferenceActivities aren't AppCompat, so AppCompatDelegate's night mode never reaches
     * them; call from attachBaseContext so their values-night resources follow the app.
     */
    @JvmStatic
    fun withAppNightMode(base: Context): Context {
        val night = if (Misc.isNightModeOn(base)) Configuration.UI_MODE_NIGHT_YES
            else Configuration.UI_MODE_NIGHT_NO
        val config = Configuration(base.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        return base.createConfigurationContext(config)
    }

    /** Call once the fragment's view exists; the adapter is wrapped whenever it is (re)bound. */
    @JvmStatic
    fun install(list: ListView) {
        val density = list.resources.displayMetrics.density
        list.divider = null
        list.dividerHeight = 0
        // Rows carry their own ripple; the list's selector would draw behind the opaque cards.
        list.selector = ColorDrawable(Color.TRANSPARENT)
        list.setPadding(0, (8 * density).toInt(), 0, (28 * density).toInt())
        list.clipToPadding = false
        list.scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        wrap(list)
        // PreferenceFragment binds (and may re-bind) the adapter after its view is created.
        list.viewTreeObserver.addOnGlobalLayoutListener { wrap(list) }
    }

    private fun wrap(list: ListView) {
        val adapter = list.adapter ?: return
        if (adapter is CardsAdapter || adapter is HeaderViewListAdapter) return
        val position = list.firstVisiblePosition
        val top = list.getChildAt(0)?.top ?: 0
        list.adapter = CardsAdapter(adapter)
        list.setSelectionFromTop(position, top)
    }

    private class CardsAdapter(private val base: ListAdapter) : ListAdapter by base {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = base.getView(position, convertView, parent)
            if (isCategory(position)) return view
            val first = position == 0 || isCategory(position - 1)
            val last = position == count - 1 || isCategory(position + 1)
            val background = when {
                first && last -> R.drawable.ui_kbs_row_single
                first -> R.drawable.ui_kbs_row_top
                last -> R.drawable.ui_kbs_row_bottom
                else -> R.drawable.ui_kbs_row_middle
            }
            // The card's inset reports itself as padding; keep the row's own padding.
            val l = view.paddingLeft
            val t = view.paddingTop
            val r = view.paddingRight
            val b = view.paddingBottom
            view.setBackgroundResource(background)
            view.setPadding(l, t, r, b)
            return view
        }

        private fun isCategory(position: Int) = base.getItem(position) is PreferenceCategory
    }
}
