package abertay.uad.ac.uk.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FacebookUsername extends AppCompatActivity {

    SharedPreferences shared;
    SharedPreferences.Editor editor;

    Button usernameRegister;
    EditText usernameField;
    String email, name, id;
    TextView nameWelcome;

    FirebaseAuth auth;
    FirebaseFirestore firestoreDB;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facebook_username);

        shared = getSharedPreferences("detail", Context.MODE_PRIVATE);
        editor = shared.edit();
        
        auth = FirebaseAuth.getInstance();
        firestoreDB = FirebaseFirestore.getInstance();
        
        usernameField = findViewById(R.id.usernameFieldRegister);
        usernameRegister = findViewById(R.id.registerUsername);
        
        getIntentFunction();
        
    }

    private void getIntentFunction() {
    }
}