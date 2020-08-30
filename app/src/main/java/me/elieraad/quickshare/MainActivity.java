package me.elieraad.quickshare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int PICKFILE_RESULT_CODE = 100;
    private static final String CHANNEL_ID = "QuickShareChannelID";
    private StorageReference mStorageRef;
    private ProgressBar progressBar;
    private TextView fileNameView;
    private int notificationId = 200;
    private PushNotification pushNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        fileNameView = (TextView) findViewById(R.id.fileName);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        createNotificationChannel();
        pushNotification = new PushNotification();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICKFILE_RESULT_CODE:
                if (resultCode == -1) {
                    Uri fileUri = data.getData();
                    String uriString = fileUri.toString();
                    File myFile = new File(uriString);
                    String displayName = "";
                    if (uriString.startsWith("file://")) {
                        displayName = myFile.getName();
                    } else {
                        try (Cursor cursor = getContentResolver().query(fileUri, null, null, null, null)) {
                            if (cursor != null && cursor.moveToFirst()) {
                                displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            }
                        }
                    }
                    fileNameView.setText(displayName);
                    pushNotification.show(getApplicationContext(), displayName);
                    uploadFile(fileUri, displayName);
                }

                break;
        }
    }

    private void uploadFile(Uri file, String displayName) {

        StorageReference filesRef = mStorageRef.child("files/" + System.currentTimeMillis() + displayName);

        progressBar.setVisibility(View.VISIBLE);
        fileNameView.setVisibility(View.VISIBLE);

        filesRef.putFile(file)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = 100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount();
                        progressBar.setProgress((int) progress);
                        pushNotification.update((int) progress);
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Task<Uri> downloadUrl = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                        downloadUrl.addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                progressBar.setVisibility(View.INVISIBLE);
                                fileNameView.setVisibility(View.INVISIBLE);
                                pushNotification.finish(getApplicationContext(), task.getResult().toString());
//                                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//                                ClipData clip = ClipData.newPlainText("url", task.getResult().toString());
//                                clipboard.setPrimaryClip(clip);
//                                Toast.makeText(getApplicationContext(), "URL copied to clipboard", Toast.LENGTH_LONG).show();
//

                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        // ...
                    }
                });
    }

    public void chooseFile(View view) {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
