package abertay.uad.ac.uk.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {


    SharedPreferences shared;
    SharedPreferences.Editor editor;
    EditText email, password, confPassword, username;
    Button signUpButton;
    private FirebaseAuth auth;
    private FirebaseUser user;
    FirebaseFirestore firestoreDB;
//    FirebaseFirestore database;

    String emailPatt = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    String passPatt = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$";
    String usernamePatt = "[^A-Za-z0-9]";

    ImageView backButton;
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // Get database reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference();

        shared = getSharedPreferences("details",
                Context.MODE_PRIVATE);
        editor = shared.edit();

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        email = findViewById(R.id.emailField);
        password = findViewById(R.id.passwordField);
        confPassword = findViewById(R.id.confirmPasswordField);
        username = findViewById(R.id.usernameField);
        signUpButton = findViewById(R.id.signUpButton);
        backButton = findViewById(R.id.signUpBack);
        signUpButton.setOnClickListener(view -> userSignUp());

        firestoreDB = FirebaseFirestore.getInstance();

    }

    private void userSignUp() {
        String emailText = email.getText().toString();
        String passwordText = password.getText().toString();
        String confPasswordText = confPassword.getText().toString();

        if(!emailText.matches(emailPatt)){
            email.setError("Please enter correct email.");
        }else if(passwordText.isEmpty()){
            password.setError("Please enter correct password.");
        }else if(passwordText.equals(confPasswordText)){
            confPassword.setError("Passwords don't match.");
        }else if(!passwordText.matches(passPatt)){
            password.setError("Password needs to have: at least 1 number, 1 lower case, 1 uppercase, one of the following characters: ! @ # $ ( ), 8-20 characters ");
        }else if(username.getText().toString().matches(usernamePatt)){
            username.setError("Only letters and numbers are available.");
        }else{
            progressBar.setVisibility(View.VISIBLE);

            auth.createUserWithEmailAndPassword(emailText, passwordText)
                    .addOnCompleteListener(this, task -> {
                        if(task.isSuccessful()){
                            editor.putString("email", emailText).apply();
                            editor.putString("password", passwordText).apply();
                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            createRecord(uid);
                            startActivity(new Intent(SignUpActivity.this, MainMenuActivity.class));
                        }else{

                        }
                    });

        }

    }

    private void createRecord(String uid) {
        Map<String, Object> user = new HashMap<>();
        String usernameText = username.getText().toString();
        String emailText = email.getText().toString();
        user.put("email", emailText);
        user.put("userID", uid);
        user.put("username", usernameText);

        firestoreDB.collection("users").document(uid).set(user).addOnSuccessListener(unused -> {
            Toast.makeText(SignUpActivity.this, "Success", Toast.LENGTH_SHORT).show();
            Intent main = new Intent(SignUpActivity.this, MainMenuActivity.class);
            startActivity(main);
        }).addOnFailureListener(e -> Toast.makeText(SignUpActivity.this, "error" + e, Toast.LENGTH_LONG).show());
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