package com.wizt.extensions


import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.media.ExifInterface
import android.widget.Switch
import java.util.Collections.rotate
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileInputStream
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage
import android.provider.MediaStore




class ImageHelper {

    companion object {

        const val TAG = "WIZT:ImageHelper"

        @JvmStatic
        fun getSquredBitmap(srcBmp: Bitmap) : Bitmap {

            if (srcBmp.getWidth() >= srcBmp.getHeight()){

                return Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.getWidth()/2 - srcBmp.getHeight()/2,
                    0,
                    srcBmp.getHeight(),
                    srcBmp.getHeight()
                )

            }else{

                return Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight()/2 - srcBmp.getWidth()/2,
                    srcBmp.getWidth(),
                    srcBmp.getWidth()
                )
            }

        }

        @JvmStatic
        fun getCroppedImage(srcBmp: Bitmap) : Bitmap {

            if (srcBmp.getWidth() >= srcBmp.getHeight()){

                return srcBmp

            }else{

                return Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight()/4,
                    srcBmp.getWidth(),
                    srcBmp.height / 2
                )
            }
        }

        @JvmStatic
        fun getCroppedImage_homeItem(srcBmp: Bitmap) : Bitmap {

            if (srcBmp.getWidth() >= srcBmp.getHeight()){

                return srcBmp

            }else{

                return Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.height / 2 - srcBmp.width / 3,
                    srcBmp.getWidth(),
                    srcBmp.width / 3 * 2
                )
            }
        }

        @JvmStatic
        fun getCroppedImage_343(srcBmp: Bitmap) : Bitmap {

            if (srcBmp.getWidth() >= srcBmp.getHeight()){

                return srcBmp

            }else{

                return Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight()/5,
                    srcBmp.getWidth(),
                    srcBmp.height / 5 * 3
                )
            }
        }

        @JvmStatic
        fun getCroppedImageForNewLabel(srcBmp: Bitmap) : Bitmap {

            if (srcBmp.getWidth() >= srcBmp.getHeight()){

                return srcBmp

            }else{

                return Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight() / 2 - srcBmp.getWidth() * 3 / 8,
                    srcBmp.getWidth(),
                    srcBmp.getWidth() * 3 / 4
                )
            }

        }

        @JvmStatic
        fun getCroppedImage_Special(srcBmp: Bitmap) : Bitmap {

            if (srcBmp.getWidth() >= srcBmp.getHeight()){

                return srcBmp

            }else{

                return Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight() / 10,
                    srcBmp.getWidth(),
                    srcBmp.height / 10 * 8
                )
            }

        }

        @JvmStatic
        fun getResizedBitmap(image : Bitmap,maxSize : Int) : Bitmap {
            var width = image.width
            var height = image.height

            val bitmapRatio = width.toFloat() / height.toFloat()
            if (bitmapRatio > 1) {
                width = maxSize
                height = (width.toFloat() / bitmapRatio).toInt()
            } else {
                height = maxSize
                width = (height.toFloat() * bitmapRatio).toInt()
            }
            return Bitmap.createScaledBitmap(image, width, height, true)
        }

        @JvmStatic
        fun modifyOrientation(bitmap : Bitmap) : Bitmap {

            if (bitmap.width > bitmap.height) {
                return rotate(bitmap,90.0f)
            }
            else return bitmap
        }

        fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {

            val matrix = Matrix()
            matrix.postRotate(degrees)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
            val matrix = Matrix()
            matrix.preScale(if (horizontal) -1.0f else 1.0f, if (vertical) -1.0f else 1.0f)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        @JvmStatic
        fun modifyOrientation_gallery(bitmap : Bitmap, image_absolute_path_uri : Uri,context: Context) : Bitmap {

            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = context.getContentResolver().query(image_absolute_path_uri, filePathColumn, null, null, null)
            cursor.moveToFirst()

            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val image_absolute_path = cursor.getString(columnIndex)
            cursor.close()

            val ei = ExifInterface(image_absolute_path)
            val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 ->
                    return rotate(bitmap, 90.0f)

                ExifInterface.ORIENTATION_ROTATE_180 ->
                    return rotate(bitmap, 180.0f)

                ExifInterface.ORIENTATION_ROTATE_270->
                    return rotate(bitmap, 270.0f)

                ExifInterface.ORIENTATION_FLIP_HORIZONTAL->
                    return flip(bitmap, true, false)

                ExifInterface.ORIENTATION_FLIP_VERTICAL->
                    return flip(bitmap, false, true)

                else -> return bitmap
            }
        }

    }


}