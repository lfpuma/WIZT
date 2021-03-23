package com.wizt.utils


/*
 *
 * Yogendra
 * 11/01/2019
 *
 * */
object DNACollectionUtils {

    fun isNotEmpty(collection: Collection<*>): Boolean {
        return !isEmpty(collection)
    }

    fun isEmpty(collection: Collection<*>?): Boolean {
        return collection == null || collection.isEmpty()
    }

    fun isNull(collection: Collection<*>?): Boolean {
        return collection == null
    }


    fun size(collection: Collection<*>?): Int {
        if (collection == null)
            return 0
        return if (collection.isEmpty()) 0 else collection.size

    }

}
