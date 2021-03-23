package com.wizt.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.wizt.common.base.BaseFragment
import com.wizt.common.http.APIManager
import com.wizt.components.paginatedRecyclerView.InfiniteScrollListener
import com.wizt.components.paginatedRecyclerView.interfaces.OnLoadMoreListener
import com.wizt.models.Notification
import com.wizt.models.Pagination

import com.wizt.R
import com.wizt.activities.MainActivity
import com.wizt.utils.DateTimeUtils
import com.wizt.utils.MyToastUtil
import com.wizt.utils.ToastUtil
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.item_clear_notification.view.*
import kotlinx.android.synthetic.main.item_notification_row.view.*


class NotificationFragment : BaseFragment(), OnLoadMoreListener {

    private var pagination: Pagination? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter

    private lateinit var infiniteScrollListener: InfiniteScrollListener

    private lateinit var swipeContainer: SwipeRefreshLayout

    private var showEmptyIV: ImageView? = null

    companion object {
        @JvmStatic
        fun newInstance() =
            NotificationFragment().apply {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadFirst()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)
        view.title.text = resources.getText(R.string.notification)
        //view.notificationBtn.setImageResource(R.drawable.ic_changecamera)
        view.notificationBtn.setImageResource(R.drawable.ic_bell)
        super.bindMenuAction(view)

        linearLayoutManager = LinearLayoutManager(context)
        infiniteScrollListener = InfiniteScrollListener(linearLayoutManager, this)
        infiniteScrollListener.setLoaded()

        recyclerView = view.findViewById(R.id.recyclerView)
        adapter = RecyclerAdapter(context!!, { labelItemClicked() }, arrayListOf())
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

    private fun labelItemClicked() {
        //ToastUtil.show(context, "Item clicked")
    }

    private fun loadFirst() {
        val successCallback: (Pagination, ArrayList<Notification>) -> Unit = { pagination, list ->
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
                    list.sortWith(object: Comparator<Notification>{
                        override fun compare(p1: Notification, p2: Notification): Int {
                            return DateTimeUtils().getDateFromString(p2.created_at).compareTo(DateTimeUtils().getDateFromString(p1.created_at))
                        }
                    })
                    this.adapter.addData(list)
                }
            }
        }
        APIManager.share.getNotificationList(successCallback, errorCallback)
    }

    override fun onLoadMore() {
        if (this.pagination?.next == null) {
            return
        }

        adapter.addNullData()
        val successCallback: (Pagination, ArrayList<Notification>) -> Unit = { pagination, list ->
            this.pagination = pagination
            activity?.runOnUiThread {
                adapter.removeNull()
                this.adapter.addData(list)
                infiniteScrollListener.setLoaded()
            }
        }
        APIManager.share.getNotificationList(pagination?.next!!, successCallback, errorCallback)
    }

    private fun fetchTimelineAsync() {
        if (this.pagination?.next == null) {
            swipeContainer.isRefreshing = false
            return
        }

        val successCallback: (Pagination, ArrayList<Notification>) -> Unit = { pagination, list ->
            this.pagination = pagination
            this.adapter.clear()
            activity?.runOnUiThread {
                this.adapter.addData(list)
                swipeContainer.isRefreshing = false
            }
        }

        APIManager.share.getNotificationList(pagination?.next!!, successCallback, errorCallback)
    }

    class RecyclerAdapter(
        private val context: Context,
        val itemClickListener: () -> Unit,
        arr: ArrayList<Notification?>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var isLoading = false
        private val VIEWTYPE_NOTIFICATION = 1
        private val VIEWTYPE_PROGRESS = 2
        private val VIEWTYPE_CLEARALL = 3

        private var notificationArr: ArrayList<Notification?> = arr

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view: View

            return if (p1 == VIEWTYPE_NOTIFICATION) {
                view = inflater.inflate(R.layout.item_notification_row, parent, false)
                NotificationViewHolder(view)
            } else if (p1 == VIEWTYPE_PROGRESS) {
                view = inflater.inflate(R.layout.item_progress_row, parent, false)
                ProgressViewHolder(view)
            }
            else {
                view = inflater.inflate(R.layout.item_clear_notification, parent, false)
                ClearViewHolder(view)
            }
        }

        override fun getItemCount(): Int {
            if (notificationArr.size == 0) return 0
            return notificationArr.size + 1
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == notificationArr.size - 1 && isLoading)
                VIEWTYPE_PROGRESS
            else if (position == notificationArr.size) {
                VIEWTYPE_CLEARALL
            } else
                VIEWTYPE_NOTIFICATION
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            if (position == notificationArr.size) {

                val activity : MainActivity = context as MainActivity

                holder.itemView.clearAllLay.setOnClickListener {

                    val successCallback: () -> Unit = {
                        activity.runOnUiThread {
                            clear()
                            notifyDataSetChanged()
                        }
                    }
                    val errorCallback: (String) -> Unit = { message ->
                        activity.runOnUiThread {
                            MyToastUtil.showWarning(activity,message)
                        }
                    }
                    APIManager.share.clearAllNotification(successCallback, errorCallback)

                }
            }
            else {
                holder.itemView.setOnClickListener {
                    itemClickListener()
                }

                if (notificationArr[position] == null) return

                val notification = notificationArr[position]!!
                val user = notification.send_by
                holder.itemView.nameTV.text = user.name
                holder.itemView.timeTV.text = DateTimeUtils().formatTimeAgo(notification.created_at)
                holder.itemView.messageTV.text = notification.message
                if (user.picture != null) {
                    Glide.with(context).load(user.picture).into(holder.itemView.imageViewNotify)
                } else {
                    holder.itemView.imageViewNotify.setImageDrawable(context.getDrawable(R.drawable.ic_default_avatar))
                }

                (holder as NotificationViewHolder).bind(itemClickListener)
            }


        }

        fun addData(arr: ArrayList<Notification>) {
            notificationArr.addAll(arr)
            notifyDataSetChanged()
        }

        fun clear() {
            this.notificationArr.clear()
        }

        fun addNullData() {
            notificationArr.add(null)
            notifyItemInserted(notificationArr.size)
        }

        fun removeNull() {
            notificationArr.removeAt(notificationArr.size - 1)
            notifyItemRemoved(notificationArr.size)
        }

        class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(clickListener: () -> Unit) {
                itemView.setOnClickListener { clickListener }
            }
        }

        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        class ClearViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}

