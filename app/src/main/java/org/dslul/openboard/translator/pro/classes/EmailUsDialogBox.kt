package org.dslul.openboard.translator.pro.classes

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import com.guru.translate.translator.pro.translation.keyboard.translator.R

class EmailUsDialogBox(var c: Activity) : Dialog(c), View.OnClickListener {
    var d: Dialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.email_us_dialog)
    }

    override fun onClick(v: View) {

    }
}