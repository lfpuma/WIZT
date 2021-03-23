package matteocrippa.it.karamba

import android.graphics.Bitmap
import android.view.View


fun View.toBitmap(): Bitmap {
    this.isDrawingCacheEnabled = true
    this.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
    this.layout(0, 0, this.measuredWidth, this.measuredHeight)
    this.buildDrawingCache(true)

    return this.drawingCache
}