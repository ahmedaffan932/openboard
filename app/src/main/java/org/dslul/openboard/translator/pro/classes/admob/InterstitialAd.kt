package org.dslul.openboard.translator.pro.classes.admob

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.interfaces.LoadInterstitialCallBack


object InterstitialAd {
    var isFirstSetRequestSent = false
    var isSecondSetRequestSent = false

    var interAdmobRequestCompleted = false
    var interAdmobOneRequestCompleted = false
    var interAdmobTwoRequestCompleted = false

    var interAdmobRequestSuccess = false
    var interAdmobOneRequestSuccess = false
    var interAdmobTwoRequestSuccess = false

    var interAdmob: com.google.android.gms.ads.interstitial.InterstitialAd? = null
    var interAdmobOne: com.google.android.gms.ads.interstitial.InterstitialAd? = null
    var interAdmobTwo: com.google.android.gms.ads.interstitial.InterstitialAd? = null
    var interAdmobFour: com.google.android.gms.ads.interstitial.InterstitialAd? = null
    var interAdmobThree: com.google.android.gms.ads.interstitial.InterstitialAd? = null

    //load Admob Interstitial
    private fun loadInterAdmob(activity: Activity, callback: LoadInterstitialCallBack? = null) {
        if (!Misc.getPurchasedStatus(activity) && interAdmob == null) {
            val admobRequest = AdRequest.Builder().build()
            com.google.android.gms.ads.interstitial.InterstitialAd.load(
                activity,
                Misc.interstitialAdIdAdMobOne,
                admobRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d("loadAdmob?", adError.message + adError.code)
                        interAdmob = null
                        interAdmobRequestCompleted = true
                        callback?.onFailed()
                    }

                    override fun onAdLoaded(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                        Log.d("loadAdmob?", "Ad was loaded.")
                        interAdmobRequestCompleted = true
                        interAdmobRequestSuccess = true
                        interAdmob = interstitialAd
                        Misc.anyAdLoaded.value = true
                        callback?.onLoaded()
                    }
                })
        } else {
            callback?.onLoaded()
        }
    }

    private fun loadInterAdmobOne(activity: Activity, callback: LoadInterstitialCallBack? = null) {
        if (!Misc.getPurchasedStatus(activity) && interAdmobOne == null) {
            val admobRequest = AdRequest.Builder().build()
            com.google.android.gms.ads.interstitial.InterstitialAd.load(
                activity,
                Misc.interstitialAdIdAdMobTwo,
                admobRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d("loadAdmobOne?", adError.message + adError.code)
                        interAdmobOneRequestCompleted = true
                        interAdmobOne = null
                        callback?.onFailed()
                    }

                    override fun onAdLoaded(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                        Log.d("loadAdmobOne?", "Ad was loaded.")
                        interAdmobOneRequestCompleted = true
                        interAdmobOneRequestSuccess = true
                        interAdmobOne = interstitialAd
                        Misc.anyAdLoaded.value = true
                        callback?.onLoaded()
                    }
                })
        } else {
            callback?.onLoaded()
        }
    }

    private fun loadInterAdmobTwo(activity: Activity, callback: LoadInterstitialCallBack? = null) {
        if (!Misc.getPurchasedStatus(activity) && interAdmobTwo == null) {
            val admobRequest = AdRequest.Builder().build()
            com.google.android.gms.ads.interstitial.InterstitialAd.load(
                activity,
                Misc.interstitialAdIdAdMobThree,
                admobRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d("loadAdmobTwo?", adError.message + adError.code)
                        interAdmobTwo = null
                        interAdmobTwoRequestCompleted = true
                        callback?.onFailed()
                    }

                    override fun onAdLoaded(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                        Log.d("loadAdmobTwo?", "Ad was loaded.")
                        interAdmobTwo = interstitialAd
                        interAdmobTwoRequestCompleted = true
                        Misc.anyAdLoaded.value = true
                        interAdmobTwoRequestSuccess = true
                        callback?.onLoaded()
                    }
                })
        } else {
            callback?.onLoaded()
        }
    }

    private fun loadInterAdmobThree(
        activity: Activity,
        callback: LoadInterstitialCallBack? = null
    ) {
        if (!Misc.getPurchasedStatus(activity) && interAdmobThree == null) {
            val admobRequest = AdRequest.Builder().build()
            com.google.android.gms.ads.interstitial.InterstitialAd.load(
                activity,
                Misc.interstitialAdIdAdMobFour,
                admobRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d("loadAdmobThree?", adError.message + adError.code)
                        interAdmobThree = null
                        callback?.onFailed()
                    }

                    override fun onAdLoaded(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                        Log.d("loadAdmobThree?", "Ad was loaded.")
                        interAdmobThree = interstitialAd
                        callback?.onLoaded()
                    }
                })
        } else {
            callback?.onLoaded()
        }
    }

    private fun loadInterAdmobFour(activity: Activity, callback: LoadInterstitialCallBack? = null) {
        if (!Misc.getPurchasedStatus(activity) && interAdmobFour == null) {
            val admobRequest = AdRequest.Builder().build()
            com.google.android.gms.ads.interstitial.InterstitialAd.load(
                activity,
                Misc.interstitialAdIdAdMobFive,
                admobRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d("loadAdmobFour?", adError.message + adError.code)
                        interAdmobFour = null
                        callback?.onFailed()
                    }

                    override fun onAdLoaded(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                        Log.d("loadAdmobFour?", "Ad was loaded.")
                        interAdmobFour = interstitialAd
                        callback?.onLoaded()
                    }
                })
        } else {
            callback?.onLoaded()
        }
    }

    //load Admob Interstitial
    fun manageLoadInterAdmob(activity: Activity) {
        if (!isFirstSetRequestSent) {
            loadInterAdmob(activity)
            loadInterAdmobOne(activity)
            loadInterAdmobTwo(activity)
            isFirstSetRequestSent = true
            return
        }

        if (!isFirstSetRequestCompleted()) {
            Log.d(Misc.logKey, "FirstSetRequest is not Completed.")
            return
        }

//        if (!isFirstSetRequestSuccess()) {
//            Log.d(Misc.logKey, "All interstitials failed in FirstSetRequest")
//            return
//        }

        if (interAdmob != null || interAdmobOne != null || interAdmobTwo != null) {
            return
        }

        if (!isSecondSetRequestSent) {
            loadInterAdmobThree(activity)
            loadInterAdmobFour(activity)
            isSecondSetRequestSent = true
            return
        }

        loadInterAdmobThree(activity)
        loadInterAdmobFour(activity)
    }


    fun show(activity: Activity, remote: String, callback: (() -> Unit)? = null) {
        if (Misc.getPurchasedStatus(activity)) {
            callback?.invoke()
            return
        }

        if (remote.contains("am")) {
            if (interAdmob != null) {
                showInterstitial(activity, callback)
            } else if (interAdmobOne != null) {
                showInterstitialOne(activity, callback)
            } else if (interAdmobTwo != null) {
                showInterstitialTwo(activity, callback)
            } else if (interAdmobThree != null) {
                showInterstitialThree(activity, callback)
            } else if (interAdmobFour != null) {
                showInterstitialFour(activity, callback)
            } else {
                Log.d("interAdmobShow", "The interstitial ad wasn't ready yet.")
                callback?.invoke()
            }
        } else {
            Log.d("interAdmobShow", "The interstitial ad wasn't ready yet.")
            callback?.invoke()
        }
    }

    private fun showInterstitial(activity: Activity, callback: (() -> Unit)? = null) {
        interAdmob?.show(activity)
        interAdmob?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("interAdmobShow", "Ad was dismissed.")
                interAdmob = null
                callback?.invoke()
                Misc.isInterstitialDisplaying = false
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                Log.d("interAdmobShow", "Ad failed to show." + p0.message + p0.code)
                interAdmob = null
                callback?.invoke()
                Misc.isInterstitialDisplaying = false
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("interAdmobShow", "Ad showed fullscreen content.")
                manageLoadInterAdmob(activity)
                interAdmob = null
                Misc.isInterstitialDisplaying = true
            }
        }
    }

    private fun showInterstitialOne(activity: Activity, callback: (() -> Unit)? = null) {
        interAdmobOne?.show(activity)
        interAdmobOne?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("interAdmobOneShow", "Ad was dismissed.")
                Misc.isInterstitialDisplaying = false
                callback?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                Log.d("interAdmobOneShow", "Ad failed to show." + p0.message + p0.code)
                Misc.isInterstitialDisplaying = false
                callback?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("interAdmobOneShow", "Ad showed fullscreen content.")
                interAdmobOne = null
                Misc.isInterstitialDisplaying = true
                manageLoadInterAdmob(activity)
            }
        }
    }

    private fun showInterstitialTwo(activity: Activity, callback: (() -> Unit)? = null) {
        interAdmobTwo?.show(activity)
        interAdmobTwo?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("interAdmobTwoShow", "Ad was dismissed.")
                Misc.isInterstitialDisplaying = false
                callback?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                Log.d("interAdmobTwoShow", "Ad failed to show." + p0.message + p0.code)
                Misc.isInterstitialDisplaying = false
                callback?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("interAdmobTwoShow", "Ad showed fullscreen content.")
                interAdmobTwo = null
                Misc.isInterstitialDisplaying = true
                manageLoadInterAdmob(activity)
            }
        }
    }

    private fun showInterstitialThree(activity: Activity, callback: (() -> Unit)? = null) {
        interAdmobThree?.show(activity)
        interAdmobThree?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("interAdmobThreeShow", "Ad was dismissed.")
                Misc.isInterstitialDisplaying = false
                callback?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                Log.d("interAdmobThreeShow", "Ad failed to show." + p0.message + p0.code)
                Misc.isInterstitialDisplaying = false
                callback?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("interAdmobThreeShow", "Ad showed fullscreen content.")
                interAdmobThree = null
                Misc.isInterstitialDisplaying = true
                manageLoadInterAdmob(activity)
            }
        }
    }

    private fun showInterstitialFour(activity: Activity, callback: (() -> Unit)? = null) {
        interAdmobFour?.show(activity)
        interAdmobFour?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("interAdmobFourShow", "Ad was dismissed.")
                Misc.isInterstitialDisplaying = false
                callback?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                Log.d("interAdmobFourShow", "Ad failed to show." + p0.message + p0.code)
                Misc.isInterstitialDisplaying = false
                callback?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("interAdmobFourShow", "Ad showed fullscreen content.")
                interAdmobFour = null
                Misc.isInterstitialDisplaying = true
                manageLoadInterAdmob(activity)
            }
        }
    }

    private fun isFirstSetRequestCompleted(): Boolean {
        return interAdmobRequestCompleted && interAdmobOneRequestCompleted && interAdmobTwoRequestCompleted
    }

    private fun isFirstSetRequestSuccess(): Boolean {
        return interAdmobRequestSuccess || interAdmobOneRequestSuccess || interAdmobTwoRequestSuccess
    }

}