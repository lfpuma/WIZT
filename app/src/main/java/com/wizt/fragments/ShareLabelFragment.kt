package com.wizt.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.wizt.models.Global
import com.wizt.models.Pagination
import com.wizt.models.ShareLabel

import com.wizt.R
import com.wizt.common.constants.Constants
import com.wizt.extensions.ImageHelper
import com.wizt.models.Label
import com.wizt.utils.PreferenceUtils
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_share_label.view.*
import kotlinx.android.synthetic.main.item_home_row.view.*
import kotlinx.android.synthetic.main.item_share_label_row.view.*


class ShareLabelFragment : BaseFragment(), OnLoadMoreListener {

    companion object {
        @JvmStatic
        fun newInstance() =
            ShareLabelFragment().apply {}
    }

    private var pagination: Pagination? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter
    private lateinit var infiniteScrollListener: InfiniteScrollListener
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var searchBarSearchView: SearchView
    private lateinit var cardView: CardView
    private var labelList = ArrayList<ShareLabel>()
    private var showEmptyIV: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceUtils.saveString(Constants.PREF_SEARCHSTRING_SHAREWITHME,"")
    }

    override fun onResume() {
        super.onResume()
        loadFirst()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_share_label, container, false)
        view.title.text = resources.getString(R.string.shared_with_me)
        super.bindMenuAction(view)
        super.bindNotificationViewAction(view)

        addAds(view)

        linearLayoutManager = LinearLayoutManager(context)
        infiniteScrollListener = InfiniteScrollListener(linearLayoutManager, this)
        infiniteScrollListener.setLoaded()

        recyclerView = view.recyclerView
        adapter = RecyclerAdapter(context!!, itemClickListener, arrayListOf())
        recyclerView.addOnScrollListener(infiniteScrollListener)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter

        showEmptyIV = view.findViewById(R.id.showEmptyIV)
//        Glide.with(context!!).load(R.drawable.animation_shared).into(showEmptyIV!!)
        showEmptyIV?.visibility = View.VISIBLE

        swipeContainer = view.findViewById(R.id.swipeContainer)
        setSwipeRefreshLayoutColor(swipeContainer)
        swipeContainer.setOnRefreshListener {
            fetchTimelineAsync()
        }

        searchBarSearchView = view.findViewById(R.id.searchBarSearchView)
        cardView = view.findViewById(R.id.cardView)
        initLayoutAndListeners()

        return view
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

    }

    fun LoadWithSearchRes(value: String?) {
        if (!value!!.isEmpty()) {
            PreferenceUtils.saveString(Constants.PREF_SEARCHSTRING_SHAREWITHME,value!!)
        }
        else {
            PreferenceUtils.saveString(Constants.PREF_SEARCHSTRING_SHAREWITHME,"")
        }

        loadWithSearchReq()
    }

    fun loadWithSearchReq() {

        var searchList = ArrayList<ShareLabel>()
        val searchString = PreferenceUtils.getString(Constants.PREF_SEARCHSTRING_SHAREWITHME,"")
        if (!searchString!!.isEmpty()) {
            for (item in labelList) {
                if(item.label.name.contains(searchString) || item.label.tags.contains(searchString)) {
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

    fun addAds(view:View) {
        if(Global.isSubscription) {
            view.adsLayout.visibility = View.GONE
            return
        }
        view.adsLayout.visibility = View.VISIBLE
        val adRequest = AdRequest.Builder().build()
        view.adsLayout.adView.loadAd(adRequest)
    }

    override fun onLoadMore() {
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

    private val itemClickListener:(ShareLabel) -> Unit = { shareLabel ->
        Global.shareLabel = shareLabel
        val fragment = LabelDetailFragment.newInstance()
        fragment.shareLabel = shareLabel
        fragment.labelType = 3
        (activity as MainActivity).popFragment()
        (activity as MainActivity).pushFragment(fragment)
    }

    fun loadFirst() {
        val successCallback: (Pagination, List<ShareLabel>) -> Unit = { pagination, list ->
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

        APIManager.share.getShareLabelList(successCallback, errorCallback)
    }

    private fun fetchTimelineAsync() {
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

    class RecyclerAdapter(private val context: Context, val itemClickListener: (shareLabel: ShareLabel) -> Unit, arr : ArrayList<ShareLabel?>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

        fun clear() {
            this.sharedLabelArr.clear()
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
