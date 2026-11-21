package org.dslul.openboard.translator.pro

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.dslul.openboard.inputmethod.latin.R
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

        // The "long-press to delete" hint sits above the cards (it used to be a toast).
        if (Misc.getHistory(this).size == 0) {
            showNoHistory()
        } else {
            binding.llHistoryHint.visibility = View.VISIBLE
        }

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnFav.setOnClickListener {
            startActivity(Intent(this@DisplayHistoryActivity, DisplayFavoritesActivity::class.java))
        }

        // Back to where the user came from, which has the text field.
        binding.btnStartTranslating.setOnClickListener {
            finish()
        }

        // Same cards as the Home tab's Recent list.
        adapter = HistoryAdapter(
            Misc.getHistory(this),
            this,
            object : InterfaceHistory {
                override fun onHistoryCrash() = showNoHistory()

                override fun onHistoryEmptied() = showNoHistory()
            },
            itemLayout = R.layout.history_item_home,
            favoriteIcon = R.drawable.ic_home_heart_filled,
            notFavoriteIcon = R.drawable.ic_home_heart_outline
        )
        binding.recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewHistory.adapter = adapter
    }

    private fun showNoHistory() {
        binding.llEmptyState.visibility = View.VISIBLE
        binding.llHistoryHint.visibility = View.GONE
        binding.recyclerViewHistory.visibility = View.GONE
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
