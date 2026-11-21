package org.dslul.openboard.translator.pro.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import org.dslul.openboard.inputmethod.latin.R

private const val ARG_PARAM1 = "param1"

class OnBaordingFragment : Fragment() {
    private var param1: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_splash, container, false)
        val (title, text) = when (param1.toString()) {
            "1" -> R.string.ui_launch_onboarding_title_2 to R.string.ui_launch_onboarding_text_2
            "2" -> R.string.ui_launch_onboarding_title_3 to R.string.ui_launch_onboarding_text_3
            else -> R.string.ui_launch_onboarding_title_1 to R.string.ui_launch_onboarding_text_1
        }
        view.findViewById<TextView>(R.id.tvHeading).setText(title)
        view.findViewById<TextView>(R.id.tvText).setText(text)

        view.findViewById<LottieAnimationView>(R.id.animation).setAnimation(
            when (param1.toString()) {
                "1" -> {
                    "anim_keyboard_translate.json"
                }

                "2" -> {
                    "anim_camera_translate.json"
                }

                else -> {
                    "splash_languages.json"
                }
            }
        )
        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String) =
            OnBaordingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}