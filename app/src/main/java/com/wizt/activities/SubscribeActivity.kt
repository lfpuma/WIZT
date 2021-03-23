package com.wizt.activities

import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.wizt.R
import com.wizt.common.base.BaseActivity
import com.wizt.common.http.APIManager
import com.wizt.components.paginatedRecyclerView.InfiniteScrollListener
import com.wizt.components.paginatedRecyclerView.interfaces.OnLoadMoreListener
import com.wizt.fragments.AddressFragment
import com.wizt.models.Global
import com.wizt.models.Pagination
import com.wizt.models.Plan
import com.wizt.utils.MyToastUtil
import com.wizt.utils.ToastUtil
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_subscribe.*
import kotlinx.android.synthetic.main.item_subscription_row.view.*

class SubscribeActivity : BaseActivity() , OnLoadMoreListener {

    private var pagination: Pagination? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var adapter: RecyclerAdapter
    private lateinit var infiniteScrollListener: InfiniteScrollListener

    private lateinit var swipeContainer: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_subscribe)

        loadFirst()

        layoutManager = GridLayoutManager(this, 2)
        infiniteScrollListener = InfiniteScrollListener(layoutManager, this)
        infiniteScrollListener.setLoaded()

        recyclerView = findViewById(R.id.recyclerView)
        adapter = RecyclerAdapter(this, { labelItemClicked() }, arrayListOf())
        recyclerView.addOnScrollListener(infiniteScrollListener)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        swipeContainer = findViewById(R.id.swipeContainer)
        setSwipeRefreshLayoutColor(swipeContainer)
        swipeContainer.setOnRefreshListener {
            fetchTimelineAsync()
        }

        backBtn.setOnClickListener {
            finish()
        }
//        buyJuniorBtn.setOnClickListener {
//            //pushShippingAddressFragment()
//            val fragment = AddressFragment.newInstance(1)
//            fragment.billingMethodStr = this.monthlyMethodTV.text.toString()
//            fragment.getLabelStr = this.monthlyLabelTV.text.toString()
//            fragment.savedMoneyStr = this.monthlySaveTV.text.toString()
//            fragment.imageType = 1
//            fragment.levelStr = "Junior"
//            pushFragment(fragment)
//        }
//        buySeniorBtn.setOnClickListener {
//            //pushShippingAddressFragment()
//            val fragment = AddressFragment.newInstance(2)
//            fragment.billingMethodStr = this.yearlyMethodTV.text.toString()
//            fragment.getLabelStr = this.yearlyLabelTV.text.toString()
//            fragment.savedMoneyStr = this.yearlySaveTV.text.toString()
//            fragment.imageType = 2
//            fragment.levelStr = "Master"
//            pushFragment(fragment)
//        }
    }

    override fun onLoadMore() {
        if (pagination?.next == null) {
            return
        }

        adapter.addNullData()
        val successCallback: (Pagination, List<Plan>) -> Unit = { pagination, list ->
            this.pagination = pagination
            runOnUiThread {
                adapter.removeNull()
                adapter.addData(list as ArrayList<Plan>)
                infiniteScrollListener.setLoaded()
            }
        }

        val errorCallback: (String) -> Unit = { message ->
            runOnUiThread {
                MyToastUtil.showWarning(this, message)
            }
        }

        APIManager.share.getPlanList(successCallback, errorCallback)
    }

    private fun labelItemClicked() {
        //ToastUtil.show(this, "Item clicked")
    }

    private fun loadFirst() {
        val successCallback: (Pagination, List<Plan>) -> Unit = { pagination, list ->
            this.pagination = pagination
            runOnUiThread {
                if (list.isNotEmpty()) {
                    adapter.clear()
                    adapter.addData(list)
                    Log.d("SubscribeActivity", "plan list -> " + list.toString())
                }
            }
        }
        val errorCallback: (String) -> Unit = { message ->
            runOnUiThread {
                MyToastUtil.showWarning(this, message)
            }
        }
        APIManager.share.getPlanList(successCallback, errorCallback)
    }

    private fun fetchTimelineAsync() {
        if (pagination?.next == null) {
            swipeContainer.isRefreshing = false
            return
        }

        val successCallback: (Pagination, List<Plan>) -> Unit = { pagination, list ->
            this.pagination = pagination
            this.adapter.clear()
            runOnUiThread {
                this.adapter.addData(list as ArrayList<Plan>)
                swipeContainer.isRefreshing = false
            }
        }

        val errorCallback: (String) -> Unit = { message ->
            runOnUiThread {
                MyToastUtil.showWarning(this, message)
            }
        }

        APIManager.share.getPlanList(successCallback, errorCallback)
    }

    class RecyclerAdapter(private var context: Context, val itemClickListener: () -> Unit, arr: ArrayList<Plan?>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var isLoading = false
        private val VIEWTYPE_FRIEND = 1
        private val VIEWTYPE_PROGRESS = 2

        private var planArr: ArrayList<Plan?> = arr

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view: View

            return if (p1 == VIEWTYPE_FRIEND) {
                val view = inflater.inflate(R.layout.item_subscription_row, parent, false)
                PlanViewHolder(view)
            } else {
                view = inflater.inflate(R.layout.item_progress_row, parent, false)
                ProgressViewHolder(view)
            }
        }

        override fun getItemCount(): Int {
            return planArr.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == planArr.size - 1 && isLoading)
                VIEWTYPE_PROGRESS
            else
                VIEWTYPE_FRIEND
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is PlanViewHolder) {
                bindPlanViewHolder(holder, position)
            }
        }

        private fun bindPlanViewHolder(holder: PlanViewHolder, position: Int) {
            holder.itemView.setOnClickListener {
                itemClickListener()
            }

            if (planArr[position] == null)    return

            val plan = planArr[position]!!

            holder.itemView.levelTV.text = plan.name
            holder.itemView.yearlyMethodTV.text = plan.sub_name
            //holder.itemView.yearlyLabelTV.text = plan.label_count.toString() + " Labels"
            holder.itemView.yearlyLabelTV.text = plan.photo_count.toString() + " Photos"
            holder.itemView.yearlySaveTV.text = "USD " + plan.price.toString()
            holder.itemView.tvDes.text = plan.description.replace(",","\n")
            if (plan.icon != null) {
                Glide.with(context).load(plan.icon).into(holder.itemView.icon)
            }

            //        buyJuniorBtn.setOnClickListener {
//            //pushShippingAddressFragment()
//            val fragment = AddressFragment.newInstance(1)
//            fragment.billingMethodStr = this.monthlyMethodTV.text.toString()
//            fragment.getLabelStr = this.monthlyLabelTV.text.toString()
//            fragment.savedMoneyStr = this.monthlySaveTV.text.toString()
//            fragment.imageType = 1
//            fragment.levelStr = "Junior"
//            pushFragment(fragment)
//        }

            val activity = context as SubscribeActivity

            holder.itemView.buySeniorBtn.setOnClickListener {

                fun continueToPayment() {
                    val fragment = AddressFragment.newInstance(1)
                    fragment.billingMethodStr = holder.itemView.yearlyMethodTV.text.toString()
                    fragment.getLabelStr = holder.itemView.yearlyLabelTV.text.toString()
                    fragment.savedMoneyStr = holder.itemView.yearlySaveTV.text.toString()
                    fragment.levelStr = holder.itemView.levelTV.text.toString()
                    fragment.planID = plan.id
                    fragment.iconUrl = plan.icon
                    activity.pushFragment(fragment)
                }

                if(Global.isSubscription) {

                    lateinit var dialog: AlertDialog
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("")
                    builder.setMessage("Your subscription is active now. Are you sure to continue?")
                    val dialogClickListener = DialogInterface.OnClickListener{ _, which ->
                        when(which){
                            DialogInterface.BUTTON_POSITIVE -> {
                                continueToPayment()
                            }
                            //DialogInterface.BUTTON_NEGATIVE ->
                        }
                    }

                    builder.setPositiveButton(Html.fromHtml("<font color='#000000'>YES</font>"),dialogClickListener)
                    builder.setNegativeButton(Html.fromHtml("<font color='#000000'>No</font>"),dialogClickListener)
                    dialog = builder.create()

                    dialog.show()

                } else {
                    continueToPayment()
                }
            }

            holder.bind()
        }

        fun addData(arr: List<Plan>) {
            planArr.addAll(arr)
            notifyDataSetChanged()
        }

        fun clear() {
            this.planArr.clear()
        }

        fun addNullData() {
            planArr.add(null)
            notifyItemInserted(planArr.size)
        }

        fun removeNull() {
            planArr.removeAt(planArr.size - 1)
            notifyItemRemoved(planArr.size)
        }

        class PlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
            }
        }

        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    /***
     * Fragment Navigate Methods
     */
    fun pushFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, fragment)
            .addToBackStack("AddressFragment")
            .commit()
    }

    fun popFragment() {
        supportFragmentManager
            .popBackStack("AddressFragment", FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    fun setSwipeRefreshLayoutColor(swipeContainer: SwipeRefreshLayout) {
        swipeContainer.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

}
