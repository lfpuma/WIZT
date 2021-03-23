package com.wizt.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wizt.activities.MainActivity
import com.wizt.common.base.BaseFragment
import kotlinx.android.synthetic.main.item_home_row.view.*
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.CardView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.animation.AnimationUtils
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.ads.AdRequest
import com.ogaclejapan.arclayout.ArcLayout
import com.wizt.common.constants.Constants
import com.wizt.common.http.APIManager
import com.wizt.components.paginatedRecyclerView.InfiniteScrollListener
import com.wizt.models.Label
import com.wizt.models.Pagination
import com.wizt.R
import com.wizt.activities.CreateLabelActivity
import com.wizt.activities.TutorialActivity
import com.wizt.components.myDialog.MyDialog
import com.wizt.components.paginatedRecyclerView.interfaces.OnLoadMoreListener
import com.wizt.extensions.ImageHelper
import com.wizt.models.Global
import com.wizt.utils.AnimatorUtils
import com.wizt.utils.DateTimeUtils
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*


class HomeFragment : BaseFragment(), OnLoadMoreListener {

    private var pagination: Pagination? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var infiniteScrollListener: InfiniteScrollListener
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var searchBarSearchView: SearchView
    private lateinit var cardView: CardView
    private lateinit var myView : View
    private var labelList = ArrayList<Label>()
    private var showEmptyIV: ImageView? = null

    private lateinit var menuLayout: View
    private lateinit var arcLayout: ArcLayout
    private lateinit var fab_add: View

    private var isReadyToShare = false

    companion object {

        const val TAG = "WIZT:HomeFragment"

        const val roundedCon = 25.0f
        const val animationPosX = 0f
        const val animationPosY = 0f

        const val animationRadius = 0f
        const val animationdelayTime: Long = 400

        @JvmStatic
        fun newInstance() =
            HomeFragment().apply {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadFirst()
        setUserVisibleHint(false)
        PreferenceUtils.saveString(Constants.PREF_SEARCHSTRING,"")
    }

    override fun onResume() {
        super.onResume()
        addAds(myView)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_home, container, false)
        myView = view
//        super.bindMenuAction(view)
        super.bindNotificationViewAction(view)

        linearLayoutManager = LinearLayoutManager(this.context)
        infiniteScrollListener = InfiniteScrollListener(linearLayoutManager, this)
        infiniteScrollListener.setLoaded()

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.addOnScrollListener(infiniteScrollListener)
        adapter = this.context?.let { RecyclerAdapter(it, itemClickListener, arrayListOf()) }!!

        showEmptyIV = view.findViewById(R.id.showEmptyIV)
//        Glide.with(context!!).load(R.drawable.animation_home).into(showEmptyIV!!)
        showEmptyIV!!.visibility = View.VISIBLE

        recyclerView.adapter = adapter

        swipeContainer = view.findViewById(R.id.swipeContainer)
        swipeContainer.setOnRefreshListener {
            fetchTimelineAsync()
        }
        //setSwipeRefreshLayoutColor(swipeContainer)

        searchBarSearchView = view.findViewById(R.id.searchBarSearchView)
        cardView = view.findViewById(R.id.cardView)
        initLayoutAndListeners()

        view.menuBtn.setOnClickListener {
            if (labelList.isEmpty()) {
                val intent = Intent(activity, TutorialActivity::class.java)
                intent.putExtra(Constants.PREF_TUTORIAL_REQUIRE,true)
                startActivity(intent)
            } else {
                (activity as? MainActivity)?.openSideMenu()
            }
        }

        return view
    }

    fun addAds(view:View) {

        Log.d(TAG,"" + Global.isSubscription)

        val successCallback: (String) -> Unit = { message ->
            Log.d(TAG,"Subscription Status -> " + message)
            val msg = message.replace("\"","")
            if (!"active".equals(msg)) {
                Global.isSubscription = false
                downgradeToFreeplan()
                activity?.runOnUiThread {
                    MyToastUtil.showWarning(context!!, getString(R.string.subscriptioncancel))
                }
            } else {
                Global.isSubscription = true
            }
            activity?.runOnUiThread {
                showAds(view)
            }
        }

        val errorCallback: (String) -> Unit = { message ->
            Global.isSubscription = false
            activity?.runOnUiThread {
                showAds(view)
            }
        }

        APIManager.share.checkSubscribe(successCallback, errorCallback)
    }

    fun showAds(view:View) {

        if(Global.isSubscription) {
            view.adsLayout.visibility = View.GONE
            return
        }
        view.adsLayout.visibility = View.VISIBLE
        val adRequest = AdRequest.Builder().build()
        view.adsLayout.adView.loadAd(adRequest)
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

    fun initLayoutAndListeners() {

        val editText: EditText = searchBarSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text)
        editText.setTextColor(Color.WHITE)
        editText.setHintTextColor(Color.WHITE)

        searchBarSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(value: String?): Boolean {
//                if (recyclerView.adapter is Filterable) {
//                    (recyclerView.adapter as Filterable).filter.filter(value)
//                }
                LoadWithSearchRes(value)
                searchBarSearchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(value: String?): Boolean {
                LoadWithSearchRes(value)
                return true
            }

        })

        cardView.setOnClickListener {
            searchBarSearchView.isIconified = false
        }

        editText.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus) {
                searchBarSearchView.isIconified = false
            }
        }

        fab_add = myView.findViewById(R.id.fab_add)
        menuLayout = myView.findViewById(R.id.menu_layout)
        arcLayout = myView.findViewById(R.id.arc_layout)

        val btn_trainFYBE = myView.findViewById<View>(R.id.btn_fybeTrain)
        val btn_share = myView.findViewById<View>(R.id.btn_share)
        val btn_newLabel = myView.findViewById<View>(R.id.btn_newLabel)

        btn_trainFYBE.setOnClickListener {
            //MyToastUtil.showMessage(context!!,"onClick TrainFYBE")
            hideMenu()
            val fybeFragment: ScanFragment = ScanFragment.newInstance()
            (activity as MainActivity).pushFragment(fybeFragment)
        }

        btn_share.setOnClickListener {
            //MyToastUtil.showMessage(context!!,"onClick Share")
            hideMenu()

//            if(!Global.isSubscription) {
//                MyToastUtil.showWarning(context!!, "To share Label with friends, please subscribe to premium plans.")
//                return@setOnClickListener
//            }

            //isReadyToShare = true
            //updateUI()
            (activity as MainActivity).popFragment()
            (activity as MainActivity).popFragment()
            (activity as MainActivity).pushFragment(ShareLogFragment.newInstance())
        }

        btn_newLabel.setOnClickListener {
            //MyToastUtil.showMessage(context!!,"onClick NewLabel")
            hideMenu()
            MyDialog.showChooseOptionDialog(context!!)
        }

        menuLayout.setOnClickListener {
            hideMenu()
        }

        fab_add.setOnClickListener {

            if(labelList.isEmpty()) {

                val intent = Intent(context, CreateLabelActivity::class.java)
                intent.putExtra(Constants.EXTRA_CREATE_LABEL_ACTIVITY_TYPE, true)
                intent.putExtra(Constants.EXTRA_CREATE_FYBE_ACTIVITY_TYPE, false)
                startActivity(intent)

                return@setOnClickListener
            }

            if(menuLayout.visibility == View.VISIBLE)
                hideMenu()
            else {
                onFabClick()
            }
        }

        myView.shareView.setOnClickListener {
            isReadyToShare = false
            updateUI()
        }

    }

    private fun updateUI() {
        if(isReadyToShare) {
            myView.shareView.visibility = View.VISIBLE
        }
        else {
            myView.shareView.visibility = View.GONE
        }
    }

    private fun hideMenu() {
        val animList = java.util.ArrayList<Animator>()

        for (i in arcLayout.childCount - 1 downTo 0) {
            animList.add(createHideItemAnimator(arcLayout.getChildAt(i)))
        }

        animList.add(
            ObjectAnimator.ofPropertyValuesHolder(
                fab_add,
                AnimatorUtils.rotation(45f, 0f)
            )
        )

        val animSet = AnimatorSet()
        animSet.duration = animationdelayTime
        animSet.interpolator = AnticipateInterpolator()
        animSet.playTogether(animList)
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)

                menuLayout.visibility = View.INVISIBLE
                fab_add.rotation = 0f
            }
        })
        animSet.start()
    }

    private fun createHideItemAnimator(item: View): Animator {
        val dx = fab_add.x - item.x
        val dy = context!!.resources.getDimension(R.dimen.fabAddPosY) - item.y

        Log.d(TAG, "fab_add -> " + fab_add.x)
        Log.d(TAG, "fab_add -> " + context!!.resources.getDimension(R.dimen.fabAddPosY))

        Log.d(TAG, "itemX -> " + item.x)
        Log.d(TAG, "itemY -> " + item.y)

        val anim = ObjectAnimator.ofPropertyValuesHolder(
            item,
            AnimatorUtils.rotation(animationRadius, 0f),
            AnimatorUtils.translationX(animationPosX, dx),
            AnimatorUtils.translationY(animationPosY, dy)
        )

        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                item.translationX = animationPosX
                item.translationY = animationPosY
            }
        })

        return anim
    }

    private fun onFabClick() {

        menuLayout.setVisibility(View.VISIBLE)

        val animList = java.util.ArrayList<Animator>()

        var i = 0
        val len = arc_layout.getChildCount()
        while (i < len) {
            animList.add(createShowItemAnimator(arc_layout.getChildAt(i)))
            i++
        }

        animList.add(
            ObjectAnimator.ofPropertyValuesHolder(
                fab_add,
                AnimatorUtils.rotation(0f, 45f)
            )
        )

        val animSet = AnimatorSet()
        animSet.duration = animationdelayTime
        animSet.interpolator = OvershootInterpolator()
        animSet.playTogether(animList)
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)

                fab_add.rotation = 45f
            }
        })
        animSet.start()
    }

    private fun createShowItemAnimator(item: View): Animator {

        val dx = fab_add.x - item.x
        val dy = context!!.resources.getDimension(R.dimen.fabAddPosY) - item.y

        Log.d(TAG, "itemX -> " + item.x)
        Log.d(TAG, "itemY -> " + item.y)

        item.rotation = 0f
        item.translationX = dx
        item.translationY = dy

        return ObjectAnimator.ofPropertyValuesHolder(
            item,
            AnimatorUtils.rotation(0f, animationRadius),
            AnimatorUtils.translationX(dx, animationPosX),
            AnimatorUtils.translationY(dy, animationPosY)
        )
    }

    fun LoadWithSearchRes(value: String?) {
        if (!value!!.isEmpty()) {
            PreferenceUtils.saveString(Constants.PREF_SEARCHSTRING,value!!)
        }
        else {
            PreferenceUtils.saveString(Constants.PREF_SEARCHSTRING,"")
        }

        loadWithSearchReq()
    }

    override fun onLoadMore() {
        if (pagination?.next == null) {
            return
        }

        adapter.addNullData()

        val successCallback: (Pagination, List<Label>) -> Unit = { pagination, list ->
            this.pagination = pagination
            activity?.runOnUiThread {
                adapter.removeNull()
                adapter.addData(list)
                infiniteScrollListener.setLoaded()
            }
        }

        APIManager.share.getLabels(pagination?.next!!, successCallback, errorCallback)
    }

    /**
     * UI Events
     */
    private val itemClickListener:(Label, ImageView, TextView) -> Unit = { label, imageView, locationTV ->
        val isFocus: Boolean = PreferenceUtils.getBoolean(Constants.PREF_HOME_IS_FOCUS)
        Global.label = label
        if (!isFocus) {

//            if (isReadyToShare) {
//
//                isReadyToShare = false
//                updateUI()
//                val fragment = FriendFragment.newInstance()
//                fragment.selectedLabel = label
//                (activity as MainActivity).replaceFragment(fragment)
//
//            } else {
//                val fragment = LabelDetailFragment.newInstance()
//                fragment.label = label
//                fragment.labelType = 1
//                (activity as MainActivity).popFragment()
//                (activity as MainActivity).popFragment()
//                (activity as MainActivity).replaceFragment(fragment)
//            }

            val fragment = LabelDetailFragment.newInstance()
            fragment.label = label
            fragment.labelType = 1
            (activity as MainActivity).popAllFragment()
            (activity as MainActivity).pushFragment_spe(locationTV, imageView,fragment)

//            val detailsIntent = Intent(activity, LabelDetailActivity::class.java)
//
//            // Start Activity with shared-transition animation
//
//            val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                activity!!,
//                imageView,
//                getString(R.string.imagetransition))
//            startActivity(detailsIntent, activityOptions.toBundle())
        }
    }

    /**
     * Load label data
     */
    fun loadFirst() {

        val successCallback: (Pagination, List<Label>) -> Unit = { pagination, list ->
            this.pagination = pagination
            activity?.runOnUiThread {
                if (showEmptyIV != null) {
                    if (list.isEmpty()) {
                        showEmptyIV!!.visibility = View.VISIBLE
                    } else {
                        showEmptyIV!!.visibility = View.GONE
                    }
                }

                labelList.clear()
                labelList.addAll(list)

                loadWithSearchReq()
            }
        }

        APIManager.share.getLabels(successCallback, errorCallback)
    }

    fun loadWithSearchReq() {

        var searchList = ArrayList<Label>()
        val searchString = PreferenceUtils.getString(Constants.PREF_SEARCHSTRING,"")
        if (!searchString!!.isEmpty()) {
            for (item in labelList) {
                if(item.name.contains(searchString) || item.tags.contains(searchString)) {
                    searchList.add(item)
                }
            }
        }
        else {
            searchList.addAll(labelList)
        }

        if (showEmptyIV != null) {
            if (searchList.isEmpty()) {
                showEmptyIV!!.visibility = View.VISIBLE
            } else {
                showEmptyIV!!.visibility = View.GONE
            }
        }

        this.adapter.clear()
        this.adapter.addData(searchList)

    }

    private fun fetchTimelineAsync() {
        if (pagination?.next == null) {
            swipeContainer.isRefreshing = false
            return
        }

        val successCallback: (Pagination, List<Label>) -> Unit = { pagination, list ->
            this.pagination = pagination
            this.adapter.clear()
            activity?.runOnUiThread {
                this.adapter.addData(list)
                swipeContainer.isRefreshing = false
            }
        }

        APIManager.share.getLabels(pagination?.next!!, successCallback, errorCallback)
    }


    class RecyclerAdapter(private var context: Context, val itemClickListener: (label: Label, imageView: ImageView, locationtv: TextView) -> Unit, arr: ArrayList<Label?>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var isLoading = false
        private val VIEWTYPE_LABEL = 1
        private val VIEWTYPE_PROGRESS = 2

        private var labelArr: ArrayList<Label?> = arr

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view: View

            return if (p1 == VIEWTYPE_LABEL) {
                view = inflater.inflate(R.layout.item_home_row, parent, false)
                LabelViewHolder(view)
            } else {
                view = inflater.inflate(R.layout.item_progress_row, parent, false)
                ProgressViewHolder(view)
            }
        }

        override fun getItemCount(): Int {
            return labelArr.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == labelArr.size - 1 && isLoading)
                VIEWTYPE_PROGRESS
            else
                VIEWTYPE_LABEL
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is LabelViewHolder) {
                bindLabelViewHolder(holder, position)
            } else {

            }
        }

        private fun bindLabelViewHolder(holder: LabelViewHolder, position: Int) {
            holder.itemView.swipeIV.setOnClickListener {

                val label = labelArr[position] ?: return@setOnClickListener

                val rightDrawable = context.getDrawable(R.drawable.ic_swipe_right)
                val leftDrawable = context.getDrawable(R.drawable.ic_swipe)

                holder.itemView.imageBG.setImageDrawable(null)
                if (holder.itemView.swipeIV.getTag() == R.drawable.ic_swipe_right) {
                    Glide.with(context!!)
                        .asBitmap()
                        .load(label.ar_mark_image)
                        .into(object : CustomTarget<Bitmap>(){
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                                val sqRes = ImageHelper.getCroppedImage_homeItem(resource)
                                val round = RoundedBitmapDrawableFactory.create(context!!.resources,sqRes)
                                round.cornerRadius = roundedCon
                                holder.itemView.imageBG.setImageDrawable(round)
                            }
                            override fun onLoadCleared(placeholder: Drawable?) {
                                //holder.itemView.imageBG.setImageDrawable(context.getDrawable(R.drawable.home_row_default))
                            }
                        })

                    val animSlide = AnimationUtils.loadAnimation(context,
                        R.anim.slide)
                    holder.itemView.imageBG.startAnimation(animSlide)
                    holder.itemView.swipeIV.setImageDrawable(leftDrawable)
                    holder.itemView.swipeIV.setTag(R.drawable.ic_swipe)
                } else {

                    if (label.images.size == 0) {
                    } else {
                        for (i in 1..label.images.size) {

                            if (label.images[i - 1].is_cover) {

                                Glide.with(context!!)
                                    .asBitmap()
                                    .load(label.images[i - 1].url)
                                    .into(object : CustomTarget<Bitmap>(){
                                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                                            val sqRes = ImageHelper.getCroppedImage(resource)
                                            val round = RoundedBitmapDrawableFactory.create(context!!.resources,sqRes)
                                            round.cornerRadius = roundedCon
                                            holder.itemView.imageBG.setImageDrawable(round)
                                        }
                                        override fun onLoadCleared(placeholder: Drawable?) {
                                        }
                                    })

                            }
                        }
                    }

                    val animSlide = AnimationUtils.loadAnimation(context,
                        R.anim.slide_right)
                    holder.itemView.imageBG.startAnimation(animSlide)
                    holder.itemView.swipeIV.setImageDrawable(rightDrawable)
                    holder.itemView.swipeIV.setTag(R.drawable.ic_swipe_right)
                }


            }

            holder.itemView.setOnClickListener {
                val label = labelArr[position]
                if (label != null) {

                    itemClickListener(label, holder.itemView.imageBG, holder.itemView.locationTextView)
                }
            }

            val label = labelArr[position] ?: return
            holder.itemView.imageBG.setImageDrawable(null)
            if (label.images.size == 0) {
            } else {
                for (i in 1..label.images.size) {

                    if (label.images[i - 1].is_cover) {

                        Glide.with(context!!)
                            .asBitmap()
                            .load(label.images[i - 1].url)
                            .into(object : CustomTarget<Bitmap>(){
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                                    val sqRes = ImageHelper.getCroppedImage(resource)
                                    val round = RoundedBitmapDrawableFactory.create(context!!.resources,sqRes)
                                    round.cornerRadius = roundedCon
                                    holder.itemView.imageBG.setImageDrawable(round)
                                }
                                override fun onLoadCleared(placeholder: Drawable?) {
                                }
                            })

                    }
                }
            }

            //holder.itemView.titleTV.text = label.name

            // Get Tags
            holder.itemView.tagHome.removeAllTags()
            if(label.tags.isNotEmpty()) {
                val tagStr: String = label.tags
                val tagArr: List<String> = tagStr.split(",").map { it.trim() }
                for (i in 1..tagArr.count()) {
                    holder.itemView.tagHome.addTag(tagArr[i - 1])
                }
            }

            holder.itemView.locationTextView.text = label.location
            val rightDrawable = context.getDrawable(R.drawable.ic_swipe_right)
            holder.itemView.swipeIV.setImageDrawable(rightDrawable)
            holder.itemView.swipeIV.setTag(R.drawable.ic_swipe_right)
            if(label.reminder_time.isEmpty()) {
                holder.itemView.reminderIV.visibility = View.GONE
            } else {
                holder.itemView.tvReminderDate.text = DateTimeUtils().getLocalDate(label.reminder_time) +
                        " " + DateTimeUtils().getLocalTime(label.reminder_time)
                holder.itemView.reminderIV.visibility = View.VISIBLE
            }

        }

        fun addData(list: List<Label>) {
            this.labelArr.addAll(list)
            notifyDataSetChanged()
        }

        fun clear() {
            this.labelArr.clear()
        }

        fun addNullData() {
            labelArr.add(null)
            notifyItemInserted(labelArr.size)
        }

        fun removeNull() {
            labelArr.removeAt(labelArr.size - 1)
            notifyItemRemoved(labelArr.size)
        }

        class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            @SuppressLint("SetTextI18n")
            fun bind() {
                itemView.setOnClickListener {  }
                //itemView.cl.setOnClickListener {  }
            }
        }

        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}
