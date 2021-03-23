package com.wizt.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.provider.SyncStateContract
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.TypedValue
import com.crashlytics.android.Crashlytics
import com.wizt.R
import com.wizt.common.constants.Constants
import com.wizt.extensions.ImageHelper
import io.fabric.sdk.android.Fabric
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.back
import io.fotoapparat.selector.front
import io.fotoapparat.selector.off
import io.fotoapparat.selector.torch
import io.fotoapparat.view.CameraView
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CameraActivity : AppCompatActivity() {

    private var fotoapparat: Fotoapparat? = null
    private var fotoapparatState: FotoapparatState? = null
    private var cameraState: CameraState? = null
    private var flashState: FlashState? = null

    private var filename : String? = null

    private val permissions = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_camera)
        val cameraName = intent.getStringExtra(Constants.EXTRA_CAMERA_NAME)
        if(cameraName != null && !cameraName.isEmpty()) {
            cameraTitle.setText(cameraName)
            cameraTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX,getResources().getDimension(R.dimen.result_font))
        }
        backBtn.setOnClickListener { finish() }
        createFotoapparat()
        cameraState = CameraState.BACK
        flashState = FlashState.OFF
        fotoapparatState = FotoapparatState.OFF

        fab_camera.setOnClickListener {
            takePhoto()
        }

        fab_switch_camera.setOnClickListener {
            switchCamera()
        }

        fab_flash.setOnClickListener {
            changeFlashState()
        }
    }

    private fun changeFlashState() {
        fotoapparat?.updateConfiguration(
            CameraConfiguration(
                flashMode = if (flashState == FlashState.TORCH) off() else torch()
            )
        )

        flashState = if (flashState == FlashState.TORCH) FlashState.OFF
        else FlashState.TORCH
    }

    private fun switchCamera() {
        fotoapparat?.switchTo(
            lensPosition = if (cameraState == CameraState.BACK) front() else back(),
            cameraConfiguration = CameraConfiguration()
        )

        cameraState = if (cameraState == CameraState.BACK) CameraState.FRONT
        else CameraState.BACK
    }

    @SuppressLint("SimpleDateFormat")
    private fun takePhoto() {
        if (hasNoPermissions()) {
            requestPermission()
        } else {
            val photoResult = fotoapparat?.autoFocus()?.takePicture()

            photoResult?.toBitmap()?.whenAvailable { bitmapPhoto ->
                val bitmap = bitmapPhoto?.bitmap
                if (bitmap != null) {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                    filename = "$timeStamp.jpg"
                    saveImage(bitmap, filename!!)
                }

                val intent = Intent()
                intent.putExtra("ImageName", filename)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun saveImage(bitmap: Bitmap, filename: String) {

        makeVibrate(this)

        val outStream: FileOutputStream
        try {
            var file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename)
            if(!file.exists()) {
                val dcimFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val isSuccess = dcimFolder.mkdir()
                file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename)
            }
            outStream = FileOutputStream(file)
            var resBitmap = ImageHelper.getResizedBitmap(bitmap,Constants.IMAGERESIZE)
            resBitmap = ImageHelper.modifyOrientation(resBitmap)
            resBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.flush()
            outStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hasNoPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 0)
    }

    private fun createFotoapparat() {
        val cameraView = findViewById<CameraView>(R.id.camera_view)

        fotoapparat = Fotoapparat(
            context = this,
            view = cameraView,
            scaleType = ScaleType.CenterCrop,
            lensPosition = back(),
            logger = loggers(
                logcat()
            ),
            cameraErrorCallback = { error ->
                println("Recode errors: $error")
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()

        if (hasNoPermissions()) {
            requestPermission()
        } else {
            fotoapparat?.start()
            fotoapparatState = FotoapparatState.ON
        }
    }

    override fun onStop() {
        super.onStop()
        fotoapparat?.stop()
        fotoapparatState = FotoapparatState.OFF
    }

    override fun onResume() {
        super.onResume()
        if (!hasNoPermissions() && fotoapparatState == FotoapparatState.OFF) {

            val intent = Intent()
            intent.putExtra("ImageName", Constants.REQUIRECALL)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }
}

fun makeVibrate(context: Context) {
    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        //deprecated in API 26
        v.vibrate(500)
    }
}

enum class CameraState {
    FRONT, BACK
}

enum class FlashState {
    TORCH, OFF
}

enum class FotoapparatState {
    ON, OFF
}
