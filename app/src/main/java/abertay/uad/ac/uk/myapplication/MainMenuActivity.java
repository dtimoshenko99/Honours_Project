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
    TextView welcomeSmile;
    Button profile, singlePlayer, multiPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        welcomeSmile = findViewById(R.id.welcomeSmile);
        welcomeSmile.setText("Hello" + new String(Character.toChars(0x1F63A)));
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
}