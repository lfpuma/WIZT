package com.wizt.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.wizt.common.constants.Constants
import com.wizt.R
import kotlinx.android.synthetic.main.activity_creat_trainobject.*
import java.io.File
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.crashlytics.android.Crashlytics
import com.wizt.common.base.BaseActivity
import com.wizt.common.http.APIManager
import com.wizt.components.myDialog.MyDialog
import com.wizt.components.myToast.Mytoast
import com.wizt.extensions.ImageHelper
import com.wizt.fragments.HomeFragment
import com.wizt.models.*
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import com.wizt.utils.ToastUtil
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.item_label_image_spe.view.*
import java.io.FileOutputStream
import kotlin.Exception
import java.io.InputStream as InputStream1


@Suppress("DEPRECATION")
class CreateTrainObjectActivity : BaseActivity() {

    companion object {
        const val TAG = "WIZT:CreateTrainObjectActivity"
        const val roundedCon = 40.0f
    }

    private var isCreate: Boolean = true
    private var cusItem = 0
    private var imageCount = 0

    // s3
    private var credentialsProvider: CognitoCachingCredentialsProvider? = null
    private var s3Clients: AmazonS3Client? = null
    private var imageFileName: String? = null
    private var filePath: File? = null
    private lateinit var fileURL : String

    // UI
    lateinit var recyclerView: RecyclerView
    lateinit var gridLayoutManager: GridLayoutManager
    lateinit var adapter: RecyclerAdapter

    // Data
    var labelImageURL: ArrayList<String> = ArrayList()
    var selectedLabelPhotoCounts = 0
    //var labelThumbURL: ArrayList<String> = ArrayList()

    private var createTrainObject:TrainObject =  TrainObject("",0, "", "",  arrayListOf(), 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_creat_trainobject)

        // check if the activity initialize for creating or editing
        isCreate = intent.getBooleanExtra(Constants.EXTRA_CREATE_TRAIN_ACTIVITY_TYPE, false)
        if (!isCreate) {
            this.titleTV.setText(R.string.edit_trainObject)
            init(Global.trainObject!!)
        }
        else {
            val titleStr = intent.getStringExtra(Constants.EXTRA_CREATE_TRAIN_ACTIVITY_TITLE)
            this.trainObjectNameTV.setText(titleStr)
        }

        contentImagesTV.visibility = View.VISIBLE
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
        saveBtn.setOnClickListener {
            if (!checkValidation()) {
                return@setOnClickListener
            }

            uploadImages()
        }

        closeBtn.setOnClickListener {
            finish()
        }

        cameraBtn.setOnClickListener {

            if(!checkValidation()) {
                return@setOnClickListener
            }

            // Get Profile
            val successCallback: (Profile) -> Unit = { profile: Profile ->
                Global.profile = profile
                PreferenceUtils.saveString(Constants.PROFILE_USER_NAME, profile.name)
                PreferenceUtils.saveString(Constants.PROFILE_USER_EMAIL, profile.email)
                if (profile.picture != null) {
                    PreferenceUtils.saveString(Constants.PROFILE_USER_PICTURE_AVATAR, profile.picture!!)
                }

                this.runOnUiThread {
                    if (Global.profile!!.fybe_cnt + selectedLabelPhotoCounts <= labelImageURL.size)
                        MyToastUtil.showWarning(this, getString(R.string.upgrade_membership_warning_msg))
                    else {
                        val intent = Intent(this, CameraActivity::class.java)
                        intent.putExtra(Constants.EXTRA_CAMERA_NAME,getString(R.string.take_multiple_angles))
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

        trainObjectNameTV.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        })

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.imageRecyclerView)
        gridLayoutManager = GridLayoutManager(applicationContext, 5)
        recyclerView.layoutManager = gridLayoutManager
        adapter = RecyclerAdapter(applicationContext, itemClickListener, labelImageURL)
        recyclerView.adapter = adapter
    }

    fun uploadImages() {
        cusItem = 0
        imageCount = 0
        showWaitDialog()
        uploadOneImage()
    }

    fun uploadOneImage() {
        filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        if(filePath == null) {
            closeWaitDialog()
            MyToastUtil.showWarning(this,"Cannot upload images on AWS")
            return
        }
        if(cusItem == labelImageURL.size) {
            closeWaitDialog()
            if (isCreate) {
                sendCreateRequest()
            } else {
                sendUpdateRequest()
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

        var className = Global.trainObject!!.name + getSubName() + ".jpg"
        className = className.replace(" ","-")
        val observer = transferUtility.upload(
            "wizt/image",
            className, /* The bucket to upload to */
            file, /* file name for uploaded object */
            CannedAccessControlList.PublicRead
        )

        observer.setTransferListener(object : TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            }

            override fun onStateChanged(id: Int, state: TransferState?) {
                if (TransferState.COMPLETED == state) {
                    file.delete()
                    val url = s3Clients?.getResourceUrl("wizt/image", className).toString()

                    labelImageURL[cusItem] = url
                    cusItem ++
                    imageCount ++
                    uploadOneImage()

                } else if (TransferState.FAILED == state) {
                    closeWaitDialog()
                    file.delete()
                    MyToastUtil.showWarning(applicationContext, "Upload Failed!")
                }
            }

            override fun onError(id: Int, ex: java.lang.Exception?) {
                closeWaitDialog()
                file.delete()
                MyToastUtil.showWarning(applicationContext, "AWS Error!")
                ex?.printStackTrace()
            }
        })
    }

    fun getSubName() : String {
        var subString = cusItem.toString()
        when (subString.length) {
            0 -> subString = "000"
            1 -> subString = "00" + subString
            2 -> subString = "0" + subString
        }

        return "_" + subString
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    @SuppressLint("LongLogTag")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK)   return

        if (requestCode == Constants.ACTIVITY_RESULT_CAMERA_OK) {
            imageFileName = data?.getStringExtra("ImageName")

            if (Constants.REQUIRECALL.equals(imageFileName)) {

                val intent = Intent(this, CameraActivity::class.java)
                intent.putExtra(Constants.EXTRA_CAMERA_NAME,getString(R.string.take_multiple_angles))
                startActivityForResult(intent, Constants.ACTIVITY_RESULT_CAMERA_OK)
                return

            }

            filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)

            val file = File(filePath, imageFileName)
            var bitmap = BitmapFactory.decodeFile(file.path)
            val fOut = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)
            fOut.flush()
            fOut.close()

            val intent = Intent(this@CreateTrainObjectActivity, SetObjRectActivity::class.java)
            intent.putExtra(Constants.EXTRA_SET_OBJ_RECT,file.path)
            intent.putExtra(Constants.EXTRA_SET_OBJ_RECT_COUNT,labelImageURL.size)
            startActivityForResult(intent, Constants.ACTIVITY_SET_OBJ_RECT_OK)


        }

        if (requestCode == Constants.ACTIVITY_SET_OBJ_RECT_OK) {

            Log.d(TAG,"imagePath -> ")

            if (data != null) {

                val fileURL = data.getStringExtra(Constants.EXTRA_OBJ_RECT_URL)
                labelImageURL.add(fileURL)
                adapter?.notifyDataSetChanged()
            }
        }
    }

    /**
     * Initialize UI
     */
    private fun init(trainObject: TrainObject) {
        this.trainObjectNameTV.setText(trainObject.name)

        createTrainObject.id = Global.trainObject!!.id
        for (i in 0 until trainObject.images.size) {
            val imageUrl = trainObject.images[i].url
            labelImageURL.add(imageUrl)
            //labelThumbURL.add(trainObject.images[i].thumbnail)
        }
        selectedLabelPhotoCounts = trainObject.images.size
    }

    private fun checkValidation(): Boolean {
        val nameStr = this.trainObjectNameTV.text.toString()
        createTrainObject.name = nameStr
        createTrainObject.train_class = nameStr
        Global.trainObject = createTrainObject
        if (nameStr.isEmpty()) {
            //ToastUtil.show(this, "")
            MyToastUtil.showWarning(this,"Name field is required")
            return false
        }

        return true

    }

    private fun sendCreateRequest() {
        createNew()
        val successCallback: (TrainObject) -> Unit = { _ ->
            closeWaitDialog()
            finish()
        }
        val subscribeCallback: () -> Unit = {

            closeWaitDialog()
            runOnUiThread {
                MyToastUtil.showWarning(applicationContext,getString(R.string.upgrade_membership_warning_msg))
            }

        }
        val errorCallback: (String) -> Unit = { message ->
            closeWaitDialog()
            runOnUiThread {
                MyToastUtil.showWarning(this, getString(R.string.errormsg_training))
            }
        }
        showWaitDialog()
        APIManager.share.postTrainObject(createTrainObject, successCallback, subscribeCallback, errorCallback)
    }

    @SuppressLint("LongLogTag")
    private fun sendUpdateRequest() {
        //editLabel(Global.label!!)
        createNew()
        val successCallback: (TrainObject) -> Unit = { _ ->
            closeWaitDialog()
            finish()
        }
        val errorCallback: (String) -> Unit = { message ->
            closeWaitDialog()
            runOnUiThread {
                MyToastUtil.showWarning(this, getString(R.string.errormsg_training))
            }
        }
        showWaitDialog()
        APIManager.share.updateTrainObject(createTrainObject, successCallback, errorCallback)
    }

    private fun createNew() {

        for (i in 1.. labelImageURL.size) {
            val imageTemp = FybeImage("", "","")
            imageTemp.id = (i - 1).toString()
            imageTemp.url = labelImageURL[i - 1]
            //imageTemp.thumbnail = labelThumbURL[i - 1]
            this.createTrainObject.images.add(imageTemp)
        }

        this.createTrainObject.imageCount = imageCount
    }

    /***
     * This method is used to upload the file to S3 by using TransferUtility class
     * @param
     */

    private fun uploadImage(filePath: File, filename: String) {
        val transferUtility = TransferUtility(s3Clients, applicationContext)

        val file = File(filePath, filename)
        var bitmap = BitmapFactory.decodeFile(file.path)
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
                    //labelThumbURL.add(fileURL)
                    adapter?.notifyDataSetChanged()

                } else if (TransferState.FAILED == state) {
                    MyToastUtil.showWarning(applicationContext, "Upload Failed!")
                }
            }

            override fun onError(id: Int, ex: Exception?) {
                MyToastUtil.showWarning(applicationContext, "Error!")
                ex?.printStackTrace()
            }
        })

        uploadContinue(filePath,filename)

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
                    MyToastUtil.showMessage(applicationContext, getString(R.string.uploadcompleted))
                    //Toast.makeText(applicationContext, "Upload Completed!", Toast.LENGTH_LONG).show()
                    //MyToastUtil.showMessage(applicationContext, getString(R.string.uploadcompleted))
                    fileURL = s3Clients?.getResourceUrl("wizt/labels", filename).toString()

                    labelImageURL.add(fileURL)

                } else if (TransferState.FAILED == state) {
                    closeWaitDialog()
                    MyToastUtil.showWarning(applicationContext, "Upload Failed! (Large Image)")
                }
            }

            override fun onError(id: Int, ex: Exception?) {
                closeWaitDialog()
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

        MyDialog.showTrainObjectImageDialog(this,labelImageURL[position],"",position)

    }

    // ImageView RecyclerView
    class RecyclerAdapter(private val context: Context, val itemClickListener: (int: Int) -> Unit, arr: ArrayList<String>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var labelThumbUrlAdapter: ArrayList<String> = arr

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_label_image_spe, parent, false)

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
                            val sqRes = ImageHelper.getSquredBitmap(resource)
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
                    val sqRes = ImageHelper.getSquredBitmap(bitmap)
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
