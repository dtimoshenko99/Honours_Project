package abertay.uad.ac.uk.myapplication;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiplayerGameLogic {
    public boolean isHost;
    public GameTurnManager turnManager;
    public GameLogic gameLogic;
    private GameInit gameInit;
    public GameDatabaseManipulations databaseManipulations;
    public HelperFunctions helperFunctions;
    private boolean nodeSelected = false;
    public TransformableNode selectedNode;
    private Vector3 pickUpPosition;

    public MultiplayerGameLogic(GameTurnManager turnManager, GameLogic gameLogic, GameInit gameInit, FirebaseFirestore db, String lobby, boolean isHost){
        this.turnManager = turnManager;
        this.isHost = isHost;
        this.gameLogic = gameLogic;
        this.gameInit = gameInit;
        this.databaseManipulations = new GameDatabaseManipulations(db, lobby, gameInit, this);
        this.helperFunctions = new HelperFunctions(gameInit, gameLogic);
    }

    public void gameStart() {

        // Check if both boards are not placed
        if (!databaseManipulations.bothBoardsPlaced) {
            // Update the board placed field in the database
            databaseManipulations.updateBoardPlacedField(isHost);
        }
        // Check if the game has started
        else if (databaseManipulations.gameStarted) {
            // Check if the board place listener exists and remove it
            if (databaseManipulations.boardPlaceListener != null) {
                databaseManipulations.boardPlaceListener.remove();
            }
            // Update the selectable nodes based on the turn and the host/guest status
            turnManager.UpdateSelectableNodesMultiplayer(gameInit.getArFragment(), isHost);
            // Start listening for board updates
            databaseManipulations.listenerToUpdateBoard();
        }
    }

    public void pieceDroppedMultiplayer(TransformableNode node) {
        // Get the world position of the dropped node
        Vector3 worldLastKnown = node.getWorldPosition();

        // Get the closest square position to the dropped node
        Vector3 worldSquarePos = gameLogic.getClosestSquarePosition(worldLastKnown);

        // Get the column and row indices of the pickup and drop positions
        int[] colAndRow = helperFunctions.getRowColFromWorldPosition(worldLastKnown, gameInit.getSquareWorldPositions(), 8, 8);
        int[] pickupRowAndCol = helperFunctions.getRowColFromWorldPosition(pickUpPosition, gameInit.getSquareWorldPositions(), 8, 8);

        // Check if the dropped node is inside the board
        boolean isInsideBoard = helperFunctions.isInsideBoard(worldLastKnown);

        // Check if the turn is valid
        boolean isTurnValid = gameLogic.isValidMove(pickupRowAndCol[1], pickupRowAndCol[0], colAndRow[1], colAndRow[0], selectedNode);

        // Check and update the game state based on the turn
        checkAndUpdateInMultiplayer(isTurnValid, isInsideBoard, pickupRowAndCol, colAndRow);
    }


    public void checkAndUpdateInMultiplayer(boolean isTurnValid, boolean isInsideBoard, int[] pickupRowAndCol, int[] colAndRow) {
        // Check if the move was invalid or outside the board
        if (!isTurnValid || !isInsideBoard) {
            // Reset the position of the selected node
            selectedNode.setLocalPosition(new Vector3(0, 0, 0));

            // Set the right position to return to
            if (gameLogic.samePlace) {
                Toast.makeText(gameInit.getContext(), "This is not valid, please move diagonally", Toast.LENGTH_SHORT).show();
                gameLogic.samePlace = false;
            } else if (!gameLogic.wronglyPlacedPiece) {
                // Set the return position and highlight the red highlight node
                gameLogic.returnTo[0] = pickupRowAndCol[1];
                gameLogic.returnTo[1] = pickupRowAndCol[0];
                gameInit.redHighlightNode.setEnabled(true);
                gameInit.redHighlightNode.setWorldPosition(new Vector3(pickUpPosition.x, pickUpPosition.y - 0.03f, pickUpPosition.z));
                gameLogic.wronglyPlacedPiece = true;
                Toast.makeText(gameInit.getContext(), "This is not a valid move, please place the piece back.", Toast.LENGTH_SHORT).show();
            } else if (colAndRow[1] == gameLogic.returnTo[0] && colAndRow[0] == gameLogic.returnTo[1] && gameLogic.wronglyPlacedPiece) {
                // Disable the red highlight node and reset the return position
                gameInit.redHighlightNode.setEnabled(false);
                gameLogic.returnTo[0] = 0;
                gameLogic.returnTo[1] = 0;
                gameLogic.wronglyPlacedPiece = false;
                Toast.makeText(gameInit.getContext(), "Thank you.", Toast.LENGTH_SHORT).show();
            }
        }
        // Valid move
        else if (!gameLogic.wronglyPlacedPiece) {
            // Disable the red highlight node
            if (gameInit.redHighlightNode != null) {
                gameInit.redHighlightNode.setEnabled(false);
            }
            // Clear capture positions and disable green highlight nodes
            if (!gameLogic.capturePositions.isEmpty()) {
                gameLogic.capturePositions.clear();
            }
            if (gameInit.greenHighlightArray.length != 0) {
                for (TransformableNode node : gameInit.greenHighlightArray) {
                    node.setEnabled(false);
                }
            }
//             Check if there are captures and the last turn was a capture
            if (gameLogic.hasCaptures(colAndRow[1], colAndRow[0], selectedNode) && gameLogic.lastTurnWasCapture) {
                // Move the selected node to the destination square and update the board
                gameLogic.updateBoardStartArray(pickupRowAndCol[1], pickupRowAndCol[0], colAndRow[1], colAndRow[0], gameLogic.middleCol, gameLogic.middleRow);
                // Update arrays in the database
                databaseManipulations.updateArrays(pickupRowAndCol, colAndRow, gameLogic.capturedAt, false, null, gameLogic.userHasCaptures);
            } else {
                gameLogic.userHasCaptures = false;

                // Switch the turn and update selectable nodes
                turnManager.switchTurn();
                turnManager.UpdateSelectableNodesMultiplayer(gameInit.getArFragment(), isHost);

                // Update board array
                gameLogic.updateBoardStartArray(pickupRowAndCol[1], pickupRowAndCol[0], colAndRow[1], colAndRow[0], gameLogic.middleCol, gameLogic.middleRow);

                // Update database
                databaseManipulations.updateArrays(pickupRowAndCol, colAndRow, gameLogic.capturedAt, true, turnManager.currentPlayer.getColor(), false);

                // Set the variable controlling turn to false
                gameLogic.captured = false;

                // Clear an array holding positions of available captures
                gameLogic.capturePositions.clear();

                // Check if the game is over
                if (helperFunctions.isGameOver(gameInit.getBoardArray())) {
                    databaseManipulations.turnChangeListener.remove();
                    Context context = gameInit.getContext();
                    Intent intent = new Intent(context, EndGameActivity.class);
                    String color = turnManager.getUserColor();
                    String winner = color.equals("red") ? "white" : "red";
                    intent.putExtra("winner", winner);
                    intent.putExtra("type", "multiplayer");
                    context.startActivity(intent);
                } else {
                    // Check if the turn change listener is null and update the board listener
                    if (databaseManipulations.turnChangeListener == null) {
                        databaseManipulations.listenerToUpdateBoard();
                    }
                }

            }
        }
    }

    public boolean onTouchMethodMultiplayer(HitTestResult hitTestResult, MotionEvent motionEvent) {
        Node node = hitTestResult.getNode();

        if (node instanceof TransformableNode) {
            String nodeName = node.getName();
            boolean moveAllowed = turnManager.isMoveAllowed(nodeName);

            if (moveAllowed) {
                if (!nodeSelected) {
                    selectedNode = (TransformableNode) node;
                    nodeSelected = true;
                }

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Store the initial position when the touch is pressed
                        pickUpPosition = new Vector3(selectedNode.getWorldPosition());
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // Handle motion while moving the piece
//                        if(selectedNode.getWorldPosition().y < gameInit.getBoardNode().getWorldPosition().y + 0.03f || selectedNode.getWorldPosition().y > gameInit.getBoardNode().getWorldPosition().y + 0.06f){
//                            selectedNode.setWorldPosition(new Vector3(selectedNode.getWorldPosition().x, gameInit.getBoardNode().getWorldPosition().y + 0.05f, selectedNode.getWorldPosition().z));
//                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // Drop the piece when the touch is released and check if it is on top of the board
//                        selectedNode.setWorldPosition(new Vector3(selectedNode.getWorldPosition().x, gameInit.getBoardNode().getWorldPosition().y + 0.05f, selectedNode.getWorldPosition().z));
                        pieceDroppedMultiplayer(selectedNode);
                        helperFunctions.updateGameBoard(gameInit.getBoardArray(), gameInit.getNodesArray());
                        nodeSelected = false;
                        break;
                }
            } else {
                nodeSelected = false;
            }

            return true;
        }

        return false;
    }

}
