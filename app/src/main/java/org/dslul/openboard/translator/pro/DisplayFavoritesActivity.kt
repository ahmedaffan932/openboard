package org.dslul.openboard.translator.pro

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.dslul.openboard.translator.pro.classes.Misc

import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityDisplayFavoritesBinding
import org.dslul.openboard.translator.pro.adaptor.FavoritesAdapter
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage

class DisplayFavoritesActivity : AppCompatActivity() {
    lateinit var adapter: FavoritesAdapter
    private lateinit var binding: ActivityDisplayFavoritesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityDisplayFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (Misc.getFavorites(this).size == 0) {
            showNoFavorites()
        }

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        // Favorites opens from History, so going back lands there.
        binding.btnOpenHistory.setOnClickListener {
            onBackPressed()
        }

        binding.recyclerViewFavorites.layoutManager = LinearLayoutManager(this)
        adapter = FavoritesAdapter(Misc.getFavorites(this), this) { showNoFavorites() }
        binding.recyclerViewFavorites.adapter = adapter

    }

    private fun showNoFavorites() {
        binding.llEmptyState.visibility = View.VISIBLE
        binding.recyclerViewFavorites.visibility = View.GONE
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