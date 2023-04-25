package abertay.uad.ac.uk.myapplication;

import static com.google.ar.sceneform.math.Vector3.back;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.android.ads.mediationtestsuite.activities.HomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    SharedPreferences shared;
    SharedPreferences.Editor editor;

    FirebaseAuth auth;
    FirebaseUser user;
    FirebaseFirestore firestoreDB;
    Button signOut, deleteUser, updateUser;
    String username, uid, usernameInput, email;

    EditText input;
    ImageView backButton;
    String usernamePatt = "[^A-Za-z0-9]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        shared = getSharedPreferences("details", Context.MODE_PRIVATE);
        editor = shared.edit();

        username = shared.getString("username", "username");
        email = shared.getString("email", "email");

        auth = FirebaseAuth.getInstance();
        firestoreDB = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        deleteUser = findViewById(R.id.deleteBttn);
        updateUser = findViewById(R.id.buttonUpdate);
        signOut = findViewById(R.id.signOutBttn);
        backButton = findViewById(R.id.arrowBackProfile);
        input = findViewById(R.id.inputUsername);
        input.setText(username);
        usernameInput = input.getText().toString();

        signOut.setOnClickListener(v -> signOut());

        backButton.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, MainMenuActivity.class)));

        updateUser.setOnClickListener(v -> updateUsername());

        deleteUser.setOnClickListener(v -> new AlertDialog.Builder(ProfileActivity.this)
        .setTitle("Confirmation")
        .setMessage("Are you sure you want to delete your account?")
        .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteUserandGames()).setNegativeButton(android.R.string.no, null)
        .show());
    }

    private void deleteUserandGames() {
        firestoreDB.collection("users").whereEqualTo("username", username).get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        QuerySnapshot doc = task.getResult();
                        String id = doc.getDocuments().get(0).getId();
                        firestoreDB.collection("users").document(id).delete()
                                .addOnSuccessListener(unused -> firestoreDB.collection("games").whereEqualTo("email",
                                        email)
                                        .get().addOnSuccessListener(queryDocumentSnapshots -> {
                                            if(!queryDocumentSnapshots.isEmpty()){
                                                WriteBatch batch = firestoreDB.batch();
                                                for (DocumentSnapshot doc1 : queryDocumentSnapshots) {
                                                    batch.delete(doc1.getReference());
                                                }
                                                batch.commit();
                                                user.delete();
                                                LoginManager.getInstance().logOut();
                                            }else {
                                                user.delete();
                                                LoginManager.getInstance().logOut();
                                                startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                                            }
                                        }).addOnFailureListener(e -> Toast.makeText(this, "Error occurred, try again!", Toast.LENGTH_SHORT).show())).addOnFailureListener(e -> Toast.makeText(this, "Error occurred, try again!", Toast.LENGTH_SHORT).show());
                    }
                }).addOnFailureListener(e -> Toast.makeText(this, "Error occurred, try again!", Toast.LENGTH_SHORT).show());

    }


    private void updateUsername() {
        if(usernameInput.isEmpty()){
            input.setError("Please fill this field.");
        }else if(usernameInput.matches(usernamePatt)){
            input.setError("Please use only numbers and letters.");
        }else{
            editor.putString("username", input.getText().toString()).apply();
            Map<String, Object> map = new HashMap<>();

            map.put("username", input.getText().toString());

            firestoreDB.collection("users").whereEqualTo("username", username).get().addOnCompleteListener(idTask ->
            {
               QuerySnapshot snap = idTask.getResult();
               uid = snap.getDocuments().get(0).getId();

               firestoreDB.collection("users").document(uid).set(map, SetOptions.merge()).addOnSuccessListener(unused -> {
                   Toast.makeText(ProfileActivity.this, "Username updated", Toast.LENGTH_LONG);
                   startActivity(new Intent(this, MainMenuActivity.class));
               });
            });

        }
    }

    private void signOut() {
        new AlertDialog.Builder(this)
                .setTitle("Decision")
                .setMessage("Are you sure you want to sighn out?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    auth.signOut();
                    LoginManager.getInstance().logOut();
                    startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                }).setNegativeButton(android.R.string.no, null)
                .show();
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