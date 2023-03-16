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
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.SplashScreenActivity
import org.dslul.openboard.translator.pro.classes.CustomDialog
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import java.util.*

@SuppressLint("StaticFieldLeak")
class LanguagesAdapter(
    private var languages: ArrayList<String>,
    private val lngTo: Boolean,
    private val activity: Activity,
    private val callback: InterstitialCallBack? = null
) :
    RecyclerView.Adapter<LanguagesAdapter.LanguageHolder>(), Filterable {

    val tempLanguages = languages

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                Misc.setPurchasedStatus(activity, true)
                Log.d(Misc.logKey, "Ya ho.....")
                Toast.makeText(activity, "Restarting Application.", Toast.LENGTH_SHORT).show()
                activity.startActivity(Intent(activity, SplashScreenActivity::class.java))
                activity.finish()
            }
        }

    private lateinit var billingClient: BillingClient

    var speak: TextToSpeech? = null
    var isBillingResultOk = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageHolder {
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
                    // The BillingClient is ready. You can query purchases here.
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(Misc.logKey, "Service disconnected")
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
        val view = inflater.inflate(R.layout.languages_list, parent, false)
        return LanguageHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LanguageHolder, position: Int) {
        holder.lngLayout.tag = languages.elementAt(position)

        if (!Misc.getPurchasedStatus(activity))
            if ((languages[position] == "he" || languages[position] == "zh")) {
                holder.textViewPremium.visibility = View.VISIBLE
            }
        if (languages.elementAt(position) == Misc.defaultLanguage) {
            holder.languageName.text = "Detect"
            holder.temp.setImageResource(Misc.getFlag(activity, "100"))
            holder.lngLayout.setOnClickListener {
                if (lngTo) {
                    Misc.setLanguageTo(activity, Misc.defaultLanguage)
                } else {
                    Misc.setLanguageFrom(activity, Misc.defaultLanguage)
                }
                activity.onBackPressed()

            }
            holder.btnSpeak.visibility = View.INVISIBLE
        } else {
//            val lngCode = languageCodeForLanguage(languages.elementAt(position))
            holder.languageName.text = Locale(languages[position]).displayName
            holder.temp.setImageResource(Misc.getFlag(activity, languages.elementAt(position)))

            holder.lngLayout.setOnClickListener {
                if ((languages[position] == "he" || languages[position] == "zh") && !Misc.getPurchasedStatus(
                        activity
                    )
                ) {
                    val objCustomDialog = CustomDialog(activity)
                    objCustomDialog.show()

                    val window: Window = objCustomDialog.window!!
                    window.setLayout(
                        WindowManager.LayoutParams.FILL_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT
                    )
                    window.setBackgroundDrawableResource(R.color.color_nothing)

                    objCustomDialog.findViewById<TextView>(R.id.tvTitle).text =
                        "You need to upgrade for this language."
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
                    if (lngTo) {
                        Misc.setLanguageTo(activity, languages.elementAt(position))
                    } else {
                        Misc.setLanguageFrom(activity, languages.elementAt(position))
                    }
                    activity.onBackPressed()
                    callback?.onDismiss()

                    Firebase.analytics.logEvent(Locale(languages[position]).displayName, null)

                    if (callback == null)
                        activity.onBackPressed()
                }
            }

            funSpeak()
            holder.btnSpeak.setOnClickListener {
                speak!!.speak(
                    Locale(languages[position]).displayName,
                    TextToSpeech.QUEUE_FLUSH,
                    null
                )
            }

        }

    }

    private suspend fun querySkuDetails() {
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Not available yet.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    private fun funSpeak() {
        speak = TextToSpeech(
            activity.applicationContext
        ) { i ->
            if (i != TextToSpeech.ERROR) {
                speak?.language = Locale.ENGLISH
            } else {
                Log.d(Misc.logKey, TextToSpeech.ERROR.toString())
            }
        }
    }

    override fun getItemCount(): Int {
        return languages.size
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    open class LanguageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
                    if (constraint?.let {
                            Locale(item).displayName.toLowerCase().contains(it)
                        } == true) {
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