package abertay.uad.ac.uk.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainMenuActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseUser user;
    TextView gameDate, gameResult;
    Button profile, singlePlayer, multiPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("onTap", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and use the token as needed
                        Log.d("onTap", "FCM registration token: " + token);
                    }
                });

//        if(user.getUid() != null){
//            getRecentGames(user.getUid());
//        }

        gameDate = findViewById(R.id.gameDate);
        gameResult = findViewById(R.id.gameResult);
        singlePlayer = findViewById(R.id.singlePlayer);
        multiPlayer = findViewById(R.id.multiPlayer);
        profile = findViewById(R.id.profileButton);
        profile.setOnClickListener(v ->{
            startActivity(new Intent(this, ProfileActivity.class));
        });

        singlePlayer.setOnClickListener(v -> {
            startActivity(new Intent(this, SinglePlayerActivity.class));
        });

        multiPlayer.setOnClickListener(v -> {
            startActivity(new Intent(this, OpenGamesActivity.class));
        });

    }

    private void getRecentGames(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference gamesRef = db.collection("games");


        db.collection("games")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    if(document.getString("playerId").equals(userId)){
                        String id = document.getId();
                        Date date = document.getDate("date");
                        String result = document.getString("result");
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                        String formattedDate = formatter.format(date);
                        gameResult.setText(result);
                        gameDate.setText(formattedDate);
                    }
                }
                // Do something with the list of recent games, such as display them in a RecyclerView

            } else {
                Log.d("onTap", "Error getting recent games: ", task.getException());
            }
        });
    }
}