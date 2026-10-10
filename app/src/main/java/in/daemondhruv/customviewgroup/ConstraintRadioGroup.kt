package `in`.daemondhruv.customviewgroup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import androidx.constraintlayout.widget.ConstraintLayout

class ConstraintRadioGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var checkedId = View.NO_ID
    private var protectFromCheckedChange = false
    private var onCheckedChangeListener: ((ConstraintRadioGroup, Int) -> Unit)? = null

    private val childCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (protectFromCheckedChange || !isChecked) return@OnCheckedChangeListener

            protectFromCheckedChange = true
            if (checkedId != View.NO_ID && checkedId != buttonView.id) {
                findViewById<RadioButton>(checkedId)?.isChecked = false
            }
            protectFromCheckedChange = false

            checkedId = buttonView.id
            onCheckedChangeListener?.invoke(this, checkedId)
        }

    override fun onFinishInflate() {
        super.onFinishInflate()
        wireRadioButtons(this)
    }

    fun setOnCheckedChangeListener(listener: ((ConstraintRadioGroup, Int) -> Unit)?) {
        onCheckedChangeListener = listener
    }

    private fun wireRadioButtons(parent: ViewGroup) {
        for (i in 0 until parent.childCount) {
            when (val child = parent.getChildAt(i)) {
                is RadioButton -> {
                    if (child.id == View.NO_ID) child.id = View.generateViewId()
                    if (child.isChecked) checkedId = child.id
                    child.setOnCheckedChangeListener(childCheckedChangeListener)
                }
                is ViewGroup -> wireRadioButtons(child)
            }
        }
    }
}
