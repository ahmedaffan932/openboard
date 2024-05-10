package org.dslul.openboard.translator.pro.classes

import android.annotation.SuppressLint
import android.app.*
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.blongho.country_data.World
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.nl.translate.TranslateLanguage
import org.dslul.openboard.inputmethod.latin.BuildConfig
import org.dslul.openboard.inputmethod.latin.LatinIME
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.*
import java.lang.reflect.Type
import java.util.Locale
import kotlin.properties.Delegates

object Misc {

    const val text: String = "text"

    var isInterstitialDisplaying = false

    var showInterstitialAfter: Int = 1
    var isLanguageSelectorInBetweenNativeEnabled: Boolean = false

    var proScreenDismissBtnVisibleAfter: Long = 3000
    var isRemoteConfigFetched: MutableLiveData<Boolean> = MutableLiveData()

    @JvmField
    var isTranslated: MutableLiveData<Boolean> = MutableLiveData()
    var proScreen = "1"

    var banner_id = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111"
    } else {
        "abc"
    }

    var lifeTimePrice: String = "Price: $12.99"
    var splashScreenOnBackPressDoNothing: Boolean = true
    var isActivityCreatingFirstTime = true


    const val words: String = "sorted_words"

    const val appUrl: String =
        "https://play.google.com/store/apps/details?id=com.guru.translate.translator.pro.translation.keyboard.translator"
    val arr = ArrayList<String>()
    var storage: FirebaseStorage =
        FirebaseStorage.getInstance()

    var inAppKey = if (BuildConfig.DEBUG) {
        "android.test.purchased"
    } else {
        "com.guru.translate.translator.pro.translation.keyboard.translator"
    }

    const val lngTo = "isLngTo"
    const val key: String = "key"
    const val data: String = "data"
    private const val theme = "theme"
    const val target: String = "target"
    const val source: String = "source"
    const val logKey: String = "logKey"
    const val history: String = "history"
    const val favorites: String = "favorites"
    const val recentLngs: String = "recentLngs"
    const val languageTo: String = "languageTo"
    private const val languageFrom: String = "languageFrom"
    private const val purchasedStatus: String = "purchasedStatus"

    var isLngTo = true
    var canWeProceed = true
    var phrasebookPosition = 0
    const val defaultLanguage = "100"

    fun setAppOpenIntAm(key: String, activity: Context) {
        val sharedPreferences =
            activity.getSharedPreferences("onAppOpenIntAm", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("onAppOpenIntAm", key)
        editor.apply()
    }

    fun getAppOpenIntAm(activity: Activity): String {
        val sharedPreferences =
            activity.getSharedPreferences("onAppOpenIntAm", AppCompatActivity.MODE_PRIVATE)
        return sharedPreferences.getString("onAppOpenIntAm", "am").toString()
    }

    fun setGoogleApi(key: String, activity: Context) {
        val sharedPreferences =
            activity.getSharedPreferences("googleApiKey", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("googleApiKey", key)
        editor.apply()
    }

    fun getGoogleApi(activity: Activity): String {
        val sharedPreferences =
            activity.getSharedPreferences("googleApiKey", AppCompatActivity.MODE_PRIVATE)
        return sharedPreferences.getString("googleApiKey", "").toString()
    }

    fun setNightMode(activity: Activity, isNightMode: Boolean) {
        val sharedPreferences = activity.getSharedPreferences(theme, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(theme, isNightMode)
        editor.apply()
    }

    fun setIsFirstTime(activity: Activity, isFirstTime: Boolean) {
        val sharedPreferences = activity.getSharedPreferences("isFirstTime", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isFirstTime", isFirstTime)
        editor.apply()
    }

    fun isNightModeOn(activity: Context): Boolean {
        val sharedPreferences =
            activity.getSharedPreferences(theme, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(theme, false)
    }

    fun isFirstTime(activity: Activity): Boolean {
        val sharedPreferences =
            activity.getSharedPreferences("isFirstTime", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isFirstTime", true)
    }

    fun selectThemeMode(activity: Context): Boolean {
        return if (isNightModeOn(activity)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            true
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            false
        }
    }

    fun getLanguageTo(activity: Context?): String {
        val sharedPreferences =
            activity!!.getSharedPreferences(languageTo, Context.MODE_PRIVATE)
        return sharedPreferences.getString(languageTo, TranslateLanguage.SPANISH).toString()
    }
    fun getLanguageToForKB(activity: Context?): String {
        val sharedPreferences =
            activity!!.getSharedPreferences(languageTo, Context.MODE_PRIVATE)
        return Locale(sharedPreferences.getString(languageTo, TranslateLanguage.SPANISH).toString()).displayName
    }

    fun getLanguageFrom(activity: Context): String {
        val sharedPreferences =
            activity.getSharedPreferences(languageFrom, Context.MODE_PRIVATE)
        return sharedPreferences.getString(languageFrom, defaultLanguage).toString()
    }

    fun getLanguageFromForKB(activity: Context): String {
        val sharedPreferences =
            activity.getSharedPreferences(languageFrom, Context.MODE_PRIVATE)
        return if (sharedPreferences.getString(languageFrom, defaultLanguage) == defaultLanguage){
            "Default"
        }else {
            Locale(sharedPreferences.getString(languageFrom, defaultLanguage).toString()).displayName
        }
    }

    fun setLanguageTo(activity: Activity, lng: String) {
        val sharedPreferences =
            activity.getSharedPreferences(languageTo, AppCompatActivity.MODE_PRIVATE)

        arr.add(lng)
        for (i in arr) {
            Log.d(logKey, i)
        }

        val editor = sharedPreferences.edit()
        editor.putString(languageTo, lng)
        editor.apply()
        val arr = getRecentLngs(activity)
        arr.remove(lng)
        if (lng != defaultLanguage)
            arr.add(0, lng)
        saveRecentLngs(activity, arr)
    }

    fun setLanguageFrom(activity: Activity, lng: String) {
        val sharedPreferences = activity.getSharedPreferences(
            languageFrom,
            AppCompatActivity.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        editor.putString(languageFrom, lng)
        editor.apply()
        val arr = getRecentLngs(activity)
        arr.remove(lng)
        if (lng != defaultLanguage)
            arr.add(0, lng)
        saveRecentLngs(activity, arr)
    }

    fun setPurchasedStatus(activity: Activity, boolean: Boolean) {
        val sharedPreferences = activity.getSharedPreferences(
            purchasedStatus,
            AppCompatActivity.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        editor.putBoolean(purchasedStatus, boolean)
        editor.apply()
    }

    fun getPurchasedStatus(activity: Context?): Boolean {
        val sharedPreferences =
            activity!!.getSharedPreferences(purchasedStatus, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(purchasedStatus, false)
    }

    fun turnOffNotification(activity: Activity, boolean: Boolean) {
        val sharedPreferences = activity.getSharedPreferences(
            "notification",
            AppCompatActivity.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        editor.putBoolean("notification", boolean)
        editor.apply()
    }

    fun isNotificationTurnedOff(activity: Context?): Boolean {
        val sharedPreferences =
            activity!!.getSharedPreferences("notification", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("notification", false)
    }

    fun getFlag(context: Context, lng: String): Int {
        World.init(context)
        return when (lng) {
            TranslateLanguage.AFRIKAANS -> {
                World.getFlagOf(140)
            }
            TranslateLanguage.ARABIC -> {
                World.getFlagOf(682)
            }
            TranslateLanguage.BELARUSIAN -> {
                R.drawable.by_flag
            }
            TranslateLanguage.BENGALI -> {
                World.getFlagOf(50)
            }
            TranslateLanguage.BULGARIAN -> {
                World.getFlagOf(100)
            }
            TranslateLanguage.CATALAN, TranslateLanguage.SPANISH -> {
                World.getFlagOf(724)
            }
            TranslateLanguage.CZECH -> {
                World.getFlagOf(203)
            }
            TranslateLanguage.WELSH -> {
                World.getFlagOf(826)
            }
            TranslateLanguage.DANISH -> {
                World.getFlagOf(276)
            }
            TranslateLanguage.GREEK -> {
                World.getFlagOf(300)
            }
            TranslateLanguage.GERMAN -> {
                World.getFlagOf(276)
            }
            TranslateLanguage.ENGLISH -> {
                R.drawable.us_flag
            }
            TranslateLanguage.ESPERANTO -> {
                World.getFlagOf(826)
            }
            TranslateLanguage.ESTONIAN -> {
                World.getFlagOf(372)
            }
            TranslateLanguage.FINNISH -> {
                World.getFlagOf(246)
            }
            TranslateLanguage.FRENCH -> {
                World.getFlagOf(250)
            }
            TranslateLanguage.IRISH -> {
                World.getFlagOf(372)
            }
            TranslateLanguage.GALICIAN -> {
                World.getFlagOf(40)
            }
            TranslateLanguage.PERSIAN -> {
                World.getFlagOf(364)
            }
            TranslateLanguage.HEBREW -> {
                World.getFlagOf(376)
            }
            TranslateLanguage.CROATIAN -> {
                World.getFlagOf(191)
            }
            TranslateLanguage.HAITIAN_CREOLE -> {
                World.getFlagOf(332)
            }
            TranslateLanguage.HUNGARIAN -> {
                World.getFlagOf(348)
            }
            TranslateLanguage.INDONESIAN -> {
                World.getFlagOf(458)
            }
            TranslateLanguage.ICELANDIC -> {
                World.getFlagOf(352)
            }
            TranslateLanguage.ITALIAN -> {
                World.getFlagOf(380)
            }
            TranslateLanguage.JAPANESE -> {
                World.getFlagOf(392)
            }
            TranslateLanguage.GEORGIAN -> {
                R.drawable.ic_georgia
            }
            TranslateLanguage.HINDI, TranslateLanguage.TELUGU, TranslateLanguage.KANNADA, TranslateLanguage.TAMIL, TranslateLanguage.GUJARATI, TranslateLanguage.MARATHI -> {
                World.getFlagOf(356)
            }
            TranslateLanguage.KOREAN -> {
                R.drawable.kr_flag
            }
            TranslateLanguage.LITHUANIAN -> {
                World.getFlagOf(440)
            }
            TranslateLanguage.LATVIAN -> {
                World.getFlagOf(428)
            }
            TranslateLanguage.MACEDONIAN -> {
                World.getFlagOf(807)
            }
            TranslateLanguage.MALAY -> {
                World.getFlagOf(458)
            }
            TranslateLanguage.MALTESE -> {
                World.getFlagOf(470)
            }
            TranslateLanguage.NORWEGIAN -> {
                World.getFlagOf(578)
            }
            TranslateLanguage.DUTCH -> {
                World.getFlagOf(528)
            }
            TranslateLanguage.POLISH -> {
                World.getFlagOf(616)
            }
            TranslateLanguage.PORTUGUESE -> {
                World.getFlagOf(620)
            }
            TranslateLanguage.ROMANIAN -> {
                World.getFlagOf(642)
            }
            TranslateLanguage.RUSSIAN -> {
                World.getFlagOf(643)
            }
            TranslateLanguage.SLOVAK -> {
                World.getFlagOf(703)
            }
            TranslateLanguage.SLOVENIAN -> {
                World.getFlagOf(705)
            }
            TranslateLanguage.ALBANIAN -> {
                World.getFlagOf(8)
            }
            TranslateLanguage.SWEDISH -> {
                World.getFlagOf(752)
            }
            TranslateLanguage.SWAHILI -> {
                World.getFlagOf(404)
            }
            TranslateLanguage.UKRAINIAN -> {
                World.getFlagOf(804)
            }
            TranslateLanguage.THAI -> {
                World.getFlagOf(764)
            }
            TranslateLanguage.TAGALOG -> {
                World.getFlagOf(608)
            }
            TranslateLanguage.TURKISH -> {
                World.getFlagOf(792)
            }
            TranslateLanguage.URDU -> {
                World.getFlagOf(586)
            }
            TranslateLanguage.VIETNAMESE -> {
                World.getFlagOf(704)
            }
            TranslateLanguage.CHINESE -> {
                World.getFlagOf(156)
            }
            "100" -> {
                com.blongho.country_data.R.drawable.globe
            }
            else -> World.getFlagOf(lng)
        }
    }

    @SuppressLint("MissingPermission")
    fun checkInternetConnection(activity: Context): Boolean {
        return try {
            val connectivityManager: ConnectivityManager? =
                activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

            connectivityManager!!.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state === NetworkInfo.State.CONNECTED ||
                    connectivityManager!!.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state === NetworkInfo.State.CONNECTED
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getRecentLngs(activity: Activity): ArrayList<String> {
        val sharedPreferences: SharedPreferences =
            activity.getSharedPreferences(recentLngs, AppCompatActivity.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString(recentLngs, null)
        val type: Type = object : TypeToken<ArrayList<String>>() {}.type
        var arrRecentLngs: ArrayList<String>? = gson.fromJson(json, type)

        if (arrRecentLngs == null) {
            arrRecentLngs = ArrayList()
        }

        return arrRecentLngs
    }

    fun saveRecentLngs(activity: Activity, arrRecentLngs: ArrayList<String>) {
        val sharedPreferences =
            activity.getSharedPreferences(recentLngs, AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences!!.edit()
        val gson = Gson()
        val json = gson.toJson(arrRecentLngs)
        editor.putString(recentLngs, json)
        editor.apply()
    }

    fun saveMultipleSelectedLanguages(activity: Activity, arrRecentLngs: ArrayList<String>) {
        val sharedPreferences =
            activity.getSharedPreferences("multiLanguages", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences!!.edit()
        val gson = Gson()
        val json = gson.toJson(arrRecentLngs)
        editor.putString("multiLanguages", json)
        editor.apply()
    }

    fun getMultipleSelectedLanguages(activity: Activity): ArrayList<String> {
        val sharedPreferences: SharedPreferences =
            activity.getSharedPreferences("multiLanguages", AppCompatActivity.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("multiLanguages", null)
        val type: Type = object : TypeToken<ArrayList<String>>() {}.type
        var arrRecentLngs: ArrayList<String>? = gson.fromJson(json, type)

        if (arrRecentLngs == null) {
            arrRecentLngs = ArrayList()
            arrRecentLngs.add(TranslateLanguage.ARABIC)
            arrRecentLngs.add(TranslateLanguage.SPANISH)
        }



        return arrRecentLngs
    }


    fun getHistory(activity: Activity): ArrayList<TranslateHistoryClass> {
        val sharedPreferences: SharedPreferences =
            activity.getSharedPreferences(history, AppCompatActivity.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString(history, null)
        val type: Type = object : TypeToken<ArrayList<TranslateHistoryClass>>() {}.type
        var historyArrayList: ArrayList<TranslateHistoryClass>? = gson.fromJson(json, type)

        if (historyArrayList == null) {
            historyArrayList = ArrayList()
        }

        return historyArrayList
    }

    fun getFavorites(activity: Activity): ArrayList<TranslateHistoryClass> {
        val sharedPreferences: SharedPreferences =
            activity.getSharedPreferences(favorites, AppCompatActivity.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString(favorites, null)
        val type: Type = object : TypeToken<ArrayList<TranslateHistoryClass>>() {}.type
        var arrayList: ArrayList<TranslateHistoryClass>? = gson.fromJson(json, type)

        if (arrayList == null) {
            arrayList = ArrayList()
        }

        return arrayList
    }

    fun zoomInView(view: View, activity: Activity, duration: Int) {
        view.visibility = View.VISIBLE
        val a: Animation =
            AnimationUtils.loadAnimation(activity, R.anim.zoom_in)
        a.duration = duration.toLong()
        view.startAnimation(a)
    }


    fun zoomInViewOvershootInterpolator(view: View, activity: Activity, duration: Int) {
        val a: Animation =
            AnimationUtils.loadAnimation(activity, R.anim.zoom_in)
        a.duration = duration.toLong()
        a.interpolator = OvershootInterpolator()
        view.startAnimation(a)
    }


    fun zoomOutView(view: View, activity: Activity, duration: Int) {
        val a: Animation =
            AnimationUtils.loadAnimation(activity, R.anim.zoom_out)
        a.duration = duration.toLong()
        view.startAnimation(a)
    }

    fun getThemeColor(color: Int, activity: Activity): Int {
        val typedValue = TypedValue()
        val theme: Resources.Theme = activity.theme
        theme.resolveAttribute(color, typedValue, true)
        return ContextCompat.getColor(activity, typedValue.resourceId)
    }

    class LoadingAdDialog
        (var c: Activity) : Dialog(c), View.OnClickListener {
        var d: Dialog? = null
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.loading_ads_dialog)
        }

        override fun onClick(v: View) {}
    }


    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    lateinit var builder: Notification.Builder
    private val channelId = BuildConfig.APPLICATION_ID
    private val description = "Translate All Languages."

    fun Context.isInputMethodSelected(): Boolean {
        val id: String = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        val defaultInputMethod = ComponentName.unflattenFromString(id)
        val myInputMethod = ComponentName(this, LatinIME::class.java)
        return myInputMethod == defaultInputMethod
    }
    fun Context.rateUs() {
        val p = "com.guru.translate.translator.pro.translation.keyboard.translator"
        val uri: Uri = Uri.parse("market://details?id=$p")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)

        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=$p")
                )
            )
        }
    }

}