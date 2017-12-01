package com.gae.scaffolder.plugin;

import android.app.NotificationChannel;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by Felipe Echanique on 08/06/2016, modified by David Briglio on 01/12/2017.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMPlugin";
    
    /**
     * Called to setup notification channel for android API >= 26.
     * @param context Context to get the notification manager from.
     */
    public static void initChannel(Context context) {
        // Do not create channel for lower android versions
        if(Build.VERSION.SDK_INT < 26) {
            return;
        }
        
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("default", "FCM Plugin Default", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("FCM Plugin Notifications");
        if(manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        Log.d(TAG, "==> MyFirebaseMessagingService onMessageReceived");

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("wasTapped", "false");
		for (String key : remoteMessage.getData().keySet()) {
                Object value = remoteMessage.getData().get(key);
                Log.d(TAG, "\tKey: " + key + " Value: " + value);
                if(!key.equals("wasTapped")) {
                    data.put(key, value);
                }
        }

		Log.d(TAG, "\tNotification Data: " + data.toString());
        FCMPlugin.sendPushPayload(data);
        sendNotification(data);
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param data data received from FCM message.
     */
    private void sendNotification(Map<String, Object> data) {
        Intent intent = new Intent(this, FCMPluginActivity.class);

        String title;
        String text;
        String groupTitle;
        Integer id = 5;
        Integer ledColour = 0xFFFFFF;

        Object titleObj = data.get("title");
        title = (titleObj == null) ? "New Notification" : titleObj.toString();

        Object textObj = data.get("text");
        text = (textObj == null) ? "" : textObj.toString();

        Object groupTitleObj = data.get("groupTitle");
        groupTitle = (groupTitleObj == null) ? "New Notifications" : groupTitleObj.toString();

        Object idObj = data.get("notificationId");
        if(idObj != null) {
            try{
                id = Integer.parseInt(idObj.toString());
            }
            catch (java.lang.NumberFormatException e) {
                id = 5;
            }
        }

        Object colourObj = data.get("led");
        if(colourObj != null) {
            try {
                ledColour = Integer.parseInt(colourObj.toString(), 16);
            }
            catch (java.lang.NumberFormatException e) {
                ledColour = 0xFFFFFF;
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		for (String key : data.keySet()) {
			intent.putExtra(key, data.get(key).toString());
		}

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(getResources().getIdentifier("notificationicon", "drawable", getPackageName()))
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setLights(ledColour, 1000, 500)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Boolean isMulti = false;
        StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
        for(int i = 0; i < notifications.length; i++){
            if(notifications[i].getId() == id){
                isMulti = true;
                Notification activeNotification = notifications[i].getNotification();
                NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
                style.addLine(text);

                //Check to see if the notification is an inbox notification or a regular notification
                CharSequence[] activeLines = activeNotification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
                if(activeLines == null || activeLines.length == 0) {
                    style.addLine(activeNotification.extras.getString(Notification.EXTRA_TEXT));
                }
                else {
                    for(int j = 0; j < activeLines.length; j++){
                        if(j > 2){
                            style.addLine("...");
                            break;
                        }
                        style.addLine(activeLines[j]);
                    }
                }

                notificationBuilder.setContentTitle(groupTitle)
                    .setStyle(style);

                break;
            }
        }

        intent.putExtra("multipleNotifications", isMulti.toString());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationManager.notify("com.fcm-cordova", id, notificationBuilder.build());
    }
}
