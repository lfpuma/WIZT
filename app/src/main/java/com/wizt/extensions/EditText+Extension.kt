package com.wizt.extensions

import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import com.wizt.R

fun EditText.setupClearButtonWithAction() {

    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            val clearIcon = if (editable?.isNotEmpty() == true) R.drawable.ic_cancel_edit else 0
            setCompoundDrawablesWithIntrinsicBounds(0, 0, clearIcon, 0)
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
    })

    onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
        } else {
            this.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }

    setOnTouchListener(View.OnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            if (event.rawX >= (this.right - this.compoundPaddingRight)) {
                this.setText("")
                this.requestFocus()
                return@OnTouchListener true
            }
        }
        return@OnTouchListener false
    })
}