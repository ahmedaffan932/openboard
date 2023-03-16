package org.dslul.openboard.translator.pro.adaptor

import android.app.Activity
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import org.dslul.openboard.inputmethod.latin.R
import com.example.translatorguru.classes.ConversationClass
import com.example.translatorguru.classes.Misc
import java.util.*
import kotlin.collections.ArrayList

class ConversationAdapter(
    private val activity: Activity,
    private val conversation: ArrayList<ConversationClass>
) : RecyclerView.Adapter<ConversationAdapter.MessageHolder>() {
    private var textToSpeech: TextToSpeech? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.conversation_item, parent, false)
        return MessageHolder(view)
    }

    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        if (conversation[position].isRight) {
            speak(conversation[position].translation, conversation[position].translatedTo)

            holder.llConversationLeft.visibility = View.GONE
            holder.textViewCRText.text = conversation[position].text
            Misc.zoomInView(holder.llConversationRight, activity, 150)
            holder.textViewCRTranslation.text = conversation[position].translation
        } else {
            speak(conversation[position].translation, conversation[position].translatedFrom)

            holder.llConversationRight.visibility = View.GONE
            holder.textViewCLText.text = conversation[position].text
            Misc.zoomInView(holder.llConversationLeft, activity, 150)
            holder.textViewCLTranslation.text = conversation[position].translation
        }

        holder.textViewCRTranslation.setOnClickListener {
            speak(conversation[position].translation, conversation[position].translatedTo)
        }

        holder.textViewCLTranslation.setOnClickListener {
            speak(conversation[position].translation, conversation[position].translatedFrom)
        }

    }

    override fun getItemCount(): Int {
        return conversation.size
    }

    private fun speak(text: String, lng: String) {
        textToSpeech = TextToSpeech(
            activity.applicationContext
        ) { i ->
            if (i != TextToSpeech.ERROR) {
                textToSpeech?.language = Locale(lng)
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            } else {
                Toast.makeText(
                    activity,
                    "Sorry! Your device does not support this language.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    open class MessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewCLText: TextView = itemView.findViewById(R.id.textViewCLText)
        val textViewCRText: TextView = itemView.findViewById(R.id.textViewCRText)
        val llConversationLeft: LinearLayout = itemView.findViewById(R.id.llConversationLeft)
        val llConversationRight: LinearLayout = itemView.findViewById(R.id.llConversationRight)
        val textViewCLTranslation: TextView = itemView.findViewById(R.id.textViewCLTranslation)
        val textViewCRTranslation: TextView = itemView.findViewById(R.id.textViewCRTranslation)
    }
}