package com.wizt.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.wizt.common.base.BaseFragment
import com.wizt.common.http.APIManager
import com.wizt.components.paginatedRecyclerView.InfiniteScrollListener
import com.wizt.components.paginatedRecyclerView.interfaces.OnLoadMoreListener
import com.wizt.R
import com.wizt.activities.MainActivity
import com.wizt.models.*
import com.wizt.utils.MyToastUtil
import com.wizt.utils.ToastUtil
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.item_share_friend_list.view.*
import kotlinx.android.synthetic.main.item_share_friend_list.view.shareBtn
import kotlinx.android.synthetic.main.share_label_dialog.*

@Suppress("UNREACHABLE_CODE")
class ShareFriendFragment: BaseFragment(), OnLoadMoreListener {

    companion object {
        @JvmStatic
        fun newInstance() =
            ShareFriendFragment().apply {
            }
    }

    lateinit var selectedLabel : Label

    private var pagination: Pagination? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private var adapter: RecyclerAdapter? = null
    private lateinit var infiniteScrollListener: InfiniteScrollListener

    private lateinit var swipeContainer: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadFirst()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_share_friend_list, container, false)

        view.title.text = resources.getString(R.string.friend)
        view.notificationBtn.visibility = View.GONE
        view.menuBtn.setImageResource(R.drawable.ic_arrow_left_white)

        layoutManager = LinearLayoutManager(context)
        infiniteScrollListener = InfiniteScrollListener(layoutManager, this)
        infiniteScrollListener.setLoaded()

        recyclerView = view.findViewById(R.id.recyclerView)
        adapter = RecyclerAdapter(context!!, { labelItemClicked() }, arrayListOf(), selectedLabel)
        recyclerView.addOnScrollListener(infiniteScrollListener)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        swipeContainer = view.findViewById(R.id.swipeContainer)
        setSwipeRefreshLayoutColor(swipeContainer)
        swipeContainer.setOnRefreshListener {
            fetchTimelineAsync()
        }

        view.menuBtn.setOnClickListener {
            backToFragment()
        }

        return view
    }

    override fun onLoadMore() {
        if (pagination?.next == null) {
            return
        }

        adapter?.addNullData()
        val successCallback: (Pagination, List<User>) -> Unit = { pagination, list ->
            this.pagination = pagination
            activity?.runOnUiThread {
                adapter?.removeNull()
                adapter?.addData(list)
                infiniteScrollListener.setLoaded()
            }
        }
        APIManager.share.getFriendList(pagination?.next!!, successCallback, errorCallback)
    }

    fun backToFragment() {

        (activity as MainActivity).popFragment()
//        Global.label = selectedLabel
//        val fragment = LabelDetailFragment.newInstance()
//        fragment.label = selectedLabel
//        fragment.labelType = 1
//        (activity as MainActivity).replaceFragment(fragment)

    }

    private fun labelItemClicked() {
        //ToastUtil.show(context, "Item clicked")
    }

    private fun loadFirst() {
        val successCallback: (Pagination, List<User>) -> Unit = { pagination, list ->
            this.pagination = pagination
            activity?.runOnUiThread {
                if (list.isNotEmpty()) {
                    adapter?.clear()
                    adapter?.addData(list)
                }
            }
        }
        APIManager.share.getFriendList(successCallback, errorCallback)
    }

    private fun fetchTimelineAsync() {
        if (pagination?.next == null) {
            swipeContainer.isRefreshing = false
            return
        }

        val successCallback: (Pagination, List<User>) -> Unit = { pagination, list ->
            this.pagination = pagination
            this.adapter?.clear()
            activity?.runOnUiThread {
                this.adapter?.addData(list)
                swipeContainer.isRefreshing = false
            }
        }

        APIManager.share.getFriendList(pagination?.next!!, successCallback, errorCallback)
    }

    @Suppress("NAME_SHADOWING")
    class RecyclerAdapter(private var context: Context, val itemClickListener: () -> Unit, arr: ArrayList<User?>, label: Label) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var isLoading = false
        private val VIEWTYPE_FRIEND = 1
        private val VIEWTYPE_PROGRESS = 2
        private val shareLabel = label

        private var friendArr: ArrayList<User?> = arr

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view: View

            return if (p1 == VIEWTYPE_FRIEND) {
                val view = inflater.inflate(R.layout.item_share_friend_list, parent, false)
                UserViewHolder(view)
            } else {
                view = inflater.inflate(R.layout.item_progress_row, parent, false)
                ProgressViewHolder(view)
            }
        }

        override fun getItemCount(): Int {
            return friendArr.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == friendArr.size - 1 && isLoading)
                VIEWTYPE_PROGRESS
            else
                VIEWTYPE_FRIEND
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is UserViewHolder) {
                bindUserViewHolder(holder, position)
            }
        }

        private fun bindUserViewHolder(holder: UserViewHolder, position: Int) {
            holder.itemView.setOnClickListener {
                itemClickListener()
            }

            if (friendArr[position] == null)    return

            val user = friendArr[position]!!

            holder.itemView.userName.text = user.name
            if (user.picture != null) {
                Glide.with(context).load(user.picture).into(holder.itemView.avatarIV)
            }
            holder.itemView.userEmailTV.text = user.email

            holder.itemView.shareBtn.setOnClickListener {
                alert(position)
            }

            holder.bind()
        }

        @SuppressLint("InflateParams")
        fun alert(position: Int) {
            // Inflate the dialog with custom view
            val mDialogView = LayoutInflater.from(context).inflate(R.layout.share_label_dialog, null)
            // AlertDialogBuilder
            val mBuilder = AlertDialog.Builder(context)
                .setView(mDialogView)
            val mAlertDialog = mBuilder.show()

            mAlertDialog.shareBtn.setOnClickListener {

                val editPermission = mAlertDialog.editCB.isChecked

                val successCallback: (ShareLabel) -> Unit = {
                    mAlertDialog.dismiss()



                    //NewCode
                    (context as Activity).runOnUiThread {
                        MyToastUtil.showMessage(context!!,"Shared Successfully!")
                        (this.context as MainActivity).popFragment()
                    }
                }
                val errorCallback: (String) -> Unit = {message ->
                    mAlertDialog.dismiss()

                    //NewCode
                    (context as Activity).runOnUiThread {
                        (this.context as MainActivity).popFragment()
                    }
                }



                APIManager.share.shareLabel(shareLabel, editPermission, friendArr[position]!!, successCallback, errorCallback)

            }
            mAlertDialog.closeBtn.setOnClickListener {
                mAlertDialog.dismiss()
            }
        }

        fun addData(arr: List<User>) {
            friendArr.addAll(arr)
            notifyDataSetChanged()
        }

        fun clear() {
            this.friendArr.clear()
        }

        fun addNullData() {
            friendArr.add(null)
            notifyItemInserted(friendArr.size)
        }

        fun removeNull() {
            friendArr.removeAt(friendArr.size - 1)
            notifyItemRemoved(friendArr.size)
        }

        class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
            }
        }
        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}