package com.wizt.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.*
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.wizt.R
import com.wizt.common.base.BaseActivity
import com.wizt.common.base.BaseFragment
import com.wizt.common.constants.Constants
import com.wizt.common.http.APIManager
import com.wizt.components.paginatedRecyclerView.InfiniteScrollListener
import com.wizt.components.paginatedRecyclerView.interfaces.OnLoadMoreListener
import com.wizt.extensions.ImageHelper
import com.wizt.models.*
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import kotlinx.android.synthetic.main.activity_object_trained.*
import kotlinx.android.synthetic.main.fragment_label_detail.view.*
import kotlinx.android.synthetic.main.item_home_row.view.*
import kotlinx.android.synthetic.main.item_label_image.view.*
import kotlinx.android.synthetic.main.item_object_row.view.*
import kotlinx.android.synthetic.main.item_object_row.view.deleteIV
import kotlinx.android.synthetic.main.item_object_row.view.editIV

class ObjectTrainedActivity : BaseActivity(), OnLoadMoreListener {

    private var pagination: Pagination? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var infiniteScrollListener: InfiniteScrollListener
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var searchBarSearchView: SearchView
    private lateinit var cardView: CardView
    private var trainObjectList = ArrayList<TrainObject>()
    private var showEmptyIV: ImageView? = null

    companion object {

        const val TAG = "WIZT:ObjectTrainedActivity"
        const val roundedCon = 25.0f
    }

    override fun onResume() {
        super.onResume()
        loadFirst()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_trained)
        PreferenceUtils.saveString(Constants.PREF_SEARCHSTRING_OBJECT,"")

        linearLayoutManager = LinearLayoutManager(this)
        infiniteScrollListener = InfiniteScrollListener(linearLayoutManager, this)
        infiniteScrollListener.setLoaded()

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.addOnScrollListener(infiniteScrollListener)
        adapter = this.let { RecyclerAdapter(it, itemClickListener, arrayListOf()) }!!

        showEmptyIV = findViewById(R.id.showEmptyIV)
        //Glide.with(this).load(R.drawable.animation_home).into(showEmptyIV!!)
        showEmptyIV!!.visibility = View.INVISIBLE

        recyclerView.adapter = adapter

        swipeContainer = findViewById(R.id.swipeContainer)
        swipeContainer.setOnRefreshListener {
            fetchTimelineAsync()
        }

        menuBtn.setOnClickListener {
            finish()
        }

        searchBarSearchView = findViewById(R.id.searchBarSearchView)
        cardView = findViewById(R.id.cardView)
        initLayoutAndListeners()
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
            PreferenceUtils.saveString(Constants.PREF_SEARCHSTRING_OBJECT,value!!)
        }
        else {
            PreferenceUtils.saveString(Constants.PREF_SEARCHSTRING_OBJECT,"")
        }

        loadWithSearchReq()
    }

    override fun onLoadMore() {
        if (pagination?.next == null) {
            return
        }

        adapter.addNullData()

        val successCallback: (Pagination, List<TrainObject>) -> Unit = { pagination, list ->
            this.pagination = pagination
            runOnUiThread {
                adapter.removeNull()
                adapter.addData(list)
                infiniteScrollListener.setLoaded()
            }
        }

        APIManager.share.getTrains(pagination?.next!!, successCallback, errorCallback)
    }

    /**
     * UI Events
     */
    private val itemClickListener:(TrainObject) -> Unit = { trainObj ->

    }

    /**
     * Load label data
     */
    fun loadFirst() {
        val successCallback: (Pagination, List<TrainObject>) -> Unit = { pagination, list ->
            this.pagination = pagination
            runOnUiThread {
                if (showEmptyIV != null) {
                    if (list.isEmpty()) {
                        showEmptyIV!!.visibility = View.VISIBLE
                    } else {
                        showEmptyIV!!.visibility = View.GONE
                    }
                }

                trainObjectList.clear()
                trainObjectList.addAll(list)

                loadWithSearchReq()
            }
        }

        APIManager.share.getTrainedObjects(successCallback, errorCallback)
    }

    fun loadWithSearchReq() {

        var searchList = ArrayList<TrainObject>()
        val searchString = PreferenceUtils.getString(Constants.PREF_SEARCHSTRING_OBJECT,"")
        if (!searchString!!.isEmpty()) {
            for (item in trainObjectList) {
                if(item.name.contains(searchString)) {
                    searchList.add(item)
                }
            }
        }
        else {
            searchList.addAll(trainObjectList)
        }

//        if (showEmptyIV != null) {
//            if (searchList.isEmpty()) {
//                showEmptyIV!!.visibility = View.VISIBLE
//            } else {
//                showEmptyIV!!.visibility = View.GONE
//            }
//        }

        this.adapter.clear()
        this.adapter.addData(searchList)

    }

    private fun fetchTimelineAsync() {
        if (pagination?.next == null) {
            swipeContainer.isRefreshing = false
            return
        }

        val successCallback: (Pagination, List<TrainObject>) -> Unit = { pagination, list ->
            this.pagination = pagination
            this.adapter.clear()
            runOnUiThread {
                this.adapter.addData(list)
                swipeContainer.isRefreshing = false
            }
        }

        APIManager.share.getTrains(pagination?.next!!, successCallback, errorCallback)
    }


    class RecyclerAdapter(private var context: Context, val itemClickListener: (label: TrainObject) -> Unit, arr: ArrayList<TrainObject?>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var isLoading = false
        private val VIEWTYPE_OBJECT = 1
        private val VIEWTYPE_PROGRESS = 2

        private var labelArr: ArrayList<TrainObject?> = arr
        private val activity : ObjectTrainedActivity? = null

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view: View

            return if (p1 == VIEWTYPE_OBJECT) {
                view = inflater.inflate(R.layout.item_object_row, parent, false)
                ObjectViewHolder(view)
            } else {
                view = inflater.inflate(R.layout.item_progress_row, parent, false)
                ProgressViewHolder(view)
            }

            activity = parent.context as ObjectTrainedActivity

        }

        override fun getItemCount(): Int {
            return labelArr.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == labelArr.size - 1 && isLoading)
                VIEWTYPE_PROGRESS
            else
                VIEWTYPE_OBJECT
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ObjectViewHolder) {
                bindObjectViewHolder(holder, position)
            } else {

            }
        }

        private fun bindObjectViewHolder(holder: ObjectViewHolder, position: Int) {
            val obj = labelArr[position] ?: return
            holder.itemView.objectNameTV.text = obj.name

            var gridLayoutManager: GridLayoutManager

            gridLayoutManager = GridLayoutManager(context, 3)
            holder.itemView.imageRecyclerView.layoutManager = gridLayoutManager
            holder.itemView.imageRecyclerView.adapter = RecyclerAdapter_Obj(context, objItemClickListener, obj.images)

            val lp = holder.itemView.imageRecyclerView.layoutParams
            var heightCount = obj.images.size / 3
            if(obj.images.size % 3 !=0 ) heightCount = heightCount + 1
            lp.height = heightCount * context.resources.getDimension(R.dimen.itemHeight).toInt()

            holder.itemView.editIV.setOnClickListener {

                Global.trainObject = obj
                val intent = Intent(context, CreateTrainObjectActivity::class.java)
                intent.putExtra(Constants.EXTRA_CREATE_TRAIN_ACTIVITY_TYPE, false)
                context.startActivity(intent)

            }

            holder.itemView.deleteIV.setOnClickListener {

                lateinit var dialog: AlertDialog
                val builder = AlertDialog.Builder(context)
                builder.setTitle("")
                builder.setMessage("Are you sure?")
                val dialogClickListener = DialogInterface.OnClickListener{ _, which ->
                    when(which){
                        DialogInterface.BUTTON_POSITIVE -> deleteOne(position,obj)
                        //DialogInterface.BUTTON_NEGATIVE ->
                    }
                }

                builder.setPositiveButton(Html.fromHtml("<font color='#000000'>YES</font>"),dialogClickListener)
                builder.setNegativeButton(Html.fromHtml("<font color='#000000'>No</font>"),dialogClickListener)
                dialog = builder.create()

                dialog.show()



            }
        }

        fun deleteOne(position: Int, obj: TrainObject) {
            val successCallback: () -> Unit = {
                (context as BaseActivity).runOnUiThread {
                    labelArr.removeAt(position)
                    notifyDataSetChanged()
                }
            }

            val errorCallback: (String) -> Unit = { message ->
                (context as BaseActivity).runOnUiThread {
                    MyToastUtil.showWarning(context,"Failed")
                }
            }

            APIManager.share.deleteTrain(obj, successCallback, errorCallback)
        }

        fun addData(list: List<TrainObject>) {
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

        class ObjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            @SuppressLint("SetTextI18n")
            fun bind() {
            }
        }

        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)


        private val objItemClickListener:(Int) -> Unit = { position ->

            //MyDialog.showCustomDialog(this,labelImageURL[position],"",position)

        }

        class RecyclerAdapter_Obj(private val context: Context, val objItemClickListener: (int: Int) -> Unit, arr: ArrayList<FybeImage>) :
            RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            private var labelThumbUrlAdapter: ArrayList<FybeImage> = arr

            override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.item_obj_image, parent, false)

                return LabelViewHolder(view)
            }

            override fun getItemCount(): Int {
                return this.labelThumbUrlAdapter.size
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                holder.itemView.setOnClickListener {
                    objItemClickListener(position)
                }

                //Glide.with(context).load(labelThumbUrlAdapter[position]).into(holder.itemView.labelImage)

                Glide.with(context)
                    .asBitmap()
                    .load(labelThumbUrlAdapter[position].url)
                    .into(object : CustomTarget<Bitmap>(){
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val sqRes = ImageHelper.getSquredBitmap(resource)
                            val round = RoundedBitmapDrawableFactory.create(context.resources,sqRes)
                            round.cornerRadius = roundedCon
                            holder.itemView.labelImage.setImageDrawable(round)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })

                (holder as LabelViewHolder).bind()
            }

            class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                @SuppressLint("SetTextI18n")
                fun bind() {
                }
            }
        }
    }
}
