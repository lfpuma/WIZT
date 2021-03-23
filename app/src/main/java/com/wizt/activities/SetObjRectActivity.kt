package com.wizt.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.support.constraint.ConstraintLayout
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.crashlytics.android.Crashlytics
import com.wizt.R
import com.wizt.common.base.BaseActivity
import com.wizt.common.constants.Constants
import com.wizt.models.Global
import com.wizt.utils.MyToastUtil
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_set_obj_rect.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SetObjRectActivity : BaseActivity() {

    // s3
    private var credentialsProvider: CognitoCachingCredentialsProvider? = null
    private var s3Clients: AmazonS3Client? = null
    private var filePath: File? = null
    private var arMarkerFileURL : String? = ""

    private var savedFilename : String? = null
    private var imageCase : Int? = 0

    private lateinit var mDrawConstraintLayout : ConstraintLayout
    private lateinit var mImageView: ImageView
    private lateinit var rectView : View

    var corx = 0f
    var cory = 0f
    var posX = 0
    var posY = 0
    var drawState = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_set_obj_rect)

        mDrawConstraintLayout = findViewById(R.id.containerLayout)
        mImageView = findViewById(R.id.imageView)
        rectView = findViewById(R.id.rectView)

        val filePath = intent.getStringExtra(Constants.EXTRA_SET_OBJ_RECT)
        imageCase = intent.getIntExtra(Constants.EXTRA_SET_OBJ_RECT_COUNT,0)

        val bitmap = BitmapFactory.decodeFile(filePath)
        mImageView.setImageBitmap(bitmap)
        val tFile = File(filePath)
        tFile.delete()

        addSelRect()


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

        // UI Event
        doneBtn.setOnClickListener {
            saveLayout()
        }

        saveBtn.setOnClickListener {
            saveLayout()
        }

        closeBtn.setOnClickListener {

            finish()
        }
    }

    fun addSelRect() {

        mImageView.setOnTouchListener { view, motionEvent ->

            val eventAction = motionEvent.action

            corx = motionEvent.x
            cory = motionEvent.y

            when (eventAction) {
                MotionEvent.ACTION_DOWN -> drawState = 1
                MotionEvent.ACTION_MOVE -> drawState = 2
                MotionEvent.ACTION_UP -> drawState = 3
            }

            invalidateRect()
            true
        }

    }

    fun invalidateRect() {
        val params = rectView.layoutParams as ConstraintLayout.LayoutParams
        if(drawState == 1) {
            params.leftMargin = corx.toInt()
            params.topMargin = cory.toInt()
            params.height = 0
            params.width = 0
            posX = corx.toInt()
            posY = cory.toInt()
        }
        else if (drawState == 2) {
            if(corx.toInt() >= posX)
                params.width = corx.toInt() - posX
            else params.width = 0
            if(cory.toInt() >= posY)
                params.height = cory.toInt() - posY
            else params.height = 0
        }
        rectView.layoutParams = params

    }

    @SuppressLint("SimpleDateFormat")
    private fun saveLayout() {

        if(rectView.width == 0 || rectView.height == 0) {
            MyToastUtil.showWarning(this,"No selected Image")
            return
        }

        this.doneBtn.visibility = View.GONE
        var bitmap = Bitmap.createBitmap(mDrawConstraintLayout.width, mDrawConstraintLayout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        mDrawConstraintLayout.draw(canvas)

        var bmpWidth = rectView.width
        var bmpHeight = rectView.height

        if(posX + rectView.width > bitmap.width) {
            bmpWidth = bitmap.width - posX
        }
        if(posY + rectView.height > bitmap.height) {
            bmpHeight = bitmap.height - posY
        }
        bitmap = Bitmap.createBitmap(
            bitmap,
            posX,
            posY,
            bmpWidth,
            bmpHeight)

        if (bitmap != null) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            savedFilename = "$timeStamp.jpg"
            saveImage(bitmap, savedFilename!!)
        }

        filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        //uploadImage(filePath!!, savedFilename!!)
        val intent = Intent()
        intent.putExtra(Constants.EXTRA_OBJ_RECT_URL, savedFilename)
        setResult(Activity.RESULT_OK, intent)
        finish()
        //this.doneBtn.visibility = View.VISIBLE
    }

    private fun saveImage(bitmap: Bitmap, filename: String) {

        val outStream: FileOutputStream
        try {
            var file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename)
            if(!file.exists()) {
                val dcimFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val isSuccess = dcimFolder.mkdir()
                file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename)
            }
            outStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.flush()
            outStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uploadImage(filePath: File, filename: String) {
        val transferUtility = TransferUtility(s3Clients, applicationContext)

        val file = File(filePath, filename)

        showWaitDialog()
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
                    closeWaitDialog()
                    file.delete()
                    MyToastUtil.showMessage(applicationContext, getString(R.string.uploadcompleted))
                    arMarkerFileURL = s3Clients?.getResourceUrl("wizt/image", className).toString()

                    val intent = Intent()
                    intent.putExtra(Constants.EXTRA_OBJ_RECT_URL, arMarkerFileURL)
                    setResult(Activity.RESULT_OK, intent)
                    finish()

                } else if (TransferState.FAILED == state) {
                    closeWaitDialog()
                    file.delete()
                    MyToastUtil.showWarning(applicationContext, "Upload Failed!")
                }
            }

            override fun onError(id: Int, ex: java.lang.Exception?) {
                closeWaitDialog()
                file.delete()
                MyToastUtil.showWarning(applicationContext, "Error!")
                ex?.printStackTrace()
            }
        })
    }

    fun getSubName() : String {
        var subString = imageCase.toString()
        when (subString.length) {
            0 -> subString = "000"
            1 -> subString = "00" + subString
            2 -> subString = "0" + subString
        }

        return "_" + subString
    }

}