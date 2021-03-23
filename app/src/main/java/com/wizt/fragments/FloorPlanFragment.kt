package com.wizt.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.wizt.activities.CreateLabelActivity
import com.wizt.activities.FloorDrawActivity
import com.wizt.activities.FloorPlanActivity
import com.wizt.common.base.BaseFragment
import com.wizt.common.constants.Constants
import com.wizt.common.http.APIManager
import com.wizt.models.FloorPlan
import com.wizt.models.Global
import com.wizt.R
import com.wizt.activities.MainActivity
import com.wizt.utils.PreferenceUtils
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_floor_plan.view.*
import kotlinx.android.synthetic.main.item_floor_plan_row.view.*

class FloorPlanFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() =
            FloorPlanFragment().apply {}
    }

    // UI
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var adapter: RecyclerAdapter

    private var showEmptyIV: ImageView? = null

    // s3
    private var credentialsProvider: CognitoCachingCredentialsProvider? = null
    private var s3Clients: AmazonS3Client? = null


//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }

    override fun onResume() {
        super.onResume()

        adapter.removeAllFloorPlan()
        loadFirst()
        adapter.notifyDataSetChanged()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_floor_plan, container, false)
        view.title.text = resources.getString(R.string.all_floor_plans)
        view.notificationBtn.setImageResource(R.drawable.ic_plus)
        super.bindMenuAction(view)

        addAds(view)

        //super.bindNotificationViewAction(view)

        recyclerView = view.findViewById(R.id.recyclerView)
        layoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = layoutManager
        adapter = RecyclerAdapter(context!!,itemClickListener, arrayListOf())
        recyclerView.adapter = adapter

        showEmptyIV = view.findViewById(R.id.showEmptyIV)
        showEmptyIV?.visibility = View.VISIBLE
//        Glide.with(context!!).load(R.drawable.animation_floor).into(showEmptyIV!!)

        // Initialize the AWS Credential
        credentialsProvider = CognitoCachingCredentialsProvider(
            context,
            Constants.IDENTITY_POOL_ID, // Identity Pool ID
            Regions.AP_SOUTHEAST_1
        ) // Region
        // Create a S3 clients
        s3Clients = AmazonS3Client(credentialsProvider)
        // Set the region of your S3 bucket
        s3Clients?.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))

        // UI Event
        view.notificationBtn.setOnClickListener {
            val intent = Intent(context, FloorDrawActivity::class.java)
            startActivityForResult(intent, Constants.ACTIVITY_FLOOR_PLAN_ADD_OK)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Constants.ACTIVITY_FLOOR_PLAN_ADD_OK) {
            if (resultCode == Activity.RESULT_OK){
                //adapter.removeAllFloorPlan()
                //loadFirst()
                //adapter.notifyDataSetChanged()
            }
        } else if (requestCode ==  Constants.ACTIVITY_RESULT_DELETE_FLOOR_PLAN_OK) {
            if (resultCode == Activity.RESULT_OK) {
                //adapter.removeAllFloorPlan()
                //loadFirst()
                //adapter.notifyDataSetChanged()
            }
        }
    }

    // Load floor plan date from server
    private fun loadFirst() {
        val successCallback: (ArrayList<FloorPlan>) -> Unit = { list ->
            activity?.runOnUiThread {

                //(activity as MainActivity).closeWaitDialog()

                if (showEmptyIV != null) {
                    if (list.isEmpty())
                        showEmptyIV?.visibility = View.VISIBLE
                    else
                        showEmptyIV?.visibility = View.GONE
                }

                if (list.isNotEmpty()) {
                    for (i in 0 until list.size) {
                        adapter.addFloorPlan(list[i])
                    }
                }
            }
        }

        val errorCallback: (String) -> Unit = {message ->
            //(activity as MainActivity).closeWaitDialog()
        }
        //(activity as MainActivity).showWaitDialog()
        APIManager.share.getFloorPlanList(successCallback, errorCallback)
    }

    // Item Click Event
    private val itemClickListener:(FloorPlan) -> Unit = { floorPlan ->
        val intent = Intent(context, FloorPlanActivity::class.java)
        Global.floorPlan = floorPlan
        if (PreferenceUtils.getBoolean(Constants.PREF_IS_FLOOR_PLAN_STATE)) { // Select Location for Label
            (activity as CreateLabelActivity).startActivityForResult(intent, Constants.ACTIVITY_RESULT_LABEL_LOCATION_OK)
            (activity as CreateLabelActivity).popFragment()
        } else {
            intent.putExtra(Constants.EXTRA_FLOORPLAN_TO_LABELS,true)
            this.startActivityForResult(intent, Constants.ACTIVITY_RESULT_DELETE_FLOOR_PLAN_OK)
        }
    }

    // RecyclerView
    class RecyclerAdapter(private val context: Context, val itemClickListener: (floorPlan: FloorPlan) -> Unit, arr: ArrayList<FloorPlan>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var floorPlanArr : ArrayList<FloorPlan> = arr

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_floor_plan_row, parent, false)

            return FloorPlanViewHolder(view)
        }

        override fun getItemCount(): Int {
            return floorPlanArr.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.setOnClickListener {
                itemClickListener(floorPlanArr[position])
            }

            val floorPlan = floorPlanArr[position]
            holder.itemView.floorEditBtn.text = floorPlan.name
            Glide.with(context).load(floorPlan.image).into(holder.itemView.floorPlanImage)

            (holder as FloorPlanViewHolder).bind()
        }

        fun addFloorPlan(floorPlan: FloorPlan) {
            floorPlanArr.add(floorPlan)
            notifyDataSetChanged()
        }

        fun removeAllFloorPlan() {
            val n = floorPlanArr.size
            for (i in 0 until n) {
                floorPlanArr.removeAt(n - 1 - i)
            }
        }

        class FloorPlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
            }
        }
    }
}

