package com.wizt.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.ads.AdRequest
import com.wizt.activities.MainActivity
import com.wizt.common.base.BaseFragment
import com.wizt.common.http.APIManager
import com.wizt.components.paginatedRecyclerView.InfiniteScrollListener
import com.wizt.components.paginatedRecyclerView.interfaces.OnLoadMoreListener
import com.wizt.models.Pagination
import com.wizt.models.ShareLabel
import com.wizt.R
import com.wizt.extensions.ImageHelper
import com.wizt.models.Global
import com.wizt.utils.DateTimeUtils
import com.wizt.utils.MyToastUtil
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_share.view.*
import kotlinx.android.synthetic.main.item_share_label_row.view.*
import kotlinx.android.synthetic.main.item_share_log_row.view.*
import kotlinx.android.synthetic.main.item_share_log_row.view.sharedLabelName
import kotlin.collections.ArrayList
import java.util.*
import kotlin.concurrent.schedule
import android.support.v7.widget.OrientationHelper
import android.util.Log


class ShareLogFragment : BaseFragment(), OnLoadMoreListener {

    companion object {

        const val roundedCon = 10.0f
        const val DELAY_TIME = 150L
        const val DELAY_TIME_PLUS = 30L

        const val TAG = "WIZT:ShareLogFragment"

        @JvmStatic
        fun newInstance() =
            ShareLogFragment().apply {}
    }

    private var pagination: Pagination? = null
    lateinit var recyclerView: RecyclerView
    lateinit var linearLayoutManager: LinearLayoutManager
    lateinit var adapter: RecyclerAdapter
    private lateinit var infiniteScrollListener: InfiniteScrollListener
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var myView: View

    private var pagination_received: Pagination? = null
    private lateinit var recyclerView_received: RecyclerView
    private lateinit var linearLayoutManager_received: LinearLayoutManager
    private lateinit var adapter_received: RecyclerAdapter_received
    private lateinit var infiniteScrollListener_received: InfiniteScrollListener
    private lateinit var swipeContainer_received: SwipeRefreshLayout
    private var labelList = ArrayList<ShareLabel>()
    private var labelshareList = ArrayList<ShareLabel>()

    private var showEmptyIV: ImageView? = null
    private var showEmptyIV_received: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadFirst()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_share, container, false)
        myView = view
        view.title.text = resources.getString(R.string.share_log)
        super.bindMenuAction(view)
        super.bindNotificationViewAction(view)

        addAds(view)

        linearLayoutManager = LinearLayoutManager(context)
        infiniteScrollListener = InfiniteScrollListener(linearLayoutManager, this)
        infiniteScrollListener.setLoaded()

        recyclerView = view.findViewById(R.id.recyclerView)
        adapter = RecyclerAdapter (context!!, itemClickListener, arrayListOf())
        recyclerView.addOnScrollListener(infiniteScrollListener)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter

        showEmptyIV = view.findViewById(R.id.showEmptyIV)
        showEmptyIV?.visibility = View.VISIBLE
        showEmptyIV_received = view.findViewById(R.id.showEmptyIV_received)
        showEmptyIV_received?.visibility = View.GONE

        swipeContainer = view.findViewById(R.id.swipeContainer)
        setSwipeRefreshLayoutColor(swipeContainer)
        swipeContainer.setOnRefreshListener {
            fetchTimelineAsync()
        }

        view.btnShared.setOnClickListener {
            if(swipeContainer.visibility == View.VISIBLE) return@setOnClickListener
            updateUI(0)
        }

        view.btnReceived.setOnClickListener {
            if(swipeContainer_received.visibility == View.VISIBLE) return@setOnClickListener
            updateUI(1)
        }

        linearLayoutManager_received = LinearLayoutManager(context)
        infiniteScrollListener_received = InfiniteScrollListener(linearLayoutManager_received, this)
        infiniteScrollListener_received.setLoaded()

        recyclerView_received = view.recyclerView_received
        adapter_received = RecyclerAdapter_received(context!!, itemClickListener_received, arrayListOf())
        recyclerView_received.addOnScrollListener(infiniteScrollListener_received)
        recyclerView_received.layoutManager = linearLayoutManager_received
        recyclerView_received.adapter = adapter_received

        swipeContainer_received = view.findViewById(R.id.swipeContainer_received)
        setSwipeRefreshLayoutColor(swipeContainer_received)
        swipeContainer_received.setOnRefreshListener {
            fetchTimelineAsync()
        }

        myView.fab_back.setOnClickListener {
            (context as MainActivity).onBackPressed()
        }

        return view
    }

    private fun updateUI(flag: Int) {
        if (flag == 0) {
            myView.btnShared.setBackgroundColor(resources.getColor(R.color.colorFriendListButton_friend))
            myView.btnReceived.setBackgroundColor(resources.getColor(R.color.colorAddFriendButton_friend))

            Timer("schedule",false).schedule(adapter_received.getAnimatedTime(recyclerView_received) + DELAY_TIME_PLUS + 100) {
                activity?.runOnUiThread {
                    swipeContainer.visibility = View.VISIBLE
                    myView.swipeContainer_received.visibility = View.INVISIBLE
                    showEmptyIV_received?.visibility = View.GONE
                    updateShowEmptyIV()
                    adapter_received.restoreItemViews(recyclerView_received)
                }
            }
            adapter_received.clear(true, recyclerView_received)

        } else {
            myView.btnReceived.setBackgroundColor(resources.getColor(R.color.colorFriendListButton_friend))
            myView.btnShared.setBackgroundColor(resources.getColor(R.color.colorAddFriendButton_friend))

            Timer("schedule",false).schedule(adapter.getAnimatedTime(recyclerView) + DELAY_TIME_PLUS + 100) {

                activity?.runOnUiThread {
                    swipeContainer.visibility = View.INVISIBLE
                    myView.swipeContainer_received.visibility = View.VISIBLE
                    updateShowEmptyIV_received()
                    showEmptyIV?.visibility = View.GONE
                    adapter.restoreItemViews(recyclerView)
                }

            }
            adapter.clear(true,recyclerView)
        }
    }

    fun updateShowEmptyIV() {
        if(labelshareList.isEmpty()) {
            showEmptyIV?.visibility = View.VISIBLE
        } else showEmptyIV?.visibility = View.GONE
    }

    fun updateShowEmptyIV_received() {
        if(labelList.isEmpty()) {
            showEmptyIV_received?.visibility = View.VISIBLE
        } else showEmptyIV_received?.visibility = View.GONE
    }

    fun addAds(view:View) {
        if(Global.isSubscription) {
            view.adsLayout.visibility = View.GONE
            return
        }
        view.adsLayout.visibility = View.VISIBLE
        val adRequest = AdRequest.Builder().build()
        view.adsLayout.adView.loadAd(adRequest)
    }

    /***
     * Load Data
     */
    private fun loadFirst() {
        loadFirst_received()
        val successCallback: (Pagination, ArrayList<ShareLabel>) -> Unit = { pagination, list ->
            this.pagination = pagination
            activity?.runOnUiThread {
                if (list.isNotEmpty()) {
                    labelshareList.clear()
                    labelshareList.addAll(list)

                    adapter.clear()
                    adapter.addData(labelshareList)


                }
            }
        }
        APIManager.share.getLabelLogList(successCallback, errorCallback)
    }

    private fun loadFirst_received() {
        val successCallback: (Pagination, List<ShareLabel>) -> Unit = { pagination, list ->
            this.pagination_received = pagination
            activity?.runOnUiThread {

                labelList.clear()
                labelList.addAll(list)

                this.adapter_received.clear()
                this.adapter_received.addData(labelList)


            }
        }

        APIManager.share.getShareLabelList(successCallback, errorCallback)
    }

    override fun onLoadMore() {
        onLoadMore_received()
        if (this.pagination?.next == null) {
            return
        }

        adapter.addNullData()
        val successCallback: (Pagination, ArrayList<ShareLabel>) -> Unit = { pagination, list ->
            this.pagination = pagination
            activity?.runOnUiThread {
                adapter.removeNull()
                this.adapter.addData(list)
                infiniteScrollListener.setLoaded()
            }
        }
        APIManager.share.getShareLabelList(pagination?.next!!, successCallback, errorCallback)
    }

    private fun onLoadMore_received() {
        if (this.pagination_received?.next == null) {
            return
        }

        adapter_received.addNullData()
        val successCallback: (Pagination, ArrayList<ShareLabel>) -> Unit = { pagination, list ->
            this.pagination_received = pagination
            activity?.runOnUiThread {
                adapter_received.removeNull()
                this.adapter_received.addData(list)
                infiniteScrollListener_received.setLoaded()
            }
        }
        APIManager.share.getShareLabelList(pagination_received?.next!!, successCallback, errorCallback)
    }

    private fun fetchTimelineAsync() {
        fetchTimelineAsync_received()
        if (this.pagination?.next == null) {
            swipeContainer.isRefreshing = false
            return
        }

        val successCallback: (Pagination, ArrayList<ShareLabel>) -> Unit = { pagination, list ->
            this.pagination = pagination
            this.adapter.clear()
            activity?.runOnUiThread {
                this.adapter.addData(list)
                swipeContainer.isRefreshing = false
            }
        }

        APIManager.share.getShareLabelList(pagination?.next!!, successCallback, errorCallback)
    }

    private fun fetchTimelineAsync_received() {
        if (this.pagination_received?.next == null) {
            swipeContainer_received.isRefreshing = false
            return
        }
        val successCallback: (Pagination, ArrayList<ShareLabel>) -> Unit = { pagination, list ->
            this.pagination_received = pagination
            this.adapter_received.clear()
            activity?.runOnUiThread {
                this.adapter_received.addData(list)
                swipeContainer_received.isRefreshing = false
            }
        }
        APIManager.share.getShareLabelList(pagination_received?.next!!, successCallback, errorCallback)
    }

    private val itemClickListener_received:(ShareLabel) -> Unit = { shareLabel ->
        Global.shareLabel = shareLabel
        val fragment = LabelDetailFragment.newInstance()
        fragment.shareLabel = shareLabel
        fragment.labelType = 3
        (activity as MainActivity).popFragment()
        (activity as MainActivity).pushFragment(fragment)
    }

    private val itemClickListener:(ShareLabel) -> Unit = { shareLabel ->
        val fragment = LabelDetailFragment.newInstance()
        fragment.shareLabel = shareLabel
        fragment.labelType = 2
        (activity as MainActivity).pushFragment(fragment)
    }

    class RecyclerAdapter(private val context: Context, val itemClickListener: (shareLabel: ShareLabel) -> Unit, arr : ArrayList<ShareLabel?>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var isLoading = false
        private val VIEWTYPE_SHARE_LOG = 1
        private val VIEWTYPE_PROGRESS = 2

        private var shareLogArr: ArrayList<ShareLabel?> = arr

        private lateinit var animateView: View
        private var lastPosition = -1

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)

            val view: View

            return if (p1 == VIEWTYPE_SHARE_LOG) {
                view = inflater.inflate(R.layout.item_share_log_row, parent, false)
                animateView = view
                ShareLogViewHolder(view)
            } else {
                view = inflater.inflate(R.layout.item_progress_row, parent, false)
                animateView = view
                ProgressViewHolder(view)
            }
        }

        override fun getItemCount(): Int {
            return shareLogArr.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == shareLogArr.size - 1 && isLoading)
                VIEWTYPE_PROGRESS
            else
                VIEWTYPE_SHARE_LOG
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.setOnClickListener {
                //itemClickListener(shareLogArr[position]!!)
            }

            if (shareLogArr[position] == null)  return

            val shareLog = shareLogArr[position]!!
            val userBy = shareLog.share_to
            val sharedLabel = shareLog.label

            if (userBy.picture != null) {

                //Glide.with(context).load(userBy.picture).into(holder.itemView.avatarImageView)
                Glide.with(context!!)
                    .asBitmap()
                    .load(userBy.picture)
                    .into(object : CustomTarget<Bitmap>(){
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val round = RoundedBitmapDrawableFactory.create(context!!.resources,resource)
                            round.cornerRadius = roundedCon
                            holder.itemView.avatarImageView.setImageDrawable(round)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })

            }
            holder.itemView.userNameSharedBy.text = userBy.name
            holder.itemView.sharedLabelName.text = sharedLabel.name

            if (shareLog.edit_permission) {
                holder.itemView.accessTV.setText(R.string.access_true)
            } else {
                holder.itemView.accessTV.setText(R.string.access_false)
            }

            holder.itemView.sharedDate.text = DateTimeUtils().formatTimeAgo(shareLog.created_at)

            val activity : MainActivity = context as MainActivity

            holder.itemView.unShareBtn.setOnClickListener {
                val errorCallback: (String) -> Unit = { message ->
                    activity.runOnUiThread {
                        MyToastUtil.showWarning(activity, message)
                    }
                }
                val successCallback: () -> Unit = {
                    activity.runOnUiThread {
                        MyToastUtil.showMessage(activity, "UnShare Completely!")
                        shareLogArr.removeAt(position)
                        notifyDataSetChanged()
                    }
                }

                APIManager.share.deleteShareLabel(shareLog, successCallback, errorCallback)

            }

//            for (i in 0 until itemCount) {
//                animate(animateView, i)
//            }

            //animate(holder.itemView, position)

            (holder as ShareLogViewHolder).bind()
        }

        private fun animate(view: View, position: Int) {

            if (position <= lastPosition) return

            val animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
            Timer("schedule",false).schedule(500) {
                view.startAnimation(animation)
                lastPosition = position
            }

        }

        fun addData(arr: ArrayList<ShareLabel>) {
            shareLogArr.addAll(arr)
            notifyDataSetChanged()
        }

        fun clear(flag: Boolean = false, recyclerV: RecyclerView? = null) {
            if (!flag) {
                this.shareLogArr.clear()
                return
            }

            if (recyclerV != null) {
                deleteAllItems(recyclerV)
            }
        }

        fun findOneVisibleChild(layoutManager: RecyclerView.LayoutManager,
            fromIndex: Int, toIndex: Int, completelyVisible: Boolean,
            acceptPartiallyVisible: Boolean
        ): View? {
            val helper: OrientationHelper
            if (layoutManager.canScrollVertically()) {
                helper = OrientationHelper.createVerticalHelper(layoutManager)
            } else {
                helper = OrientationHelper.createHorizontalHelper(layoutManager)
            }

            val start = helper.getStartAfterPadding()
            val end = helper.getEndAfterPadding()
            val next = if (toIndex > fromIndex) 1 else -1
            var partiallyVisible: View? = null
            var i = fromIndex
            while (i != toIndex) {
                val child = layoutManager.getChildAt(i)
                val childStart = helper.getDecoratedStart(child)
                val childEnd = helper.getDecoratedEnd(child)
                if (childStart < end && childEnd > start) {
                    if (completelyVisible) {
                        if (childStart >= start && childEnd <= end) {
                            return child
                        } else if (acceptPartiallyVisible && partiallyVisible == null) {
                            partiallyVisible = child
                        }
                    } else {
                        return child
                    }
                }
                i += next
            }
            return partiallyVisible
        }

        fun findFirstVisibleItemPosition(recyclerView: RecyclerView): Int {
            val child = findOneVisibleChild(recyclerView.layoutManager!!, 0, recyclerView.layoutManager!!.getChildCount(), false, true)
            return if (child == null) RecyclerView.NO_POSITION else recyclerView.getChildAdapterPosition(child)
        }

        fun findLastVisibleItemPosition(recyclerView: RecyclerView): Int {
            val child = findOneVisibleChild(recyclerView.layoutManager!!, recyclerView.layoutManager!!.getChildCount() - 1, -1, false, true)
            return if (child == null) RecyclerView.NO_POSITION else recyclerView.getChildAdapterPosition(child)
        }

        private fun deleteItem(rowView: View, position: Int) {

            val anim = AnimationUtils.loadAnimation(
                context,
                R.anim.slide_out_left
            )
            anim.duration = DELAY_TIME + DELAY_TIME_PLUS
            rowView.startAnimation(anim)

            Handler().postDelayed(Runnable {
                if (shareLogArr.size == 0) {
                    //addEmptyView() // adding empty view instead of the RecyclerView
                    return@Runnable
                }
                //shareLogArr.removeAt(position) //Remove the current content from the array
                //notifyDataSetChanged() //Refresh list
                rowView.visibility = View.INVISIBLE
            }, anim.duration)
        }

        fun getAnimatedTime(mRecyclerView: RecyclerView): Long {
            if(itemCount < 2) return DELAY_TIME
            return (findLastVisibleItemPosition(mRecyclerView) - findFirstVisibleItemPosition(mRecyclerView) + 1) * DELAY_TIME
        }

        private fun deleteAllItems(mRecyclerView: RecyclerView) {

            if(itemCount == 0) return

            Log.d(TAG, "First Visible Item -> " + findFirstVisibleItemPosition(mRecyclerView))
            Log.d(TAG, "Last Visible Item -> " + findLastVisibleItemPosition(mRecyclerView))

            var mStopHandler: Boolean = false
            var cusPosition = findFirstVisibleItemPosition(mRecyclerView)

            val handler = Handler()
            val runnable = object : Runnable {
                override fun run() {

                    if (cusPosition == findLastVisibleItemPosition(mRecyclerView) + 1) {
                        mStopHandler = true
                    }

                    if (!mStopHandler) {
                        val v = mRecyclerView.findViewHolderForAdapterPosition(cusPosition)!!.itemView
                        deleteItem(v, cusPosition)
                        cusPosition++
                    } else {
                        handler.removeCallbacksAndMessages(null)
                    }

                    val postDelayed = handler.postDelayed(this, DELAY_TIME)
                }
            }
            (context as MainActivity).runOnUiThread(runnable)
        }

        fun restoreItemViews(mRecyclerView: RecyclerView) {

            if (itemCount == 0) return

            for (i in findFirstVisibleItemPosition(mRecyclerView) .. findLastVisibleItemPosition(mRecyclerView)) {
                val v = mRecyclerView.findViewHolderForAdapterPosition(i)!!.itemView
                v.visibility = View.VISIBLE
            }
        }

        fun addNullData() {
            shareLogArr.add(null)
            notifyItemInserted(shareLogArr.size)
        }

        fun removeNull() {
            shareLogArr.removeAt(shareLogArr.size - 1)
            notifyItemRemoved(shareLogArr.size)
        }

        class ShareLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
                //itemView.cl.setOnClickListener { clickListener }
            }
        }

        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }


    class RecyclerAdapter_received(private val context: Context, val itemClickListener: (shareLabel: ShareLabel) -> Unit, arr : ArrayList<ShareLabel?>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var isLoading = false
        private val VIEWTYPE_SHARED_LABEL = 1
        private val VIEWTYPE_PROGRESS = 2

        private var sharedLabelArr : ArrayList<ShareLabel?> = arr

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view: View


            return if (p1 == VIEWTYPE_SHARED_LABEL) {
                view = inflater.inflate(R.layout.item_share_label_row, parent, false)
                SharedLabelViewHolder(view)
            } else {
                view = inflater.inflate(R.layout.item_progress_row, parent, false)
                ProgressViewHolder(view)
            }
        }

        override fun getItemCount(): Int {
            return sharedLabelArr.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == sharedLabelArr.size - 1 && isLoading)
                VIEWTYPE_PROGRESS
            else
                VIEWTYPE_SHARED_LABEL
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.setOnClickListener {
                itemClickListener(sharedLabelArr[position]!!)
            }

            if (sharedLabelArr[position] == null)
                return

            val sharedLabel = sharedLabelArr[position]!!
            val userSharedBy = sharedLabel.share_by
            val labelShared = sharedLabel.label

            if (labelShared.images.size == 0) {
//                holder.itemView.imageView.setImageResource(R.drawable.logo)
            } else {
                for (i in 1..labelShared.images.size) {
                    if (labelShared.images[i - 1].is_cover) {
                        //Glide.with(context).load(labelShared.images[i - 1].thumbnail).into(holder.itemView.sharedLabelIV)

                        Glide.with(context!!)
                            .asBitmap()
                            .load(labelShared.images[i - 1].thumbnail)
                            .into(object : CustomTarget<Bitmap>(){
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    val sqRes = ImageHelper.getSquredBitmap(resource)
                                    val round = RoundedBitmapDrawableFactory.create(context.resources,sqRes)
                                    round.cornerRadius = HomeFragment.roundedCon
                                    holder.itemView.sharedLabelIV.setImageDrawable(round)
                                }
                                override fun onLoadCleared(placeholder: Drawable?) {
                                }
                            })

                    }
                }
            }
            holder.itemView.sharedLabelName.text = labelShared.name
            // Get Tags
            holder.itemView.tagShare.removeAllTags()
            val tagStr: String = labelShared.tags
            val tagArr: List<String> = tagStr.split(",").map { it.trim() }
            for (i in 1..tagArr.count()) {
                holder.itemView.tagShare.addTag(tagArr[i - 1])
            }
            holder.itemView.userEmailSharedTV.text = userSharedBy.email
            if (sharedLabel.edit_permission) {
                holder.itemView.accessLabelTV.setText(R.string.access_true)
            } else {
                holder.itemView.accessLabelTV.setText(R.string.access_false)
            }

            (holder as SharedLabelViewHolder).bind()
        }


        fun addData(arr: ArrayList<ShareLabel>) {
            sharedLabelArr.addAll(arr)
            notifyDataSetChanged()
        }

        fun clear(flag: Boolean = false, recyclerV: RecyclerView? = null) {
            if (!flag) {
                this.sharedLabelArr.clear()
                return
            }

            if (recyclerV != null) {
                deleteAllItems(recyclerV)
            }
        }

        fun findOneVisibleChild(layoutManager: RecyclerView.LayoutManager,
                                fromIndex: Int, toIndex: Int, completelyVisible: Boolean,
                                acceptPartiallyVisible: Boolean
        ): View? {
            val helper: OrientationHelper
            if (layoutManager.canScrollVertically()) {
                helper = OrientationHelper.createVerticalHelper(layoutManager)
            } else {
                helper = OrientationHelper.createHorizontalHelper(layoutManager)
            }

            val start = helper.getStartAfterPadding()
            val end = helper.getEndAfterPadding()
            val next = if (toIndex > fromIndex) 1 else -1
            var partiallyVisible: View? = null
            var i = fromIndex
            while (i != toIndex) {
                val child = layoutManager.getChildAt(i)
                val childStart = helper.getDecoratedStart(child)
                val childEnd = helper.getDecoratedEnd(child)
                if (childStart < end && childEnd > start) {
                    if (completelyVisible) {
                        if (childStart >= start && childEnd <= end) {
                            return child
                        } else if (acceptPartiallyVisible && partiallyVisible == null) {
                            partiallyVisible = child
                        }
                    } else {
                        return child
                    }
                }
                i += next
            }
            return partiallyVisible
        }

        fun findFirstVisibleItemPosition(recyclerView: RecyclerView): Int {
            val child = findOneVisibleChild(recyclerView.layoutManager!!, 0, recyclerView.layoutManager!!.getChildCount(), false, true)
            return if (child == null) RecyclerView.NO_POSITION else recyclerView.getChildAdapterPosition(child)
        }

        fun findLastVisibleItemPosition(recyclerView: RecyclerView): Int {
            val child = findOneVisibleChild(recyclerView.layoutManager!!, recyclerView.layoutManager!!.getChildCount() - 1, -1, false, true)
            return if (child == null) RecyclerView.NO_POSITION else recyclerView.getChildAdapterPosition(child)
        }

        private fun deleteItem(rowView: View, position: Int) {

            val anim = AnimationUtils.loadAnimation(
                context,
                android.R.anim.slide_out_right
            )
            anim.duration = DELAY_TIME + DELAY_TIME_PLUS
            rowView.startAnimation(anim)

            Handler().postDelayed(Runnable {
                if (sharedLabelArr.size == 0) {
                    //addEmptyView() // adding empty view instead of the RecyclerView
                    return@Runnable
                }
                //shareLogArr.removeAt(position) //Remove the current content from the array
                //notifyDataSetChanged() //Refresh list
                rowView.visibility = View.INVISIBLE
            }, anim.duration)
        }

        fun getAnimatedTime(mRecyclerView: RecyclerView): Long {
            if(itemCount < 2) return DELAY_TIME
            return (findLastVisibleItemPosition(mRecyclerView) - findFirstVisibleItemPosition(mRecyclerView) + 1) * DELAY_TIME
        }

        private fun deleteAllItems(mRecyclerView: RecyclerView) {

            if(itemCount == 0) return

            Log.d(TAG, "First Visible Item -> " + findFirstVisibleItemPosition(mRecyclerView))
            Log.d(TAG, "Last Visible Item -> " + findLastVisibleItemPosition(mRecyclerView))

            var mStopHandler: Boolean = false
            var cusPosition = findFirstVisibleItemPosition(mRecyclerView)

            val handler = Handler()
            val runnable = object : Runnable {
                override fun run() {

                    if (cusPosition == findLastVisibleItemPosition(mRecyclerView) + 1) {
                        mStopHandler = true
                    }

                    if (!mStopHandler) {
                        val v = mRecyclerView.findViewHolderForAdapterPosition(cusPosition)!!.itemView
                        deleteItem(v, cusPosition)
                        cusPosition++
                    } else {
                        handler.removeCallbacksAndMessages(null)
                    }

                    val postDelayed = handler.postDelayed(this, DELAY_TIME)
                }
            }
            (context as MainActivity).runOnUiThread(runnable)
        }

        fun restoreItemViews(mRecyclerView: RecyclerView) {

            if (itemCount == 0) return

            for (i in findFirstVisibleItemPosition(mRecyclerView) .. findLastVisibleItemPosition(mRecyclerView)) {
                val v = mRecyclerView.findViewHolderForAdapterPosition(i)!!.itemView
                v.visibility = View.VISIBLE
            }
        }


        fun addNullData() {
            sharedLabelArr.add(null)
            notifyItemInserted(sharedLabelArr.size)
        }

        fun removeNull() {
            sharedLabelArr.removeAt(sharedLabelArr.size - 1)
            notifyItemRemoved(sharedLabelArr.size)
        }

        class SharedLabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            @SuppressLint("SetTextI18n")
            fun bind() {
            }
        }

        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}
