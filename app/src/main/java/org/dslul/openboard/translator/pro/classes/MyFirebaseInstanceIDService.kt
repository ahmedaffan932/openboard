package org.dslul.openboard.translator.pro.classes

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService


class MyFirebaseInstanceIDService: FirebaseMessagingService() {
    override fun onNewToken(p0: String) {
        Log.d(Misc.logKey, p0)

        super.onNewToken(p0)
    }

    companion object{


    }

}