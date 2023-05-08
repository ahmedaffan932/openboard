package org.dslul.openboard.translator.pro

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.android.synthetic.main.activity_document_translation.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.CustomDialog
import org.dslul.openboard.translator.pro.classes.FileUtils
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.startProActivity
import org.dslul.openboard.translator.pro.classes.admob.InterstitialAd
import java.io.*
import java.util.*


class DocumentTranslationActivity : AppCompatActivity() {
    private val lngSelectorRequestCode = 1230
    private val READ_STORAGE_PERMISSION_REQUEST_CODE = 41
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_translation)

        InterstitialAd.show(this, Misc.documentTranslationInt)
        Firebase.analytics.logEvent("DocumentTranslation", null)

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager())
                        Toast.makeText(this, "We Have Permission", Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(this, "You Denied the permission", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "You Denied the permission", Toast.LENGTH_SHORT).show()
                }
            }


        llLngFromFrag.setOnClickListener {
            val intent = Intent(this, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorRequestCode)
        }

        llLngToFrag.setOnClickListener {
            startActivityForResult(
                Intent(
                    this, LanguageSelectorActivity::class.java
                ), lngSelectorRequestCode
            )
        }

        tvTranslateFile.setOnClickListener {
            getDocument()
        }

        ivDocuments.setOnClickListener {
            getDocument()
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnSwitchLngs.setOnClickListener {
            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 150
                rotate.interpolator = LinearInterpolator()
                val image = btnSwitchLngs
                image.startAnimation(rotate)

                Misc.zoomOutView(llLngToFrag, this, 150)
                Misc.zoomOutView(llLngFromFrag, this, 150)

                Handler().postDelayed({
                    val temp = Misc.getLanguageFrom(this)
                    Misc.setLanguageFrom(this, Misc.getLanguageTo(this))
                    Misc.setLanguageTo(this, temp)

                    setSelectedLng()

                    Misc.zoomInView(llLngToFrag, this, 150)
                    Misc.zoomInView(llLngFromFrag, this, 150)

                }, 150)
            } else {
                Toast.makeText(
                    this, "Please select language you want to translate from.", Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            textViewLngFromFrag.text = "Detect"
            flagFromFrag.setImageResource(Misc.getFlag(this, "100"))
        } else {
            textViewLngFromFrag.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName

            flagFromFrag.setImageResource(Misc.getFlag(this, Misc.getLanguageFrom(this)))
        }

        flagToFrag.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))
        textViewLngToFrag.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val uri = data?.data
            val path = FileUtils.getPath(this, uri!!)
            val file = File(path)
            if (file.exists()) {
                startTranslationActivity(path.toString())
//                when (file.extension) {
//                    "txt" -> {
//                        readTextFile(file)
//                    }
//                    "pdf" -> {
//                        try {
//                            var parsedText = ""
//                            Log.d(Misc.logKey, "Uri ${file.absolutePath}")
//                            val pdfInputStream = FileInputStream(file)
//                            val reader = PdfReader(pdfInputStream)
//                            val n: Int = reader.numberOfPages
//                            for (i in 0 until n) {
//                                parsedText = """$parsedText${
//                                    PdfTextExtractor.getTextFromPage(reader, i + 1)
//                                        .trim { it <= ' ' }
//                                }""".trimIndent() //Extracting the content from the different pages
//                            }
//                            startTranslationActivity(parsedText)
//                            reader.close()
//                        } catch (e: Exception) {
//                            e.printStackTrace()
//                        }
//                    }
//                    "docx" -> {
//                        Toast.makeText(
//                            this,
//                            "Please convert Word file to PDF and the translate.",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                    else -> {
//                        Toast.makeText(
//                            this,
//                            "This file type can not be translated yet, We are working on it.",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
            } else {
                Log.d(Misc.logKey, "File not exist.")
            }
        }
    }

    private fun getDocument() {
        if (checkPermissionForReadExternalStorage()) {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, 101)
        } else {
            requestPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
        Misc.isDocumentTranslationActivity = true
    }


    private fun checkPermissionForReadExternalStorage(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val readCheck: Int =
                ContextCompat.checkSelfPermission(applicationContext, READ_EXTERNAL_STORAGE)
            val writeCheck: Int =
                ContextCompat.checkSelfPermission(applicationContext, WRITE_EXTERNAL_STORAGE)
            readCheck == PackageManager.PERMISSION_GRANTED && writeCheck == PackageManager.PERMISSION_GRANTED
        }
    }

    private val permissions = arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AlertDialog.Builder(this)
                .setTitle("Permission")
                .setMessage("Please give the Storage permission")
                .setPositiveButton(
                    android.R.string.yes
                ) { dialog, which ->
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        intent.addCategory("android.intent.category.DEFAULT")
                        intent.data = Uri.parse(
                            String.format(
                                "package:%s", *arrayOf<Any>(
                                    applicationContext.packageName
                                )
                            )
                        )
                        activityResultLauncher.launch(intent)
                    } catch (e: java.lang.Exception) {
                        val intent = Intent()
                        intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                        activityResultLauncher.launch(intent)
                    }
                }
                .setCancelable(false)
                .show()
        } else {
            ActivityCompat.requestPermissions(this, permissions, 30)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDocument()
            }
        }
    }



    private fun startTranslationActivity(string: String) {
        if (Misc.getPurchasedStatus(this) || string.length < 3000) {
            val intent = Intent(this, DetailedDocumentTranslationActivity::class.java)
            intent.putExtra(Misc.text, string)
            Log.d(Misc.logKey, "Path $string")
            startActivity(intent)
        } else {
            val objCustomDialog = CustomDialog(this)
            objCustomDialog.show()

            val window: Window = objCustomDialog.window!!
            window.setLayout(
                WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawableResource(R.color.color_nothing)

            objCustomDialog.findViewById<TextView>(R.id.tvTitle).text =
                "Your document is too long Upgrade to premium to translate it."
            objCustomDialog.findViewById<TextView>(R.id.btnYes).text = "Upgrade"
            objCustomDialog.findViewById<TextView>(R.id.btnNo).text = "May be later."
            objCustomDialog.setCancelable(true)

            objCustomDialog.findViewById<TextView>(R.id.btnYes).setOnClickListener {
                this.startProActivity(Misc.data)
                objCustomDialog.dismiss()
            }

            objCustomDialog.findViewById<TextView>(R.id.btnNo).setOnClickListener {
                objCustomDialog.dismiss()
            }

        }
    }

    override fun onPause() {
        super.onPause()
        Misc.isDocumentTranslationActivity = false
    }
}