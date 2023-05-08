package org.dslul.openboard.translator.pro

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.android.synthetic.main.activity_preview_document.*
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import java.io.*

class PreviewDocumentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_document)

//        textViewTranslatedText.text = intent.getStringExtra(Misc.text)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnCopyTranslatedText.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Camera Translator", textViewTranslatedText.text
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }

        btnShareTranslation.setOnClickListener {
            if (textViewTranslatedText.text != "") {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, textViewTranslatedText.text)
                startActivity(Intent.createChooser(sharingIntent, "Share."))
            }
        }

        getTextFromFile()
    }

    private fun getTextFromFile() {
        val path = intent.getStringExtra(Misc.text).toString()

        Log.d(Misc.logKey, "Path in preview document activity $path")

        val file = File(path)
        when (file.extension) {
            "txt" -> {
                readTextFile(file)
            }
            "pdf" -> {
                try {
                    var parsedText = ""
                    Log.d(Misc.logKey, "Uri ${file.absolutePath}")
                    val pdfInputStream = FileInputStream(file)
                    val reader = PdfReader(pdfInputStream)
                    val n: Int = reader.numberOfPages
                    for (i in 0 until n) {
                        parsedText = """$parsedText${
                            PdfTextExtractor.getTextFromPage(reader, i + 1)
                                .trim { it <= ' ' }
                        }""".trimIndent() //Extracting the content from the different pages
                    }
                    textViewTranslatedText.text = parsedText
                    reader.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            "docx" -> {
                Toast.makeText(
                    this,
                    "Please convert Word file to PDF and the translate.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                Toast.makeText(
                    this,
                    "This file type can not be translated yet, We are working on it.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun readTextFile(file: File) {
        val stringBuilder = StringBuilder()
        try {
            val bufferedReader = BufferedReader(FileReader(file))
            var line = bufferedReader.readLine()
            while (line != null) {
                stringBuilder.append(line).append("\n")
                line = bufferedReader.readLine()
            }
            bufferedReader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val fileContent = stringBuilder.toString()

        if (fileContent.isEmpty()) {
            textViewTranslatedText.text = "Your file is empty"
        } else {
            textViewTranslatedText.text = fileContent
        }
        Log.d(Misc.logKey, "fileContents $fileContent")
    }


}