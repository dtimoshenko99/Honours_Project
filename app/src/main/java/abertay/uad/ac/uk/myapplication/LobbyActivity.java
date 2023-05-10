package abertay.uad.ac.uk.myapplication;

import androidx.annotation.NonNull;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LobbyActivity extends AppCompatActivity {

    private boolean isHost;
    private String TAG = "onTap";
    private boolean opponentReady, hostReady;
    FirebaseAuth authFirebase;
    String userFirebase;
    private TextView opponentUsername, user, lobbyFieldName, opponentFieldReady, userFieldReady;
    private String lobbyId;
    private FirebaseFirestore db;
    private Button readyButton, leaveButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        db = FirebaseFirestore.getInstance();
        opponentFieldReady = findViewById(R.id.opponentReady);
        userFieldReady = findViewById(R.id.userReady);
        readyButton = findViewById(R.id.readyButton);
        leaveButton = findViewById(R.id.button3);
        lobbyFieldName = findViewById(R.id.lobbyActivityName);
        opponentUsername = findViewById(R.id.opponentUsername);
        user = findViewById(R.id.playerOneUsername);
        Intent intent = getIntent();
        intent.getExtras();
        isHost = intent.getBooleanExtra("isHost", true);

        authFirebase = FirebaseAuth.getInstance();
        userFirebase = authFirebase.getCurrentUser().getUid();

        readyButton.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: IsHost " + isHost);
            updateDB(isHost);
        });
        getDataAssignListeners(intent,isHost);

        leaveButton.setOnClickListener(v -> {
            if(isHost){
                deleteLobbyAndLeave();
            }else{
                updateFieldsAndLeave();
            }
        });

    }



    private void updateDB(boolean isHost) {
        String updateField;
        if(isHost){
            updateField = "hostReady";
            Log.d(TAG, "updateDB: IsHOstReady");
        }else{
            Log.d(TAG, "updateDB: IsOpponentReady");
            updateField = "opponentReady";
        }
        db.collection("lobbies").document(lobbyId)
                .update(updateField, true)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        userFieldReady.setText("Yes");
                        Log.d("onTap", "onSuccess: UpdatedSuccessReady");
                    }
                });
    }

    private void getDataAssignListeners(Intent intent,boolean isHost) {
        if(!isHost){

            lobbyId = intent.getStringExtra("lobbyId");
            String lobbyName = intent.getStringExtra("lobbyName");
            String hostUsername = intent.getStringExtra("hostUsername");
            String guestUsername = intent.getStringExtra("guestUsername");
            Log.d(TAG, "getDataAssignListeners: " + hostUsername + " " + guestUsername);
            Log.d(TAG, "getDataAssignListeners: lobbyName: " + lobbyName + " " + lobbyId);
            userFieldReady.setText("No");
            opponentUsername.setText(hostUsername);
            user.setText(guestUsername);
            lobbyFieldName.setText(lobbyName);

            Map<String, Object> lobbyData = new HashMap<>();
            lobbyData.put("currentPlayers", 2);
            lobbyData.put("opponentReady", false);
            lobbyData.put("guestUsername", guestUsername);
            lobbyData.put("guestId", userFirebase);
            lobbyData.put("opponentJoined", true);

            db.collection("lobbies").document(lobbyId).update(lobbyData)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "onSuccess: UpdatedOnJoin");
                            db.collection("lobbies").document(lobbyId).get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if(documentSnapshot.getBoolean("hostReady")){
                                                opponentFieldReady.setText("Yes");
                                            }else{
                                                opponentFieldReady.setText("No");
                                                databaseReadyListener();
                                            }

                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(Exception e) {
                                    Log.d("onTap", "onFailure: Something failed" + e);
                                }
                            });
                        }
                    });

        }else{
            lobbyId = intent.getStringExtra("lobbyId");
            lobbyFieldName.setText(intent.getStringExtra("lobbyName"));
            user.setText(intent.getStringExtra("hostUsername"));
            userFieldReady.setText("No");
            opponentFieldReady.setText("No");
            opponentUsername.setText("Waiting...");
            databaseReadyListener();
        }
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
                hostReady = snapshot.getBoolean("hostReady");
                opponentReady = snapshot.getBoolean("opponentReady");

                if (opponentReady && hostReady) {
                    if(isHost){
                        deleteLobbyAndCreateGame();
                    }
                    Intent intent = new Intent(LobbyActivity.this, MultiPlayerActivity.class);
                    intent.putExtra("lobbyId", lobbyId);
                    if(isHost){
                        intent.putExtra("hostUserId", userFirebase);
                    }else{
                        intent.putExtra("guestUserId", userFirebase);
                    }
                    startActivity(intent);
                    finish();
                }else if (opponentReady) {
                    // Guest is ready, host is not
                    opponentFieldReady.setText("Ready");
                    userFieldReady.setText("Not ready");
                } else if (hostReady) {
                    // Host is ready, guest is not
                    opponentFieldReady.setText("Not ready");
                    userFieldReady.setText("Ready");
                } else {
                    // Neither player is ready
                    opponentFieldReady.setText("Not ready");
                    userFieldReady.setText("Not ready");
                }
            } else {
                Log.d("onTap", "Current data: null");
            }
        });
    }

    private void deleteLobbyAndCreateGame(){



        db.collection("lobbies").document(lobbyId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Map<String, Object> gameData = new HashMap<>();
                String guestId = documentSnapshot.getString("guestId");
                gameData.put("lobbyId", lobbyId);
                gameData.put("hostUserId", userFirebase);
                gameData.put("guestUserId", guestId);
                gameData.put("state", "started");
                gameData.put("occupiedValueList", null);
                gameData.put("isHostBoardPlaced", false);
                gameData.put("isGuestBoardPlaced", false);
                gameData.put("turn", "Black");
                gameData.put("wasRow", -1);
                gameData.put("wasCol", -1);
                gameData.put("nowRow", -1);
                gameData.put("nowCol", -1);
                gameData.put("capturedRow", -1);
                gameData.put("capturedCol", -1);
                db.collection("lobbies").document(lobbyId)
                        .delete()
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                db.collection("games").document(lobbyId)
                                        .set(gameData)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Log.d(TAG, "onComplete: deleted lobby, created a game");
                                            }
                                        });
                            }
                        });
            }
        });




    }

    private void updateFieldsAndLeave() {
        Map<String, Object> lobbyData = new HashMap<>();
        lobbyData.put("currentPlayers", 1);
        lobbyData.put("opponentReady", false);
        lobbyData.put("opponentJoined", false);
        db.collection("lobbies").document(lobbyId)
                .update(lobbyData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Left and updated fields");
                        Intent intent = new Intent(LobbyActivity.this, OpenGamesActivity.class);
                        startActivity(intent);
                    }
                });
    }

    private void deleteLobbyAndLeave() {
        Log.d(TAG, "deleteLobbyAndLeave: " + lobbyId);
        db.collection("lobbies").whereEqualTo("id", lobbyId).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<DocumentSnapshot> doc =  queryDocumentSnapshots.getDocuments();
                        Log.d(TAG, "onSuccess: " + doc);
                        String id = doc.get(0).getId();
                        db.collection("lobbies").document(id).delete();
                        startActivity(new Intent(LobbyActivity.this, OpenGamesActivity.class));
                    }
                });
    }

    @Override
    public void onBackPressed() {
        // TODO: Dialog : really want to leave this lobby?
    }
}
