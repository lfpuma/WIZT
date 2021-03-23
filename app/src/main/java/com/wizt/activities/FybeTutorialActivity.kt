package com.wizt.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.crashlytics.android.Crashlytics
import com.wizt.R
import com.wizt.activities.auth.LoginActivity
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_fybe_tutorial.*
import kotlinx.android.synthetic.main.content_tutorial.view.*

class FybeTutorialActivity : AppCompatActivity() {

    companion object {
        val circleImagesID = intArrayOf(
            R.id.circle1,
            R.id.circle2,
            R.id.circle3,
            R.id.circle4)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_fybe_tutorial)

        btnFinish.setOnClickListener {
            finish()
        }

        val lists = listOf(
            R.drawable.screen_6_1,
            R.drawable.screen_6_2,
            R.drawable.screen_6_3,
            R.drawable.screen_6_4
        )
        val adapter = TutorialPagerAdapter(lists) { gotoMainPage() }
        viewPager.adapter = adapter
        viewPager.setPageTransformer(true,ZoomOutPageTransformer())
        viewPager.addOnPageChangeListener (object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }
            override fun onPageSelected(position: Int) {

                for(objID in circleImagesID) {
                    val circleV = findViewById<View>(objID)
                    circleV.setBackgroundResource(R.drawable.circle_detail)
                }

                findViewById<View>(circleImagesID[position]).setBackgroundResource(R.drawable.circlesel_tutorial)
            }

        })
    }

    private fun gotoMainPage() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    class TutorialPagerAdapter(private val list: List<Int>, private val onClickListener: () -> Unit) : PagerAdapter() {


        override fun isViewFromObject(v: View, `object`: Any): Boolean {
            return v === `object` as View
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val view = LayoutInflater.from(container.context).inflate(R.layout.content_tutorial, container, false)
            val sqRes = BitmapFactory.decodeResource(container.context.resources, list[position])
            val res = RoundedBitmapDrawableFactory.create(container.context.resources,sqRes)
            res.cornerRadius = 20.0f
            view.imageView.setImageDrawable(res)
            container.addView(view)

            return view
        }

        override fun destroyItem(parent: ViewGroup, position: Int, `object`: Any) {
            parent.removeView(`object` as View)
        }

    }

    class ZoomOutPageTransformer : ViewPager.PageTransformer {

        companion object {
            private const val MIN_SCALE = 0.85f
            private const val MIN_ALPHA = 0.5f
        }

        override fun transformPage(view: View, position: Float) {
            view.apply {
                val pageWidth = width
                val pageHeight = height
                when {
                    position < -1 -> { // [-Infinity,-1)
                        // This page is way off-screen to the left.
                        alpha = 0f
                    }
                    position <= 1 -> { // [-1,1]
                        // Modify the default slide transition to shrink the page as well
                        val scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position))
                        val vertMargin = pageHeight * (1 - scaleFactor) / 2
                        val horzMargin = pageWidth * (1 - scaleFactor) / 2
                        translationX = if (position < 0) {
                            horzMargin - vertMargin / 2
                        } else {
                            horzMargin + vertMargin / 2
                        }

                        // Scale the page down (between MIN_SCALE and 1)
                        scaleX = scaleFactor
                        scaleY = scaleFactor

                        // Fade the page relative to its size.
                        alpha = (MIN_ALPHA +
                                (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA)))
                    }
                    else -> { // (1,+Infinity]
                        // This page is way off-screen to the right.
                        alpha = 0f
                    }
                }
            }
        }
    }
}
