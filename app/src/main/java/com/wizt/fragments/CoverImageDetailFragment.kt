package com.wizt.fragments

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.wizt.R
import com.wizt.activities.MainActivity
import com.wizt.components.myDialog.MyDialog
import com.wizt.extensions.ImageHelper
import com.wizt.models.Image

class CoverImageDetailFragment : Fragment() {

    companion object {

        const val IMAGEDATAID = "imagedata_id"
        const val IMAGEDATACOVER = "imagedata_iscover"
        const val IMAGEDATAURL = "imagedata_url"
        const val IMAGETHUMURL = "imagethum_url"
        const val IMAGEDATATOTALAMOUNT = "iamgedata_totalamount"
        const val IMAGEDATACURRENTPOS = "imagedata_cuspos"

        const val TAG = "WIZT:CoverImageDetailFragment"
        const val roundedCon = 25.0f
        const val circleSize = 16

        var animFragment: Fragment? = null

        @JvmStatic
        fun newInstance(image: Image, totalAmount: Int, currentPos: Int, animFrag: Fragment) =
            CoverImageDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(IMAGEDATAID,image.id)
                    putBoolean(IMAGEDATACOVER,image.is_cover)
                    putString(IMAGEDATAURL,image.url)
                    putString(IMAGETHUMURL,image.thumbnail)
                    putInt(IMAGEDATATOTALAMOUNT,totalAmount)
                    putInt(IMAGEDATACURRENTPOS,currentPos)

                    animFragment = animFrag

                }
            }
    }

    private lateinit var imageView : ImageView
    private lateinit var circleLinear : LinearLayout

    @SuppressLint("LongLogTag")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_coverimage_detail, container, false)

        val imageID = arguments!!.getString(IMAGEDATAID)
        val isCover = arguments!!.getBoolean(IMAGEDATACOVER)
        val imageURL = arguments!!.getString(IMAGEDATAURL)
        val imageThumURL = arguments!!.getString(IMAGETHUMURL)
        val totalSize = arguments!!.getInt(IMAGEDATATOTALAMOUNT)
        val currentPos = arguments!!.getInt(IMAGEDATACURRENTPOS)

        imageView = view.findViewById(R.id.imageview)
        circleLinear = view.findViewById(R.id.circleLinear)

        preSetup(totalSize, currentPos)

//        Glide.with(context!!)
//            .asBitmap()
//            .load(imageThumURL)
//            .into(object : CustomTarget<Bitmap>(){
//                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                    val sqRes = ImageHelper.getCroppedImage(resource)
//                    val round = RoundedBitmapDrawableFactory.create(context!!.resources,sqRes)
//                    round.cornerRadius = roundedCon
//                    imageView.setImageDrawable(round)
//                }
//                override fun onLoadCleared(placeholder: Drawable?) {
//                }
//            })

        Glide.with(context!!)
            .asBitmap()
            .load(imageThumURL)
                .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    imageView.setImageBitmap(resource)
                    if(currentPos == 0) animFragment?.startPostponedEnterTransition()
                }
                override fun onLoadCleared(placeholder: Drawable?) {

                }
            })

        imageView.setOnClickListener {

            //MyDialog.showCustomDialog(context!!,imageURL,"",-2)

        }

        return view
    }

    fun preSetup(totalAmount: Int, currentPos: Int) {

        if(totalAmount <= 1) {
            return
        }

        for (i in 0 .. totalAmount - 1) {

            val imageView = ImageView(activity)
            imageView.layoutParams = LinearLayout.LayoutParams(circleSize, circleSize)

            val param = imageView.layoutParams as LinearLayout.LayoutParams
            param.setMargins(10,10,10,10)
            imageView.layoutParams = param

            var imgResId = R.drawable.circle_detail
            if (i == currentPos) {
                imgResId = R.drawable.circlesel_detail
            }
            imageView.setImageResource(imgResId)

            circleLinear.addView(imageView)

        }
    }


}