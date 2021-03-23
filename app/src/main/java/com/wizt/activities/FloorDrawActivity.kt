package com.wizt.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.crashlytics.android.Crashlytics
import com.google.android.gms.ads.AdRequest
import com.wizt.common.http.APIManager
import com.wizt.models.FloorPlan
import com.wizt.R
import com.wizt.common.base.BaseActivity
import com.wizt.models.Global
import com.wizt.utils.MyToastUtil
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_floor_draw.*
import kotlinx.android.synthetic.main.activity_floor_draw.view.*
import kotlinx.android.synthetic.main.floor_dialog.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class FloorDrawActivity: BaseActivity() {

    companion object {
        const val TAG = "WIZT:FloorDrawActivity"
    }

    private val typeSquare = 1
    private val typeCircle = 2
    private val typeLineH = 3
    private val typeLineV = 4
    private val typeRectangle = 5
    private val typeSquare_another = 6

    internal var minValue: Int = 0

    private lateinit var mConstraintLayout: ConstraintLayout
    private lateinit var mDrawConstraintLayout : ConstraintLayout
    private lateinit var nameLayout: ConstraintLayout

    private lateinit var seekBar: SeekBar

    private lateinit var encoderStr : String
    private var tagString: String? = ""
    private var tagCount: Int? = 0
    private var savedFilename : String? = null

    internal var currentView: TextView? = null
    private var nameEdit: EditText? = null

    private lateinit var newFloorPlan: FloorPlan

    // s3
    private var credentialsProvider: CognitoCachingCredentialsProvider? = null
    private var s3Clients: AmazonS3Client? = null
    private var filePath: File? = null
    private lateinit var fileURL : String
    private var startID = -1

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_floor_draw)

        //addAds()

        exitBtn.setOnClickListener { finish() }

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

        saveBtn.setOnClickListener {
            saveLayout()
        }
        tagCount = 0
        minValue = resources.getDimensionPixelSize(R.dimen.minValue)
        //currentView?.setTextColor(R.color.colorWhite)

        mConstraintLayout = findViewById(R.id.mainConstraint)
        mDrawConstraintLayout = findViewById(R.id.drawConstraint)
        seekBar = findViewById(R.id.seekBar)
        seekBar.progress = minValue
        //seekBar.visibility = View.GONE

        seekBar.setOnSeekBarChangeListener(seekBarChangeListener)

        nameLayout = findViewById(R.id.cons_name)
        nameLayout.visibility = View.INVISIBLE

        nameEdit = findViewById(R.id.nameEdit)

        nameEdit?.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                setCurrentText()
                val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        })


        val deleteButton: TextView = findViewById(R.id.buttonDelete)

        deleteButton.setOnClickListener { deleteCurrentShape() }

        val squareView: View = findViewById(R.id.cons_square)
        val squareView_another: View = findViewById(R.id.cons_square_another)
        val circleView: View = findViewById(R.id.cons_circle)
        val lineHView: View = findViewById(R.id.cons_line_h)
        val lineVView: View = findViewById(R.id.cons_line_v)
        val rectangleView : View = findViewById(R.id.cons_rectangle)

        rectangleView.setOnClickListener {
            rectangleView.setBackgroundResource(R.drawable.image_rectangle)
            squareView.setBackgroundResource(R.drawable.image_square_white)
            squareView_another.setBackgroundResource(R.drawable.image_square_white_another)
            circleView.setBackgroundResource(R.drawable.image_circle_white)
            subConsh.setBackgroundResource(R.drawable.image_line_h_white)
            subConsv.setBackgroundResource(R.drawable.image_line_v_white)
            addOne(typeRectangle)
        }


        squareView.setOnClickListener {
            rectangleView.setBackgroundResource(R.drawable.image_rectangle_white)
            squareView.setBackgroundResource(R.drawable.image_square)
            squareView_another.setBackgroundResource(R.drawable.image_square_white_another)
            circleView.setBackgroundResource(R.drawable.image_circle_white)
            subConsh.setBackgroundResource(R.drawable.image_line_h_white)
            subConsv.setBackgroundResource(R.drawable.image_line_v_white)
            addOne(typeSquare)
        }

        squareView_another.setOnClickListener {
            rectangleView.setBackgroundResource(R.drawable.image_rectangle_white)
            squareView.setBackgroundResource(R.drawable.image_square_white)
            squareView_another.setBackgroundResource(R.drawable.image_square_another)
            circleView.setBackgroundResource(R.drawable.image_circle_white)
            subConsh.setBackgroundResource(R.drawable.image_line_h_white)
            subConsv.setBackgroundResource(R.drawable.image_line_v_white)
            addOne(typeSquare_another)
        }

        circleView.setOnClickListener {
            rectangleView.setBackgroundResource(R.drawable.image_rectangle_white)
            squareView.setBackgroundResource(R.drawable.image_square_white)
            squareView_another.setBackgroundResource(R.drawable.image_square_white_another)
            circleView.setBackgroundResource(R.drawable.imagecircle)
            subConsh.setBackgroundResource(R.drawable.image_line_h_white)
            subConsv.setBackgroundResource(R.drawable.image_line_v_white)
            addOne(typeCircle)
        }

        lineHView.setOnClickListener {
            rectangleView.setBackgroundResource(R.drawable.image_rectangle_white)
            squareView.setBackgroundResource(R.drawable.image_square_white)
            squareView_another.setBackgroundResource(R.drawable.image_square_white_another)
            circleView.setBackgroundResource(R.drawable.image_circle_white)
            subConsh.setBackgroundResource(R.drawable.image_line_h)
            subConsv.setBackgroundResource(R.drawable.image_line_v_white)
            addOne(typeLineH)
        }

        lineVView.setOnClickListener {
            rectangleView.setBackgroundResource(R.drawable.image_rectangle_white)
            squareView.setBackgroundResource(R.drawable.image_square_white)
            squareView_another.setBackgroundResource(R.drawable.image_square_white_another)
            circleView.setBackgroundResource(R.drawable.image_circle_white)
            subConsh.setBackgroundResource(R.drawable.image_line_h_white)
            subConsv.setBackgroundResource(R.drawable.image_line_v)
            addOne(typeLineV)
        }
    }

    private var seekBarChangeListener: SeekBar.OnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

            if (currentView != null) {
                val params = currentView!!.layoutParams as ConstraintLayout.LayoutParams
                if (params.width >= minValue) params.width = progress + minValue
                if (params.height >= minValue) params.height = progress + minValue
                currentView!!.layoutParams = params
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }

    @SuppressLint("InflateParams")
    private fun alert() {
        // Inflate the dialog with custom view

        seekBar.visibility = View.VISIBLE
        nameLayout.visibility = View.VISIBLE
        nameEdit!!.setText("")

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.floor_dialog, null)
        // AlertDialogBuilder
        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)
            .setTitle("Add Floor Plan Description")
        val mAlertDialog = mBuilder.show()

        mAlertDialog.addFloorPlanBtn.setOnClickListener {

            val editText = mAlertDialog.findViewById<EditText>(R.id.floorPlanNameET)
            val floorPlanNameStr = editText.text.toString()

            if (floorPlanNameStr.isNullOrEmpty()) {
                MyToastUtil.showWarning(applicationContext, "Feel Floor Plan Description")
                return@setOnClickListener
            }
            mAlertDialog.dismiss()

            if (tagString.isNullOrEmpty()) {
                MyToastUtil.showWarning(applicationContext, "Select anyone")
                return@setOnClickListener
            }

            newFloorPlan = FloorPlan.emptyFloorPlan()
            newFloorPlan.name = floorPlanNameStr
            newFloorPlan.tags = tagString!!
            newFloorPlan.image = fileURL
            newFloorPlan.thumbnail = fileURL

            val errorCallback: (String) -> Unit = { message ->
                this.runOnUiThread {
                    MyToastUtil.showWarning(applicationContext, message)
                }
            }
            val successCallback: (FloorPlan) -> Unit = {floorPlan ->
                setResult(Activity.RESULT_OK)
                finish()
            }
            APIManager.share.postFloorPlan(newFloorPlan, successCallback, errorCallback)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun saveLayout() {
        nameLayout.visibility = View.INVISIBLE
        seekBar.visibility = View.INVISIBLE
        cons_bottom.visibility = View.INVISIBLE
        cC1.visibility = View.INVISIBLE

        for (i in 1..tagCount!!) {
            if(mDrawConstraintLayout.getViewById(i + startID - 1) != null) {
                val textView = mDrawConstraintLayout.getViewById(i + startID - 1) as TextView
                var tempStr: String
                if (textView.visibility == View.VISIBLE) {
                    tempStr = if (i == tagCount) {
                        textView.text.toString()
                    } else {
                        textView.text.toString() + ","
                    }
                    tagString += tempStr
                }
            }

        }

        val bitmap = Bitmap.createBitmap(mDrawConstraintLayout.width, mDrawConstraintLayout.height, Bitmap.Config.ARGB_8888)
        val paint : Paint? = null
        val filter = PorterDuffColorFilter(ContextCompat.getColor(this, R.color.colorPrimary), PorterDuff.Mode.SRC_IN)
        paint?.colorFilter = filter
        val canvas = Canvas(bitmap)
        val left = 0
        val top = 0
        canvas.drawBitmap(bitmap, left.toFloat(), top.toFloat(), paint)
        mDrawConstraintLayout.draw(canvas)

        if (bitmap != null) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            savedFilename = "$timeStamp.jpg"
            saveImage(bitmap, savedFilename!!)
        }

        filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        uploadImage(filePath!!, savedFilename!!)

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        encoderStr = Base64.encodeToString(byteArray, Base64.DEFAULT)

        nameLayout.visibility = View.VISIBLE
        seekBar.visibility = View.VISIBLE
        cons_bottom.visibility = View.VISIBLE
        cC1.visibility = View.VISIBLE
    }

    @SuppressLint("ResourceAsColor", "ClickableViewAccessibility", "SetTextI18n")
    private fun addOne(shapeType: Int) {
        tagCount = tagCount?.plus(1)
        val textView = TextView(this)
        textView.id = View.generateViewId()
        if(startID == -1) {
            startID = textView.id
        }
        Log.d(TAG, "id -> " + textView.id)
        //textView.setHintTextColor(R.color.colorText2)

        val set = ConstraintSet()
        mDrawConstraintLayout.addView(textView)
        set.clone(mDrawConstraintLayout)
        set.connect(textView.id, ConstraintSet.BOTTOM, mDrawConstraintLayout.id, ConstraintSet.BOTTOM, 0)
        set.connect(textView.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0)
        set.connect(textView.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0)
        set.applyTo(mDrawConstraintLayout)

        val params = textView.layoutParams as ConstraintLayout.LayoutParams
        params.height = resources.getDimensionPixelSize(R.dimen.defaultHeight)
        params.width = resources.getDimensionPixelSize(R.dimen.defaultWidth)

        when (shapeType) {
            typeSquare -> {
                textView.background = resources.getDrawable(R.drawable.image_square)
                //params.width = resources.getDimensionPixelSize(R.dimen.defaultSelectionRectange)
                textView.text = "Add Description"
            }
            typeSquare_another -> {
                textView.background = resources.getDrawable(R.drawable.image_square_another)
                //params.height = resources.getDimensionPixelSize(R.dimen.defaultSelectionRectange)
                textView.text = "Add Description"
            }
            typeCircle -> {
                textView.background = resources.getDrawable(R.drawable.imagecircle)
                textView.text = "Add Description"
            }
            typeLineH -> {
                textView.background = resources.getDrawable(R.drawable.image_line_h)
                params.height = resources.getDimensionPixelSize(R.dimen.defaultSelectionLine)
            }
            typeLineV -> {
                textView.background = resources.getDrawable(R.drawable.image_line_v)
                params.width = resources.getDimensionPixelSize(R.dimen.defaultSelectionLine)
            }
            typeRectangle -> {
                textView.background = resources.getDrawable(R.drawable.image_rectangle)
                textView.text = "Add Description"
            }
            else -> {
            }
        }

        textView.layoutParams = params
        textView.gravity = Gravity.CENTER
        //textView.setTextColor(R.color.colorWhite)
        textView.setTextColor(Color.WHITE)

        textView.setOnTouchListener(MyTouchListener())
        mDrawConstraintLayout.setOnDragListener(MyDragListener())

        setCurrentView(textView)

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

    @SuppressLint("ResourceAsColor")
    private fun setCurrentText() {
        if (currentView != null) {
            val params = currentView!!.layoutParams as ConstraintLayout.LayoutParams
            val minVal = if (params.width < params.height) params.width else params.height
            if (minVal < minValue) return

            val name: String
            if (nameEdit!!.text.toString().isNotEmpty()) {
                name = nameEdit!!.text.toString()
                currentView!!.text = name
                //currentView!!.setTextColor(R.color.colorWhite)
            }
        }
    }

    private fun deleteCurrentShape() {
        //tagCount = tagCount?.minus(1)
        currentView?.visibility = View.GONE
        currentView = null
        seekBar.visibility = View.GONE
        nameLayout.visibility = View.GONE
    }

    @SuppressLint("ResourceAsColor")
    internal fun setCurrentView(view: View) {
        currentView = view as TextView
        //currentView!!.setTextColor(R.color.colorWhite)
        val params = currentView!!.layoutParams as ConstraintLayout.LayoutParams
        val maxVal = if (params.width > params.height) params.width else params.height
        seekBar.visibility = View.VISIBLE
        seekBar.progress = maxVal - minValue

        nameLayout.visibility = View.VISIBLE
        nameEdit!!.setText("")
        setCurrentText()
    }

    /***
     * This method is used to upload the file to S3 by using TransferUtility class
     * @param
     */
    private fun uploadImage(filePath: File, filename: String) {
        val transferUtility = TransferUtility(s3Clients, applicationContext)

        val file = File(filePath, filename)

        val observer = transferUtility.upload(
            "wizt/labels",
            filename, /* The bucket to upload to */
            file, /* file name for uploaded object */
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
                    fileURL = s3Clients?.getResourceUrl("wizt/labels", filename).toString()
                    file.delete()
                    closeWaitDialog()
                    alert()
                    //labelImageURL.add(fileURL)

                    //file.delete()
                } else if (TransferState.FAILED == state) {
                    file.delete()
                    MyToastUtil.showWarning(applicationContext, "Upload Failed!")
                    closeWaitDialog()
                    //file.delete()
                }
            }

            override fun onError(id: Int, ex: java.lang.Exception?) {
                file.delete()
                closeWaitDialog()
                ex?.printStackTrace()
            }
        })
    }

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

                setCurrentView(view)

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
