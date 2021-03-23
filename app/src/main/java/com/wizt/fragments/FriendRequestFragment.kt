package com.wizt.fragments

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.wizt.common.base.BaseFragment
import com.wizt.common.http.APIManager
import com.wizt.components.paginatedRecyclerView.InfiniteScrollListener
import com.wizt.components.paginatedRecyclerView.interfaces.OnLoadMoreListener
import com.wizt.models.FriendRequest
import com.wizt.models.Pagination

import com.wizt.R
import com.wizt.models.Global
import com.wizt.utils.MyToastUtil
import com.wizt.utils.ToastUtil
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_friend_request.view.*
import kotlinx.android.synthetic.main.item_friend_request_row.view.*


class FriendRequestFragment : BaseFragment(), OnLoadMoreListener {

    companion object {
        @JvmStatic
        fun newInstance() =
            FriendRequestFragment().apply {
            }
    }

    private var pagination: Pagination? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter
    private lateinit var infiniteScrollListener: InfiniteScrollListener
    private lateinit var swipeContainer: SwipeRefreshLayout

    private var showEmptyIV: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadFirst()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_friend_request, container, false)
        view.title.text = resources.getString(R.string.friend_request)
        bindMenuAction(view)
        bindNotificationViewAction(view)

        addAds(view)

        linearLayoutManager = LinearLayoutManager(context)
        infiniteScrollListener = InfiniteScrollListener(linearLayoutManager, this)
        infiniteScrollListener.setLoaded()

        recyclerView = view.findViewById(R.id.recyclerView)
        adapter = RecyclerAdapter(this, { labelItemClicked() }, arrayListOf())
        recyclerView.addOnScrollListener(infiniteScrollListener)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter

        showEmptyIV = view.findViewById(R.id.showEmptyIV)
        showEmptyIV?.visibility = View.VISIBLE

        swipeContainer = view.findViewById(R.id.swipeContainer)
        setSwipeRefreshLayoutColor(swipeContainer)
        swipeContainer.setOnRefreshListener {
            fetchTimelineAsync()
        }


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

    override fun onLoadMore() {
        if (this.pagination?.next == null) {
            return
        }

        adapter.addNullData()
        val successCallback: (Pagination, ArrayList<FriendRequest>) -> Unit = { pagination, list ->
            this.pagination = pagination
            activity?.runOnUiThread {
                adapter.removeNull()
                this.adapter.addData(list)
                infiniteScrollListener.setLoaded()
            }
        }
        APIManager.share.getFriendRequestList(pagination?.next!!, successCallback, errorCallback)
    }

    private fun labelItemClicked() {
        //ToastUtil.show(context, "Item clicked")
    }

    private fun loadFirst() {
        val successCallback: (Pagination, ArrayList<FriendRequest>) -> Unit = { pagination, list ->
            this.pagination = pagination
            activity?.runOnUiThread {
                if (showEmptyIV != null) {
                    if (list.isEmpty())
                        showEmptyIV?.visibility = View.VISIBLE
                    else
                        showEmptyIV?.visibility = View.GONE
                }
                if (list.isNotEmpty()) {
                    adapter.clear()
                    adapter.addData(list)
                }
            }
        }
        APIManager.share.getFriendRequestList(successCallback, errorCallback)
    }

    private fun fetchTimelineAsync() {
        if (this.pagination?.next == null) {
            swipeContainer.isRefreshing = false
            return
        }

        val successCallback: (Pagination, ArrayList<FriendRequest>) -> Unit = { pagination, list ->
            this.pagination = pagination
            this.adapter.clear()
            activity?.runOnUiThread {
                this.adapter.addData(list)
                swipeContainer.isRefreshing = false
            }
        }

        APIManager.share.getFriendRequestList(pagination?.next!!, successCallback, errorCallback)
    }


    class RecyclerAdapter(private val context: FriendRequestFragment, val itemClickListener: () -> Unit, arr: ArrayList<FriendRequest?>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var isLoading = false
        private val VIEWTYPE_FRIEND_REQUEST = 1
        private val VIEWTYPE_PROGRESS = 2

        private val friendRequestArr : ArrayList<FriendRequest?> = arr

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view: View

            return if (p1 == VIEWTYPE_FRIEND_REQUEST) {
                view = inflater.inflate(R.layout.item_friend_request_row, parent, false)
                FriendRequestLabelViewHolder(view)
            } else {
                view = inflater.inflate(R.layout.item_progress_row, parent, false)
                ProgressViewHolder(view)
            }
        }

        override fun getItemCount(): Int {
            return friendRequestArr.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == friendRequestArr.size - 1 && isLoading)
                VIEWTYPE_PROGRESS
            else
                VIEWTYPE_FRIEND_REQUEST
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.setOnClickListener {
                itemClickListener()
            }

            if (friendRequestArr[position] == null)
                return

            val friendRequest = friendRequestArr[position]!!
            val userTo = friendRequest.from_user

            if (userTo.picture != null) {
                Glide.with(context).load(userTo.picture).into(holder.itemView.userFromAvatar)
            }
            holder.itemView.userFromNameTV.text = userTo.username

            // Accept and Decline Event
            val errorCallback: (String) -> Unit = {message ->
                MyToastUtil.showWarning(context.activity!!, message)
            }
            holder.itemView.acceptBtn.setOnClickListener {
                val successCallback: () -> Unit = {
                    this.friendRequestArr.removeAt(position)
                    context.activity?.runOnUiThread {
                        notifyDataSetChanged()
                    }
                }
                APIManager.share.acceptFriendRequest(friendRequestArr[position]!!,successCallback, errorCallback)
            }

            holder.itemView.declineBtn.setOnClickListener {
                val successCallback: () -> Unit = {
                    this.friendRequestArr.removeAt(position)
                    context.activity?.runOnUiThread {
                        notifyDataSetChanged()
                    }
                }
                APIManager.share.deleteFriendRequest(friendRequestArr[position]!!, successCallback, errorCallback)
            }

            (holder as FriendRequestLabelViewHolder).bind()
        }

        fun addData(arr: ArrayList<FriendRequest>) {
            friendRequestArr.addAll(arr)
            notifyDataSetChanged()
        }

        fun clear() {
            this.friendRequestArr.clear()
        }

        fun addNullData() {
            friendRequestArr.add(null)
            notifyItemInserted(friendRequestArr.size)
        }

        fun removeNull() {
            friendRequestArr.removeAt(friendRequestArr.size - 1)
            notifyItemRemoved(friendRequestArr.size)
        }

        class FriendRequestLabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
            }
        }

        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}
