package abertay.uad.ac.uk.myapplication;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameDatabaseManipulations {

    public boolean bothBoardsPlaced = false;
    public boolean userReady = false;
    public boolean gameStarted = false;
    private boolean boardListenerActive = false;
    private boolean placeListenerPlaced = false;
    public ListenerRegistration boardPlaceListener, turnChangeListener;
    private final String TAG = "onTap";
    private final FirebaseFirestore db;
    private final String lobbyId;
    private final GameInit gameInit;
    private final MultiplayerGameLogic multiplayerGameLogic;
    public GameDatabaseManipulations(FirebaseFirestore mDb, String mLobbyId, GameInit gameInit, MultiplayerGameLogic multiplayerGameLogic){
        this.db = mDb;
        this.lobbyId = mLobbyId;
        this.gameInit = gameInit;
        this.multiplayerGameLogic = multiplayerGameLogic;
    }

    public void updateBoardPlacedField(boolean isHost) {
        DocumentReference docRef = db.collection("games").document(lobbyId);

        // Update the field of isBoardPlaced for users
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Map<String, Object> mapSet = new HashMap<>();
                if (isHost && !userReady) {
                    mapSet.put("isHostBoardPlaced", true);
                    docRef.update(mapSet);
                    userReady = true;
                } else if (!isHost && !userReady) {
                    mapSet.put("isGuestBoardPlaced", true);
                    docRef.update(mapSet);
                    userReady = true;
                }
                docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        // Check if both boards are placed before adding the listener
                        if (documentSnapshot.getBoolean("isGuestBoardPlaced") && documentSnapshot.getBoolean("isHostBoardPlaced")) {
                            bothBoardsPlaced = true;
                            gameStarted = true;
                            multiplayerGameLogic.gameStart();
                        } else {
                            placeListener();
                        }
                    }
                });
            }
        });
    }

    private void placeListener() {
        if (placeListenerPlaced || bothBoardsPlaced) {
            return;
        }

        DocumentReference docRef = db.collection("games").document(lobbyId);
        boardPlaceListener = docRef.addSnapshotListener((snapshot, e) -> {
            if (snapshot.getBoolean("isGuestBoardPlaced") && snapshot.getBoolean("isHostBoardPlaced")) {
                gameStarted = true;
                multiplayerGameLogic.gameStart();
            }
        });

        placeListenerPlaced = true;
    }

    public void updateArrays(int[] updateFrom, int[] updateTo, int[] capturedAt, boolean switchTurn, String switchTurnTo ,boolean hasCaptures) {
        int wasRow = updateFrom[0];
        int wasCol = updateFrom[1];
        int nowRow = updateTo[0];
        int nowCol = updateTo[1];
        int capturedRow = capturedAt[0];
        int capturedCol = capturedAt[1];
        Map<String, Object> updateFields = new HashMap<>();
        if(switchTurn){
            updateFields.put("turn", switchTurnTo);
        }

        updateFields.put("wasRow", wasRow);
        updateFields.put("wasCol", wasCol);
        updateFields.put("nowRow", nowRow);
        updateFields.put("nowCol", nowCol);
        updateFields.put("hasCaptures", hasCaptures);
        updateFields.put("capturedRow", capturedRow);
        updateFields.put("capturedCol", capturedCol);

        db.collection("games").document(lobbyId)
                .update(updateFields).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });
    }

    public void listenerToUpdateBoard() {
        if (boardListenerActive) {
            return;
        }

        turnChangeListener = db.collection("games").document(lobbyId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        int wasRow = snapshot.getLong("wasRow").intValue();
                        int wasCol = snapshot.getLong("wasCol").intValue();
                        int nowRow = snapshot.getLong("nowRow").intValue();
                        int nowCol = snapshot.getLong("nowCol").intValue();
                        int capturedRow = snapshot.getLong("capturedRow").intValue();
                        int capturedCol = snapshot.getLong("capturedCol").intValue();
                        String turn = snapshot.getString("turn");
                        String state = snapshot.getString("state");
                        if (state.equals("finished")) {
                            Intent intent = new Intent(gameInit.getContext(), EndGameActivity.class);
                            String winner = turn.equals("red") ? "white" : "red";
                            intent.putExtra("winner", winner);
                            intent.putExtra("type", "multiplayer");
                            gameInit.getContext().startActivity(intent);
                        } else if(wasCol != -1 && wasRow != -1 && turn.equals(multiplayerGameLogic.turnManager.getUserColor())) {
                            multiplayerGameLogic.helperFunctions.updateNodesArray(gameInit.getNodesArray(), wasRow, wasCol,
                                    nowRow, nowCol, capturedRow, capturedCol);
                            multiplayerGameLogic.helperFunctions.updateBoardArrayFromPositions(wasRow, wasCol, nowRow, nowCol, capturedRow, capturedCol, gameInit.getBoardArray());
                            multiplayerGameLogic.helperFunctions.updateGameBoard(gameInit.getBoardArray(), gameInit.getNodesArray());
                            multiplayerGameLogic.turnManager.switchTurn();
                            multiplayerGameLogic.turnManager.UpdateSelectableNodesMultiplayer(gameInit.getArFragment(), multiplayerGameLogic.isHost);
                            multiplayerGameLogic.gameLogic.checkForAvailableCapturesOnStartTurn();
                        }
                    } else {
                        Log.d(TAG, "Current data: null");
                    }
                });
    }

    public void notifyFinished() {
        db.collection("games").document(lobbyId).update("state", "finished");
    }
}
