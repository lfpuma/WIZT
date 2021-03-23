package com.wizt.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.wizt.R

class CreateFloorPlan : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_creat_floor_plan, container, false)
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            DrawFloorPlan().apply {}
    }
}
