package com.wizt.common.aws

class ItemToDisplay// Constructor
    (// Text for display
    var labelText: String?, var dataText: String?, var messageText: String?,
    // Display text colors
    var labelColor: Int, var dataColor: Int, var messageColor: Int,
    // Data box background
    var dataBackground: Int, // Data box drawable
    var dataDrawable: String?

) {

    override fun toString(): String {
        return "ItemToDisplay{" +
                "labelText='" + labelText + '\''.toString() +
                ", dataText='" + dataText + '\''.toString() +
                ", messageText='" + messageText + '\''.toString() +
                ", labelColor=" + labelColor +
                ", dataColor=" + dataColor +
                ", messageColor=" + messageColor +
                ", dataBackground=" + dataBackground +
                ", dataDrawable='" + dataDrawable + '\''.toString() +
                '}'.toString()
    }
}
