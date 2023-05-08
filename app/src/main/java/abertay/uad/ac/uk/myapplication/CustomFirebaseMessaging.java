package abertay.uad.ac.uk.myapplication;

import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CustomFirebaseMessaging extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Extract the data from the message
        String lobbyId = remoteMessage.getData().get("lobbyId");
        String ready = remoteMessage.getData().get("isReady");

        Intent intent = new Intent(this, MultiPlayerActivity.class);
        intent.putExtra("lobbyId", lobbyId);
        startActivity(intent);


        Log.d("onTap", "onMessageReceived: " + remoteMessage);

    }

    @Override
    public void onNewToken(String token) {
        // Save the FCM token in Firestore or update an existing document
    }
}