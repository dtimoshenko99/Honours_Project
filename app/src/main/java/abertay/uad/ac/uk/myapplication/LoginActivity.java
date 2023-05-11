
package abertay.uad.ac.uk.myapplication;

import static android.widget.Toast.LENGTH_SHORT;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.checkerframework.checker.nullness.qual.NonNull;


import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    SharedPreferences shared;
    SharedPreferences.Editor editor;
    private static final int REQ_ONE_TAP = 2;  // Can be any integer unique to the Activity.
    private boolean showOneTapUI = true;

    EditText email, password;
    String emailText, passwordText;
    TextView createButton;
    private FirebaseAuth auth;
    Button loginButton;

    FirebaseFirestore firestoreDB;
    FirebaseUser user;
    String patt = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private BeginSignInRequest signUpRequest;
    SignInButton googleSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        shared = getSharedPreferences("details", Context.MODE_PRIVATE);
        editor = shared.edit();
        checkUser();
        boolean signedin = isSignedIn();

        setContentView(R.layout.activity_login);

        googleSignInButton = findViewById(R.id.sign_in_button);
        oneTapClient = Identity.getSignInClient(this);
        signUpRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        // Your server's client ID, not your Android client ID.
                        .setServerClientId(getString(R.string.web_client_id))
                        // Show all accounts on the device.
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();
        
        googleSignInButton.setOnClickListener(v ->{
           googleSignIn();
        });

        loginButton = findViewById(R.id.loginButton);

        firestoreDB = FirebaseFirestore.getInstance();

        email = findViewById(R.id.textEmail);
        password = findViewById(R.id.textPassword);
        createButton = findViewById(R.id.createAccount);

        createButton.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        loginButton.setOnClickListener(view -> signIn());

    }

    private void googleSignIn() {
        oneTapClient.beginSignIn(signUpRequest)
                .addOnSuccessListener(this, new OnSuccessListener<BeginSignInResult>() {
                    @Override
                    public void onSuccess(BeginSignInResult result) {
                        try {
                            startIntentSenderForResult(
                                    result.getPendingIntent().getIntentSender(), REQ_ONE_TAP,
                                    null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e("onTap", "Couldn't start One Tap UI: " + e.getLocalizedMessage());
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // No saved credentials found. Launch the One Tap sign-up flow, or
                        // do nothing and continue presenting the signed-out UI.
                        Log.d("onTap", e.getLocalizedMessage());
                    }
                });
    }
    private boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(this) != null;
    }

    private void checkUser() {
        if (user != null) {
            firestoreDB = FirebaseFirestore.getInstance();
            firestoreDB.collection("users").whereEqualTo("email", user.getEmail()).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            QuerySnapshot doc = task.getResult();
                            String username = Objects.requireNonNull(doc.getDocuments().get(0).get("username")).toString();
                            Log.d("onTap", "getdisplay name" + user.getDisplayName());
                            Log.d("onTap", "checkUser: " + (user.getDisplayName().isEmpty()));
                            editor.putString("username", username);
                            editor.apply();
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
            auth.signInWithEmailAndPassword(emailText, passwordText)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            editor.putString("email", emailText).apply();
                            startActivity(new Intent(this, MainMenuActivity.class));
                        }
                    }).addOnFailureListener(e -> {
                Toast.makeText(this, "Authentication failed.",
                        LENGTH_SHORT).show();
                Toast.makeText(this, "SignIn()" + e.getLocalizedMessage(), LENGTH_SHORT).show();
            });
        }
    }

    @Override
    protected void onActivityResult ( int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_ONE_TAP:
                try {
                    SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                    String idToken = credential.getGoogleIdToken();
                    if (idToken !=  null) {
                        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                        auth.signInWithCredential(firebaseCredential)
                                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>(){
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            checkAndCreate(credential, idToken);
                                        } else {
                                            // If sign in fails, display a message to the user.
                                            Log.w("onTap", "signInWithCredential:failure", task.getException());
                                        }
                                    }
                                });
                    }
                } catch (ApiException e) {
                    switch (e.getStatusCode()) {
                        case CommonStatusCodes.CANCELED:
                            // Don't re-prompt the user.
                            showOneTapUI = false;
                            break;
                        case CommonStatusCodes.NETWORK_ERROR:
                            // Try again or just ignore.
                            break;
                        default:
                            break;
                    }
                }
                break;
        }

    }

    private void checkAndCreate(SignInCredential credential, String idToken){
        String displayName = credential.getDisplayName();
        String email = credential.getId();
        editor.putString("email", email).apply();
        firestoreDB.collection("users").whereEqualTo("email", email)
                .get().addOnCompleteListener(task1 -> {
            if(task1.isSuccessful() && task1.getResult().isEmpty()){
//                                progress.setVisibility(View.INVISIBLE);
                Intent i = new Intent(LoginActivity.this, FacebookUsername.class);
                i.putExtra("uID", idToken);
                i.putExtra("userEmail", email);
                i.putExtra("name", displayName);
                editor.putString("email", email).apply();
                startActivity(i);
            }else if(task1.isSuccessful() && !task1.getResult().isEmpty()){
                QuerySnapshot doc = task1.getResult();
                String username = doc.getDocuments().get(0).get("username").toString();
                editor.putString("username", username).apply();
                startActivity(new Intent(LoginActivity.this, MainMenuActivity.class));
            }
        }).addOnFailureListener(e -> Toast.makeText(LoginActivity.this, ""+e.getLocalizedMessage(), LENGTH_SHORT).show());
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