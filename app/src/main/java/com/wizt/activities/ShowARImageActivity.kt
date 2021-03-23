package com.wizt.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.crashlytics.android.Crashlytics
import com.wizt.R
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_show_ar_image.*

class ShowARImageActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_show_ar_image)
//
//        val imageUrl = intent.getStringExtra(Constants.EXTRA_AR_MARKER_URL)
//        Glide.with(applicationContext).load(imageUrl).into(this.arMarkerImageIV)

        this.backBtn.setOnClickListener {
            finish()
        }
    }
}