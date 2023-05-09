package abertay.uad.ac.uk.myapplication;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameDatabaseManipulations {

    public boolean bothBoardsPlaced = false;
    public boolean listenerPlaced = false;
    private String TAG = "onTap";
    private FirebaseFirestore db;
    private String lobbyId;
    private GameInit gameInit;
    private MultiplayerGameLogic multiplayerGameLogic;
    public GameDatabaseManipulations(FirebaseFirestore mDb, String mLobbyId, GameInit gameInit, MultiplayerGameLogic multiplayerGameLogic){
        this.db = mDb;
        this.lobbyId = mLobbyId;
        this.gameInit = gameInit;
        this.multiplayerGameLogic = multiplayerGameLogic;
    }

    public void updateBoardPlacedField(boolean isHost, int[][] boardArray, TransformableNode[][] nodeArray){

        DocumentReference docRef = db.collection("games").document(lobbyId);

        Log.d(TAG, "updateBoardPlacedField: isHost: " + isHost);
        // Update the field of isBoardPlaced for users
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        boolean isHostBoardPlaced;
                        boolean isGuestBoardPlaced;
                        Map<String, Object> mapSet = new HashMap<>();
                        if(isHost) {
                            mapSet.put("isHostBoardPlaced", true);
                            isHostBoardPlaced = true;
                            isGuestBoardPlaced = documentSnapshot.getBoolean("isGuestBoardPlaced");
                        }else{
                            mapSet.put("isGuestBoardPlaced", true);
                            isGuestBoardPlaced = true;
                            isHostBoardPlaced = documentSnapshot.getBoolean("isHostBoardPlaced");
                        }
                        docRef.update(mapSet);
                        if(isGuestBoardPlaced && isHostBoardPlaced){
                            Log.d(TAG, "onSuccess: ");
                            List<Integer> boardList;
                            List<Integer> nodesList;
                            boardList = multiplayerGameLogic.helperFunctions.translateBoardArrayToList(boardArray);
                            nodesList = multiplayerGameLogic.helperFunctions.translateNodeArrayToList(nodeArray);
                            docRef.update("boardList", boardList);
                            docRef.update("nodesList" , nodesList);
                        }
                        Log.d(TAG, "onSuccess: Updated Board Placed Fiels");
                    }
                });

    }

//    public void hz(){
//        DocumentReference lobbyRef = db.collection("games").document(lobbyId);
//        lobbyRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
//            @Override
//            public void onSuccess(DocumentSnapshot documentSnapshot) {
//                if (documentSnapshot.exists()) {
//                    String hostId = documentSnapshot.getString("hostUserId");
//                    String guestId = documentSnapshot.getString("guestUserId");
//
//                    lobbyRef.update("boardArray", boardList);
//                    lobbyRef.update("nodesArray", nodeList);
//                    Log.d(TAG, "onSuccess: boardArray: " + boardArray);
//                    Log.d(TAG, "onSuccess: nodeArray" + nodeArray);
//                    // Update the board array in the database for the appropriate user
//                    if (userId.equals(hostId)) {
//                        Log.d(TAG, "onSuccess: updateBoardPlaced  HOST = true");
//                        isHost = true;
//                        lobbyRef.update("isHostBoardPlaced", true).addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.d(TAG, "onFailure: HOST BOARD PLACED UPDAT FAILED");
//                            }
//                        });
//                    } else if (userId.equals(guestId)) {
//                        Log.d(TAG, "onSuccess: updateBoardPlaced  GUEST = true");
//                        isHost = false;
//                        lobbyRef.update("isGuestBoardPlaced", true);
//                    }
//                    // I probably need to pass a turn here
//                    boardPlacedListener(isHost);
//                }
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.d(TAG, "onFailure: FAILURE : " + e);
//            }
//        });
//    }

    public void boardPlacedListener(boolean isHost) {

        if(!listenerPlaced){
            listenerPlaced = true;
        }
        db.collection("games").document(lobbyId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {

                            List<List<TransformableNode>> nodeList = (List<List<TransformableNode>>) snapshot.get("nodesArray");

                            TransformableNode[][] nodeArray = new TransformableNode[nodeList.size()][nodeList.get(0).size()];
                            for (int i = 0; i < nodeList.size(); i++) {
                                for (int j = 0; j < nodeList.get(i).size(); j++) {
                                    nodeArray[i][j] = nodeList.get(i).get(j);
                                }
                            }
                            int[][] boardArray = (int[][]) snapshot.get("boardArray");
                            TransformableNode[][] nodesArray = (TransformableNode[][]) snapshot.get("nodesArray");
                            // Do something with the isHostBoardPlaced value
                            arraysUpdatedInDatabase(nodesArray, boardArray);

                        } else {
                            Log.d(TAG, "Current data: null");
                        }
                    }
                });
    }

    public void arraysUpdatedInDatabase(TransformableNode[][] nodesArray, int[][] boardArray){
        gameInit.setBoardArray(boardArray);
        gameInit.setNodesArray(nodesArray);

    }

    public void updateArrays(int[][] boardArray, TransformableNode[][] nodesArray, boolean updateTurn, String switchTurnTo) {
        DocumentReference docRef = db.collection("games").document(lobbyId);
        Map<String, int[][]> boardMap = new HashMap<>();
        boardMap.put("boardArray", boardArray);
        if(updateTurn){
            Map<String, String> updateTurnInDb = new HashMap<>();
            updateTurnInDb.put("turn", switchTurnTo);
        }
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

    public void placeBoardArrayListener() {

    }

    private void updateBoardFromDatabase(String lobbyId) {
        db.collection("lobbies").document(lobbyId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            // Get the boardArray data from the document snapshot
                            int[][] boardArray = (int[][]) documentSnapshot.get("boardArray");
                            TransformableNode[][] nodesArray = (TransformableNode[][]) documentSnapshot.get("nodesArray");
                            // Call a function to update the game board with the new data
                            gameInit.setNodesArray(nodesArray);
                            gameInit.setBoardArray(boardArray);
                            multiplayerGameLogic.updateBoard(boardArray, nodesArray);
                        } else {
                            Log.d(TAG, "Current data: null");
                        }
                    }});
    }

//    public void setTurnListener(String lobbyId){
//        // Create a reference to the document that contains the turn value
//        DocumentReference turnRef = db.collection("lobbies").document(lobbyId);
//
//// Add a listener to the turn value field
//        turnRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
//            @Override
//            public void onEvent(@Nullable DocumentSnapshot snapshot,
//                                @Nullable FirebaseFirestoreException e) {
//                if (e != null) {
//                    Log.w(TAG, "Listen failed.", e);
//                    return;
//                }
//
//                // Check if the snapshot exists and has the turn value field
//                if (snapshot != null && snapshot.exists()) {
//                    String currentPlayer = snapshot.getString("currentPlayer");
//
//                    // Check if it's the current player's turn
//                    if (currentPlayer.equals(playerId) && turn != isPlayerTurn) {
//                        // If it's the current player's turn, update the local turn value
//                        isPlayerTurn = turn;
//                        // Do something
//                    }
//                } else {
//                    Log.d(TAG, "Current data: null");
//                }
//            }
//        });
//    }


}
