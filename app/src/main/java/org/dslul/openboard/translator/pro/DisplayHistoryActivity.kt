package org.dslul.openboard.translator.pro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.dslul.openboard.translator.pro.adaptor.HistoryAdapter
import org.dslul.openboard.translator.pro.classes.Misc

import org.dslul.openboard.inputmethod.latin.databinding.ActivityDisplayHistoryBinding
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.interfaces.InterfaceHistory

class DisplayHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDisplayHistoryBinding
    lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityDisplayHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Misc.getHistory(this).size == 0) {
            binding.tvNoHistory.visibility = View.VISIBLE
            binding.animLoading.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "Long press to Remove item from history.", Toast.LENGTH_SHORT)
                .show()
        }

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnFav.setOnClickListener {
            startActivity(Intent(this@DisplayHistoryActivity, DisplayFavoritesActivity::class.java))
        }

        adapter = HistoryAdapter(Misc.getHistory(this), this, object : InterfaceHistory {
            override fun onHistoryCrash() {
                binding.tvNoHistory.visibility = View.VISIBLE
                binding.animLoading.visibility = View.VISIBLE
                binding.recyclerViewHistory.visibility = View.GONE
            }
        })
        binding.recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewHistory.adapter = adapter
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