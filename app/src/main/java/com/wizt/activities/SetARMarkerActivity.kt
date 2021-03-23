package com.wizt.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.support.constraint.ConstraintLayout
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import com.wizt.common.constants.Constants
import com.wizt.R
import com.wizt.common.base.BaseActivity
import com.wizt.components.myDialog.MyDialog
import com.wizt.utils.MyToastUtil
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_set_ar_marker.*
import kotlinx.android.synthetic.main.activity_set_ar_marker.closeBtn
import kotlinx.android.synthetic.main.activity_set_ar_marker.saveBtn
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class SetARMarkerActivity: BaseActivity() {

    // s3
    private var credentialsProvider: CognitoCachingCredentialsProvider? = null
    private var s3Clients: AmazonS3Client? = null
    private var filePath: File? = null
    private var arMarkerFileURL : String? = ""

    private var savedFilename : String? = null

    private lateinit var mDrawConstraintLayout : ConstraintLayout
    private lateinit var mARMarkerIV : ImageView
    private lateinit var mImageView: ImageView

    var isFinalSave = false
    var roomName = ""

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_set_ar_marker)

        mDrawConstraintLayout = findViewById(R.id.containerLayout)
        mARMarkerIV = findViewById(R.id.arImageView)
        mImageView = findViewById(R.id.imageView)

        val filePath = intent.getStringExtra(Constants.EXTRA_SET_AR_MARKER)
        //Glide.with(applicationContext).load(imageUrl).into(mImageView)

        val bitmap = BitmapFactory.decodeFile(filePath)
        mImageView.setImageBitmap(bitmap)
        val tFile = File(filePath)
        tFile.delete()


        mARMarkerIV.setOnTouchListener(MyTouchListener())
        mDrawConstraintLayout.setOnDragListener(MyDragListener())

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

    override fun onBackPressed() {
        super.onBackPressed()
        isFinalSave = false
    }

    private fun saveLayout() {

        if(isFinalSave) {
            saveLayoutFinal()
        } else {
            this.titleTV.text = getString(R.string.location_info_set_ar)
            MyDialog.showLocationInfoDialog(this)
        }

    }

    @SuppressLint("SimpleDateFormat", "RestrictedApi")
    fun saveLayoutFinal() {
        this.doneIV.visibility = View.GONE
        this.doneBtn.visibility = View.GONE

        val bitmap = Bitmap.createBitmap(mDrawConstraintLayout.width, mDrawConstraintLayout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        mDrawConstraintLayout.draw(canvas)

        if (bitmap != null) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            savedFilename = "$timeStamp.jpg"
            saveImage(bitmap, savedFilename!!)
        }

        filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        uploadImage(filePath!!, savedFilename!!)
        this.doneBtn.visibility = View.VISIBLE
        this.doneIV.visibility = View.VISIBLE
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
            //Toast.makeText(this, "Picture Saved: $filename", Toast.LENGTH_SHORT).show()
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
                    file.delete()
                    MyToastUtil.showMessage(applicationContext, getString(R.string.uploadcompleted))
                    arMarkerFileURL = s3Clients?.getResourceUrl("wizt/labels", filename).toString()

                    val intent = Intent()
                    intent.putExtra(Constants.EXTRA_AR_MARKER_URL, arMarkerFileURL)
                    intent.putExtra(Constants.EXTRA_AR_MARKER_NAME, roomName)
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

    @Suppress("DEPRECATION")
    private inner class MyTouchListener : View.OnTouchListener {

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            return if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val data = ClipData.newPlainText("", "")
                val shadowBuilder = View.DragShadowBuilder(
                    view
                )
                view.startDrag(data, shadowBuilder, view, 0)
                view.visibility = View.INVISIBLE

                //setCurrentView(view)

                true
            } else {
                false
            }
        }

    }

    internal inner class MyDragListener : View.OnDragListener {

        override fun onDrag(v: View, event: DragEvent): Boolean {
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                }
                DragEvent.ACTION_DROP -> {
                    val view = event.localState as View
                    val owner = view.parent as ViewGroup
                    owner.removeView(view)
                    val container = v as ConstraintLayout
                    container.addView(view)
                    view.x = event.x - view.width / 2
                    view.y = event.y - view.height / 2
                    view.visibility = View.VISIBLE
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                }
                else -> {
                }
            }
            return true
        }
    }
}