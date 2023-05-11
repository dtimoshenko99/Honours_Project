package abertay.uad.ac.uk.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class EndGameActivity extends AppCompatActivity {

    TextView winnerField;
    Button mainMenuButton, openGamesButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_game);

        mainMenuButton = findViewById(R.id.mainMenuButton);
        openGamesButton = findViewById(R.id.playAgainButton);
        winnerField = findViewById(R.id.winnerTextView);

        Intent intent = getIntent();
        String winner = intent.getStringExtra("winner");
        String type = intent.getStringExtra("type");
        if(winner.equals("red")){
            winnerField.setText("Red" + new String(Character.toChars(0x1F61D)));
        }else{
            winnerField.setText("White" + new String(Character.toChars(0x1F609)));
        }
        if(type.equals("singleplayer")){
            openGamesButton.setOnClickListener(v -> {startActivity(new Intent(this, SinglePlayerActivity.class));});
        } else{
            openGamesButton.setOnClickListener(v -> {startActivity(new Intent(this, OpenGamesActivity.class));});
        }

        mainMenuButton.setOnClickListener(v -> {startActivity(new Intent(this, MainMenuActivity.class));});
    }

    @Override
    public void onBackPressed() {
        // Do nothing
    }
}