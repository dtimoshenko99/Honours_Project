package abertay.uad.ac.uk.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LobbyActivity extends AppCompatActivity {

    private boolean isHost;
    private String hostUsername;
    private ListenerRegistration readyFieldListener;
    private boolean opponentReady = false;
    private boolean hostReady = false;
    private String userFirebase;
    private TextView opponentUsername, user, lobbyFieldName, opponentFieldReady, userFieldReady;
    private String lobbyId;
    private FirebaseFirestore db;
    private Button readyButton, leaveButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        // Get instances of firebase and get current user ID
        db = FirebaseFirestore.getInstance();
        FirebaseAuth authFirebase = FirebaseAuth.getInstance();
        userFirebase = authFirebase.getCurrentUser().getUid();

        // Assign viewvs to variables
        opponentFieldReady = findViewById(R.id.opponentReady);
        userFieldReady = findViewById(R.id.userReady);
        readyButton = findViewById(R.id.readyButton);
        leaveButton = findViewById(R.id.button3);
        lobbyFieldName = findViewById(R.id.lobbyActivityName);
        opponentUsername = findViewById(R.id.opponentUsername);
        user = findViewById(R.id.playerOneUsername);

        // Get data from last intent
        Intent intent = getIntent();
        intent.getExtras();
        isHost = intent.getBooleanExtra("isHost", true);


        getDataAssignListeners(intent,isHost);

        readyButton.setOnClickListener(v -> {
            String field = isHost ? "hostReady" : "opponentReady";
            hostReady = field.equals("hostReady");
            opponentReady = !field.equals("hostReady");
            updateUi();
            updateReadyField(field);
        });


        leaveButton.setOnClickListener(v -> {
            if(isHost){
                deleteLobbyAndLeave();
            }else{
                updateFieldsAndLeave();
            }
        });

    }

    private void updateReadyField(String field) {
        db.collection("lobbies").document(lobbyId).update(field, true);
    }

    private void updateUi(){
        if (isHost) {
            userFieldReady.setText(hostReady ? "Ready" : "Not ready");
            opponentFieldReady.setText(opponentReady ? "Ready" : "Not ready");
        } else {
            userFieldReady.setText(opponentReady ? "Ready" : "Not ready");
            opponentFieldReady.setText(hostReady ? "Ready" : "Not ready");
        }

    }

    private void getDataAssignListeners(Intent intent,boolean isHost) {
        if(!isHost){
            // Get data from last intent and set text Fields
            lobbyId = intent.getStringExtra("lobbyId");
            String lobbyName = intent.getStringExtra("lobbyName");
            String hostUsername = intent.getStringExtra("hostUsername");
            String guestUsername = intent.getStringExtra("guestUsername");
            userFieldReady.setText("Not Ready");
            opponentUsername.setText(hostUsername);
            user.setText(guestUsername);
            lobbyFieldName.setText(lobbyName);
            updateOnJoin(guestUsername);

        }else{
            // Get data from last intent and set text Fields
            lobbyId = intent.getStringExtra("lobbyId");
            lobbyFieldName.setText(intent.getStringExtra("lobbyName"));
            hostUsername = intent.getStringExtra("hostUsername");
            user.setText(hostUsername);
            userFieldReady.setText("Not Ready");
            opponentFieldReady.setText("Not Ready");
            opponentUsername.setText("Waiting...");
            databaseReadyFieldListener();
        }
    }


    private void updateOnJoin(String guestName) {
        // Update fields of lobby in DB and assign listener
        Map<String, Object> lobbyData = new HashMap<>();
        lobbyData.put("currentPlayers", 2);
        lobbyData.put("opponentReady", false);
        lobbyData.put("guestUsername", guestName);
        lobbyData.put("guestId", userFirebase);
        lobbyData.put("opponentJoined", true);
        db.collection("lobbies").document(lobbyId)
                .update(lobbyData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                db.collection("lobbies").document(lobbyId)
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        boolean isReady = documentSnapshot.getBoolean("hostReady");
                        boolean guestReady = documentSnapshot.getBoolean("opponentReady");

                        if(guestReady && isReady){
                            readyFieldListener.remove();
                            if(isHost){
                                createGame();
                            }
                                Intent intent = new Intent(LobbyActivity.this, MultiPlayerActivity.class);
                                intent.putExtra("lobbyId", lobbyId);
                                intent.putExtra(isHost ? "hostUserId" : "guestUserId", userFirebase);
                                startActivity(intent);
                        } else{
                            databaseReadyFieldListener();
                            updateUi();
                        }
                    }
                });
            }
        });
    }

    private void databaseReadyFieldListener(){
        // Assign a listener for a Ready fields in database
       readyFieldListener = db.collection("lobbies").document(lobbyId)
                .addSnapshotListener((snapshot, e ) ->{
                   if(e != null){
                   }

                   if(snapshot != null && snapshot.exists()){

                       if(snapshot.getBoolean("opponentJoined")){
                           opponentReady = snapshot.getBoolean("opponentReady");
                           hostReady = snapshot.getBoolean("hostReady");
                           db.collection("lobbies").document(lobbyId).get()
                                   .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                       @Override
                                       public void onSuccess(DocumentSnapshot documentSnapshot) {
                                           if(isHost){
                                               String guestUsername = documentSnapshot.getString("guestUsername");
                                               opponentUsername.setText(guestUsername);
                                           }

                                       }
                                   });
                           if(opponentReady && hostReady){
                               readyFieldListener.remove();
                               if(isHost){
                                   updateUi();
                                   createGame();
                               }
                               Intent intent = new Intent(LobbyActivity.this, MultiPlayerActivity.class);
                               intent.putExtra("lobbyId", lobbyId);
                               intent.putExtra(isHost ? "hostUserId" : "guestUserId", userFirebase);
                               startActivity(intent);
                           }
                           updateUi();
                       }
                   }
                });
    }


    private void createGame(){
        // Get guest ID and create a game in database
        db.collection("lobbies").document(lobbyId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Map<String, Object> gameData = new HashMap<>();
                String guestId = documentSnapshot.getString("guestId");
                gameData.put("lobbyId", lobbyId);
                gameData.put("hostUserId", userFirebase);
                gameData.put("guestUserId", guestId);
                gameData.put("state", "started");
                gameData.put("isHostBoardPlaced", false);
                gameData.put("isGuestBoardPlaced", false);
                gameData.put("turn", "Black");
                gameData.put("wasRow", -1);
                gameData.put("wasCol", -1);
                gameData.put("nowRow", -1);
                gameData.put("nowCol", -1);
                gameData.put("capturedRow", -1);
                gameData.put("capturedCol", -1);
                gameData.put("hasCaptures", false);

                db.collection("games").document(lobbyId)
                        .set(gameData)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                            }
                        });
            }
        });




    }

    private void updateFieldsAndLeave() {
        // Update fields in database if guest leaves
        Map<String, Object> lobbyData = new HashMap<>();
        lobbyData.put("currentPlayers", 1);
        lobbyData.put("opponentReady", false);
        lobbyData.put("opponentJoined", false);
        db.collection("lobbies").document(lobbyId)
                .update(lobbyData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Intent intent = new Intent(LobbyActivity.this, OpenGamesActivity.class);
                        startActivity(intent);
                    }
                });
    }

    private void deleteLobbyAndLeave() {
        // Delete lobby in database if host leaves
        db.collection("lobbies").whereEqualTo("id", lobbyId).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<DocumentSnapshot> doc =  queryDocumentSnapshots.getDocuments();
                        String id = doc.get(0).getId();
                        db.collection("lobbies").document(id).delete();
                        startActivity(new Intent(LobbyActivity.this, OpenGamesActivity.class));
                    }
                });
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }
}
