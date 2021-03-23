package com.wizt.myservice

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.support.v4.app.NotificationCompat
import android.support.v4.util.ArrayMap
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonObject
import com.wizt.R
import com.wizt.activities.MainActivity
import android.app.NotificationChannel
import android.os.Build
import com.wizt.activities.auth.SplashActivity


class MyFcmListenerService : FirebaseMessagingService() {
    companion object {
        const val TAG = "MyFirebaseMsgService"
        const val CHANNELID = "WIZT"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Receive MSG From: " + remoteMessage.getFrom())
        if (remoteMessage.getData().size > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData())
        }

        val body : ArrayMap<String,String> = remoteMessage.data as ArrayMap<String,String>
        if (body.get("default") != null) {
            sendNotification(body.get("default") as String)
        }

    }

    private fun sendNotification(body: String) {

        Log.d(TAG,"Notification body -> " + body)

        val intent = Intent(this, SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNELID)
            .setContentText(body)
            .setContentTitle("WIZT")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNELID, "WIZT", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(1001 /* ID of notification */, notificationBuilder.build())
    }
}