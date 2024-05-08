package org.dslul.openboard.translator.pro.adaptor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.guru.translate.translator.pro.translation.keyboard.translator.R
import org.dslul.openboard.translator.pro.classes.Misc
import java.util.*
import kotlin.collections.ArrayList

class OCRResultAdapter(
    private val context: Context,
    private val arrText: ArrayList<String>,
    private val arrTranslation: ArrayList<String>
) : RecyclerView.Adapter<OCRResultAdapter.ViewHolder>() {

    class ViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvText: TextView = itemView.findViewById(R.id.tvText)
        val btnCopyText: View = itemView.findViewById(R.id.btnCopyText)
        val btnSpeakText: View = itemView.findViewById(R.id.btnSpeakText)
        val tvTranslation: TextView = itemView.findViewById(R.id.tvTranslation)
        val btnCopyTranslation: View = itemView.findViewById(R.id.btnCopyTranslation)
        val btnSpeakTranslation: View = itemView.findViewById(R.id.btnSpeakTranslation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.ocr_result_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvText.text = arrText[position]
        holder.tvTranslation.text = arrTranslation[position]

        holder.btnCopyText.setOnClickListener {
            val clipboard: ClipboardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Camera Translator",
                holder.tvText.text
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        }

        holder.btnCopyTranslation.setOnClickListener {
            val clipboard: ClipboardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Camera Translator",
                holder.tvTranslation.text
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        }

        holder.btnSpeakTranslation.setOnClickListener {
            var textToSpeechLngFrom: TextToSpeech? = null
            textToSpeechLngFrom = TextToSpeech(context) { i ->
                if (i == TextToSpeech.ERROR) {
                    Toast.makeText(
                        context,
                        "Sorry! Your device does not support this language.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    textToSpeechLngFrom?.language = Locale(Misc.getLanguageTo(context))

                    textToSpeechLngFrom?.speak(
                        holder.tvTranslation.text.toString(),
                        TextToSpeech.QUEUE_FLUSH,
                        null
                    )
                }
            }
        }

        holder.btnSpeakText.setOnClickListener {
            var textToSpeechLngTo: TextToSpeech? = null
            textToSpeechLngTo = TextToSpeech(context) { i ->
                if (i == TextToSpeech.ERROR) {
                    Toast.makeText(
                        context,
                        "Sorry! Your device does not support this language.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    textToSpeechLngTo?.language = Locale(Misc.getLanguageFrom(context))
                    textToSpeechLngTo?.speak(
                        holder.tvText.text.toString(),
                        TextToSpeech.QUEUE_FLUSH,
                        null
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return arrText.size
    }
}