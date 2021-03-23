package com.wizt.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.*
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.wizt.activities.MainActivity
import com.wizt.common.base.BaseFragment
import com.wizt.common.http.APIManager
import com.wizt.components.paginatedRecyclerView.InfiniteScrollListener
import com.wizt.components.paginatedRecyclerView.interfaces.OnLoadMoreListener
import com.wizt.R
import com.wizt.common.constants.Constants
import com.wizt.components.controllers.SwipeController
import com.wizt.components.controllers.SwipeControllerActions
import com.wizt.models.*
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_friend.*
import kotlinx.android.synthetic.main.fragment_friend.view.*
import kotlinx.android.synthetic.main.item_friend_request_row.view.*
import kotlinx.android.synthetic.main.item_friend_row.view.*
import kotlinx.android.synthetic.main.item_user_list_row.view.*
import kotlinx.android.synthetic.main.share_label_dialog.*


@Suppress("NAME_SHADOWING")
class FriendFragment : BaseFragment(), OnLoadMoreListener {

    companion object {

        const val TAG = "WIZT:FriendFragment"

        @JvmStatic
        fun newInstance() =
            FriendFragment().apply {
            }
    }

    var selectedLabel : Label? = null

    private var pagination: Pagination? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter
    private lateinit var infiniteScrollListener: InfiniteScrollListener

    private lateinit var btnFriendList: View
    private lateinit var btnAddFriend: View

    private var showEmptyIV: ImageView? = null
    private var showEmptyIV_second: ImageView? = null

    private lateinit var swipeContainer: SwipeRefreshLayout

    private var pagination_add: Pagination? = null
    private lateinit var recyclerView_add: RecyclerView
    private lateinit var linearLayoutManager_add: LinearLayoutManager

    private lateinit var adapter_add: RecyclerAdapter_add

    private lateinit var searchBarSearchView: SearchView
    private lateinit var cardView: CardView

    private var pagination_request: Pagination? = null

    private lateinit var recyclerView_request: RecyclerView
    private lateinit var linearLayoutManager_request: LinearLayoutManager
    private lateinit var adapter_request: RecyclerAdapter_request

    private var userList = ArrayList<User>()
    private var friendList = ArrayList<User>()
    private var requestList = ArrayList<FriendRequest>()
    private var isList = true

    private lateinit var swipeController: SwipeController

    @SuppressLint("ResourceAsColor")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_friend, container, false)

        layoutManager = LinearLayoutManager(context)
        infiniteScrollListener = InfiniteScrollListener(layoutManager, this)
        infiniteScrollListener.setLoaded()

        recyclerView = view.findViewById(R.id.recyclerView)
        adapter = RecyclerAdapter(context!!, arrayListOf(), selectedLabel)
        recyclerView.addOnScrollListener(infiniteScrollListener)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        showEmptyIV = view.findViewById(R.id.showEmptyIV)
        showEmptyIV?.visibility = View.VISIBLE

        showEmptyIV_second = view.findViewById(R.id.showEmptyIV_second)
        showEmptyIV_second?.visibility = View.GONE

        swipeContainer = view.findViewById(R.id.swipeContainer)
        setSwipeRefreshLayoutColor(swipeContainer)
        swipeContainer.setOnRefreshListener {
            fetchTimelineAsync()
        }

        super.bindMenuAction(view)
        view.notificationBtn.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(NotificationFragment.newInstance())
        }

        btnFriendList = view.findViewById(R.id.btnFriendList)
        btnAddFriend = view.findViewById(R.id.btnAddFriend)

        btnFriendList.setOnClickListener {
            updateUI(0)
        }

        btnAddFriend.setOnClickListener {
            updateUI(1)
        }

        linearLayoutManager_add = LinearLayoutManager(context)

        recyclerView_add = view.findViewById(R.id.recyclerViewUserList)
        adapter_add = RecyclerAdapter_add(this, arrayListOf())
        recyclerView_add.layoutManager = linearLayoutManager_add
        recyclerView_add.adapter = adapter_add

        searchBarSearchView = view.findViewById(R.id.searchBarSearchView)
        cardView = view.findViewById(R.id.cardView)

        linearLayoutManager_request = LinearLayoutManager(context)

        recyclerView_request = view.findViewById(R.id.recyclerView_request)
        adapter_request = RecyclerAdapter_request(this, arrayListOf())
        recyclerView_request.layoutManager = linearLayoutManager_request
        recyclerView_request.adapter = adapter_request

        initLayoutAndListeners()
        loadFirst()
        setupRecyclerView()

        view.fab_back.setOnClickListener {
            (context as MainActivity).onBackPressed()
        }

        return view
    }

    fun setupRecyclerView() {

        swipeController = SwipeController(context, object : SwipeControllerActions() {
            override fun onRightClicked(position: Int) {

                deleteRequest(position)

            }
        })

        val itemTouchhelper = ItemTouchHelper(swipeController)
        itemTouchhelper.attachToRecyclerView(recyclerView_request)

        recyclerView_request.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                swipeController.onDraw(c)
            }
        })

    }

    fun deleteRequest(position: Int) {
        val successCallback: () -> Unit = {
            this.requestList.removeAt(position)
            activity?.runOnUiThread {
                MyToastUtil.showMessage(context!!,"Removed Successfully!")
                adapter_request.removeAt(position)
            }
        }
        APIManager.share.deleteFriendRequest(requestList[position], successCallback, errorCallback)
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

        loadWithSearchReq()
        loadFirstAddLists()
    }

    fun loadWithSearchReq() {

        var searchList = ArrayList<User>()
        val searchString = PreferenceUtils.getString(Constants.PREF_SEARCHSTRING_USERLIST,"")
        if (!searchString!!.isEmpty()) {
            for (item in friendList) {
                if(item.username.contains(searchString) || item.name.contains(searchString)) {
                    searchList.add(item)
                }
            }
        }
        else {
            searchList.addAll(friendList)
        }

        this.adapter.clear()
        this.adapter.addData(searchList)

    }


    private fun updateUI(flag: Int) {
        if (flag == 0) {
            isList = true
            btnFriendList.setBackgroundColor(resources.getColor(R.color.colorFriendListButton_friend))
            btnAddFriend.setBackgroundColor(resources.getColor(R.color.colorAddFriendButton_friend))
            swipeContainer.visibility = View.VISIBLE
            myScrollView.visibility = View.GONE
            if(friendList.size == 0) showEmptyIV?.visibility = View.VISIBLE
            else showEmptyIV?.visibility = View.GONE
        } else {
            isList = false
            btnAddFriend.setBackgroundColor(resources.getColor(R.color.colorFriendListButton_friend))
            btnFriendList.setBackgroundColor(resources.getColor(R.color.colorAddFriendButton_friend))
            swipeContainer.visibility = View.GONE
            myScrollView.visibility = View.VISIBLE
            showEmptyIV?.visibility = View.GONE
        }
        updateShowEmptyIV_second()
    }

    private fun updateShowEmptyIV_second() {
        if(isList) {
            showEmptyIV_second?.visibility = View.GONE
            return
        }
        if (userList.size == 0 && requestList.size == 0) {
            showEmptyIV_second?.visibility = View.VISIBLE
        } else {
            showEmptyIV_second?.visibility = View.GONE
        }
    }

    override fun onLoadMore() {
        loadMoreFriendList()
    }

    fun loadMoreFriendList() {
//        if (pagination?.next == null) {
//            return
//        }
//        adapter.addNullData()
//        val successCallback: (Pagination, List<User>) -> Unit = { pagination, list ->
//            this.pagination = pagination
//            activity?.runOnUiThread {
//                adapter.removeNull()
//                adapter.addData(list as ArrayList<User>)
//                infiniteScrollListener.setLoaded()
//            }
//        }
//        APIManager.share.getFriendList(pagination?.next!!, successCallback, errorCallback)
        Log.d(TAG, "loadMoreFriendList")
        loadFirst()
    }
    private fun loadFirst() {
        Log.d(TAG, "loadFirst")
        loadFirstFriends()
        loadFirstAddLists()
        loadFirstRequests()
    }
    private fun loadFirstRequests() {
        val successCallback: (Pagination, ArrayList<FriendRequest>) -> Unit = { pagination, list ->
            this.pagination_request = pagination
            activity?.runOnUiThread {
                requestList.clear()
                if (list.isNotEmpty()) {
                    adapter_request.clear()
                    requestList.addAll(list)
                    adapter_request.addData(requestList)
                }
            }
        }
        APIManager.share.getFriendRequestList(successCallback, errorCallback)
    }
    private fun loadFirstAddLists() {
        val successCallback: (Pagination, List<User>) -> Unit = { pagination, list ->
            this.pagination_add = pagination
            activity?.runOnUiThread {

                userList.clear()
                userList.addAll(list)

                this.adapter_add.clear()
                this.adapter_add.addData(userList)
                updateShowEmptyIV_second()
            }
        }
        val searchString = PreferenceUtils.getString(Constants.PREF_SEARCHSTRING_USERLIST,"")
        APIManager.share.getUserList(searchString, successCallback, errorCallback)
    }
    private fun loadFirstFriends(isRef: Boolean = false) {
        val successCallback: (Pagination, List<User>) -> Unit = { pagination, list ->
            this.pagination = pagination
            activity?.runOnUiThread {
                if(isRef) swipeContainer.isRefreshing = false
                if (showEmptyIV != null) {
                    if (list.isEmpty())
                        showEmptyIV?.visibility = View.VISIBLE
                    else
                        showEmptyIV?.visibility = View.GONE
                }
                if (list.isNotEmpty()) {
                    adapter.clear()
                    adapter.addData(list)
                    friendList.clear()
                    friendList.addAll(list)
                }
            }
        }
        APIManager.share.getFriendList(successCallback, errorCallback)
    }
    private fun fetchTimelineAsync() {
        fetchTimelineAsyncLists()
    }
    private fun fetchTimelineAsyncLists() {
//        if (pagination?.next == null) {
//            swipeContainer.isRefreshing = false
//            return
//        }
//
//        val successCallback: (Pagination, List<User>) -> Unit = { pagination, list ->
//            this.pagination = pagination
//            this.adapter.clear()
//            activity?.runOnUiThread {
//                this.adapter.addData(list as ArrayList<User>)
//                swipeContainer.isRefreshing = false
//            }
//        }
//
//        APIManager.share.getFriendList(pagination?.next!!, successCallback, errorCallback)
        loadFirstFriends(true)
    }

    class RecyclerAdapter(private var context: Context, arr: ArrayList<User?>, label: Label?) :
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
                val view = inflater.inflate(R.layout.item_friend_row, parent, false)
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

            if (friendArr[position] == null)    return

            val user = friendArr[position]!!

            holder.itemView.userNameTV.text = user.name
            if (user.picture != null) {
                Glide.with(context).load(user.picture).into(holder.itemView.userAvatar)
            } else {
                holder.itemView.userAvatar.setImageDrawable(context.getDrawable(R.drawable.ic_default_avatar))
            }

            holder.itemView.shareBtn.setOnClickListener {
                alert(position)
            }

            holder.bind()
        }

        fun alert(position: Int) {

            if (shareLabel == null) {
                return
            }

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

                    (context as Activity).runOnUiThread {
                        MyToastUtil.showMessage(context!!,"Shared Successfully!")
                        (this.context as MainActivity).popFragment()
                    }
                }
                val errorCallback: (String) -> Unit = {message ->
                    mAlertDialog.dismiss()

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

    class RecyclerAdapter_add(private var context: FriendFragment, arr: ArrayList<User?>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

            if (userArr[position] == null)  return

            val user = userArr[position]!!
            holder.itemView.userName.text = user.name
            if (user.picture != null) {
                Glide.with(context).load(user.picture).into(holder.itemView.avatarIV)
            } else {
                holder.itemView.avatarIV.setImageDrawable(null)
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

    class RecyclerAdapter_request(private val context: FriendFragment, arr: ArrayList<FriendRequest?>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

            if (friendRequestArr[position] == null)
                return

            val friendRequest = friendRequestArr[position]!!
            val userTo = friendRequest.from_user

            if (userTo.picture != null) {
                Glide.with(context).load(userTo.picture).into(holder.itemView.userFromAvatar)
            }
            holder.itemView.userFromNameTV.text = userTo.name

            // Accept and Decline Event
            val errorCallback: (String) -> Unit = {message ->
                MyToastUtil.showWarning(context.activity!!, message)
            }
            holder.itemView.acceptBtn.setOnClickListener {
                val successCallback: () -> Unit = {
                    this.friendRequestArr.removeAt(position)
                    context.activity?.runOnUiThread {
                        MyToastUtil.showMessage(context.activity!!, "Accepted successfully!")
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

        fun removeAt(position: Int) {
            friendRequestArr.removeAt(position)
            notifyDataSetChanged()
        }

        class FriendRequestLabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
            }
        }

        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}
