package abertay.uad.ac.uk.myapplication;

import static android.widget.Toast.LENGTH_SHORT;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;
import com.facebook.login.Login;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.filament.utils.Utils;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    SharedPreferences shared;
    SharedPreferences.Editor editor;

    EditText email, password;
    String emailText, passwordText;
    TextView createButton;

    private FirebaseAuth auth;
    LoginButton faceBookLogin;
    CallbackManager callbackManager;
    Button loginButton, googlePlayLogin;
    FirebaseFirestore firestoreDB;
    FirebaseUser user;
    String patt = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    String passPatt = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        shared = getSharedPreferences("details", Context.MODE_PRIVATE);
        editor = shared.edit();
        checkUser();
        setContentView(R.layout.activity_login);

        loginButton = findViewById(R.id.loginButton);

        callbackManager = CallbackManager.Factory.create();

        faceBookLogin = findViewById(R.id.fbButton);
        faceBookLogin.setReadPermissions(Arrays.asList("public_profile", "email"));

        googlePlayLogin = findViewById(R.id.gpButton);

        firestoreDB = FirebaseFirestore.getInstance();

        email = findViewById(R.id.textEmail);
        password = findViewById(R.id.textPassword);
        createButton = findViewById(R.id.createAccount);

        createButton.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        loginButton.setOnClickListener(view -> signIn());

        faceBookLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                facebookLoginAccessToken(loginResult.getAccessToken());
//                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancel() {

//                progress.setVisibility(View.INVISIBLE);
            }
            @Override
            public void onError(@NonNull FacebookException error) {
//                progress.setVisibility(View.INVISIBLE);
                Toast.makeText(LoginActivity.this, "Error occurred, try again!", LENGTH_SHORT).show();
            }
        });

    }

    private void checkUser() {
        if (user != null) {
            firestoreDB = FirebaseFirestore.getInstance();
            firestoreDB.collection("users").whereEqualTo("email", user.getEmail()).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            QuerySnapshot doc = task.getResult();
                            String username = Objects.requireNonNull(doc.getDocuments().get(0).get("username")).toString();
                            editor.putString("username", username).apply();
                            startActivity(new Intent(LoginActivity.this, MainMenuActivity.class));
                        }
                    }).addOnFailureListener(e -> Toast.makeText(this, "CheckUser()" + e.getLocalizedMessage(), LENGTH_SHORT).show());

        } else if (user == null) {
            editor.putString("username", "notset");
            editor.putString("email", "notset");
        }
    }


    private void signIn() {
        emailText = email.getText().toString();
        passwordText = password.getText().toString();
        if (!emailText.matches(patt)) {
            email.setError("Please enter correct email.");
        } else if (passwordText.isEmpty()) {
            password.setError("Please enter your password.");
        } else {
            // ADD a progress here
//            progress.setVisibility(View.VISIBLE);

            auth.signInWithEmailAndPassword(emailText, passwordText)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            editor.putString("email", emailText).apply();
//                            progress.setVisibility(View.INVISIBLE);
                            startActivity(new Intent(this, MainMenuActivity.class));
                        }
                    }).addOnFailureListener(e -> {
//                progress.setVisibility(View.INVISIBLE);
                Toast.makeText(this, "Authentication failed.",
                        LENGTH_SHORT).show();
                Toast.makeText(this, "SignIn()" + e.getLocalizedMessage(), LENGTH_SHORT).show();
            });
        }
    }

    private void facebookLoginAccessToken(AccessToken token) {

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = auth.getCurrentUser();
                        String id = user.getUid();
                        String email = user.getEmail();
                        String name = user.getDisplayName();
                        editor.putString("email", email).apply();
                        firestoreDB.collection("users").whereEqualTo("email", email)
                                .get().addOnCompleteListener(task1 -> {
                            if(task1.isSuccessful() && task1.getResult().isEmpty()){
//                                progress.setVisibility(View.INVISIBLE);
                                Intent i = new Intent(LoginActivity.this, FacebookUsername.class);
                                i.putExtra("uID", id);
                                i.putExtra("userEmail", email);
                                i.putExtra("name", name);
                                editor.putString("email", email).apply();
                                startActivity(i);
                            }else if(task1.isSuccessful() && !task1.getResult().isEmpty()){
                                QuerySnapshot doc = task1.getResult();
                                String username = doc.getDocuments().get(0).get("username").toString();
                                editor.putString("username", username).apply();
                                startActivity(new Intent(LoginActivity.this, MainMenuActivity.class));
                            }
                        }).addOnFailureListener(e -> Toast.makeText(LoginActivity.this, ""+e.getLocalizedMessage(), LENGTH_SHORT).show());


                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    protected void onActivityResult ( int requestCode, int resultCode, Intent data){
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
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