package com.wizt.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.cunoraz.gifview.library.GifView
import com.wizt.common.base.BaseFragment

import com.wizt.R
import com.wizt.activities.*
import com.wizt.common.constants.Constants
import com.wizt.utils.MyToastUtil
import kotlinx.android.synthetic.main.activity_creat_trainobject.view.*
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_scan.view.*


class ScanFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() =
            ScanFragment().apply {}
    }

    //private lateinit var animationGif: GifView
    private lateinit var myView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_scan, container, false)
        myView = view
        view.title.text = resources.getString(R.string.train_fybe)

        // Bind Menu and Push Notification Actions
        bindMenuAction(view)
        bindNotificationViewAction(view)

        // Gif animation
//        animationGif = view.findViewById(R.id.animationGif)
//        animationGif.visibility = View.VISIBLE
//        animationGif.play()
        val animationGif = view.findViewById<ImageView>(R.id.animationGif)
        Glide.with(context!!).load(R.drawable.animation_fybe).into(animationGif)

        view.helpBtn.setOnClickListener {
            val intent = Intent(context, FybeTutorialActivity::class.java)
            startActivity(intent)
        }

        view.startBtn.setOnClickListener {
            startTrain()
        }

        view.trainedBtn.setOnClickListener {
            trainObjList()
        }

        return view
    }

    fun startTrain() {
        val intent = Intent(context, CreateTrainObjectActivity::class.java)
        intent.putExtra(Constants.EXTRA_CREATE_TRAIN_ACTIVITY_TYPE, true)
        intent.putExtra(Constants.EXTRA_CREATE_TRAIN_ACTIVITY_TITLE, myView.etObjName.text.toString())
        startActivity(intent)
    }

    fun trainObjList() {

        val intent = Intent(context, ObjectTrainedActivity::class.java)
        startActivity(intent)
    }


}
