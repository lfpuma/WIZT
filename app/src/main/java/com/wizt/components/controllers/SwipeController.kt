package com.wizt.components.controllers

import android.content.Context
import android.graphics.*
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.helper.ItemTouchHelper.*
import android.view.MotionEvent
import android.view.View
import com.wizt.R
import android.R.attr.bottom
import android.R.attr.right
import android.R.attr.top
import android.R.attr.left



internal enum class ButtonsState {
    GONE,
    LEFT_VISIBLE,
    RIGHT_VISIBLE
}

internal class SwipeController(context: Context?, buttons: SwipeControllerActions) : ItemTouchHelper.Callback() {

    companion object {

        //private val buttonWidth = 200f
    }

    private var buttonWidth = 300f
    private var insidePadding = 20f

    private var swipeBack = false

    private var buttonShowedState = ButtonsState.GONE

    private var buttonInstance: RectF? = null

    private var currentItemViewHolder: RecyclerView.ViewHolder? = null

    private var buttonsActions: SwipeControllerActions? = null
    private var context: Context? = null

    init {
        this.buttonsActions = buttons
        this.context = context
        if(context != null) {
            this.buttonWidth = context.resources.getDimension(R.dimen.request_item_row_height)
            this.insidePadding = context.resources.getDimension(R.dimen.request_item_row_padding)
        }
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        //return ItemTouchHelper.Callback.makeMovementFlags(0, LEFT or RIGHT)
        return ItemTouchHelper.Callback.makeMovementFlags(0, LEFT)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

    }

    override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
        if (swipeBack) {
            swipeBack = buttonShowedState != ButtonsState.GONE
            return 0
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        var dX = dX
        if (actionState == ACTION_STATE_SWIPE) {
            if (buttonShowedState != ButtonsState.GONE) {
                if (buttonShowedState == ButtonsState.LEFT_VISIBLE) dX = Math.max(dX, buttonWidth)
                if (buttonShowedState == ButtonsState.RIGHT_VISIBLE) dX = Math.min(dX, -buttonWidth)
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            } else {
                setTouchListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        if (buttonShowedState == ButtonsState.GONE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
        currentItemViewHolder = viewHolder
    }

    private fun setTouchListener(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        recyclerView.setOnTouchListener { v, event ->
            swipeBack = event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
            if (swipeBack) {
                if (dX < -buttonWidth)
                    buttonShowedState = ButtonsState.RIGHT_VISIBLE
                else if (dX > buttonWidth) buttonShowedState = ButtonsState.LEFT_VISIBLE

                if (buttonShowedState != ButtonsState.GONE) {
                    setTouchDownListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    setItemsClickable(recyclerView, false)
                }
            }
            false
        }
    }

    private fun setTouchDownListener(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        recyclerView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                setTouchUpListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
            false
        }
    }

    private fun setTouchUpListener(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        recyclerView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                super@SwipeController.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, isCurrentlyActive)
                recyclerView.setOnTouchListener { v, event -> false }
                setItemsClickable(recyclerView, true)
                swipeBack = false

                if (buttonsActions != null && buttonInstance != null && buttonInstance!!.contains(event.x, event.y)) {
                    if (buttonShowedState == ButtonsState.LEFT_VISIBLE) {
                        buttonsActions!!.onLeftClicked(viewHolder.adapterPosition)
                    } else if (buttonShowedState == ButtonsState.RIGHT_VISIBLE) {
                        buttonsActions!!.onRightClicked(viewHolder.adapterPosition)
                    }
                }
                buttonShowedState = ButtonsState.GONE
                currentItemViewHolder = null
            }
            false
        }
    }

    private fun setItemsClickable(recyclerView: RecyclerView, isClickable: Boolean) {
        for (i in 0 until recyclerView.childCount) {
            recyclerView.getChildAt(i).isClickable = isClickable
        }
    }

    private fun drawButtons(c: Canvas, viewHolder: RecyclerView.ViewHolder) {
        val buttonWidthWithoutPadding = buttonWidth - insidePadding * 2
        val corners = buttonWidthWithoutPadding / 2

        val itemView = viewHolder.itemView
        val p = Paint()

        val leftButton = RectF(
            itemView.left.toFloat(),
            itemView.top.toFloat() + insidePadding,
            itemView.left + buttonWidthWithoutPadding,
            itemView.bottom.toFloat() - insidePadding
        )
        p.color = Color.TRANSPARENT
        c.drawRoundRect(leftButton, corners, corners, p)
        drawText("", c, leftButton, p)

        val rightButton = RectF(
            itemView.right - buttonWidthWithoutPadding,
            itemView.top.toFloat() + insidePadding,
            itemView.right.toFloat(),
            itemView.bottom.toFloat() - insidePadding
        )
        p.color = Color.YELLOW
        if(context!= null) p.color = context!!.getColor(R.color.colorText1)
        c.drawRoundRect(rightButton, corners, corners, p)
        drawIcon(c, rightButton, p, buttonWidthWithoutPadding / 4)

        buttonInstance = null
        if (buttonShowedState == ButtonsState.LEFT_VISIBLE) {
            buttonInstance = leftButton
        } else if (buttonShowedState == ButtonsState.RIGHT_VISIBLE) {
            buttonInstance = rightButton
        }
    }

    private fun drawText(text: String, c: Canvas, button: RectF, p: Paint) {
        val textSize = 60f
        p.color = Color.WHITE
        p.isAntiAlias = true
        p.textSize = textSize

        val textWidth = p.measureText(text)
        c.drawText(text, button.centerX() - textWidth / 2, button.centerY() + textSize / 2, p)
    }

    private fun drawIcon(c: Canvas, button: RectF, p: Paint, padding: Float) {

        if(context == null ) return

        val icon = context!!.getDrawable(R.drawable.ic_delete_new)
        icon.setBounds(button.centerX().toInt() - padding.toInt(),
         button.centerY().toInt() - padding.toInt(),
        button.centerX().toInt() + padding.toInt(),
      button.centerY().toInt() + padding.toInt())
        icon.draw(c)

    }

    fun onDraw(c: Canvas) {
        if (currentItemViewHolder != null) {
            drawButtons(c, currentItemViewHolder!!)
        }
    }
}

