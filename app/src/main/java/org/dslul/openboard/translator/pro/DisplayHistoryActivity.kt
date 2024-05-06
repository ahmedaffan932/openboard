package org.dslul.openboard.translator.pro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.dslul.openboard.translator.pro.adaptor.HistoryAdapter
import org.dslul.openboard.translator.pro.classes.Misc

import kotlinx.android.synthetic.main.activity_display_history.*
import com.guru.translate.translator.pro.translation.keyboard.translator.R
import org.dslul.openboard.translator.pro.interfaces.InterfaceHistory

class DisplayHistoryActivity : AppCompatActivity() {
    lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_history)

        if (Misc.getHistory(this).size == 0) {
            tvNoHistory.visibility = View.VISIBLE
            animLoading.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "Long press to Remove item from history.", Toast.LENGTH_SHORT)
                .show()
        }

        btnBackHistory.setOnClickListener {
            onBackPressed()
        }

        btnFav.setOnClickListener {
            startActivity(Intent(this@DisplayHistoryActivity, DisplayFavoritesActivity::class.java))
        }

        adapter = HistoryAdapter(Misc.getHistory(this), this, object : InterfaceHistory {
            override fun onHistoryCrash() {
                tvNoHistory.visibility = View.VISIBLE
                animLoading.visibility = View.VISIBLE
                recyclerViewHistory.visibility = View.GONE
            }
        })
        recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        recyclerViewHistory.adapter = adapter
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        adapter.myCallBack()
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.myCallBack()
    }

    override fun onPause() {
        super.onPause()
        adapter.myCallBack()
    }
}