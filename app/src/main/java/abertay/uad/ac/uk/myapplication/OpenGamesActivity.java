package abertay.uad.ac.uk.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenGamesActivity extends AppCompatActivity {

    private ImageView arrowBack;
    private SharedPreferences shared;
    private SharedPreferences.Editor editor;
    private FirebaseAuth auth;
    private List<Lobby> lobbyList;
    private EditText lobbyName;
    private FirebaseFirestore db;
    private DatabaseReference databaseReference;
    private RecyclerView recyclerView;
    private LobbyAdapter lobbyListAdapter;
    private Button refresh, create;
    private String userToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_games);

        shared = getSharedPreferences("details", Context.MODE_PRIVATE);
        editor = shared.edit();
        auth = FirebaseAuth.getInstance();
        lobbyList = new ArrayList<>();

        lobbyName = findViewById(R.id.lobbyName);
        // Set up the RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        lobbyListAdapter = new LobbyAdapter(lobbyList, this);
        recyclerView.setAdapter(lobbyListAdapter);

        // Get a reference to the "lobbies" node in the database
        db = FirebaseFirestore.getInstance();

        getLobbies();
        refresh = findViewById(R.id.refreshButton);
        refresh.setOnClickListener(v -> {
            refreshData();
        });

        create = findViewById(R.id.createLobbyButton);
        create.setOnClickListener(v -> {
            //TODO: add checks here
            if(!lobbyName.getText().toString().isEmpty()){
                createLobby(lobbyName.getText().toString());
                Log.d("onTap", "onCreate: Hell, not empty");
            }
            else{
                lobbyName.setError("Please enter lobby name");
            }
        });

        arrowBack = findViewById(R.id.openGamesArrowBack);
        arrowBack.setOnClickListener(v -> {
            startActivity(new Intent(OpenGamesActivity.this, MainMenuActivity.class));
        });
    }

    @Override
    public void onBackPressed(){
        startActivity(new Intent(OpenGamesActivity.this, MainMenuActivity.class));
        finish();
    }

    public void createLobby(String lobbyName) {
        // Get a reference to the Firebase Firestore database
        db = FirebaseFirestore.getInstance();
        // Create a new lobby document with a unique ID
        DocumentReference document = db.collection("lobbies").document();


        // Create a data object for the new lobby document
        Map<String, Object> lobbyData = new HashMap<>();
        lobbyData.put("name", lobbyName);
        lobbyData.put("hostUsername", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        lobbyData.put("currentPlayers", 1);
        lobbyData.put("id" , document.getId());
        lobbyData.put("hostReady", false);
        lobbyData.put("opponentReady", false);
        lobbyData.put("opponentJoined", false);
        lobbyData.put("guestUsername", "");
        lobbyData.put("guestId", "");
        lobbyData.put("hostId", auth.getCurrentUser().getUid());


        // Set the data for the new lobby document
        document.set(lobbyData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("onTap", "Lobby created successfully!");
                        Intent intent = new Intent(OpenGamesActivity.this, LobbyActivity.class);
                        intent.putExtra("lobbyId", document.getId());
                        intent.putExtra("lobbyName", lobbyName);
                        intent.putExtra("hostUsername",FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("onTap", "Error creating lobby", e);
                        // Handle any errors here
                    }
                });
    }


    private void refreshData() {
        // Clear the data list
        lobbyList.clear();

        // Populate the data list with updated data from database
        getLobbies();

        // Notify the adapter that the data set has changed
        lobbyListAdapter.notifyDataSetChanged();
    }

    private void getLobbies(){
        db.collection("lobbies")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {

                            Log.d("onTap", document.getId() + " => " + document.getData());
                            if(document.getLong("currentPlayers") != 2){
                                Lobby lobby = new Lobby();
                                lobby.setId(document.getId());
                                lobby.setName(document.getString("name"));
                                lobby.setHostUsername(document.getString("hostUsername"));
                                lobby.setCurrentPlayers(Math.toIntExact(document.getLong("currentPlayers")));
                                lobbyList.add(lobby);
                            }
                            lobbyListAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.w("onTap", "Error getting documents.", task.getException());
                    }
                });
    }

    private class LobbyHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private Lobby lobby;
        private TextView lobbyNameTextView;
        private TextView lobbyStatusTextView;
        private Button joinButton;

        public LobbyHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_lobby, parent, false));
            itemView.setOnClickListener(this);

            // Get references to UI elements
            lobbyNameTextView = itemView.findViewById(R.id.lobby_name_text_view);
            lobbyStatusTextView = itemView.findViewById(R.id.lobby_status_text_view);
            joinButton = itemView.findViewById(R.id.join_button);
        }

        public void bind(Lobby lobby) {
            this.lobby = lobby;

            // Set text on UI elements
            lobbyNameTextView.setText("Name: " + lobby.getName());
            lobbyStatusTextView.setText("Hostname: " + lobby.getHostUsername());

            // Attach click listener to join button
            joinButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.join_button) {
                // Start activity to join lobby
                Intent intent = new Intent(OpenGamesActivity.this, LobbyActivity.class);
                intent.putExtra("lobbyId", lobby.getId());
                intent.putExtra("isHost", false);
                intent.putExtra("hostUsername", lobby.getHostUsername());
                intent.putExtra("lobbyName", lobby.getName());
                if(auth.getCurrentUser().getDisplayName() == null){
                    intent.putExtra("guestUsername", shared.getString("username", "User"));
                }else{
                    intent.putExtra("guestUsername", auth.getCurrentUser().getDisplayName());
                }
                startActivity(intent);
            }
        }
    }

    private class LobbyAdapter extends RecyclerView.Adapter<LobbyHolder> {

        private List<Lobby> lobbies;

        public LobbyAdapter(List<Lobby> lobbies, OpenGamesActivity openGamesActivity) {
            this.lobbies = lobbies;
        }

        @NonNull
        @Override
        public LobbyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(OpenGamesActivity.this);
            return new LobbyHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull LobbyHolder holder, int position) {
            Lobby lobby = lobbies.get(position);
            holder.bind(lobby);
        }

        @Override
        public int getItemCount() {
            return lobbies.size();
        }
    }
}
