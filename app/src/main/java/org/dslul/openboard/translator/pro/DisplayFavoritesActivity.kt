package org.dslul.openboard.translator.pro

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.admob.InterstitialAd
import kotlinx.android.synthetic.main.activity_display_favorites.*
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.adaptor.FavoritesAdapter

class DisplayFavoritesActivity : AppCompatActivity() {
    lateinit var adapter: FavoritesAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_favorites)

        InterstitialAd.show(this, Misc.favoritesIntAm)

        if (Misc.getFavorites(this).size == 0) {
            tvNoFavorites.visibility = View.VISIBLE
            animLoading.visibility = View.VISIBLE
        }

        btnBackFavorites.setOnClickListener {
            onBackPressed()
        }

        recyclerViewFavorites.layoutManager = LinearLayoutManager(this)
        adapter = FavoritesAdapter(Misc.getFavorites(this), this)
        recyclerViewFavorites.adapter = adapter

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