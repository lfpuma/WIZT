package com.wizt.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.wizt.activities.CreateLabelActivity
import com.wizt.common.base.BaseFragment
import com.wizt.common.constants.Constants
import com.wizt.R
import com.wizt.models.Global
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_new_label.view.*


class NewLabelFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() =
            NewLabelFragment().apply {}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_new_label, container, false)
        view.title.text = resources.getString(R.string.create_new_label)
        addAds(view)
        super.bindMenuAction(view)
        super.bindNotificationViewAction(view)
        view.notificationBtn.visibility = View.INVISIBLE

        view.newLabelBtn.setOnClickListener {
            val intent = Intent(context, CreateLabelActivity::class.java)
            intent.putExtra(Constants.EXTRA_CREATE_LABEL_ACTIVITY_TYPE, true)
            intent.putExtra(Constants.EXTRA_CREATE_FYBE_ACTIVITY_TYPE, false)
            startActivity(intent)
        }
        view.fybeBtn.setOnClickListener {
            val intent = Intent(context, CreateLabelActivity::class.java)
            intent.putExtra(Constants.EXTRA_CREATE_LABEL_ACTIVITY_TYPE, true)
            intent.putExtra(Constants.EXTRA_CREATE_FYBE_ACTIVITY_TYPE, true)
            startActivity(intent)
        }

        return view
    }

    fun addAds(view:View) {
        if(Global.isSubscription) {
            view.adsLayout.visibility = View.GONE
            return
        }
        view.adsLayout.visibility = View.VISIBLE
        val adRequest = AdRequest.Builder().build()
        view.adsLayout.adView.loadAd(adRequest)
    }
}
