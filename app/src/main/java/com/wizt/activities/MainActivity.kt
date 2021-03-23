package com.wizt.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AlertDialog
import android.transition.ChangeImageTransform
import android.transition.Fade
import android.transition.TransitionInflater
import android.transition.TransitionSet
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.google.android.gms.ads.MobileAds
import com.wizt.common.constants.Constants
import com.wizt.components.bottomNavigationView.BottomNavigationViewEx
import com.wizt.fragments.*
import com.wizt.R
import com.wizt.activities.animationfrag.GalleryFragment
import com.wizt.common.base.BaseActivity
import com.wizt.common.http.APIManager
import com.wizt.components.activityImprove.IOnBackPressed
import com.wizt.models.Global
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.nav_header_main.view.*

@Suppress("DEPRECATION")
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        const val TAG = "WIZT:MainActivity"
        const val MOVE_DEFAULT_TIME:Long = 1000
        const val FADE_DEFAULT_TIME:Long = 300
        const val ANIMATION_DELAY_TIME = 1200L
    }

    private lateinit var viewPager: SwipeLockableViewPager
    private lateinit var pagerAdapter: ViewPagerAdapter
    private lateinit var navigationBar: BottomNavigationViewEx
    private lateinit var floatBtn: FloatingActionButton
    private lateinit var mDrawerLayout: DrawerLayout

    private var userName: TextView? = null
    private var userEmail: TextView? = null

    // Bottom Bar Fragments
    val homeFragment: HomeFragment = HomeFragment.newInstance()
    val scanFragment: ScanFragment = ScanFragment.newInstance()
    val newLabelFragment: NewLabelFragment = NewLabelFragment.newInstance()
    val shareLabelFragment: ShareLabelFragment = ShareLabelFragment.newInstance()
    val floorPlanFragment: FloorPlanFragment = FloorPlanFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this, getString(R.string.adsPubID))

        containerFL.visibility = View.GONE
        viewPager = findViewById(R.id.viewPager)
        pagerAdapter = ViewPagerAdapter(supportFragmentManager, this)
        viewPager.adapter = pagerAdapter

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
                Log.d(TAG,"state -> " + state)

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }
            override fun onPageSelected(position: Int) {
                Log.d(TAG,"selectedPageView Position -> " + position)
                navigationBar.setCurrentItem(position)
            }

        })

        PreferenceUtils.saveBoolean(Constants.PREF_IS_FLOOR_PLAN_STATE, false)

        // Bottom Bar
        navigationBar = findViewById(R.id.bottom_bar)
        navigationBar.enableItemShiftingMode(false)
        navigationBar.enableShiftingMode(false)
        navigationBar.enableAnimation(false)
        navigationBar.setOnNavigationItemSelectedListener(object :
            BottomNavigationView.OnNavigationItemSelectedListener {
            private var previousPosition = -1

            override fun onNavigationItemSelected(p0: MenuItem): Boolean {
                checkSubscription()
                val position: Int = when (p0.itemId) {
                    R.id.menu_home -> 0
                    R.id.menu_scan -> 1
                    R.id.menu_add -> 2
                    R.id.menu_share -> 3
                    R.id.menu_floorplan -> 4
                    else -> return false
                }
                if (previousPosition != position) {
                    viewPager.setCurrentItem(position, false)
                    previousPosition = position
                }
                return true
            }
        })

        floatBtn = findViewById(R.id.fab)
        floatBtn.setOnClickListener {
            viewPager.currentItem = 2
        }

        setupPermissions()
        initSideMenu()

        //initHomeScreen()
    }

    fun initHomeScreen() {

        viewPager.visibility = View.GONE
        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerFL, HomeFragment.newInstance())
            .addToBackStack(null)
            .commit()
        containerFL.visibility = View.VISIBLE

    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(ProfileFragment.TAG, "Permission to denied")
            makeRequest()
        }

    }


    private fun makeRequest() {

        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE),
            101)
    }

    override fun onBackPressed() {

        val fragment = this.supportFragmentManager.findFragmentById(R.id.containerFL)
        (fragment as? IOnBackPressed)?.onBackPressed()?.not()?.let {
            return
        }

        PreferenceUtils.saveBoolean(Constants.PREF_HOME_IS_FOCUS, false)
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START)
        }

        if (supportFragmentManager.backStackEntryCount >= 0) {
            popFragment()
        } else {
            super.onBackPressed()
        }

        if ( containerFL.visibility != View.VISIBLE )
        {
            if(viewPager.currentItem == 0) {
                Log.d(TAG, " Fragment -> " + "homeFragment")
                val Frag = viewPager.adapter!!.instantiateItem(viewPager,0) as? HomeFragment
                Frag!!.loadFirst()
            }
            else if (viewPager.currentItem == 3) {
                Log.d(TAG, " Fragment -> " + "shareFragment")
                val Frag = viewPager.adapter!!.instantiateItem(viewPager,3) as? ShareLabelFragment
                Frag!!.loadFirst()
            }
        }

    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        mDrawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * SideMenu Methods
     */
    fun initSideMenu() {
        // SideMenu
        mDrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Initialize HeaderView of SideMenu
        val headerView = navView.getHeaderView(0)
        val avatarUrl = PreferenceUtils.getString(Constants.PROFILE_USER_PICTURE_AVATAR)
        if(!avatarUrl.isEmpty())
            Glide.with(applicationContext).load(avatarUrl).into(headerView.profile_image)
        else {
            headerView.profile_image.setImageResource(R.drawable.ic_default_avatar)
        }
        userName = headerView.findViewById(R.id.nameTV)
        userName?.text = PreferenceUtils.getString(Constants.PROFILE_USER_NAME)
        userEmail = headerView.findViewById(R.id.emailTV)
        userEmail?.text = PreferenceUtils.getString(Constants.PROFILE_USER_EMAIL)
        navView.setNavigationItemSelectedListener(this)
        val headerSideMenuView = navView.getHeaderView(0)
        headerSideMenuView.profileBtn.setOnClickListener {
            replaceFragment(ProfileFragment.newInstance())
            PreferenceUtils.saveBoolean(Constants.PREF_HOME_IS_FOCUS, true)
        }
        headerSideMenuView.friendBtn.setOnClickListener {
            replaceFragment(FriendFragment.newInstance())
        }
        headerSideMenuView.shareBtn.setOnClickListener {
            replaceFragment(ShareLogFragment.newInstance())
        }
        headerSideMenuView.friendRequestBtn.setOnClickListener {
            replaceFragment(FriendRequestFragment.newInstance())
        }
        headerSideMenuView.tutorialReplyBtn.setOnClickListener {
            //closeSideMenu()
            val intent = Intent(applicationContext, TutorialActivity::class.java)
            intent.putExtra(Constants.PREF_TUTORIAL_REQUIRE,true)
            startActivity(intent)
        }
        headerSideMenuView.closeBtn.setOnClickListener {
            closeSideMenu()
        }
    }

    fun openSideMenu() {
        mDrawerLayout.openDrawer(Gravity.START)
    }

    private fun closeSideMenu() {
        mDrawerLayout.closeDrawer(Gravity.START)
    }

    override fun onResume() {
        super.onResume()
        checkSubscription()
        closeSideMenu()
    }

    private fun checkSubscription() {
        Log.d(TAG,"Check Subscription")
        val successCallback: (String) -> Unit = { message ->
            Log.d(TAG,"Subscription Status -> " + message)
            val msg = message.replace("\"","")
            if (!"active".equals(msg)) {
                Global.isSubscription = false
                downgradeToFreeplan()
                runOnUiThread {
                    MyToastUtil.showWarning(this, getString(R.string.subscriptioncancel))
                }
            } else {
                Global.isSubscription = true
            }
        }

        val errorCallback: (String) -> Unit = { message ->
            Global.isSubscription = false
        }

        APIManager.share.checkSubscribe(successCallback, errorCallback)
    }

    fun downgradeToFreeplan() {

        val successCallback: (Int) -> Unit = { freePlanID ->
            Log.d(TAG,"FreePlanID -> " + freePlanID)

            if (freePlanID != -1) {
                val successCallback: () -> Unit = {
                    Log.d(TAG,"cancelSubscription -> " + "Success")
                }

                val errorCallback: (String) -> Unit = { message ->
                    Log.d(TAG,"cancelSubscription -> " + "Error")
                    Log.d(TAG,"cancelSubscription -> " + message)
                }

                APIManager.share.subscribe("noNeed", freePlanID, successCallback, errorCallback)
            }
        }

        val errorCallback: (String) -> Unit = { message ->
            Log.d(TAG,"FreePlanID_Error -> " + message)
        }

        APIManager.share.getFreePlanID(successCallback, errorCallback)
    }

    fun reloadHomeFragment() {
        homeFragment.loadFirst()
    }

    /**
     * Navigation Methods
     */
    @SuppressLint("RestrictedApi")
    fun pushFragment(fragment: Fragment) {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START)
        }
        navigationBar.visibility = View.GONE
        floatBtn.visibility = View.GONE

        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerFL, fragment)
            .addToBackStack(null)
            .commit()
        containerFL.visibility = View.VISIBLE
    }

    @SuppressLint("RestrictedApi")
    fun pushFragment_spe(locationTV: TextView, imageView: ImageView, fragmentTwo: Fragment) {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START)
        }
        navigationBar.visibility = View.GONE
        floatBtn.visibility = View.GONE

//        val fragmentTransaction = supportFragmentManager.beginTransaction()
//
//        // 1. Exit for Previous Fragment
//        val exitFade = Fade()
//        exitFade.duration = FADE_DEFAULT_TIME
//        fragmentOne.setExitTransition(exitFade)
//
//        // 2. Shared Elements Transition
//        val enterTransitionSet = TransitionSet()
//        enterTransitionSet.addTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.move))
//        enterTransitionSet.duration = MOVE_DEFAULT_TIME
//        enterTransitionSet.startDelay = FADE_DEFAULT_TIME
//        fragmentTwo.setSharedElementEnterTransition(enterTransitionSet)
//
//        // 3. Enter Transition for New Fragment
//        val enterFade = Fade()
//        enterFade.startDelay = MOVE_DEFAULT_TIME + FADE_DEFAULT_TIME
//        enterFade.duration = FADE_DEFAULT_TIME
//        fragmentTwo.setEnterTransition(enterFade)
//
//        val iv = this.findViewById<ImageView>(R.id.imageBG)
//
//        fragmentTransaction.addSharedElement(iv, getString(R.string.imagetransition))
//        fragmentTransaction.addToBackStack("transaction")
//        fragmentTransaction.replace(R.id.containerFL, fragmentTwo)
//        fragmentTransaction.commitAllowingStateLoss()

//        val changeImageAnimation = ChangeImageTransform()
//        fragmentTwo.sharedElementEnterTransition = changeImageAnimation
//        fragmentTwo.sharedElementReturnTransition = changeImageAnimation
//
//        supportFragmentManager
//            .beginTransaction()
//            .setReorderingAllowed(true)
//            .addSharedElement(imageView, getString(R.string.imagetransition))
//            .replace(R.id.containerFL,fragmentTwo, LabelDetailFragment::class.java.simpleName)
//            .addToBackStack(null)
//            .commit()

        val changeImageAnimation = TransitionInflater.from(this).inflateTransition(R.transition.change_image_transform)
        changeImageAnimation.duration = ANIMATION_DELAY_TIME
        val fadeTransform  = TransitionInflater.from(this).inflateTransition(android.R.transition.fade)
        fadeTransform.duration = ANIMATION_DELAY_TIME

        // Set animation as shared-element transition
        //val galleryFragment = GalleryFragment()
        fragmentTwo.sharedElementEnterTransition = changeImageAnimation
        fragmentTwo.enterTransition = fadeTransform
        fragmentTwo.sharedElementReturnTransition = changeImageAnimation
        fragmentTwo.exitTransition = fadeTransform

        // Show GalleryFragment with transition animation
        supportFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(imageView, getString(R.string.imagetransition))
            .addSharedElement(locationTV, getString(R.string.locationtransition))
            .replace(R.id.containerFL, fragmentTwo)
            .addToBackStack(null)
            .commit()


        containerFL.visibility = View.VISIBLE
    }

    @SuppressLint("RestrictedApi")
    fun replaceFragment(fragment: Fragment) {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START)
        }

        if (supportFragmentManager.backStackEntryCount == 0) {
            pushFragment(fragment)
            return
        }

        navigationBar.visibility = View.GONE
        floatBtn.visibility = View.GONE

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.containerFL, fragment)
            .commit()
        containerFL.visibility = View.VISIBLE
    }

    @SuppressLint("RestrictedApi")
    fun popAllFragment() {

        checkSubscription()

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.containerFL, EmptyFragment.newInstance())
            .commit()
        containerFL.visibility = View.VISIBLE
    }

    @SuppressLint("RestrictedApi")
    fun popFragment() {

        if (supportFragmentManager.backStackEntryCount == 1) {
            popAllFragment()
            containerFL.visibility = View.GONE
        }
        navigationBar.visibility = View.GONE
        floatBtn.visibility = View.GONE

        supportFragmentManager
            .popBackStack()
    }

    @SuppressLint("RestrictedApi")
    fun popFragment_spe() {

        if (supportFragmentManager.backStackEntryCount == 1) {
            popAllFragment()
            containerFL.visibility = View.VISIBLE
        }
        navigationBar.visibility = View.GONE
        floatBtn.visibility = View.GONE

        supportFragmentManager
            .popBackStack()
    }

    class ViewPagerAdapter(fragmentManager: FragmentManager, val activity: MainActivity) : FragmentStatePagerAdapter(fragmentManager) {
        override fun getItem(position: Int): Fragment {
            val fragment: Fragment
            when (position) {
                0 -> fragment = activity.homeFragment
                1 -> fragment = activity.scanFragment
                2 -> fragment = activity.newLabelFragment
                3 -> fragment = activity.shareLabelFragment
                else -> fragment = activity.floorPlanFragment
            }
            return fragment
        }
        override fun getCount(): Int {
            return 5
        }
    }
}

class SwipeLockableViewPager(context: Context, attrs: AttributeSet): ViewPager(context, attrs) {
    private var swipeEnabled = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (swipeEnabled) {
            true -> super.onTouchEvent(event)
            false -> false
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return when (swipeEnabled) {
            true -> super.onInterceptTouchEvent(event)
            false -> false
        }
    }

    fun setSwipePagingEnabled(swipeEnabled: Boolean) {
        this.swipeEnabled = swipeEnabled
    }
}