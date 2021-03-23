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
import com.wizt.R
import com.wizt.activities.FloorPlanActivity
import com.wizt.activities.MainActivity
import com.wizt.common.base.BaseFragment
import com.wizt.common.constants.Constants
import com.wizt.common.http.APIManager
import com.wizt.components.paginatedRecyclerView.InfiniteScrollListener
import com.wizt.components.paginatedRecyclerView.interfaces.OnLoadMoreListener
import com.wizt.extensions.ImageHelper
import com.wizt.models.Global
import com.wizt.models.Label
import com.wizt.models.Pagination
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import kotlinx.android.synthetic.main.activity_object_trained.view.*
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.app_bar.view.menuBtn
import kotlinx.android.synthetic.main.item_home_row.view.*

class FloorplanLabelsFragment : BaseFragment(), OnLoadMoreListener {

    private var pagination: Pagination? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var infiniteScrollListener: InfiniteScrollListener
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var searchBarSearchView: SearchView
    private lateinit var cardView: CardView
    private var labelList = ArrayList<Label>()
    private var showEmptyIV: ImageView? = null
    var searchTagName = "Nothing"

    companion object {

        const val TAG = "WIZT:FloorplanLabelsFragment"

        const val roundedCon = 25.0f

        @JvmStatic
        fun newInstance() =
            FloorplanLabelsFragment().apply {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //loadFirst()
        setUserVisibleHint(false)
    }

    override fun onResume() {
        super.onResume()
        loadFirst()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_floorplanlabels, container, false)
        view.title.text = searchTagName
        view.menuBtn.visibility = View.GONE
        view.notificationBtn.visibility = View.GONE

        super.bindMenuAction(view)
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
    private val itemClickListener:(Label) -> Unit = { label ->
        val isFocus: Boolean = PreferenceUtils.getBoolean(Constants.PREF_HOME_IS_FOCUS)
        Global.label = label
        if (!isFocus) {
            val fragment = LabelDetailFragment.newInstance()
            fragment.label = label
            fragment.labelType = 1
            (activity as FloorPlanActivity).pushFragment(fragment)
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
        val searchString = searchTagName
        if (!searchString!!.isEmpty()) {
            for (item in labelList) {
                if(item.location.contains(searchString)) {
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

        if (searchList.isEmpty()) {

            MyToastUtil.showWarning(context!!,"No Room")

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


    class RecyclerAdapter(private var context: Context, val itemClickListener: (label: Label) -> Unit, arr: ArrayList<Label?>) :
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
            holder.itemView.setOnClickListener {

                MyToastUtil.showMessage(context,"You can edit this label on Home Screen.")

                val label = labelArr[position]
                if (label != null) {
                    //itemClickListener(label)
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
                            .load(label.images[i - 1].thumbnail)
                            .into(object : CustomTarget<Bitmap>(){
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    val sqRes = ImageHelper.getSquredBitmap(resource)
                                    val round = RoundedBitmapDrawableFactory.create(context.resources,sqRes)
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
            val tagStr: String = label.tags
            val tagArr: List<String> = tagStr.split(",").map { it.trim() }
            for (i in 1..tagArr.count()) {
                holder.itemView.tagHome.addTag(tagArr[i - 1])
            }
            holder.itemView.locationTextView.text = label.location

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
