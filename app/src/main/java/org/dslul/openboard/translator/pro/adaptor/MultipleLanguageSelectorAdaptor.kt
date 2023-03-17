package org.dslul.openboard.translator.pro.adaptor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.*
import org.dslul.openboard.translator.pro.classes.CustomDialog
import org.dslul.openboard.translator.pro.classes.Misc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.SplashScreenActivity
import org.dslul.openboard.translator.pro.interfaces.OnItemClick
import java.util.*

class MultipleLanguageSelectorAdaptor(
    private val activity: Activity,
    private var allLanguages: ArrayList<String>,
    private var selectedLanguages: ArrayList<String>,
    private val callback: OnItemClick

) : RecyclerView.Adapter<MultipleLanguageSelectorAdaptor.LanguageHolder>() {
    var isBillingResultOk = false
    private lateinit var billingClient: BillingClient

    @Suppress("CAST_NEVER_SUCCEEDS")
    open class LanguageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val languageName: TextView = itemView.findViewById(R.id.languageName)
        val lngLayout: LinearLayout = itemView.findViewById(R.id.lngLayoutActivity)
        val temp: ImageView = itemView.findViewById(R.id.flagView)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
        val textViewPremium: TextView = itemView.findViewById(R.id.textViewPremium)
    }

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
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(Misc.logKey, "Service disconnected")
            }
        })
        val view = inflater.inflate(R.layout.multi_select_language_item, parent, false)
        return LanguageHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LanguageHolder, position: Int) {
        holder.lngLayout.tag = allLanguages.elementAt(position)

        for (selectedLng in selectedLanguages) {
            if (allLanguages[position] == selectedLng) {
                holder.checkbox.isChecked = true
            }
        }

        if (!Misc.getPurchasedStatus(activity))
            if ((allLanguages[position] == "he" || allLanguages[position] == "zh")) {
                holder.textViewPremium.visibility = View.VISIBLE
            }

        holder.languageName.text = Locale(allLanguages[position]).displayName
        holder.temp.setImageResource(Misc.getFlag(activity, allLanguages.elementAt(position)))

        holder.lngLayout.setOnClickListener {
            if ((allLanguages[position] == "he" || allLanguages[position] == "zh") && !Misc.getPurchasedStatus(
                    activity
                )
            ) {
                getPro()
            } else {
                holder.checkbox.isChecked = !holder.checkbox.isChecked
            }

        }

        holder.checkbox.setOnCheckedChangeListener { p0, p1 ->
            if ((allLanguages[position] == "he" || allLanguages[position] == "zh") && !Misc.getPurchasedStatus(
                    activity
                )
            ) {
                getPro()
                holder.checkbox.isChecked = false
                return@setOnCheckedChangeListener
            }
            val arr = Misc.getMultipleSelectedLanguages(activity)
            if (arr.size < 2 && !p1) {
                holder.checkbox.isChecked = true
                val objCustomDialog = CustomDialog(activity)
                objCustomDialog.show()

                val window: Window = objCustomDialog.window!!
                window.setLayout(
                    WindowManager.LayoutParams.FILL_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                window.setBackgroundDrawableResource(R.color.color_nothing)

                objCustomDialog.findViewById<TextView>(R.id.tvTitle).text =
                    "You have to select at least 1 language, You can not unselect last language"
                objCustomDialog.findViewById<TextView>(R.id.btnYes).text = "Ok"
                objCustomDialog.findViewById<TextView>(R.id.btnNo).visibility = View.GONE
                objCustomDialog.setCancelable(true)

                objCustomDialog.findViewById<TextView>(R.id.btnYes).setOnClickListener {
                    objCustomDialog.dismiss()
                }

                objCustomDialog.findViewById<TextView>(R.id.btnNo).setOnClickListener {
                    objCustomDialog.dismiss()
                }
                return@setOnCheckedChangeListener
            }

            if (p1) {
                if (arr.size < 5) {
                    arr.add(allLanguages[position])
                } else {
                    holder.checkbox.isChecked = false
                    val objCustomDialog = CustomDialog(activity)
                    objCustomDialog.show()

                    val window: Window = objCustomDialog.window!!
                    window.setLayout(
                        WindowManager.LayoutParams.FILL_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT
                    )
                    window.setBackgroundDrawableResource(R.color.color_nothing)

                    objCustomDialog.findViewById<TextView>(R.id.tvTitle).text =
                        "You can select 5 languages at on time."
                    objCustomDialog.findViewById<TextView>(R.id.btnYes).text = "Ok"
                    objCustomDialog.findViewById<TextView>(R.id.btnNo).visibility = View.GONE
                    objCustomDialog.setCancelable(true)

                    objCustomDialog.findViewById<TextView>(R.id.btnYes).setOnClickListener {
                        objCustomDialog.dismiss()
                    }

                    objCustomDialog.findViewById<TextView>(R.id.btnNo).setOnClickListener {
                        objCustomDialog.dismiss()
                    }
                }
            } else {
                arr.remove(allLanguages[position])
            }

            Misc.saveMultipleSelectedLanguages(activity, arr)
            callback.onClick()

        }

    }

    private suspend fun querySkuDetails() {
        try {
            val skuList = ArrayList<String>()
            skuList.add(Misc.inAppKey)

            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

            val skuDetailsResult = withContext(Dispatchers.IO) {
                billingClient.querySkuDetails(params.build())
            }

            val flowParams = skuDetailsResult.skuDetailsList?.get(0)?.let {
                BillingFlowParams.newBuilder()
                    .setSkuDetails(it)
                    .build()
            }

            flowParams?.let {
                billingClient.launchBillingFlow(
                    activity,
                    it
                ).responseCode
            }

        } catch (e: Exception) {
            e.printStackTrace()
            try {
                Toast.makeText(activity, "Not available yet.", Toast.LENGTH_SHORT).show()
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int {
        return allLanguages.size
    }

    fun getPro() {
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
    }


}