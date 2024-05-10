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

class SplashFragment : Fragment() {
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
        view.findViewById<TextView>(R.id.tvHeading).text = when (param1.toString()) {
            "1" -> {
                "Translator keyboard for any app"
            }

            "2" -> {
                "Translate everything with camera"
            }

            else -> {
                "Speak & Translate in 50+ Languages."
            }
        }

        view.findViewById<TextView>(R.id.tvText).text = when (param1.toString()) {
            "1" -> {
                "Get instant translation right in your favourite messaging app."
            }

            "2" -> {
                "Just take take a photo of any object and get a fast and accurate translation"
            }

            else -> {
                "Easily translate text, photos or voice in over 50 languages"
            }
        }

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
            SplashFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}