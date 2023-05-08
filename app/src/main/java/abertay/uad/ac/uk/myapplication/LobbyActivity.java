package abertay.uad.ac.uk.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

public class LobbyActivity extends AppCompatActivity {

    private boolean opponentReady, hostReady;
    private TextView opponent, user, lobbyFieldName, opponentFieldReady, userFieldReady;
    private String lobbyId;
    private FirebaseFirestore db;
    private String guestFcmToken;
    private Button readyButton, leaveButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        db = FirebaseFirestore.getInstance();
        opponentFieldReady = findViewById(R.id.opponentReady);
        userFieldReady = findViewById(R.id.userReady);
        readyButton = findViewById(R.id.readyButton);
        // TODO: change button
        leaveButton = findViewById(R.id.button3);
        lobbyFieldName = findViewById(R.id.lobbyActivityName);
        opponent = findViewById(R.id.opponentUsername);
        user = findViewById(R.id.playerOneUsername);
        Intent intent = getIntent();
        intent.getExtras();
        boolean isHost = intent.getBooleanExtra("isHost", true);

        readyButton.setOnClickListener(v -> {
            userFieldReady.setText("Yes");
            updateDB(isHost);
        });
        getDataAssignListeners(intent,isHost);


    }

    private void updateDB(boolean isHost) {
        String updateField;
        if(isHost){
            updateField = "hostReady";
        }else{
            updateField = "guestReady";
        }
        db.collection("lobbies").document(lobbyId)
                .update(updateField, true)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("onTap", "onSuccess: UpdatedSuccessReady");
                    }
                });
    }

    private void getDataAssignListeners(Intent intent,boolean isHost) {
        if(!isHost){
            guestFcmToken = intent.getStringExtra("guestFcmToken");
            lobbyId = intent.getStringExtra("lobbyId");
            String lobbyName = intent.getStringExtra("lobbyName");
            String hostUsername = intent.getStringExtra("hostUsername");
            String guestUsername = intent.getStringExtra("guestUsername");
            userFieldReady.setText("No");
            opponent.setText(hostUsername);
            user.setText(guestUsername);
            lobbyFieldName.setText(lobbyName);
            updateLobbyInfo(lobbyId);
            db.collection("lobbies").document(lobbyId).get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if(documentSnapshot.get("hostReady").equals("yes")){
                                opponentFieldReady.setText("Yes");
                            }else{
                                databaseReadyListener();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.d("onTap", "onFailure: Something failed" + e);
                }
            });
        }else{
            lobbyId = intent.getStringExtra("lobbyId");
            lobbyFieldName.setText(intent.getStringExtra("lobbyName"));
            user.setText(intent.getStringExtra("hostUsername"));
            userFieldReady.setText("No");
            opponentFieldReady.setText("No");
            opponent.setText("Waiting...");
            databaseListeners();
        }
    }

    private void databaseListeners(){
        DocumentReference lobbyRef = db.collection("lobbies").document(lobbyId);

// Add a listener to the document to check if the opponent has joined the lobby
        lobbyRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w("onTap", "Listen failed.", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                boolean opponentJoined = snapshot.getBoolean("opponentJoined");
                if (opponentJoined) {
                    Log.d("onTap", "Opponent has joined the lobby");
                    db.collection("lobbies").document(lobbyId).get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(Task<DocumentSnapshot> task) {
                                    opponent.setText(task.getResult().getString("guestUsername"));
                                    db.collection("lobbies").document(lobbyId).update("opponentJoined", true)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    Log.d("onTap", "onSuccess: Success udpate OpponentJoined");
                                                    databaseReadyListener();
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(Exception e) {
                                            Log.d("onTap", "onFailure: Failed update");
                                        }
                                    });

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            opponent.setText("Noname");
                        }
                    });

                } else {
                    Log.d("onTap", "Opponent has not joined the lobby yet");
                    // Update UI accordingly
                    opponent.setText("Not Joined");
                }
            } else {
                Log.d("onTap", "Current data: null");
            }
        });
    }
    private void databaseReadyListener(){

        DocumentReference lobbyRef = db.collection("lobbies").document(lobbyId);

// Add a listener to the document to check if the opponent has joined the lobby
        lobbyRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w("onTap", "Listen failed.", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                opponentReady = snapshot.getBoolean("opponentReady");

                if (opponentReady) {
                    // TODO: send message here FCM
                    opponentFieldReady.setText("Yes");
                } else {
                   opponentFieldReady.setText("No");
                }
            } else {
                Log.d("onTap", "Current data: null");
            }
        });
    }




    private void updateLobbyInfo(String lobbyId) {
        db = FirebaseFirestore.getInstance();

        db.collection("lobbies").document(lobbyId)
                .update("guestFcmToken", guestFcmToken)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("onTap", "onSuccess: updated" );
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.d("onTap", "onFailure: failed");
            }
        });
    }
//        FirebaseMessaging.getInstance().send(new RemoteMessage.Builder("your-sender-id@fcm.googleapis.com")
//                .setMessageId(Integer.toString(msgId.incrementAndGet()))
//                .addData("lobbyId", lobbyId)
//                .addData("gameType", gameType)
//                .addData("otherData", otherData)
//                .build());
    }
