package com.wizt.activities.animationfrag

import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wizt.R
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION.SDK_INT
import android.support.annotation.Nullable
import android.transition.TransitionInflater
import kotlinx.android.synthetic.main.fragment_gallery.*


class GalleryFragment : Fragment() {

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

}