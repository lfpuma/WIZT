package matteocrippa.it.karamba

import android.os.Build


inline fun support(version: Int, code: () -> Unit) {
    if(Build.VERSION.SDK_INT > version) {
        code.invoke()
    }
}

inline fun supportKitkat(code: () -> Unit) {
    support(18) {
        code.invoke()
    }
}

inline fun supportLollipop(code: () -> Unit) {
    support(21) {
        code.invoke()
    }
}