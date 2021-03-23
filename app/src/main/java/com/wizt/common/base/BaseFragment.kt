package com.wizt.common.base

import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.wizt.activities.MainActivity
import kotlinx.android.synthetic.main.app_bar.view.*
import android.support.constraint.ConstraintSet
import android.support.v4.widget.SwipeRefreshLayout
import com.wizt.fragments.NotificationFragment
import com.wizt.utils.MyToastUtil


open class BaseFragment : Fragment() {

    /**
     * Bind menu button to open the side menu
     */
    fun bindMenuAction(view: View) {
        view.menuBtn.setOnClickListener {
            (activity as? MainActivity)?.openSideMenu()
        }
    }

    fun bindNotificationViewAction(view: View) {
        view.notificationBtn.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(NotificationFragment.newInstance())
        }
    }

    /**
     * Display the image that show no data
     */
    fun displayEmptyDataImage(container: ViewGroup, resId: Int) {
        var imageView = ImageView(context)
        imageView.setImageResource(resId)

        if (container is ConstraintLayout) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(container)
            constraintSet.connect(imageView.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0)
            constraintSet.connect(imageView.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0)
            constraintSet.connect(imageView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
            constraintSet.connect(imageView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
            constraintSet.applyTo(container)
        }

        container.addView(imageView)
    }

    /**
     * API Request Error Handler
     */
    val errorCallback: (String) -> Unit = { message ->
        activity?.runOnUiThread {
            MyToastUtil.showWarning(context!!, message)
        }
    }

    /**
     * Setting SwipeRefreshLayout's Color
     */
    fun setSwipeRefreshLayoutColor(swipeContainer: SwipeRefreshLayout) {
        swipeContainer.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }
}