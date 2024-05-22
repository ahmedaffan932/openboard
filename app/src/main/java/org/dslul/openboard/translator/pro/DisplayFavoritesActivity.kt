package org.dslul.openboard.translator.pro

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.dslul.openboard.translator.pro.classes.Misc

import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityDisplayFavoritesBinding
import org.dslul.openboard.translator.pro.adaptor.FavoritesAdapter

class DisplayFavoritesActivity : AppCompatActivity() {
    lateinit var adapter: FavoritesAdapter
    private lateinit var binding: ActivityDisplayFavoritesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisplayFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (Misc.getFavorites(this).size == 0) {
            binding.tvNoFavorites.visibility = View.VISIBLE
            binding.animLoading.visibility = View.VISIBLE
        }

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.recyclerViewFavorites.layoutManager = LinearLayoutManager(this)
        adapter = FavoritesAdapter(Misc.getFavorites(this), this)
        binding.recyclerViewFavorites.adapter = adapter

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