package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityFragmentsDashboardBinding
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.fragments.CameraFragment
import org.dslul.openboard.translator.pro.fragments.ChatFragment
import org.dslul.openboard.translator.pro.fragments.HomeFragment
import org.dslul.openboard.translator.pro.fragments.PhrasesFragment

class FragmentsDashboardActivity : AppCompatActivity() {
    lateinit var binding: ActivityFragmentsDashboardBinding
    var lastSelectedItem = R.id.home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityFragmentsDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.menu

        setCurrentFragment(HomeFragment())

        binding.bottomNavigation.selectedItemId = R.id.home

        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            if (it.itemId != lastSelectedItem) {
                when (it.itemId) {
                    R.id.home -> setCurrentFragment(HomeFragment())
                    R.id.camera -> setCurrentFragment(CameraFragment())
                    R.id.chat -> setCurrentFragment(ChatFragment())
                    R.id.phrasebook -> setCurrentFragment(PhrasesFragment())
                }
                lastSelectedItem = it.itemId
            }
            true
        }

    }

    private fun setCurrentFragment(fragment: Fragment) {
//        for (frag in fragmentManager.fragments) {
//
//            if (fragment.javaClass.name == frag.javaClass.name) {
//                fragmentManager.beginTransaction().apply {
//                    fragmentManager.fragments.forEach {
//                        if (it != frag) {
//                            hide(it)
//                        }
//                    }
//                    show(frag)
//                    commit()
//                }
//                return
//            }
//        }

        val fragmentManager = supportFragmentManager

        fragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer, fragment)
            addToBackStack("")
            commit()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (lastSelectedItem == R.id.home) {
            startActivity(Intent(this, ExitActivity::class.java))
        }else{
            binding.bottomNavigation.selectedItemId = R.id.home
//            setCurrentFragment(HomeFragment())
        }
    }

}