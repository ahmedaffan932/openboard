package org.dslul.openboard.translator.pro

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityFragmentsDashboardBinding
import org.dslul.openboard.translator.pro.fragments.CameraFragment
import org.dslul.openboard.translator.pro.fragments.ChatFragment
import org.dslul.openboard.translator.pro.fragments.HomeFragment
import org.dslul.openboard.translator.pro.fragments.PhrasesFragment

class FragmentsDashboardActivity : AppCompatActivity() {
    lateinit var binding: ActivityFragmentsDashboardBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFragmentsDashboardBinding.inflate(layoutInflater)
//        enableEdgeToEdge()
        setContentView(binding.root)

        binding.bottomNavigation.menu
        val fragmentManager = supportFragmentManager

        setCurrentFragment(fragmentManager, HomeFragment())

        binding.bottomNavigation.selectedItemId = R.id.home

        var lastSelectedItem = R.id.home
        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            if (it.itemId != lastSelectedItem) {
                when (it.itemId) {
                    R.id.home -> setCurrentFragment(fragmentManager, HomeFragment())
                    R.id.camera -> setCurrentFragment(fragmentManager, CameraFragment())
                    R.id.chat -> setCurrentFragment(fragmentManager, ChatFragment())
                    R.id.phrasebook -> setCurrentFragment(fragmentManager, PhrasesFragment())
                }
                lastSelectedItem = it.itemId
            }
            true
        }

    }

    private fun setCurrentFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        for (frag in fragmentManager.fragments) {

            if (fragment.javaClass.name == frag.javaClass.name) {
                fragmentManager.beginTransaction().apply {
                    fragmentManager.fragments.forEach {
                        if (it != frag) {
                            hide(it)
                        }
                    }
                    show(frag)
                    commit()
                }
                return
            }
        }



        fragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, fragment)
            addToBackStack("")
            commit()
        }
    }

}