package org.dslul.openboard.translator.pro


import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.activity_settings.*
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.EmailUsDialogBox
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.rateUs
import org.dslul.openboard.translator.pro.classes.RateUsDialog

class SettingsActivity : AppCompatActivity() {

    @SuppressLint("QueryPermissionsNeeded")
    //@DelicateCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_settings)


        Misc.isActivityCreatingFirstTime = true


        if (Misc.selectThemeMode(this)) {
            btnSwitchDarkTheme.isChecked = true
        }

        llShareApp.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Have a look to this interesting application:- \n \n${Misc.appUrl}"
            )
            startActivity(Intent.createChooser(sharingIntent, "Share via"))
        }

        llPrivacyPolicy.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://sites.google.com/view/elite-translator/translatorpro")
            )
            startActivity(intent)
        }

        llTermsAndConditions.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://sites.google.com/view/elite-translator/translatorpro-term-conditions")
            )
            startActivity(intent)
        }

        btnSwitchDarkTheme.setOnCheckedChangeListener { _, _ ->
            if (Misc.isNightModeOn(this)) {
                Misc.setNightMode(this, false)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                Misc.setNightMode(this, true)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }

        llFeedback.setOnClickListener {
            val objEmailUsDialog = EmailUsDialogBox(this)
            objEmailUsDialog.show()
            val window: Window = objEmailUsDialog.window!!
            window.setLayout(
                WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawableResource(R.color.color_nothing)
            objEmailUsDialog.findViewById<ConstraintLayout>(R.id.btnPublishFeedback)
            objEmailUsDialog.findViewById<ConstraintLayout>(R.id.btnPublishFeedback)
                .setOnClickListener {
                    val sub = objEmailUsDialog.findViewById<TextView>(R.id.etTopic).text.toString()
                    val i = Intent(Intent.ACTION_SEND)
                    i.type = "message/rfc822"
                    i.putExtra(Intent.EXTRA_EMAIL, arrayOf("elitetranslatorapps@gmail.com"))
                    i.putExtra(
                        Intent.EXTRA_TEXT,
                        objEmailUsDialog.findViewById<EditText>(R.id.etFeedbackBody).text
                    )
                    i.putExtra(Intent.EXTRA_SUBJECT, sub)
                    i.setPackage("com.google.android.gm")

                    try {
                        startActivity(Intent.createChooser(i, "Send mail..."))
                        objEmailUsDialog.dismiss()
                    } catch (ex: ActivityNotFoundException) {
                        Toast.makeText(
                            this,
                            "Some error occurred in sending email.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            objEmailUsDialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
                objEmailUsDialog.dismiss()
            }
        }

        llRateUs.setOnClickListener {
            val objRateUsDialog = RateUsDialog(this)
            objRateUsDialog.show()
            val window: Window = objRateUsDialog.window!!
            window.setLayout(
                WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawableResource(R.color.color_nothing)

            objRateUsDialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
                objRateUsDialog.dismiss()
            }
            objRateUsDialog.findViewById<ImageView>(R.id.btnRateA).setOnClickListener {
                objRateUsDialog.dismiss()
                Toast.makeText(this, "Thanks.", Toast.LENGTH_SHORT).show()
            }
            objRateUsDialog.findViewById<ImageView>(R.id.btnRateB).setOnClickListener {
                objRateUsDialog.dismiss()
                Toast.makeText(this, "Thanks.", Toast.LENGTH_SHORT).show()
            }
            objRateUsDialog.findViewById<ImageView>(R.id.btnRateC).setOnClickListener {
                objRateUsDialog.dismiss()
                Toast.makeText(this, "Thanks.", Toast.LENGTH_SHORT).show()
            }
            objRateUsDialog.findViewById<ImageView>(R.id.btnRateD).setOnClickListener {
                objRateUsDialog.dismiss()
                rateUs()
            }
            objRateUsDialog.findViewById<ImageView>(R.id.btnRateE).setOnClickListener {
                objRateUsDialog.dismiss()
                rateUs()
            }
            objRateUsDialog.findViewById<ConstraintLayout>(R.id.btnRateUs).setOnClickListener {
                objRateUsDialog.dismiss()
                rateUs()
            }
        }
    }



    override fun onBackPressed() {
        if (intent.getStringExtra(Misc.data) != null) {
            startActivity(Intent(this, TranslateActivity::class.java))
            finish()
        } else {
            super.onBackPressed()
        }
    }

}