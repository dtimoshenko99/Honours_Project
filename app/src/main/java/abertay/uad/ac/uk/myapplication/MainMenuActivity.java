package abertay.uad.ac.uk.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainMenuActivity extends AppCompatActivity {


    Button profile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        profile = findViewById(R.id.profileButton);
        profile.setOnClickListener(v ->{
            startActivity(new Intent(this, ProfileActivity.class));
        });


    }

    // TODO: Need to implement last game retrieval from DB into this activity and show it.
}