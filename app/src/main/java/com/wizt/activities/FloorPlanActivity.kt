package com.wizt.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import co.lujun.androidtagview.TagView
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.wizt.common.constants.Constants
import com.wizt.common.http.APIManager
import com.wizt.models.Global
import com.wizt.R
import com.wizt.common.base.BaseActivity
import com.wizt.fragments.FloorplanLabelsFragment
import com.wizt.fragments.LabelDetailFragment
import com.wizt.models.Label
import com.wizt.models.Pagination
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_floor_plan.*

class FloorPlanActivity: BaseActivity() {

    var isRequreLabels = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_floor_plan)

        isRequreLabels = intent.getBooleanExtra(Constants.EXTRA_FLOORPLAN_TO_LABELS,false)

        // Initialize UI
        Glide.with(applicationContext).load(Global.floorPlan?.image).into(floorImage)
        tagFloor.removeAllTags()

        this.tagFloor.removeAllTags()
        val tagStr: String = Global.floorPlan!!.tags
        val tagArr: List<String> = tagStr.split(",").map { it.trim() }
        for (i in 0 until tagArr.size) {
            this.tagFloor.addTag(tagArr[i])
        }

        namePlanTV.text = Global.floorPlan!!.name

        sideFBtn.setOnClickListener {
            super.onBackPressed()
        }

//        tagFloor.setOnTagClickListener(object : TagView.OnTagClickListener{
//            override fun onTagCrossClick(position: Int) {
//            }
//
//            override fun onTagClick(position: Int, text: String?) {
//                val tagName = tagFloor.getTagText(position)
//
//                if (isRequreLabels) {
//                    goShowLabels(tagName)
//                }
//                else {
//                    goCreateLabelPage(tagName)
//                }
//
//            }
//
//            override fun onTagLongClick(position: Int, text: String?) {
//                val tagName = tagFloor.getTagText(position)
//
//                if (isRequreLabels) {
//                    goShowLabels(tagName)
//                }
//                else {
//                    goCreateLabelPage(tagName)
//                }
//            }
//        })

        deleteBtn.setOnClickListener {
            val errorCallback: (String) -> Unit = { message ->
                this.runOnUiThread {
                    MyToastUtil.showWarning(applicationContext, message)
                }
            }
            val successCallback: () -> Unit = {
                setResult(Activity.RESULT_OK)
                finish()
            }
            APIManager.share.deleteFloorPlan(Global.floorPlan!!, successCallback, errorCallback)
        }
    }

    private fun goShowLabels(tag: String) {
        checkRooms(tag)
    }

    fun checkRooms(tag : String) {
        val successCallback: (Pagination, List<Label>) -> Unit = { pagination, list ->
            closeWaitDialog()
            runOnUiThread {
                var count = 0
                for (item in list) {
                    if(item.location.contains(tag)) {
                        count ++
                    }
                }
                if (count == 0) {
                    MyToastUtil.showWarning(this,"No Room")
                }
                else {
                    val fragment = FloorplanLabelsFragment.newInstance()
                    fragment.searchTagName = tag
                    pushFragment(fragment)
                }
            }
        }

        val errorCallback: (String) -> Unit = { message ->
            closeWaitDialog()
            runOnUiThread {
                MyToastUtil.showWarning(this, message)
            }
        }

        showWaitDialog()
        APIManager.share.getLabels(successCallback, errorCallback)
    }

    private fun goCreateLabelPage(tag: String) {
        PreferenceUtils.saveBoolean(Constants.PREF_IS_CURR_LOCATION, true)
        val intent = Intent()
        intent.putExtra("TagName", tag)
        intent.putExtra("TagID",Global.floorPlan?.id)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    @SuppressLint("RestrictedApi")
    fun pushFragment(fragment: Fragment) {

        val commit = supportFragmentManager
            .beginTransaction()
            .add(R.id.containerFL, fragment)
            .addToBackStack(null)
            .commit()
        containerFL.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        containerFL.visibility = View.GONE
    }

    override fun onBackPressed() {
        super.onBackPressed()
        containerFL.visibility = View.GONE
    }
}