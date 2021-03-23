package com.wizt.utils

import android.content.Context
import android.widget.Toast
import com.wizt.components.myToast.Mytoast
import com.wizt.R

class MyToastUtil {

    companion object {
        fun showWarning(context: Context, text: String) {
            Mytoast().make(
                context,
                text,
                Toast.LENGTH_SHORT,
                "#f44336",
                "#ffffff",
                R.drawable.ic_warning_black_24dp
            ).show()
        }

        fun showNotice(context: Context, text: String) {
            Mytoast().make(
                context,
                text,
                Toast.LENGTH_SHORT,
                "#f44336",
                "#ffffff",
                R.drawable.ic_add_circle_outline_black_24dp
            ).show()
        }

        fun showMessage(context: Context, text: String) {
            Mytoast().make(
                context,
                text,
                Toast.LENGTH_SHORT,
                "#1480ff",
                "#ffffff",
                R.drawable.ic_add_circle_outline_black_24dp
            ).show()
        }
    }
}