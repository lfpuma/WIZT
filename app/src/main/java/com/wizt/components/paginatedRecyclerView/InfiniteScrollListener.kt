package com.wizt.components.paginatedRecyclerView

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.wizt.components.paginatedRecyclerView.interfaces.OnLoadMoreListener


class InfiniteScrollListener(
    private val linearLayoutManager: LinearLayoutManager,
    private val listener: OnLoadMoreListener?
) : RecyclerView.OnScrollListener() {

    companion object {
        private const val VISIBLE_THRESHOLD = 2
    }

    private var loading: Boolean = false // LOAD MORE Progress dialog
    private var pauseListening = false

    private var END_OF_FEED_ADDED = false
    private val NUM_LOAD_ITEMS = 10

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (dx == 0 && dy == 0)
            return
        val totalItemCount = linearLayoutManager.itemCount
        val lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition()
        if (!loading && totalItemCount <= lastVisibleItem + VISIBLE_THRESHOLD && totalItemCount != 0 && !END_OF_FEED_ADDED && !pauseListening) {
            listener?.onLoadMore()
            loading = true
        }
    }

    fun setLoaded() {
        loading = false
    }

    fun addEndOfRequests() {
        this.END_OF_FEED_ADDED = true
    }

    fun pauseScrollListener(pauseListening: Boolean) {
        this.pauseListening = pauseListening
    }
}