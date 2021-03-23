package com.wizt.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.bumptech.glide.Glide
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.wizt.activities.CameraActivity
import com.wizt.activities.MainActivity
import com.wizt.common.aws.AppHelper
import com.wizt.activities.SubscribeActivity
import com.wizt.common.base.BaseFragment
import com.wizt.common.constants.Constants
import com.wizt.common.http.APIManager
import com.wizt.models.Global
import com.wizt.models.Profile

import com.wizt.R
import com.wizt.utils.DateTimeUtils
import com.wizt.activities.auth.LoginActivity
import com.wizt.common.base.BaseActivity
import com.wizt.extensions.ImageHelper
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_profile.view.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.android.synthetic.main.suggestion_fybe_dialog.view.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

@Suppress("DEPRECATION")
class ProfileFragment : BaseFragment() {

    companion object {

        const val TAG = "WIZT:ProfileFragment"

        @JvmStatic
        fun newInstance() = ProfileFragment().apply {
        }
    }

    private var user: CognitoUser? = null
    private lateinit var profileImage: CircleImageView
    private lateinit var nameTV: TextView
    private lateinit var labelTV: TextView
    private lateinit var yourLabelTV: TextView
    private lateinit var friendsTV: TextView
    private lateinit var emailTV: TextView
    private lateinit var phoneTV: TextView
    private lateinit var memberDate: TextView
    private lateinit var myView : View
    private var userProfile : Profile? = Profile.emptyProfile()

    // s3
    private var credentialsProvider: CognitoCachingCredentialsProvider? = null
    private var s3Clients: AmazonS3Client? = null
    private lateinit var fileURL : String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadFirst()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        myView = view
        view.title.text = resources.getString(R.string.profile)
        view.notificationBtn.setImageDrawable(null)
        super.bindMenuAction(view)

        // Initialize the AWS Credential
        credentialsProvider = CognitoCachingCredentialsProvider(
            context,
            "ap-southeast-1:992a1271-46fb-4b01-9fc7-06cd8d5cd779", // Identity Pool ID
            Regions.AP_SOUTHEAST_1
        ) // Region
        // Create a S3 clients
        s3Clients = AmazonS3Client(credentialsProvider)
        // Set the region of your S3 bucket
        s3Clients?.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))

        profileImage = view.findViewById(R.id.profileImage)
        nameTV = view.findViewById(R.id.nameTV)
        labelTV = view.findViewById(R.id.labelTV)
        yourLabelTV = view.findViewById(R.id.yourLabelTV)
        friendsTV = view.findViewById(R.id.friendsTV)
        emailTV = view.findViewById(R.id.emailTV)
        phoneTV = view.findViewById(R.id.phoneTV)
        memberDate = view.findViewById(R.id.memberDate)


        view.logoutBtn.setOnClickListener {
            user?.signOut()
            PreferenceUtils.saveBoolean(Constants.PREF_IS_LOGIN, false)
            //PreferenceUtils.saveBoolean(Constants.PREF_TUTORIAL, false)
            PreferenceUtils.saveBoolean(Constants.PREF_HOME_IS_FOCUS, false)

            FacebookSdk.sdkInitialize(context)

            LoginManager.getInstance().logOut()

            APIManager.share.logout()

            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }
        view.notificationBtn.setOnClickListener {
        }
        view.buyBtn.setOnClickListener {
            val intent = Intent(context, SubscribeActivity::class.java)
            startActivity(intent)
        }
        view.cancelPayment.setOnClickListener {

            lateinit var dialog: AlertDialog
            val builder = AlertDialog.Builder(context)
            builder.setTitle("")
            builder.setMessage("Are you sure?")
            val dialogClickListener = DialogInterface.OnClickListener{ _, which ->
                when(which){
                    DialogInterface.BUTTON_POSITIVE -> downgradeToFreeplan()
                    //DialogInterface.BUTTON_NEGATIVE ->
                }
            }

            builder.setPositiveButton(Html.fromHtml("<font color='#000000'>YES</font>"),dialogClickListener)
            builder.setNegativeButton(Html.fromHtml("<font color='#000000'>No</font>"),dialogClickListener)
            dialog = builder.create()

            dialog.show()
        }

        profileImage.setOnClickListener { showPictureDialog() }

        user = AppHelper.pool?.getUser(PreferenceUtils.getString(Constants.PREF_USER_EMAIL))

        return view
    }

    fun downgradeToFreeplan() {

        val successCallback: (Int) -> Unit = { freePlanID ->
            Log.d(TAG,"FreePlanID -> " + freePlanID)

            if (freePlanID != -1) {
                val successCallback: () -> Unit = {
                    Log.d(TAG,"cancelSubscription -> " + "Success")
                    Global.isSubscription = false
                    activity?.runOnUiThread {
                        MyToastUtil.showMessage(context!!, "Subscription Cancel Successfully!")
                        onResume()
                        //DisplayCancelPlanButton()
                    }
                }

                val errorCallback: (String) -> Unit = { message ->
                    Log.d(TAG,"cancelSubscription -> " + "Error")
                    Log.d(TAG,"cancelSubscription -> " + message)
                    activity?.runOnUiThread {
                        MyToastUtil.showWarning(context!!, "Subscription Cancel Failed")
                    }
                }

                APIManager.share.subscribe("noNeed", freePlanID, successCallback, errorCallback)
            }
        }

        val errorCallback: (String) -> Unit = { message ->
            Log.d(TAG,"FreePlanID_Error -> " + message)
        }

        APIManager.share.getFreePlanID(successCallback, errorCallback)
    }

    override fun onResume() {
        super.onResume()

        setData()
        myProfile()
        DisplayCancelPlanButton()
    }

    fun DisplayCancelPlanButton() {
        if (Global.isSubscription) {
            myView.cancelPayment.visibility = View.VISIBLE
        }
        else {
            myView.cancelPayment.visibility = View.GONE
        }
    }

    fun setData() {

        if (Global.profile?.picture != null) {
            Glide.with(context!!).load(Global.profile?.picture).into(profileImage)
        }
        else {
            //profileImage.setImageDrawable(null)
        }
        //Glide.with(context!!).load(Global.profile?.picture).into(profileImage)
        nameTV.text = Global.profile?.name
        labelTV.text = Global.profile?.photo_in_use.toString()
        yourLabelTV.text = Global.profile?.photo_cnt.toString()
        friendsTV.text = Global.profile?.friends_count.toString()
        emailTV.text = Global.profile?.email
        phoneTV.text = Global.profile?.phone_number
//        if(Global.profile?.created_at != null) {
//            val date = SimpleDateFormat("yyyy-MM-dd").parse(Global.profile?.created_at)
//            memberDate.text = DateTimeUtils().formatDate(DateTimeUtils().DATE_FORMAT_dd_MM_yyyy, date)
//        }
//        else {
//            memberDate.text = ""
//        }


    }

    @SuppressLint("SimpleDateFormat")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Constants.ACTIVITY_PROFILE_GALLERY_OK) {
            if (data != null) {

                val contentURI = data.data
                val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                var savedFilename: String? = ""
                try {
                    var bitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, contentURI)
                    bitmap = ImageHelper.getResizedBitmap(bitmap,Constants.PHOTOIMAGERESIZE)
                    bitmap = ImageHelper.modifyOrientation_gallery(bitmap,contentURI,context!!)
                    //profileImage.setImageBitmap(bitmap)

                    if (bitmap != null) {
                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                        savedFilename = "$timeStamp.jpg"
                        saveImage(bitmap, savedFilename)
                    }

                    if (savedFilename != null) {
                        uploadImage(filePath, savedFilename)
                    }

                    //Global.profile?.picture = fileURL

                    //updatePictureRequest(fileURL)

                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        } else {
            val imageFileName = data?.getStringExtra("ImageName")

            if (Constants.REQUIRECALL.equals(imageFileName)) {

                val intent = Intent(context, CameraActivity::class.java)
                startActivityForResult(intent, Constants.ACTIVITY_PROFILE_CAMERA_OK)
                return

            }

            val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            try {

                if (imageFileName != null) {
                    val file = File(filePath, imageFileName)
                    var bitmap = BitmapFactory.decodeStream(FileInputStream(file))
                    bitmap = ImageHelper.getResizedBitmap(bitmap,Constants.PHOTOIMAGERESIZE)
                    profileImage.setImageBitmap(bitmap)
                    val fOut = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut)
                    fOut.flush()
                    fOut.close()
                    uploadImage(filePath, imageFileName)
                }

            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }



    override fun onStop() {
        super.onStop()
        PreferenceUtils.saveBoolean(Constants.PREF_HOME_IS_FOCUS, false)
    }

    private fun sendUpdatePictureRequest(pictureUrl: String){
        val successCallback: () -> Unit = {
            Global.profile!!.picture = pictureUrl
            PreferenceUtils.saveString(Constants.PROFILE_USER_PICTURE_AVATAR, pictureUrl)
            activity?.runOnUiThread {
                (activity as MainActivity).initSideMenu()
            }
        }
        val errorCallback: (String) -> Unit = {message ->

        }
        APIManager.share.updateProfilePicture(pictureUrl, successCallback, errorCallback)
    }

    private fun loadFirst() {
        // Get Profile
        val successCallback: (Profile) -> Unit = { profile: Profile ->

            activity?.runOnUiThread {
                userProfile = profile
            }
        }

        val errorCallback: (String) -> Unit = {
        }

        APIManager.share.getMyProfile(successCallback, errorCallback)
    }

    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(context)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
        pictureDialog.setItems(
            pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> choosePhotoFromGallery()
                1 -> takePhotoFromCamera()
            }
        }
        pictureDialog.show()
    }

    private fun takePhotoFromCamera() {
        val intent = Intent(context, CameraActivity::class.java)
        startActivityForResult(intent, Constants.ACTIVITY_PROFILE_CAMERA_OK)
    }

    private fun choosePhotoFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, Constants.ACTIVITY_PROFILE_GALLERY_OK)
    }

    /***
     * This method is used to upload the file to S3 by using TransferUtility class
     * @param
     */
    private fun uploadImage(filePath: File, filename: String) {
        val transferUtility = TransferUtility(s3Clients, context)

        val file = File(filePath, filename)

        val observer = transferUtility.upload(
            "wizt/profiles",
            filename, /* The bucket to upload to */
            file, /* file name for uploaded object */
            CannedAccessControlList.PublicRead
        )

        observer.setTransferListener(object : TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            }

            override fun onStateChanged(id: Int, state: TransferState?) {
                if (TransferState.COMPLETED == state) {
                    fileURL = s3Clients?.getResourceUrl("wizt/profiles", filename).toString()
                    Global.profile!!.picture = fileURL
                    PreferenceUtils.saveString(Constants.PROFILE_USER_PICTURE_AVATAR, fileURL)
                    sendUpdatePictureRequest(fileURL)
                    activity?.runOnUiThread {
                        setData()
                        (activity as MainActivity).initSideMenu()
                    }

                } else if (TransferState.FAILED == state) {

                    MyToastUtil.showWarning(context!!, "Upload Failed!")
                    //file.delete()
                }
            }

            override fun onError(id: Int, ex: java.lang.Exception?) {
                ex?.printStackTrace()
            }
        })
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

    private fun myProfile() {
        // Get Profile
        val successCallback: (Profile) -> Unit = { profile: Profile ->
            Global.profile = profile
            PreferenceUtils.saveString(Constants.PROFILE_USER_NAME, profile.name)
            PreferenceUtils.saveString(Constants.PROFILE_USER_EMAIL, profile.email)
            if (profile.picture != null) {
                PreferenceUtils.saveString(Constants.PROFILE_USER_PICTURE_AVATAR, profile.picture!!)
            }
            activity?.runOnUiThread {
                this.setData()
            }
        }

        val errorCallback: (String) -> Unit = { message ->

        }

        APIManager.share.getMyProfile(successCallback, errorCallback)
    }
}
