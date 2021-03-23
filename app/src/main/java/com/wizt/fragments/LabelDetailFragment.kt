package com.wizt.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ogaclejapan.arclayout.ArcLayout
import com.wizt.activities.CreateLabelActivity
import com.wizt.activities.MainActivity
import com.wizt.common.base.BaseFragment
import com.wizt.common.constants.Constants
import com.wizt.common.http.APIManager
import com.wizt.R
import com.wizt.activities.FloorPlanActivity
import com.wizt.components.activityImprove.IOnBackPressed
import com.wizt.components.adapters.CoverImagePagerAdapter
import com.wizt.components.myDialog.MyDialog
import com.wizt.extensions.ImageHelper
import com.wizt.fragments.HomeFragment.Companion.animationPosX
import com.wizt.fragments.HomeFragment.Companion.animationPosY
import com.wizt.fragments.HomeFragment.Companion.animationRadius
import com.wizt.fragments.HomeFragment.Companion.animationdelayTime
import com.wizt.models.*
import com.wizt.utils.AnimatorUtils
import com.wizt.utils.DateTimeUtils
import com.wizt.utils.MyToastUtil

import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_label_detail.view.*
import kotlinx.android.synthetic.main.share_label_dialog.*

@Suppress("UNREACHABLE_CODE")
class LabelDetailFragment : BaseFragment(), IOnBackPressed {

    companion object {

        const val TAG = "WIZT:LabelDetailFragment"
        const val roundedCon = 20.0f

        @JvmStatic
        fun newInstance() =
            LabelDetailFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    lateinit var label: Label
    lateinit var shareLabel: ShareLabel
    var labelType : Int = 0
    private lateinit var viewPager: ViewPager
    private lateinit var pagerAdapter: CoverImagePagerAdapter
    private lateinit var activity: MainActivity
    private lateinit var imageCheckBox: CheckBox
    private lateinit var ivSend : ImageView
    private lateinit var ivLocation : ImageView
    private lateinit var locationLL: View
    private lateinit var myView : View
    private lateinit var fabEdit: android.support.design.widget.FloatingActionButton
    private lateinit var menuLayout: View
    private lateinit var arcLayout: ArcLayout

    var currentSelectedPosition = 0

    var images = ArrayList<Image>()

    @SuppressLint("LongLogTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "labelType -> " + labelType)
        if(labelType == 1 && label.images.size > 0)
            postponeEnterTransition()
    }

    @SuppressLint("LongLogTag")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_label_detail, container, false)
        myView = view

        view.setOnClickListener {

        }

        view.title.setText(R.string.label_details)
        bindMenuAction(view)
        bindNotificationViewAction(view)
        activity = getActivity() as MainActivity

        ivSend = view.findViewById(R.id.iv_send)
        ivSend.setOnClickListener {

        }



        // UI Events
        view.shareIV.setOnClickListener {

        }

        view.deleteIV.setOnClickListener {

        }

//        val tvLocation : TextView
//        tvLocation = view.findViewById(R.id.locationTV)
//        tvLocation.setOnClickListener {
//
//            Log.d(TAG,"LocationTV onClick Listener")
//
//            if (labelType != 1) {
//                return@setOnClickListener
//            }
//
//            val intent = Intent(context, FloorPlanActivity::class.java)
//            StartIntent(label.floor_plan,intent)
//        }

        ivLocation = view.findViewById(R.id.ivLocation)
        locationLL = view.findViewById(R.id.locationLL)
        locationLL.setOnClickListener {

            var imageURL : String = ""
            var title: String

            if (labelType == 1) {
                imageURL = label.ar_mark_image
                title = label.name
            }
            else {
                imageURL = shareLabel.label.ar_mark_image
                title = shareLabel.label.name
            }
            if (imageURL == null || imageURL.isEmpty()) {
                MyToastUtil.showWarning(activity,"No AR marker")
            } else {
                //MyDialog.showCustomDialog(context!!,imageURL,title,-2)
                showLocationImage()
            }

        }

        view.closeImgBtn.setOnClickListener {
            myView.locationImageCL.visibility = View.GONE
        }

        view.locationImageCL.setOnClickListener {  }

        initIVLocation()

        fabEdit = view.findViewById(R.id.fab_edit)
        menuLayout = myView.findViewById(R.id.menu_layout)
        arcLayout = myView.findViewById(R.id.arc_layout)

        menuLayout.setOnClickListener {
            hideMenu()
        }

        fabEdit.setOnClickListener {

            if(menuLayout.visibility == View.VISIBLE)
                hideMenu()
            else {
                onFabClick()
            }

            //showSelectDialog()

        }

        view.btn_edit.setOnClickListener {
            hideMenu()
            chooseEdit()
        }

        view.btn_delete.setOnClickListener {
            hideMenu()
            chooseDelete()
        }

        view.btn_share.setOnClickListener {
            hideMenu()
            chooseShareLabel()
        }


        view.imageBackButton.setOnClickListener {
            onBackPressed()
        }

        PreSetupPagerView(view)

        loadData(false)

        animationLife()

        return view
    }

    private fun createHideItemAnimator(item: View): Animator {
        val dx = fabEdit.x - item.x
        val dy = context!!.resources.getDimension(R.dimen.fabAddPosY) - item.y

        val anim = ObjectAnimator.ofPropertyValuesHolder(
            item,
            AnimatorUtils.rotation(animationRadius, 0f),
            AnimatorUtils.translationX(animationPosX, dx),
            AnimatorUtils.translationY(animationPosY, dy)
        )

        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                item.translationX = animationPosX
                item.translationY = animationPosY
            }
        })

        return anim
    }

    private fun hideMenu() {
        val animList = java.util.ArrayList<Animator>()

        for (i in arcLayout.childCount - 1 downTo 0) {
            animList.add(createHideItemAnimator(arcLayout.getChildAt(i)))
        }

        animList.add(
            ObjectAnimator.ofPropertyValuesHolder(
                fabEdit,
                AnimatorUtils.rotation(45f, 0f)
            )
        )

        val animSet = AnimatorSet()
        animSet.duration = animationdelayTime
        animSet.interpolator = AnticipateInterpolator()
        animSet.playTogether(animList)
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)

                menuLayout.visibility = View.INVISIBLE
                fabEdit.rotation = 0f
            }
        })
        animSet.start()
    }

    private fun onFabClick() {

        menuLayout.setVisibility(View.VISIBLE)

        val animList = java.util.ArrayList<Animator>()

        var i = 0
        val len = arcLayout.getChildCount()
        while (i < len) {
            animList.add(createShowItemAnimator(arcLayout.getChildAt(i)))
            i++
        }

        animList.add(
            ObjectAnimator.ofPropertyValuesHolder(
                fabEdit,
                AnimatorUtils.rotation(0f, 45f)
            )
        )

        val animSet = AnimatorSet()
        animSet.duration = animationdelayTime
        animSet.interpolator = OvershootInterpolator()
        animSet.playTogether(animList)
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)

                fabEdit.rotation = 0f
            }
        })
        animSet.start()
    }

    @SuppressLint("LongLogTag")
    private fun createShowItemAnimator(item: View): Animator {

        val dx = fabEdit.x - item.x
        val dy = context!!.resources.getDimension(R.dimen.fabAddPosY) - item.y

        Log.d(TAG, "itemX -> " + item.x)
        Log.d(TAG, "itemY -> " + item.y)

        item.rotation = 0f
        item.translationX = dx
        item.translationY = dy

        return ObjectAnimator.ofPropertyValuesHolder(
            item,
            AnimatorUtils.rotation(0f, animationRadius),
            AnimatorUtils.translationX(dx, animationPosX),
            AnimatorUtils.translationY(dy, animationPosY)
        )
    }

    override fun onBackPressed(): Boolean {

        activity.popFragment_spe()
        return true

    }

    fun animationLife() {

        // Transition animation lifecycle
        activity?.window?.sharedElementEnterTransition?.addListener(
            object : android.transition.Transition.TransitionListener {

                @SuppressLint("LongLogTag")
                override fun onTransitionStart(transition: android.transition.Transition) {
                    Log.d(TAG,"TransitionStart")

//                    val animation1 = AlphaAnimation(0.2f, 1.0f)
//                    animation1.setDuration(MainActivity.ANIMATION_DELAY_TIME)
//                    animation1.setStartOffset(5000)
//                    myView.tvDate.startAnimation(animation1)
//                    myView.ivLocation.startAnimation(animation1)
//                    myView.tvDescription.startAnimation(animation1)
//                    myView.editIV_anim.startAnimation(animation1)
//                    myView.alarmDateLL.startAnimation(animation1)
//                    myView.tagLabel.startAnimation(animation1)
                }

                @SuppressLint("LongLogTag")
                override fun onTransitionEnd(transition: android.transition.Transition) {
                    Log.d(TAG,"TransitionEnd")
                }

                override fun onTransitionCancel(transition: android.transition.Transition) {}

                override fun onTransitionPause(transition: android.transition.Transition) {}

                override fun onTransitionResume(transition: android.transition.Transition) {}
            })

    }

    fun showLocationImage() {
        myView.locationImageCL.visibility = View.VISIBLE
    }

    private fun showSelectDialog() {



        val dialog = android.app.AlertDialog.Builder(context)
        dialog.setTitle("Select Action")
        val dialogItems = arrayOf("Edit", "Delete", "Share Label")
        dialog.setItems(
            dialogItems
        ) { dialog, which ->
            when (which) {
                0 -> chooseEdit()
                1 -> chooseDelete()
                2 -> chooseShareLabel()
            }
        }
        dialog.show()

    }

    private fun chooseShareLabel() {

        if (labelType != 1) {
            MyToastUtil.showWarning(context!!, "Already Shared")
            return
        }

//        if(!Global.isSubscription) {
//            MyToastUtil.showWarning(context!!, "To share Label with friends, please subscribe to premium plans.")
//            return
//        }

        val fragment = FriendFragment.newInstance()
        fragment.selectedLabel = label
        (activity as MainActivity).pushFragment(fragment)
    }

    private fun chooseDelete() {

        when (labelType) {
            3 -> { // Don't delete share label
                MyToastUtil.showNotice(context!!, "You don't have permission to delete the share label")
                return
            }
            2 -> { // delete share label
                val successCallback: () -> Unit = {
                    activity?.runOnUiThread {
                        (activity as MainActivity).popFragment()
                    }
                }
                APIManager.share.deleteShareLabel(shareLabel, successCallback, errorCallback)

            }
            1 -> { // delete label
                val successCallback: () -> Unit = {
                    activity?.runOnUiThread {
                        (activity as MainActivity).popFragment()
                        (activity as MainActivity).reloadHomeFragment()
                    }
                }
                APIManager.share.deleteLabel(label, successCallback, errorCallback)

            }
            else -> return
        }

    }

    private fun chooseEdit() {
        when (labelType) {
            3 -> { // Edit share label

                if (!shareLabel.edit_permission) {
                    MyToastUtil.showWarning(context!!, "You can't edit the label. Don't have permission")
                    return
                }
                activity.popFragment()
                val intent = Intent(this.context, CreateLabelActivity::class.java)
                intent.putExtra(Constants.EXTRA_CREATE_LABEL_ACTIVITY_TYPE, false)
                intent.putExtra(Constants.EXTRA_EDIT_LABEL_TYPE, labelType)
                startActivity(intent)
            }
            2 -> { // Edit share log label
                //alert(shareLabel)
            }
            1 -> { // Edit label
                val intent = Intent(this.context, CreateLabelActivity::class.java)
                intent.putExtra(Constants.EXTRA_CREATE_LABEL_ACTIVITY_TYPE, false)
                intent.putExtra(Constants.EXTRA_EDIT_LABEL_TYPE, labelType)
                startActivity(intent)
            }
            else -> return
        }
    }

    private fun initIVLocation() {
        var imageURL : String = ""
        var title: String

        if (labelType == 1) {
            imageURL = label.ar_mark_image
            title = label.name
        }
        else {
            imageURL = shareLabel.label.ar_mark_image
            title = shareLabel.label.name
        }
        if (imageURL == null || imageURL.isEmpty()) {

        } else {
            //Glide.with(context!!).load(imageURL).into(ivLocation)

            Glide.with(context!!)
                .asBitmap()
                .load(imageURL)
                .into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                        if(context != null) {
                            val sqRes = ImageHelper.getSquredBitmap(resource)
                            val round = RoundedBitmapDrawableFactory.create(context!!.resources,sqRes)
                            round.cornerRadius = roundedCon
                            ivLocation.setImageDrawable(round)
                        }
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                        //holder.itemView.imageBG.setImageDrawable(context.getDrawable(R.drawable.home_row_default))
                    }
                })

            Glide.with(context!!)
                .asBitmap()
                .load(imageURL)
                .into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                        if(context != null) {
                            val sqRes = ImageHelper.getCroppedImage_343(resource)
                            val round = RoundedBitmapDrawableFactory.create(context!!.resources,sqRes)
                            round.cornerRadius = 1.0f
                            myView.imageViewLocation.setImageDrawable(round)
                        }
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                        //holder.itemView.imageBG.setImageDrawable(context.getDrawable(R.drawable.home_row_default))
                    }
                })
        }
    }

    private fun StartIntent(floorPlanID : String, intent: Intent) {
        val successCallback: (ArrayList<FloorPlan>) -> Unit = { list ->
            activity?.runOnUiThread {

                activity.closeWaitDialog()
                var flag = 0
                if (!list.isEmpty()) {
                    for (item in list) {
                        if (item.id.equals(floorPlanID)) {
                            flag = 1
                            Global.floorPlan = item
                            startActivity(intent)
                        }
                    }
                }
                if (flag == 0) {
                    MyToastUtil.showWarning(context!!, "Location maybe removed")
                }
            }
        }

        val errorCallback: (String) -> Unit = {message ->
            activity.closeWaitDialog()
            activity?.runOnUiThread {
                MyToastUtil.showWarning(context!!, "Connection Failed")
            }
        }
        activity.showWaitDialog()
        APIManager.share.getFloorPlanList(successCallback, errorCallback)
    }

    override fun onResume() {
        super.onResume()

        if (labelType == 1) { // Show Label
            myView.labelNameTV.text = label.name
            myView.locationTV.text = label.location
            myView.tvDate.text = DateTimeUtils().getLocalDate(label.created_at)
            myView.tvDescription.text = label.name
            if(label.reminder_time.isEmpty()) {
                myView.reminderIV.visibility = View.GONE
            } else {
                myView.alarmDate1.text = DateTimeUtils().getLocalDate(label.reminder_time) +
                        "\n" + DateTimeUtils().getLocalTime(label.reminder_time)
            }

            myView.tagLabel.removeAllTags()
            if(label.tags.isNotEmpty()) {
                val tagStr: String = label.tags
                val tagArr: List<String> = tagStr.split(",").map { it.trim() }
                for (i in 0 until tagArr.size) {
                    myView.tagLabel.addTag(tagArr[i])
                }
            }

        } else if (labelType == 2 || labelType == 3) { // Show share log(2) or share label(3)
            myView.labelNameTV.text = shareLabel.label.name
            myView.locationTV.text = shareLabel.label.location
            myView.tvDate.text = DateTimeUtils().getLocalDate(shareLabel.label.created_at)
            myView.tvDescription.text = shareLabel.label.name
            if(shareLabel.label.reminder_time.isEmpty()) {
                myView.reminderIV.visibility = View.GONE
            } else {
                myView.alarmDate1.text = DateTimeUtils().getLocalDate(shareLabel.label.reminder_time) +
                        "\n" + DateTimeUtils().getLocalTime(shareLabel.label.reminder_time)
            }

            myView.tagLabel.removeAllTags()
            if(shareLabel.label.tags.isNotEmpty()) {
                val tagStr: String = shareLabel.label.tags
                val tagArr: List<String> = tagStr.split(",").map { it.trim() }
                for (i in 0 until tagArr.size) {
                    myView.tagLabel.addTag(tagArr[i])
                }
            }

        } else  return

        //loadData(true)

    }

    @SuppressLint("LongLogTag")
    fun PreSetupPagerView(view: View) {

        viewPager = view.findViewById(R.id.viewpager) as ViewPager
        imageCheckBox = view.findViewById(R.id.imageCheckBox)

        imageCheckBox.setOnClickListener {

            if(labelType == 3) {
                if (!shareLabel.edit_permission) {
                    MyToastUtil.showWarning(context!!, "You can't edit the label. Don't have permission")
                    imageCheckBox.isChecked = !imageCheckBox.isChecked
                    return@setOnClickListener
                }
            }

            if(images.size == 0) {
                imageCheckBox.isChecked = !imageCheckBox.isChecked
                return@setOnClickListener
            }

            imageCheckBox.isChecked = true

            for (item in images)
                item.is_cover = false
            images[currentSelectedPosition].is_cover = true

            if (labelType == 2 || labelType == 3) {
                shareLabel.label.images.clear()
                shareLabel.label.images.addAll(images)
            }
            else if(labelType == 1){
                label.images.clear()
                label.images.addAll(images)
            }

            loadData(true)

        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }
            override fun onPageSelected(position: Int) {

                currentSelectedPosition = position

                if(images[position].is_cover) {
                    imageCheckBox.isChecked = true
                }
                else {
                    imageCheckBox.isChecked = false
                }

            }

        })

        pagerAdapter = CoverImagePagerAdapter(activity!!.supportFragmentManager, images, this)
        viewPager.adapter = pagerAdapter


    }

    @SuppressLint("LongLogTag")
    fun loadData(isUpgrade : Boolean) {

        if (labelType == 2 || labelType == 3) {

            if (!isUpgrade) {

                images.clear()
                images.addAll(shareLabel.label.images)
                pagerAdapter.notifyDataSetChanged()

                if(images.size > 0) {
                    if(images[0].is_cover) {
                        imageCheckBox.isChecked = true
                    }
                    else {
                        imageCheckBox.isChecked = false
                    }
                }
                else {
                }
            }

            val successCallback: (ShareLabel) -> Unit = {
            }
            val errorCallback: (String) -> Unit = {
                Log.d(TAG," error!")
            }

            if(isUpgrade) APIManager.share.updateShareLabel(shareLabel, successCallback, errorCallback)

        } else if (labelType == 1){

            if(!isUpgrade) {

                images.clear()
                images.addAll(label.images)
                pagerAdapter.notifyDataSetChanged()

                if(images.size > 0) {
                    if(images[0].is_cover) {
                        imageCheckBox.isChecked = true
                    }
                    else {
                        imageCheckBox.isChecked = false
                    }
                }
            }

            val successCallback: (Label) -> Unit = {
            }
            val subscribeCallback: () -> Unit = {
            }
            val errorCallback: (String) ->Unit = {
                activity?.runOnUiThread {
                    MyToastUtil.showWarning(context!!, "Something error, please try again.")
                }
            }
            if(isUpgrade) APIManager.share.updateLabel(label, successCallback, subscribeCallback,errorCallback)
        } else{
            return
        }

    }


    @SuppressLint("InflateParams")
    fun alert(shareLabel: ShareLabel) {
        // Inflate the dialog with custom view
        val mDialogView = LayoutInflater.from(context).inflate(R.layout.share_label_dialog, null)
        // AlertDialogBuilder
        val mBuilder = AlertDialog.Builder(context!!)
            .setView(mDialogView)
        val mAlertDialog = mBuilder.show()

        mAlertDialog.shareBtn.setOnClickListener {

            val editPermission = mAlertDialog.editCB.isChecked
            shareLabel.edit_permission = editPermission

            val successCallback: (ShareLabel) -> Unit = {
                mAlertDialog.dismiss()
            }
            val errorCallback: (String) -> Unit = {message ->
                MyToastUtil.showWarning(context!!, message)
            }
            APIManager.share.updateShareLabel(shareLabel, successCallback, errorCallback)

        }
        mAlertDialog.closeBtn.setOnClickListener {
            mAlertDialog.dismiss()
        }
    }
}
