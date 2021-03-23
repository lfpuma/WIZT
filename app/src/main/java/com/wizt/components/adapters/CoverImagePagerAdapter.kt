package com.wizt.components.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.wizt.fragments.CoverImageDetailFragment
import com.wizt.models.Image

// 1
class CoverImagePagerAdapter(fragmentManager: FragmentManager, private val images: ArrayList<Image>,private val animFrag: Fragment) :
    FragmentStatePagerAdapter(fragmentManager) {

    // 2
    override fun getItem(position: Int): Fragment {
        return CoverImageDetailFragment.newInstance(images[position], images.size, position, animFrag)
    }

    // 3
    override fun getCount(): Int {
        return images.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return images[position].id
    }
}