package org.dslul.openboard.translator.pro.adaptor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.*
import org.dslul.openboard.translator.pro.classes.CustomDialog
import org.dslul.openboard.translator.pro.classes.Misc
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.guru.translate.translator.pro.translation.keyboard.translator.R
import org.dslul.openboard.translator.pro.SplashScreenActivity
import java.util.*

class LanguagesAdapterConversation(
    private var languages: ArrayList<String>,
    private val activity: Activity,
    private val bottomSheetId: Int,
    private val view: View

) : RecyclerView.Adapter<LanguagesAdapterConversation.LanguagesHolder>(), Filterable {
    val tempLanguages = languages

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if(billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null){
                Misc.setPurchasedStatus(activity, true)
                Log.d(Misc.logKey, "Ya hooo.....")
                Toast.makeText(activity, "Restarting Application.", Toast.LENGTH_SHORT).show()
                activity.startActivity(Intent(activity, SplashScreenActivity::class.java))
                activity.finish()
            }
        }

    private lateinit var billingClient: BillingClient

    var isBillingResultOk = false
    var speak: TextToSpeech? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguagesHolder {
        val inflater = LayoutInflater.from(parent.context)


        billingClient = BillingClient.newBuilder(activity)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isBillingResultOk = true

                    Log.d(Misc.logKey, "Billing Result Ok")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(Misc.logKey, "Service disconnected")
            }
        })

        val view = inflater.inflate(R.layout.languages_list, parent, false)
        return LanguagesHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LanguagesHolder, position: Int) {
        holder.lngLayout.tag = languages.elementAt(position)

        if (!Misc.getPurchasedStatus(activity))
            if (languages[position] == "he" || languages[position] == "zh") {
                holder.textViewPremium.visibility = View.VISIBLE
            }

//            val lngCode = languageCodeForLanguage(languages.elementAt(position))
        holder.languageName.text = Locale(languages[position]).displayName
        holder.temp.setImageResource(Misc.getFlag(activity, languages.elementAt(position)))

        funSpeak()
        holder.btnSpeak.setOnClickListener {
            speak!!.speak(
                Locale(languages[position]).displayName,
                TextToSpeech.QUEUE_FLUSH,
                null
            )
        }

        holder.lngLayout.setOnClickListener {
            if (view.findViewById<View>(bottomSheetId) != null) {

                if ((languages[position] == "he" || languages[position] == "zh") && !Misc.getPurchasedStatus(activity)
                ) {
                    val objCustomDialog = CustomDialog(activity)
                    objCustomDialog.show()

                    val window: Window = objCustomDialog.window!!
                    window.setLayout(
                        WindowManager.LayoutParams.FILL_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT
                    )
                    window.setBackgroundDrawableResource(R.color.color_nothing)

                    objCustomDialog.findViewById<TextView>(R.id.tvTitle).text = "You need to upgrade for this language."
                    objCustomDialog.findViewById<TextView>(R.id.btnYes).text = "Upgrade"
                    objCustomDialog.findViewById<TextView>(R.id.btnNo).text = "May be later."
                    objCustomDialog.setCancelable(true)

                    objCustomDialog.findViewById<TextView>(R.id.btnYes).setOnClickListener {
                        GlobalScope.launch {
                            querySkuDetails()
                        }
                        objCustomDialog.dismiss()
                    }

                    objCustomDialog.findViewById<TextView>(R.id.btnNo).setOnClickListener {
                        objCustomDialog.dismiss()
                    }


                } else {
                    if (Misc.isLngTo) {
                        Misc.setLanguageTo(activity, languages.elementAt(position))
                    } else {
                        Misc.setLanguageFrom(activity, languages.elementAt(position))
                    }

                    val bottomSheetBehavior =
                        BottomSheetBehavior.from(view.findViewById(bottomSheetId)!!)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }

            }
        }
    }

    private suspend fun querySkuDetails() {
        try{
            val skuList = ArrayList<String>()
            skuList.add(Misc.inAppKey)

            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

            // leverage querySkuDetails Kotlin extension function
            val skuDetailsResult = withContext(Dispatchers.IO) {
                billingClient.querySkuDetails(params.build())
            }

            val flowParams = skuDetailsResult.skuDetailsList?.get(0)?.let {
                BillingFlowParams.newBuilder()
                    .setSkuDetails(it)
                    .build()
            }
            val responseCode = flowParams?.let {
                billingClient.launchBillingFlow(
                    activity,
                    it
                ).responseCode
            }

            // Process the result.
        }catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(activity,"Not available yet.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun getItemCount(): Int {
        return languages.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    private fun funSpeak() {
        speak = TextToSpeech(
            activity
        ) { i ->
            if (i != TextToSpeech.ERROR) {
                speak?.language = Locale.ENGLISH
            } else {
                Log.d(Misc.logKey, TextToSpeech.ERROR.toString())
            }
        }
    }

    open class LanguagesHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val languageName: TextView = itemView.findViewById(R.id.languageName)
        val lngLayout: LinearLayout = itemView.findViewById(R.id.lngLayoutActivity)
        val temp: ImageView = itemView.findViewById(R.id.flagView)
        val btnSpeak: ImageView = itemView.findViewById(R.id.btnSpeakTranslation)
        val textViewPremium: TextView = itemView.findViewById(R.id.textViewPremium)
    }


    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val result = FilterResults()
                val founded: MutableList<String> = ArrayList()
                languages = tempLanguages

                for (item in languages) {
                    if (constraint?.let { Locale(item).displayName.toLowerCase().contains(it) } == true) {
                        founded.add(item)
                        Log.d(Misc.logKey, item)
                    }
                }
                result.values = founded;
                result.count = founded.size;

                return result
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                languages = tempLanguages
                languages = results!!.values as ArrayList<String>
                notifyDataSetChanged()

            }

        }

    }


}