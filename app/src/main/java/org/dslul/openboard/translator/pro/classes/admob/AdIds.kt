package org.dslul.openboard.translator.pro.classes.admob

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import org.dslul.openboard.inputmethod.latin.BuildConfig

object AdIds {

    var bannerAdIdAdOne = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111"
    } else {
        "ca-app-pub-6814505709397727/9113781148"
    }

    var appOpenAdIdOne = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/3419835294"
    } else {
        "ca-app-pub-6814505709397727/2248951904"
    }

    var nativeAdIdAdMobOne: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        "ca-app-pub-6814505709397727/1291093454"
    }

    var nativeAdIdAdMobSplash: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        "ca-app-pub-6814505709397727/3725685104"
    }

    var interstitialAdIdAdMobSplash: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        "ca-app-pub-6814505709397727/6566087979"
    }

    var interstitialAdIdAdMobOne: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        "ca-app-pub-6814505709397727/1853366084"
    }

}