package abertay.uad.ac.uk.myapplication;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GameDatabaseManipulations {

    private String TAG = "onTap";
    private FirebaseFirestore db;
    private String lobbyId;
    public GameDatabaseManipulations(FirebaseFirestore mDb, String mLobbyId){
        this.db = mDb;
        this.lobbyId = mLobbyId;
    }

    public void updateArrays(int[][] boardArray, TransformableNode[][] nodesArray) {
        DocumentReference docRef = db.collection("games").document(lobbyId);
        Map<String, int[][]> boardMap = new HashMap<>();
        boardMap.put("boardArray", boardArray);
        docRef.set(boardMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Map<String, TransformableNode[][]> nodeMap = new HashMap<>();
                nodeMap.put("nodesArray", nodesArray);
                docRef.set(nodeMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Success, board and nodes updated in DB");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: nodeArray updateFailed:" + e);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: Boardarray updateFailed: " + e);
            }
        });
    }
}
