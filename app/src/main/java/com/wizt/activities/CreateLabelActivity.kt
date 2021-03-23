package com.wizt.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import co.lujun.androidtagview.TagView
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.wizt.common.constants.Constants
import com.wizt.extensions.setupClearButtonWithAction
import com.wizt.R
import kotlinx.android.synthetic.main.activity_creat_label.*
import java.io.File
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.TextView
import android.widget.Toast
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.crashlytics.android.Crashlytics
import com.wizt.activities.auth.LoginActivity
import com.wizt.common.aws.AppHelper
import com.wizt.common.base.BaseActivity
import com.wizt.common.http.APIManager
import com.wizt.components.myDialog.MyDialog
import com.wizt.dialog.CustomProgressDialog
import com.wizt.extensions.ImageHelper
import com.wizt.fragments.FloorPlanFragment
import com.wizt.fragments.HomeFragment
import com.wizt.models.*
import com.wizt.utils.DateTimeUtils
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import com.wizt.utils.ToastUtil
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.item_label_image.view.*
import kotlinx.android.synthetic.main.item_label_image.view.labelImage
import kotlinx.android.synthetic.main.item_obj_image.*
import java.io.FileOutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Exception
import kotlin.collections.ArrayList
import java.io.InputStream as InputStream1


@Suppress("DEPRECATION")
class CreateLabelActivity : BaseActivity() {

    companion object {
        const val TAG = "WIZT:CreateLabelActivity"
        const val roundedCon = 15f
    }

    private var cusItem = 0
    private var imageCount = 0
    private var isCreate: Boolean = true
    private var isFybe : Boolean = false
    private var labelType: Int = 0

    // s3
    private var credentialsProvider: CognitoCachingCredentialsProvider? = null
    private var s3Clients: AmazonS3Client? = null
    private var imageFileName: String? = null
    private var filePath: File? = null
    private lateinit var fileURL : String

    // UI
    lateinit var recyclerView: RecyclerView
    lateinit var linearLayoutManager: LinearLayoutManager
    var adapter: RecyclerAdapter? = null

    // Data
    var labelImageURL: ArrayList<String> = ArrayList()
    var labelThumbURL: ArrayList<String> = ArrayList()
    var locationID  = ""

    private var imageNumber : Int = 0
    private lateinit var editLabel: Label
    private var createLabel:Label =  Label(0,0, "", "", "","", "", arrayListOf(),"","", 0)
    var arMarkerImageUrl : String? = ""
    private var isUpload = true
    var selectedLabelPhotoCounts = 0
    var prepareCameraName = ""
    var reminderDate = ""
    var reminderTime = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_creat_label)

        //GONE
        cameraBtn_addmore.visibility = View.GONE

        // check if the activity initialize for creating or editing
        isCreate = intent.getBooleanExtra(Constants.EXTRA_CREATE_LABEL_ACTIVITY_TYPE, false)
        isFybe = intent.getBooleanExtra(Constants.EXTRA_CREATE_FYBE_ACTIVITY_TYPE, false)
        labelType = intent.getIntExtra(Constants.EXTRA_EDIT_LABEL_TYPE, 0)
        if (!isCreate) {

            when (labelType) {
                1 -> {
                    this.titleTV.setText(R.string.edit_label)
                    init(Global.label!!)
                }
                3 -> {
                    this.titleTV.setText(R.string.edit_share_label)
                    init(Global.shareLabel!!.label)
                }
                else -> {
                    return
                }
            }
        }

        //contentImagesTV.visibility = View.VISIBLE
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        // Initialize the AWS Credential
        credentialsProvider = CognitoCachingCredentialsProvider(
            applicationContext,
            "ap-southeast-1:992a1271-46fb-4b01-9fc7-06cd8d5cd779", // Identity Pool ID
            Regions.AP_SOUTHEAST_1
        ) // Region
        // Create a S3 clients
        s3Clients = AmazonS3Client(credentialsProvider)
        // Set the region of your S3 bucket
        s3Clients?.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))

        // UI Events
        fab_done.setOnClickListener {
            if (!checkValidation()) {
                return@setOnClickListener
            }

            uploadImages()
        }

        closeBtn.setOnClickListener {
            finish()
        }

        cameraBtn.setOnClickListener {
            AddContent()
        }

        bottomBtn_content.setOnClickListener {
            AddContent()
        }

        cameraBtn_addmore.setOnClickListener {
            AddContent()
        }

        setCurrLocationBtn.setOnClickListener {
            if (labelType == 3) {
                MyToastUtil.showWarning(applicationContext, "You can't edit the location. Don't have permission")
                return@setOnClickListener
            }

            replaceFragment(FloorPlanFragment.newInstance())
        }

        this.imageARMarker_btn.setOnClickListener {
            onClickLocation()
        }

        this.bottomBtn_location.setOnClickListener {
            onClickLocation()
        }

        this.alarmLL.setOnClickListener {
            onClickAlarm()
        }

        this.bottomBtn_reminder.setOnClickListener {
            onClickAlarm()
        }

        this.bottomBtn_tags.setOnClickListener {
            if(this.tagET.requestFocus()) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this.tagET, InputMethodManager.SHOW_FORCED)
            }
        }

//        imageARMarker.setOnClickListener {
//            if (arMarkerImageUrl == "")
//                return@setOnClickListener
//
//            MyDialog.showCustomDialog(this,arMarkerImageUrl!!,"",-1)
//
//            /*val intent = Intent(this@CreateLabelActivity, ShowARImageActivity::class.java)
//            intent.putExtra(Constants.EXTRA_SET_AR_MARKER, arMarkerImageUrl)
//            startActivity(intent)*/
//        }

        // Add Tag
        labelNameTV.setupClearButtonWithAction()
        tagET.setupClearButtonWithAction()

        labelNameTV.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        })

        tagET.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                if (!tagET.text.isBlank()) {
                    tagContainer.addTag(tagET.text.toString())
                    tagET.text.clear()
                } else {
                    tagET.text.clear()
                }
                return@OnKeyListener true
            }
            false
        })

        // Tag Action
        tagContainer.setOnTagClickListener(object : TagView.OnTagClickListener {

            override fun onTagCrossClick(position: Int) {
                tagContainer.removeTag(position)
            }

            override fun onTagClick(position: Int, text: String?) {
            }

            override fun onTagLongClick(position: Int, text: String?) {
            }
        })

        // Set Location
        if (PreferenceUtils.getBoolean(Constants.PREF_IS_CURR_LOCATION)) {
            PreferenceUtils.saveBoolean(Constants.PREF_IS_CURR_LOCATION, false)
            val locationStr = intent.getStringExtra("TagName")
            val locationid = intent.getStringExtra("TagID")
            locationTV.text = locationStr
            if(locationid != null)
                locationID = locationid
        }

        PreferenceUtils.saveBoolean(Constants.PREF_IS_FLOOR_PLAN_STATE, true)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.imageRecyclerView)
        linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = linearLayoutManager
        adapter = RecyclerAdapter(applicationContext, itemClickListener, labelThumbURL)
        recyclerView.adapter = adapter
    }

    fun updateReminder() {
        if (reminderDate.isEmpty()) {
            alarmTV.text = ""
            return
        }
        alarmTV.text = reminderDate
        if (reminderTime.isEmpty()) return
        alarmTV.text = reminderDate + "\n" + reminderTime
    }

    fun onClickLocation() {
        val intent = Intent(this, CameraActivity::class.java)
        isUpload = false
        prepareCameraName = getString(R.string.snap_where_you_placed_it)
        intent.putExtra(Constants.EXTRA_CAMERA_NAME,getString(R.string.snap_where_you_placed_it))
        startActivityForResult(intent, Constants.ACTIVITY_RESULT_CAMERA_OK)
    }

    fun onClickAlarm() {
        MyDialog.showReminderDialog(this)
    }

    fun AddContent() {

        // Get Profile
        val successCallback: (Profile) -> Unit = { profile: Profile ->
            Global.profile = profile
            PreferenceUtils.saveString(Constants.PROFILE_USER_NAME, profile.name)
            PreferenceUtils.saveString(Constants.PROFILE_USER_EMAIL, profile.email)
            if (profile.picture != null) {
                PreferenceUtils.saveString(Constants.PROFILE_USER_PICTURE_AVATAR, profile.picture!!)
            }

            this.runOnUiThread {
                if (Global.profile!!.photo_cnt + selectedLabelPhotoCounts <= labelImageURL.size && labelType != 3)
                    MyToastUtil.showWarning(this, getString(R.string.upgrade_membership_warning_msg))
                else {
                    val intent = Intent(this, CameraActivity::class.java)
                    isUpload = true
                    if(isFybe) {
                        prepareCameraName = getString(R.string.camera_current_version_of_fybe)
                        intent.putExtra(Constants.EXTRA_CAMERA_NAME,getString(R.string.camera_current_version_of_fybe))
                    } else {
                        prepareCameraName = getString(R.string.snap_content_images)
                        intent.putExtra(Constants.EXTRA_CAMERA_NAME,getString(R.string.snap_content_images))
                    }
                    startActivityForResult(intent, Constants.ACTIVITY_RESULT_CAMERA_OK)
                }
            }
        }

        val errorCallback: (String) -> Unit = { message ->
            this.runOnUiThread {
                MyToastUtil.showWarning(this, "Server Connection Failed")
            }
        }

        APIManager.share.getMyProfile(successCallback, errorCallback)

    }

    override fun onBackPressed() {
        super.onBackPressed()
        PreferenceUtils.saveBoolean(Constants.PREF_IS_FLOOR_PLAN_STATE, false)
    }

    @SuppressLint("LongLogTag")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK)   return

        if (requestCode == Constants.ACTIVITY_RESULT_CAMERA_OK) {
            imageFileName = data?.getStringExtra("ImageName")

            if (Constants.REQUIRECALL.equals(imageFileName)) {

                val intent = Intent(this, CameraActivity::class.java)
                intent.putExtra(Constants.EXTRA_CAMERA_NAME,prepareCameraName)
                startActivityForResult(intent, Constants.ACTIVITY_RESULT_CAMERA_OK)
                return

            }

            filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)

            if(this.filePath == null || this.imageFileName == null) {
                this.runOnUiThread {
                    MyToastUtil.showWarning(applicationContext,getString(R.string.filepathorimagefileError))
                }
                return
            }

            if (isUpload == true) {
                //uploadImage(this.filePath!!, this.imageFileName!!)
                setImage(this.filePath!!, this.imageFileName!!)
            }
            else {

                val file = File(filePath, imageFileName)
                var bitmap = BitmapFactory.decodeFile(file.path)
                val fOut = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)
                fOut.flush()
                fOut.close()

                val intent = Intent(this@CreateLabelActivity, SetARMarkerActivity::class.java)
                intent.putExtra(Constants.EXTRA_SET_AR_MARKER,file.path)
                startActivityForResult(intent, Constants.ACTIVITY_SET_AR_MARKER_OK)

            }

        }
        if (requestCode == Constants.ACTIVITY_RESULT_LABEL_LOCATION_OK) {
            if (data != null) {
                val tag = data.getStringExtra("TagName")
                val tagID = data.getStringExtra("TagID")
                this.locationTV.text = tag
                this.locationID = tagID
            }
        }

        if (requestCode == Constants.ACTIVITY_SET_AR_MARKER_OK) {

            Log.d(TAG,"imagePath -> ")

            if (data != null) {

//                val filePath = data.getStringExtra(Constants.EXTRA_AR_MARKER_URL)
//                Log.d(TAG,"imagePath -> " + filePath)
//                val bitmap = BitmapFactory.decodeFile(filePath)
//                imageARMarker.setImageBitmap(bitmap)

                arMarkerImageUrl = data.getStringExtra(Constants.EXTRA_AR_MARKER_URL)
                val roomName = data.getStringExtra(Constants.EXTRA_AR_MARKER_NAME)

                if(!roomName.isEmpty()) {

                    this.locationLL.visibility = View.VISIBLE
                    this.locationTV.text = roomName

                }

                //Glide.with(applicationContext).load(arMarkerImageUrl).into(this.imageARMarker)

                Glide.with(applicationContext)
                    .asBitmap()
                    .load(arMarkerImageUrl)
                    .into(object : CustomTarget<Bitmap>(){
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val sqRes = ImageHelper.getCroppedImageForNewLabel(resource)
                            val round = RoundedBitmapDrawableFactory.create(applicationContext.resources,sqRes)
                            round.cornerRadius = roundedCon
                            imageARMarker.setImageDrawable(round)
                            imageARMarker_btn.setImageDrawable(null)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
            }
        }
    }

    /**
     * Initialize UI
     */
    private fun init(label: Label) {
        this.labelNameTV.setText(label.name)
        this.locationTV.setText(label.location)
        if( label.floor_plan != null)
            this.locationID = label.floor_plan
        this.tagContainer.removeAllTags()
        if(label.tags.isNotEmpty()) {
            val tagStr: String = label.tags
            val tagArr: List<String> = tagStr.split(",").map { it.trim() }
            for (i in 0 until tagArr.size) {
                this.tagContainer.addTag(tagArr[i])
            }
        }

        injectRemindertime(label.reminder_time)
        for (i in 0 until label.images.size) {
            val imageUrl = label.images[i].url
            labelImageURL.add(imageUrl)
            labelThumbURL.add(label.images[i].thumbnail)
        }

        selectedLabelPhotoCounts = label.images.size

        arMarkerImageUrl = label.ar_mark_image
        //Glide.with(applicationContext).load(arMarkerImageUrl).into(this.imageARMarker)

        Glide.with(applicationContext)
            .asBitmap()
            .load(arMarkerImageUrl)
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val sqRes = ImageHelper.getCroppedImageForNewLabel(resource)
                    val round = RoundedBitmapDrawableFactory.create(applicationContext.resources,sqRes)
                    round.cornerRadius = roundedCon
                    imageARMarker.setImageDrawable(round)
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })

        if(!label.location.isEmpty()) {
            this.locationLL.visibility = View.VISIBLE
        }
        updateUI()
    }

    private fun checkValidation(): Boolean {
        val nameStr = this.labelNameTV.text.toString()
        this.createLabel.name = nameStr
        Global.label?.name = nameStr

        val locationStr = this.locationTV.text.toString()
        this.createLabel.floor_plan = locationID
        this.createLabel.location = locationStr
        Global.label?.floor_plan = locationID
        Global.label?.location = locationStr
        if (locationStr.isEmpty()) {
            //ToastUtil.show(this, "Location field is required")
            //return false
            this.createLabel.location = ""
            Global.label?.location = ""
        }
        var tagStr = ""
        for (i in 0 until tagContainer.childCount) {
            val tempStr: String = if (i == tagContainer.childCount - 1) {
                tagContainer.getTagText(i)
            } else {
                tagContainer.getTagText(i) + ","
            }
            tagStr += tempStr
        }
        this.createLabel.tags = tagStr
        Global.label?.tags = tagStr

        if (nameStr.isEmpty() && labelImageURL.isEmpty() && tagStr.isEmpty()) {
            MyToastUtil.showWarning(this, "Enter a note, add a snap, or add a tag.")
            return false
        }

//        if (labelImageURL.isEmpty()) {
//            MyToastUtil.showWarning(this, "Snap content is required.")
//            return false
//        }

//        if (tagStr.isEmpty()) {
//            MyToastUtil.showWarning(this, "Tags are required.")
//            return false
//        }

        return true

    }

    /***
     * Fragment Navigate Methods
     */
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, fragment)
            .addToBackStack("FloorPlanFragment")
            .commit()
    }

    fun popFragment() {
        supportFragmentManager
            .popBackStack("FloorPlanFragment", FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun sendCreateLabelRequest() {
        createNewLabel()
        val successCallback: (Label) -> Unit = { _ ->
            this.runOnUiThread {
                MyToastUtil.showMessage(applicationContext,"Success!")
            }
            val intent = Intent(this@CreateLabelActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val subscribeCallback: () -> Unit = {
            this.runOnUiThread {
                MyToastUtil.showWarning(applicationContext,getString(R.string.upgrade_membership_warning_msg))
            }
        }

        val errorCallback: (String) -> Unit = { message ->
            this.runOnUiThread {
                //MyToastUtil.showWarning(this, message)
                //MyToastUtil.showWarning(applicationContext,"Please check input fields")
            }
        }

        if(createLabel.images.size > 0) {
            createLabel.images[0].is_cover = true
        }

        APIManager.share.postLabel(this.createLabel, successCallback, subscribeCallback, errorCallback)
    }

    fun isCoverImageAnyOne(images: List<Image>) : Boolean {

        for ( image in images )
            if(image.is_cover)
                return true
        return false
    }

    @SuppressLint("LongLogTag")
    private fun sendUpdateLabelRequest() {
        //editLabel(Global.label!!)
        createNewLabel()
        val successCallback: (Label) -> Unit = { _ ->
            this.runOnUiThread {
                MyToastUtil.showMessage(applicationContext,"Success!")
            }
            val intent = Intent(this@CreateLabelActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        val subscribeCallback: () -> Unit = {
            this.runOnUiThread {
                MyToastUtil.showWarning(applicationContext,getString(R.string.upgrade_membership_warning_msg))
            }
        }
        if(!isCoverImageAnyOne(createLabel.images)) {
            if(createLabel.images.size > 0) {
                createLabel.images[0].is_cover = true
            }
        }


        createLabel.id = Global.label!!.id

        Log.d(TAG,"id -> " + createLabel.id)
        Log.d(TAG,"name -> " + createLabel.name)
        Log.d(TAG,"tags -> " + createLabel.tags)
        Log.d(TAG,"user -> " + createLabel.user)
        Log.d(TAG,"location -> " + createLabel.location)
        Log.d(TAG,"locationID -> " + createLabel.floor_plan)

        APIManager.share.updateLabel(createLabel, successCallback, subscribeCallback, super.errorCallback)
    }

    @SuppressLint("LongLogTag")
    private fun sendUpdateShareLabelRequest() {
        //editLabel(Global.shareLabel!!.label)
        createNewLabel()
        val successCallback: (Label) -> Unit = { sharelabel ->
            //Global.shareLabel = sharelabel
            this.runOnUiThread {
                MyToastUtil.showMessage(applicationContext,"Success!")
            }
            finish()
        }
        val subscribeCallback: () -> Unit = {
            this.runOnUiThread {
                MyToastUtil.showWarning(applicationContext,getString(R.string.upgrade_membership_warning_msg))
            }
        }
        if(!isCoverImageAnyOne(createLabel.images)) {
            if(createLabel.images.size > 0) {
                createLabel.images[0].is_cover = true
            }
        }

        createLabel.id = Global.shareLabel!!.label.id
        Global.shareLabel!!.label.name = createLabel.name
        Global.shareLabel!!.label.tags = createLabel.tags
        Global.shareLabel!!.label.user = createLabel.user
        Global.shareLabel!!.label.location = createLabel.location
        Global.shareLabel!!.label.images.clear()
        Global.shareLabel!!.label.images.addAll(createLabel.images)
        Global.shareLabel!!.label.reminder_time = createLabel.reminder_time

        Log.d(TAG,"id -> " + createLabel.id)
        Log.d(TAG,"name -> " + createLabel.name)
        Log.d(TAG,"tags -> " + createLabel.tags)
        Log.d(TAG,"user -> " + createLabel.user)
        Log.d(TAG,"location -> " + createLabel.location)

        APIManager.share.updateLabel(createLabel, successCallback,subscribeCallback, super.errorCallback)
    }

    private fun editLabel(label: Label) {
        if (arMarkerImageUrl.toString().isNotEmpty())
            label.ar_mark_image = arMarkerImageUrl.toString()

        val count = labelImageURL.size
        for (i in 0 until count) {
            label.images.removeAt(count - 1 - i)
        }

        for (i in 1..count) {
            val imageTemp = Image("", false, "","")
            imageTemp.id = (i - 1).toString()
            imageTemp.is_cover = false
            imageTemp.url = labelImageURL[i - 1]
            imageTemp.thumbnail = labelThumbURL[i - 1]
            label.images.add(imageTemp)
        }

        Global.label = label
    }

    private fun createNewLabel() {
        if (arMarkerImageUrl == null) {
            this.createLabel.ar_mark_image = ""
        } else {
            this.createLabel.ar_mark_image = arMarkerImageUrl as String
        }

        for (i in 1.. labelImageURL.size) {
            val imageTemp = Image("", false, "","")
            imageTemp.id = (i - 1).toString()
            imageTemp.is_cover = false
            imageTemp.url = labelImageURL[i - 1]
            imageTemp.thumbnail = labelThumbURL[i - 1]
            this.createLabel.images.add(imageTemp)
        }

        this.createLabel.imageCount = imageCount
        this.createLabel.reminder_time = getRemindertime()

    }

    private fun getRemindertime(): String {
        if (reminderDate.isEmpty() || reminderTime.isEmpty()) return ""

        val dtStart = "${reminderDate} $reminderTime"
        val format = SimpleDateFormat("MM/dd/yyyy hh:mm a")
        try {
            val myDate = format.parse(dtStart)

            val calendar = Calendar.getInstance()
            calendar.setTime(myDate)
            val time = calendar.getTime()
            val outputFmt = SimpleDateFormat(DateTimeUtils.CONVERTPATTEN)
            outputFmt.timeZone = TimeZone.getTimeZone("UTC")
            val dateAsString = outputFmt.format(time)
            return dateAsString

        } catch (e: ParseException) {
            e.printStackTrace()
            return ""
        }

        return ""
    }

    private fun injectRemindertime(reminderT: String) {
        if(reminderT.isEmpty()) return

        reminderDate = DateTimeUtils().getLocalDate(reminderT)
        reminderTime = DateTimeUtils().getLocalTime(reminderT)
        updateReminder()
    }

    /***
     * This method is used to upload the file to S3 by using TransferUtility class
     * @param
     */

    private fun setImage(filePath: File, filename: String) {
        labelThumbURL.add(filename)
        labelImageURL.add(filename)
        updateUI()
        adapter?.notifyDataSetChanged()

        if (isFybe) {
            val file = File(filePath, filename)
            MyToastUtil.showMessage(applicationContext, "Predicting object...")
            doFybe(file)
        }

    }

    private fun uploadImages() {

        cusItem = 0
        imageCount = 0
        showWaitDialog()
        uploadOneImage()

    }

    private fun uploadOneImage() {
        filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        if(filePath == null) {
            closeWaitDialog()
            MyToastUtil.showWarning(this,"Cannot upload images on AWS")
            return
        }
        if(cusItem == labelImageURL.size) {
            closeWaitDialog()
            if (isCreate) {
                sendCreateLabelRequest()
            } else {
                when (labelType) {
                    1 -> {
                        sendUpdateLabelRequest()
                    }
                    3 -> {
                        sendUpdateShareLabelRequest()
                    }
                }
            }
            return
        }
        if(labelImageURL[cusItem].contains("http")) {
            cusItem ++
            uploadOneImage()
            return
        }

        val transferUtility = TransferUtility(s3Clients, applicationContext)

        val filename = labelImageURL[cusItem]
        val file = File(filePath, filename)
        var bitmap = BitmapFactory.decodeFile(file.path)
        if( bitmap == null) {
            this.runOnUiThread {
                MyToastUtil.showWarning(applicationContext,getString(R.string.filepathorimagefileError))
            }
            return
        }

        val bitmap_thum = ImageHelper.getResizedBitmap(bitmap,Constants.THUMBNAILSIZE)
        // Create Thumb File
        val file_path_thumbnail = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/PhysicsThumbnails"
        val dir_thumbnail = File(file_path_thumbnail)
        if(!dir_thumbnail.exists())
            dir_thumbnail.mkdirs()
        val file_thumbnail = File(dir_thumbnail, filename + "thumbnail" + ".png")

        // Write Images into Storage.
        val fOut_thumbnail = FileOutputStream(file_thumbnail)
        bitmap_thum.compress(Bitmap.CompressFormat.PNG, 85, fOut_thumbnail)
        fOut_thumbnail.flush()
        fOut_thumbnail.close()

        val observer = transferUtility.upload(
            "wizt/labels",
            filename + "thumbnail", /* The bucket to upload to */
            file_thumbnail, /* file name for uploaded object */
            CannedAccessControlList.PublicRead
        )

        observer.setTransferListener(object : TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            }

            override fun onStateChanged(id: Int, state: TransferState?) {
                if (TransferState.COMPLETED == state) {
                    fileURL = s3Clients?.getResourceUrl("wizt/labels", filename + "thumbnail").toString()

                    //labelImageURL.add(fileURL)
                    labelThumbURL[cusItem] = fileURL
                    file_thumbnail.delete()

                    uploadContinueBigImage(labelImageURL[cusItem])

                } else if (TransferState.FAILED == state) {
                    MyToastUtil.showWarning(applicationContext, "Upload Failed!")
                    file_thumbnail.delete()
                }
            }

            override fun onError(id: Int, ex: Exception?) {
                MyToastUtil.showWarning(applicationContext, "AWS Error!")
                file_thumbnail.delete()
                ex?.printStackTrace()
            }
        })

    }

    fun uploadContinueBigImage(filename: String) {
        val transferUtility = TransferUtility(s3Clients, applicationContext)

        // Large Image GOT
        val file = File(filePath, filename)
        var bitmap = BitmapFactory.decodeFile(file.path)

        val fOut = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)
        fOut.flush()
        fOut.close()

        val observer = transferUtility.upload(
            "wizt/labels",
            filename, /* The bucket to upload to */
            file, /* file name for uploaded object */
            CannedAccessControlList.PublicRead
        )

        observer.setTransferListener(object : TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            }

            override fun onStateChanged(id: Int, state: TransferState?) {
                if (TransferState.COMPLETED == state) {

                    fileURL = s3Clients?.getResourceUrl("wizt/labels", filename).toString()
                    file.delete()
                    labelImageURL[cusItem] = fileURL
                    cusItem ++
                    imageCount ++
                    uploadOneImage()

                } else if (TransferState.FAILED == state) {
                    closeWaitDialog()
                    file.delete()
                    MyToastUtil.showWarning(applicationContext, "Upload Failed! (Large Image)")
                }
            }

            override fun onError(id: Int, ex: Exception?) {
                closeWaitDialog()
                file.delete()
                MyToastUtil.showWarning(applicationContext, "AWS Error!")
                ex?.printStackTrace()
            }
        })

    }

    private fun uploadImage(filePath: File, filename: String) {
        val transferUtility = TransferUtility(s3Clients, applicationContext)

        val file = File(filePath, filename)
        var bitmap = BitmapFactory.decodeFile(file.path)
        if( bitmap == null) {
            this.runOnUiThread {
                MyToastUtil.showWarning(applicationContext,getString(R.string.filepathorimagefileError))
            }
            return
        }
        val bitmap_thum = ImageHelper.getResizedBitmap(bitmap,Constants.THUMBNAILSIZE)
        // Create Thumb File
        val file_path_thumbnail = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/PhysicsThumbnails"
        val dir_thumbnail = File(file_path_thumbnail)
        if(!dir_thumbnail.exists())
            dir_thumbnail.mkdirs()
        val file_thumbnail = File(dir_thumbnail, filename + "thumbnail" + ".png")

        // Write Images into Storage.
        val fOut_thumbnail = FileOutputStream(file_thumbnail)
        bitmap_thum.compress(Bitmap.CompressFormat.PNG, 85, fOut_thumbnail)
        fOut_thumbnail.flush()
        fOut_thumbnail.close()

        val observer = transferUtility.upload(
            "wizt/labels",
            filename + "thumbnail", /* The bucket to upload to */
            file_thumbnail, /* file name for uploaded object */
            CannedAccessControlList.PublicRead
        )

        showWaitDialog()

        observer.setTransferListener(object : TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
//                val percentDoneF = bytesCurrent.toFloat() / bytesTotal.toFloat() * 100
//                Toast.makeText(
//                    applicationContext, "Progress in %"
//                            + percentDoneF, Toast.LENGTH_SHORT
//                ).show()
            }

            override fun onStateChanged(id: Int, state: TransferState?) {
                if (TransferState.COMPLETED == state) {
                    //Toast.makeText(applicationContext, "Upload Completed!", Toast.LENGTH_LONG).show()
                    fileURL = s3Clients?.getResourceUrl("wizt/labels", filename + "thumbnail").toString()

                    //labelImageURL.add(fileURL)
                    labelThumbURL.add(fileURL)
                    updateUI()
                    adapter?.notifyDataSetChanged()
                    file_thumbnail.delete()

                } else if (TransferState.FAILED == state) {
                    MyToastUtil.showWarning(applicationContext, "Upload Failed!")
                    file_thumbnail.delete()
                }
            }

            override fun onError(id: Int, ex: Exception?) {
                MyToastUtil.showWarning(applicationContext, "Error!")
                file_thumbnail.delete()
                ex?.printStackTrace()
            }
        })

        uploadContinue(filePath,filename)

    }

    fun updateUI() {
        if (labelThumbURL.size == 0) {
            cameraBtn.visibility = View.VISIBLE
            cameraBtn_addmore.visibility = View.GONE
        }
        else {
            cameraBtn.visibility = View.INVISIBLE
            cameraBtn_addmore.visibility = View.VISIBLE
        }
    }

    private fun doFybe(file : File) {

        val successCallback: (PredictionModel) -> Unit = { predictionModel ->

            closeWaitDialog()

            val tags : MutableMap<String,Double> = mutableMapOf()
            for ( tag in predictionModel.nn_predictions)
            {
                if( tags.keys.contains(tag.key)) {
                    if (tags[tag.key]!!.compareTo(tag.value) < 0) {
                        tags[tag.key] = tag.value
                    }
                } else {
                    tags.put(tag.key, tag.value)
                }
            }
            for ( tag in predictionModel.knn_predictions)
            {
                if( tags.keys.contains(tag.key)) {
                    if (tags[tag.key]!!.compareTo(tag.value) < 0) {
                        tags[tag.key] = tag.value
                    }
                } else {
                    tags.put(tag.key, tag.value)
                }
            }
            for ( tag in predictionModel.log_reg_predictions)
            {
                if( tags.keys.contains(tag.key)) {
                    if (tags[tag.key]!!.compareTo(tag.value) < 0) {
                        tags[tag.key] = tag.value
                    }
                } else {
                    tags.put(tag.key, tag.value)
                }
            }

            val sortedMap = tags


            runOnUiThread {
//                var count = 0
//                for ( key in sortedMap.keys) {
//                    tagContainer.addTag(key)
//                    count ++
//                    if( count == 3) {
//                        break
//                    }
//                }
                if (sortedMap.keys.size == 0) {
                    MyToastUtil.showWarning(applicationContext,"No Prediction Result")
                }
                else {
                    MyDialog.showCustomSuggestionFybeDialog(this,labelImageURL[labelImageURL.size - 1],sortedMap.keys)
                }

            }

            //file.delete()
        }

        val errorCallback: (String) -> Unit = { message ->
            closeWaitDialog()
            //file.delete()
            runOnUiThread {
                MyToastUtil.showWarning(applicationContext,"No Prediction Result")
            }
        }

        showWaitDialog()
        APIManager.share.postPredictionModel(Global.profile!!.id, file, successCallback, errorCallback)
    }

    private fun uploadContinue(filePath: File, filename: String) {
        val transferUtility = TransferUtility(s3Clients, applicationContext)

        // Large Image GOT
        val file = File(filePath, filename)
        var bitmap = BitmapFactory.decodeFile(file.path)

        val fOut = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)
        fOut.flush()
        fOut.close()

        val observer = transferUtility.upload(
            "wizt/labels",
            filename, /* The bucket to upload to */
            file, /* file name for uploaded object */
            CannedAccessControlList.PublicRead
        )

        observer.setTransferListener(object : TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            }

            override fun onStateChanged(id: Int, state: TransferState?) {
                if (TransferState.COMPLETED == state) {
                    closeWaitDialog()
                    fileURL = s3Clients?.getResourceUrl("wizt/labels", filename).toString()
                    if (!isFybe) {
                        MyToastUtil.showMessage(applicationContext, getString(R.string.uploadcompleted))
                        file.delete()
                    }
                    else {
                        MyToastUtil.showMessage(applicationContext, "Predicting object...")
                        doFybe(file)
                    }
                    labelImageURL.add(fileURL)

                } else if (TransferState.FAILED == state) {
                    closeWaitDialog()
                    file.delete()
                    MyToastUtil.showWarning(applicationContext, "Upload Failed! (Large Image)")
                }
            }

            override fun onError(id: Int, ex: Exception?) {
                closeWaitDialog()
                file.delete()
                MyToastUtil.showWarning(applicationContext, "Error!")
                ex?.printStackTrace()
            }
        })


    }

    /**
     * Item Click Event Handler
     */

    private val itemClickListener:(Int) -> Unit = { position ->
        //imageNumber = position
        //Glide.with(applicationContext).load(labelThumbURL[position]).into(this.imageARMarker)

        MyDialog.showCustomDialog(this,labelImageURL[position],"",position)

    }

    // ImageView RecyclerView
    class RecyclerAdapter(private val context: Context, val itemClickListener: (int: Int) -> Unit, arr: ArrayList<String>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var labelThumbUrlAdapter: ArrayList<String> = arr

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_label_image, parent, false)

            return LabelViewHolder(view)
        }

        override fun getItemCount(): Int {
            return this.labelThumbUrlAdapter.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.setOnClickListener {
                itemClickListener(position)
            }

            //Glide.with(context).load(labelThumbUrlAdapter[position]).into(holder.itemView.labelImage)

            if(labelThumbUrlAdapter[position].contains("http")) {
                Glide.with(context)
                    .asBitmap()
                    .load(labelThumbUrlAdapter[position])
                    .into(object : CustomTarget<Bitmap>(){
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val sqRes = ImageHelper.getCroppedImageForNewLabel(resource)
                            val round = RoundedBitmapDrawableFactory.create(context.resources,sqRes)
                            round.cornerRadius = roundedCon
                            holder.itemView.labelImage.setImageDrawable(round)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
            } else {
                val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val file = File(filePath, labelThumbUrlAdapter[position])
                var bitmap = BitmapFactory.decodeFile(file.path)
                if( bitmap != null) {
                    val sqRes = ImageHelper.getCroppedImageForNewLabel(bitmap)
                    val round = RoundedBitmapDrawableFactory.create(context.resources,sqRes)
                    round.cornerRadius = roundedCon
                    holder.itemView.labelImage.setImageDrawable(round)
                }
            }

            (holder as LabelViewHolder).bind()
        }

        class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            @SuppressLint("SetTextI18n")
            fun bind() {
            }
        }
    }
}
