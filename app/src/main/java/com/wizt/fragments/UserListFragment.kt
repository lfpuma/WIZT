package com.wizt.fragments

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.nicolettilu.hiddensearchwithrecyclerview.HiddenSearchWithRecyclerView
import com.wizt.common.base.BaseFragment
import com.wizt.common.http.APIManager
import com.wizt.components.paginatedRecyclerView.InfiniteScrollListener
import com.wizt.components.paginatedRecyclerView.interfaces.OnLoadMoreListener
import com.wizt.R
import com.wizt.activities.MainActivity
import com.wizt.common.constants.Constants
import com.wizt.components.activityImprove.IOnBackPressed
import com.wizt.models.*
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import com.wizt.utils.ToastUtil
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_user_list.view.*
import kotlinx.android.synthetic.main.item_user_list_row.view.*

class UserListFragment : BaseFragment(), OnLoadMoreListener, IOnBackPressed {

    var mDelayHandler: Handler? = null

    companion object {

        const val TAG = "WIZT:UserListFragment"

        @JvmStatic
        fun newInstance() =
            UserListFragment().apply {}
    }

    private var pagination: Pagination? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var adapter: RecyclerAdapter

    private lateinit var infiniteScrollListener: InfiniteScrollListener
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var searchBarSearchView: SearchView
    private lateinit var cardView: CardView

    private var userList = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_user_list, container, false)
        view.title.setText(R.string.user_list)
        view.notificationBtn.visibility = View.GONE
        super.bindMenuAction(view)

        addAds(view)

        //super.bindNotificationViewAction(view)

        linearLayoutManager = LinearLayoutManager(context)
        infiniteScrollListener = InfiniteScrollListener(linearLayoutManager, this)
        infiniteScrollListener.setLoaded()

        recyclerView = view.findViewById(R.id.recyclerViewUserList)
        adapter = RecyclerAdapter(this, { labelItemClicked() }, arrayListOf())
        recyclerView.addOnScrollListener(infiniteScrollListener)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter

        swipeContainer = view.findViewById(R.id.swipeContainer)
        setSwipeRefreshLayoutColor(swipeContainer)
        swipeContainer.setOnRefreshListener {
            fetchTimelineAsync()
        }

        searchBarSearchView = view.findViewById(R.id.searchBarSearchView)
        cardView = view.findViewById(R.id.cardView)
        initLayoutAndListeners()
        loadFirst()

        return view
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

    fun initLayoutAndListeners() {

        PreferenceUtils.saveString(Constants.PREF_SEARCHSTRING_USERLIST,"")

        val editText: EditText = searchBarSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text)
        editText.setTextColor(Color.WHITE)
        editText.setHintTextColor(Color.WHITE)

        searchBarSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(value: String?): Boolean {
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
            PreferenceUtils.saveString(Constants.PREF_SEARCHSTRING_USERLIST,value!!)
        }
        else {
            PreferenceUtils.saveString(Constants.PREF_SEARCHSTRING_USERLIST,"")
        }

        //loadWithSearchReq()
        loadFirst()
    }

    fun loadWithSearchReq() {

        var searchList = ArrayList<User>()
        val searchString = PreferenceUtils.getString(Constants.PREF_SEARCHSTRING_USERLIST,"")
        if (!searchString!!.isEmpty()) {
            for (item in userList) {
                if(item.username.contains(searchString)) {
                    searchList.add(item)
                }
            }
        }
        else {
            searchList.addAll(userList)
        }

        this.adapter.clear()
        this.adapter.addData(searchList)

    }

    override fun onLoadMore() {
        if (this.pagination?.next == null) {
            return
        }

        adapter.addNullData()
        val successCallback: (Pagination, List<User>) -> Unit = { pagination, list ->
            this.pagination = pagination
            activity?.runOnUiThread {
                adapter.removeNull()
                this.adapter.addData(list)
                infiniteScrollListener.setLoaded()
            }
        }
        APIManager.share.getUserListWithURL(pagination?.next!!, successCallback, errorCallback)
    }

    override fun onBackPressed(): Boolean {

        Log.d(TAG, "backPress button Tapped!")

        return if (true) {
            (activity as MainActivity).replaceFragment(FriendFragment.newInstance())
            true
        } else {
            false
        }
    }

    private fun loadFirst() {
        val successCallback: (Pagination, List<User>) -> Unit = { pagination, list ->
            this.pagination = pagination
            activity?.runOnUiThread {

                userList.clear()
                userList.addAll(list)

                loadWithSearchReq()

            }
        }

        val searchString = PreferenceUtils.getString(Constants.PREF_SEARCHSTRING_USERLIST,"")
        APIManager.share.getUserList(searchString, successCallback, errorCallback)
    }

    private fun fetchTimelineAsync() {
        if (this.pagination?.next == null) {
            swipeContainer.isRefreshing = false
            return
        }

        val successCallback: (Pagination, List<User>) -> Unit = { pagination, list ->
            this.pagination = pagination
            this.adapter.clear()
            activity?.runOnUiThread {
                this.adapter.addData(list)
                swipeContainer.isRefreshing = false
            }
        }

        APIManager.share.getUserListWithURL(pagination?.next!!, successCallback, errorCallback)
    }

    private fun labelItemClicked() {
        //ToastUtil.show(context, "Item clicked")
    }

    class RecyclerAdapter(private var context: UserListFragment, val itemClickListener: () -> Unit, arr: ArrayList<User?>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var isLoading = false
        private val VIEWTYPE_USER = 1
        private val VIEWTYPE_PROGRESS = 2

        private var userArr: ArrayList<User?> = arr

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view: View

            return if (p1 == VIEWTYPE_USER) {
                view = inflater.inflate(R.layout.item_user_list_row, parent, false)
                UserViewHolder(view)
            } else {
                view = inflater.inflate(R.layout.item_progress_row, parent, false)
                ProgressViewHolder(view)
            }
        }

        override fun getItemCount(): Int {
            return userArr.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == userArr.size - 1 && isLoading)
                VIEWTYPE_PROGRESS
            else
                VIEWTYPE_USER
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.setOnClickListener {
                itemClickListener()
            }

            if (userArr[position] == null)  return

            val user = userArr[position]!!
            holder.itemView.userName.text = user.name
            if (user.picture != null) {
                Glide.with(context).load(user.picture).into(holder.itemView.avatarIV)
            }



            val activity = context.activity

            // Send friend Request
            val errorCallback: (String) -> Unit = {message ->
                activity?.runOnUiThread {
                    MyToastUtil.showWarning(context.context!!, message)
                }
            }
            holder.itemView.addFriendBtn.setOnClickListener {
                val successCallback: (FriendRequest) -> Unit = { request ->
                    removeDataAtPosition(position)
                }
                APIManager.share.sendFriendRequest(userArr[position]!!, successCallback, errorCallback)
            }

            (holder as UserViewHolder).bind()
        }

        fun addData(arr: List<User>) {
            userArr.addAll(arr)
            notifyDataSetChanged()
        }

        fun clear() {
            this.userArr.clear()
        }

        fun addNullData() {
            userArr.add(null)
            notifyItemInserted(userArr.size)
        }

        fun removeDataAtPosition(position: Int) {
            val activity = (context).activity
            userArr.removeAt(position)
            activity?.runOnUiThread {
                notifyItemRemoved(position)
            }
        }

        fun removeNull() {
            userArr.removeAt(userArr.size - 1)
            notifyItemRemoved(userArr.size)
        }

        class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
            }
        }

        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}