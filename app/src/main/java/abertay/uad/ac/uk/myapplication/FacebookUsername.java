package abertay.uad.ac.uk.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
//import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Map;

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
        nameWelcome = findViewById(R.id.nameForWelcome);
        getIntentFunction();

        // Add code here
        nameWelcome.setText("Welcome, "+name);
        Log.d("onTap", "onCreate: " + email + name + id);

        usernameRegister.setOnClickListener(v -> {
//            progress.setVisibility(View.VISIBLE);
            // FUNCTION TO CHECK IF USERNAME ALREADY EXISTS
            checkUsername();
        });

        // ON ARROW BACK SIGN USER OUT AND SEND THEM BACK TO LOGIN SCREEN
//        ImageView arrow = findViewById(R.id.usernameBackArrow);
//        arrow.setOnClickListener(v -> {
//            auth.signOut();
//            LoginManager.getInstance().logOut();
//            startActivity(new Intent(RegistrationUsername.this, LoginActivity.class));
//        });
        
    }

    private void getIntentFunction() {
        Intent signUp = getIntent();
        email = signUp.getStringExtra("userEmail");
        name = signUp.getStringExtra("name");
        id = signUp.getStringExtra("uID");
    }

    private void insertUser() {
        // CREATE MAP OF VALUES TO INSERT
        Map<String, Object> user = new HashMap<>();
        user.put("username", usernameField.getText().toString());
        user.put("userID", id);
        user.put("email", email);
        firestoreDB.collection("users").document(id).set(user).addOnSuccessListener(unused -> {
            editor.putString("username", "notset").apply();
            editor.putString("username", usernameField.getText().toString()).apply();
            startActivity(new Intent(this,  MainMenuActivity.class));
//            progress.setVisibility(View.INVISIBLE);
        }).addOnFailureListener(e -> {
//            progress.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Sorry, try again!", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkUsername() {
        firestoreDB.collection("users").whereEqualTo("username", usernameField.getText().toString())
                .get().addOnCompleteListener(task -> {
            if(task.isSuccessful() && task.getResult().isEmpty()){
                insertUser();
            }else{
                usernameField.setError("Username already exists!");
//                progress.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        try{
            super.onRestoreInstanceState(savedInstanceState);
        }catch(Exception e){
            savedInstanceState = null;
        }
    }
}