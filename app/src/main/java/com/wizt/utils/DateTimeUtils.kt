package com.wizt.utils


import android.text.TextUtils
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class DateTimeUtils {

    companion object {
        const val TAG = "DateTimeUtils"
        const val CONVERTPATTEN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
    }
    /**
     * Format                         time
     *
     * "yyyy.MM.dd G 'at' HH:mm:ss z"	   2001.07.04 AD at 12:08:56 PDT
     *
     * "EEE, MMM d, ''yy"	    Wed, Jul 4, '01
     *
     * "h:mm a"	  12:08 PM
     *
     * "hh 'o''clock' a, zzzz"	12 o'clock PM,                  Pacific Daylight Time
     *
     * "K:mm a, z"	   0:08 PM, PDT
     *
     * "yyyyy.MMMMM.dd GGG hh:mm aaa"	  02001.July.04 AD 12:08 PM
     *
     * "EEE, d MMM yyyy HH:mm:ss Z"	   Wed, 4 Jul 2001 12:08:56 -0700
     *
     * "yyMMddHHmmssZ"     010704120856-0700
     *
     * "yyyy-MM-dd'T'HH:mm:ss.SSSZ"	2001-07-04T12:08:56.235-0700
     *
     * "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"	2001-07-04T12:08:56.235-07:00
     *
     * "YYYY-'W'ww-u"	2001-W27-3
     *
     *
     */


    // time

    val DATE_FORMAT_HH_mm = "HH:mm" // 23:30

    val DATE_FORMAT_hh_mm = "hh:mm" // 12:30

    val DATE_FORMAT_hh_mm_ss = "hh:mm:ss" // 12:30:30

    val DATE_FORMAT_hh_mm_ss_aa = "hh:mm:ss aa" // 12:30:30 am

    val DATE_FORMAT_hh_mm_aa = "hh:mm aa" // 12:30 am


    // date

    val DATE_FORMAT_yyyy_dd_MM = "yyyy, dd MMM" // 1989, 3 jun

    val DATE_FORMAT_yyyy_MM_dd = "yyyy-MM-dd" // 19891231

    val DATE_FORMAT_yyyyMMdd = "yyyyMMdd" // 19891231

    val DATE_FORMAT_ddMMyyyy = "ddMMyyyy" // 31121989

    val DATE_FORMAT_dd_MM_yyyy = "dd-MM-yyyy" // 31121989


    // date time
    val Date_POST_SQL = "yyyy-MM-ddThh:mm:ss" // 2019-05-21T10:07:25.525174Z
    val DATE_FORMAT_dd_MM_yyyy_hh_mm_ss = "dd-MM-yyyy hh:mm:ss" // 31-12-1989 12:30:30
    val DATE_FORMAT_dd_MM_yyyy_hh_mm = "dd-MM-yyyy hh:mm" // 31-12-1989 12:30
    val DATE_FORMAT_dd_MM_yyyy_hh_mm_aa = "dd-MM-yyyy hh:mm aa" // 31-12-1989 12:30 am
    val DATE_FORMAT_MMM_dd_yyyy_hh_aa = "MMM dd, yyyy hh aa" // Jan 15, 2019 12 pm


    fun getCurrentDate(format: String): String {
        var ff = format

        if (TextUtils.isEmpty(format)) {
            ff = DATE_FORMAT_dd_MM_yyyy_hh_mm
        }
        return DateFormat.format(ff, java.util.Date()).toString()
    }

    fun formatDate(format: String, timeStamp: Long): String {
        var timeStampVal = timeStamp
        if (timeStamp <= 0) {
            timeStampVal = System.currentTimeMillis()
        }

        return DateFormat.format(format, java.util.Date(timeStampVal)).toString()
    }

    fun formatDate(format: String, date: Date?): String {
        return DateFormat.format(format, date ?: Date()).toString()
    }

    fun formatDate(format: String, calendar: Calendar?): String {
        return DateFormat.format(format, if (calendar == null) Calendar.getInstance().time else calendar.time).toString()
    }



    /*
    *
    * It's return date  before one week timestamp
    *
    *  like return
    *
    *  1 day ago
    *  2 days ago
    *  5 days ago
    *  21 April 2019
    *
    *
    * */
    fun getAgoDateTime(pastTimeStamp: Long): String {
        var pastTS = pastTimeStamp

        // for 2 min ago   use  DateUtils.MINUTE_IN_MILLIS
        // for 2 sec ago   use  DateUtils.SECOND_IN_MILLIS
        // for 1 hours ago use  DateUtils.HOUR_IN_MILLIS

        val now = System.currentTimeMillis()

        if (now - pastTimeStamp < 1000) {
            pastTS = pastTimeStamp + 1000
        }


        val ago = DateUtils.getRelativeTimeSpanString(pastTS, now, DateUtils.SECOND_IN_MILLIS)


        return ago.toString()
    }


    /*
     *
     * It's return date  before one week timestamp
     *
     *  like return
     *
     *  1 day ago
     *  2 days ago
     *  5 days ago
     *  2 weeks ago
     *  2 months ago
     *  2 years ago
     *
     * */

    fun formatTimeAgo(timeStr: String?): String {
        //val dateStr = "2019-05-21T10:07:25.886432Z"

        Log.i(TAG,"time before changed -> " + timeStr)
        val df = SimpleDateFormat(CONVERTPATTEN,Locale.ENGLISH)
        df.timeZone = TimeZone.getTimeZone("UTC")
        val date = df.parse(timeStr)
        df.timeZone = TimeZone.getDefault()
        val formattedDate = df.format(date)
        //return formattedDate
        return DateTimeUtils().getTimeAgo(date.time)
    }

    fun getDateFromString(timeStr: String) : Date {

        val df = SimpleDateFormat(CONVERTPATTEN,Locale.ENGLISH)
        df.timeZone = TimeZone.getTimeZone("UTC")
        val date = df.parse(timeStr)
        Log.i(TAG,"Date is converted from timeStr -> " + timeStr)
        return date
    }



    fun getLocalDate(timeStr: String): String {

        if(timeStr.isEmpty()) return ""

        val df = SimpleDateFormat(CONVERTPATTEN,Locale.ENGLISH)
        val df_p = SimpleDateFormat("MM/dd/yyyy",Locale.ENGLISH)
        df.timeZone = TimeZone.getTimeZone("UTC")
        val date = df.parse(timeStr)
        Log.i(TAG,"Date is converted from timeStr -> " + timeStr)
        return df_p.format(date)
    }

    fun getLocalTime(timeStr: String): String {
        if(timeStr.isEmpty()) return ""
        val df = SimpleDateFormat(CONVERTPATTEN,Locale.ENGLISH)
        val df_p = SimpleDateFormat("hh:mm aa",Locale.ENGLISH)
        df.timeZone = TimeZone.getTimeZone("UTC")
        val date = df.parse(timeStr)
        Log.i(TAG,"Date is converted from timeStr -> " + timeStr)
        return df_p.format(date)
    }

    fun getDateFromLString(timeStr: String): Date {

        val df = SimpleDateFormat("MM/dd/yyyy hh:mm aa",Locale.ENGLISH)
        val date = df.parse(timeStr)
        return date
    }

    fun getTimeAgo(mReferenceTime: Long): String {

        val SECOND_MILLIS = 1000
        val MINUTE_MILLIS = 60 * SECOND_MILLIS
        val HOUR_MILLIS = 60 * MINUTE_MILLIS
        val DAY_MILLIS = 24 * HOUR_MILLIS

        val now = System.currentTimeMillis()
        val diff = now - mReferenceTime
        if (diff < DateUtils.WEEK_IN_MILLIS) {

            if (diff < MINUTE_MILLIS) return "just now"
            else if( diff < 2 * MINUTE_MILLIS) return "a minute ago"
            else if (diff < 50 * MINUTE_MILLIS) {
                return (diff / MINUTE_MILLIS).toString() + " minutes ago"
            } else if (diff < 90 * MINUTE_MILLIS) {
                return "an hour ago"
            } else if (diff < 24 * HOUR_MILLIS) {
                return (diff / HOUR_MILLIS).toString() + " hours ago"
            } else if (diff < 48 * HOUR_MILLIS) {
                return "yesterday"
            } else {
                return (diff / DAY_MILLIS).toString() + " days ago"
            }

//            return if (diff <= 60000)
//                "just now"
//            else
//                DateUtils.getRelativeTimeSpanString(mReferenceTime, now, DateUtils.MINUTE_IN_MILLIS,
//                    DateUtils.FORMAT_ABBREV_RELATIVE).toString()
        } else if (diff <= 4 * DateUtils.WEEK_IN_MILLIS) {
            val week = (diff / DateUtils.WEEK_IN_MILLIS).toInt()
            return if (week > 1) week.toString() + " weeks ago" else week.toString() + " week ago"
        } else if (diff < DateUtils.YEAR_IN_MILLIS) {
            val month = (diff / (4 * DateUtils.WEEK_IN_MILLIS)).toInt()
            return if (month > 1) month.toString() + " months ago" else month.toString() + " month ago"
        } else {
            val year = (diff / DateUtils.YEAR_IN_MILLIS).toInt()
            return if (year > 1) year.toString() + " years ago" else year.toString() + " year ago"
        }
    }
}
