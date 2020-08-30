package me.elieraad.quickshare;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class PushNotification {

    private static final String CHANNEL_ID = "QuickShareChannelID";
    private static final int PROGRESS_MAX = 100;
    private static int notificationId = 200;

    private NotificationCompat.Builder builder;
    private NotificationManagerCompat notificationManager;

    public PushNotification() {
    }

    public void show(Context context, String title) {
        notificationManager = NotificationManagerCompat.from(context);
        builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText("Upload in progress")
                .setSmallIcon(R.drawable.ic_upload_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationManager.notify(notificationId, builder.build());
    }

    public void update(int currentProgress) {
        builder.setProgress(PROGRESS_MAX, currentProgress, false);
        notificationManager.notify(notificationId, builder.build());
    }

    public void finish(Context context, String url) {
        builder.setContentText("Upload complete")
                .setProgress(0, 0, false);

        //This is the intent of PendingIntent
        Intent intentAction = new Intent(context, ActionReceiver.class);

        //This is optional if you have more than one buttons and want to differentiate between two
        intentAction.putExtra("copyURL", url);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intentAction, PendingIntent.FLAG_UPDATE_CURRENT);
        //Using this action button I would like to call logTest
        builder.addAction(R.drawable.ic_file_copy, "Copy URL", pendingIntent);
        notificationManager.notify(notificationId, builder.build());
    }

    public static class ActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String url = intent.getStringExtra("copyURL");

            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("url", url);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, url, Toast.LENGTH_LONG).show();

            //This is used to close the notification tray
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
        }


    }

}
