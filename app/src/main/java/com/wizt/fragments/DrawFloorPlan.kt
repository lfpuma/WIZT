package com.wizt.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.wizt.R

class DrawFloorPlan : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //        view.title.text = resources.getString(R.string.all_floor_plans)
        return inflater.inflate(R.layout.fragment_draw_floor_plan, container, false)
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            DrawFloorPlan().apply {}
    }
}
