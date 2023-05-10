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
    private GameLogic gameLogic;
    private GameInit gameInit;
    public GameDatabaseManipulations databaseManipulations;
    public HelperFunctions helperFunctions;
    private boolean nodeSelected = false;
    public boolean gameStarted = false;
    private TransformableNode selectedNode;
    private Vector3 pickUpPosition;
    public String winner;
    private int middleRow, middleCol;
    List<int[]> capturePositions = new ArrayList<>();
    List<TransformableNode> nodesToExclude = new ArrayList<>();

    // Variables to hold state of the game
    private boolean lastTurnWasCapture = false;
    private boolean captured = false;
    private boolean userHasCaptures = false;
    private boolean wronglyPlacedPiece = false;
    private boolean samePlace = false;
    private int[] captureAt = new int[2];
    private int[] returnTo = new int[2];

    private final String TAG = "onTap";

    public MultiplayerGameLogic(GameTurnManager turnManager, GameLogic gameLogic, GameInit gameInit, FirebaseFirestore db, String lobby, boolean isHost){
        this.turnManager = turnManager;
        this.isHost = isHost;
        this.gameLogic = gameLogic;
        this.gameInit = gameInit;
        this.databaseManipulations = new GameDatabaseManipulations(db, lobby, gameInit, this);
        this.helperFunctions = new HelperFunctions(gameInit, gameLogic);
    }

    public void gameStart(int[][] boardArray, TransformableNode[][] nodesArray) {
        // Update database with User ID and
        if(!databaseManipulations.bothBoardsPlaced){
            databaseManipulations.updateBoardPlacedField(isHost, boardArray, nodesArray);
            Log.d(TAG, "gameStart: Updated Boardplaced Field");
        }else if(databaseManipulations.gameStarted) {
            if (databaseManipulations.boardPlaceListener != null) {
                databaseManipulations.boardPlaceListener.remove();
            }
            turnManager.UpdateSelectableNodesMultiplayer(gameInit.getArFragment(), isHost);
            Log.d(TAG, "gameStart: This is not triggered, am I right?");
            String userColor = turnManager.getUserColor();
            Log.d(TAG, "gameStart: " + turnManager.getUserColor());
            if (turnManager.getUserColor().equals("red") && databaseManipulations.turnChangeListener == null) {
                Log.d(TAG, "checkAndUpdate: TURN CHANGE LISTENER NOT NULL ASSIGNING NEW LISTENER");
                databaseManipulations.listenerToUpdateBoard();
            }
        }

    }

    private List<Vector3> squareWorldPositions;

    public void pieceDroppedMultiplayer(TransformableNode node){

        squareWorldPositions = gameInit.getSquareWorldPositions();

        Vector3 worldLastKnown = node.getWorldPosition();
        Vector3 worldSquarePos = gameLogic.getClosestSquarePosition(worldLastKnown);

        int[] colAndRow = HelperFunctions.getRowColFromWorldPosition(worldLastKnown, squareWorldPositions, 8,8);
        int[] pickupRowAndCol = HelperFunctions.getRowColFromWorldPosition(pickUpPosition, squareWorldPositions, 8,8);

        boolean isInsideBoard = helperFunctions.isInsideBoard(worldLastKnown);
        boolean isTurnValid = isValidMove(pickupRowAndCol[1], pickupRowAndCol[0], colAndRow[1], colAndRow[0], selectedNode);

        checkAndUpdate(isTurnValid, isInsideBoard, pickupRowAndCol, colAndRow, worldSquarePos);
    }

    public void checkAndUpdate(boolean isTurnValid, boolean isInsideBoard, int[] pickupRowAndCol, int[] colAndRow, Vector3 worldSquarePos){
        // Check if the move was valid
        if(!isTurnValid || !isInsideBoard){
            selectedNode.setLocalPosition(new Vector3(0,0,0));
            // Set the right position to return to
            if(samePlace){
                Toast.makeText(gameInit.getContext(), "This is not valid, please move diagonaly", Toast.LENGTH_SHORT).show();
                samePlace = false;
            }
            else if(!wronglyPlacedPiece) {
                returnTo[0] = pickupRowAndCol[1];
                returnTo[1] = pickupRowAndCol[0];
                gameInit.redHighlightNode.setEnabled(true);
                gameInit.redHighlightNode.setWorldPosition(new Vector3(pickUpPosition.x, pickUpPosition.y - 0.02f, pickUpPosition.z));
                wronglyPlacedPiece = true;
                Toast.makeText(gameInit.getContext(), "This is not a valid move, please place the piece back.", Toast.LENGTH_SHORT).show();

            }else if(colAndRow[1] == returnTo[0] && colAndRow[0] == returnTo[1] && wronglyPlacedPiece){
                gameInit.redHighlightNode.setEnabled(false);
                returnTo[0] = 0;
                returnTo[1] = 0;
                wronglyPlacedPiece = false;
                Toast.makeText(gameInit.getContext(), "Thank you.", Toast.LENGTH_SHORT).show();
            }
        }else if(!wronglyPlacedPiece){
            gameInit.redHighlightNode.setEnabled(false);
            capturePositions.clear();
            for(TransformableNode node : gameInit.greenHighlightArray){
                node.setEnabled(false);
            }
//            if(hasCaptures(colAndRow[1], colAndRow[0], selectedNode) && lastTurnWasCapture){
//                Log.d(TAG, "checkAndUpdate: HAS CAPTURES, isHost:" + isHost);
//                selectedNode.setWorldPosition(worldSquarePos);
//                updateBoardStartArray(pickupRowAndCol[1], pickupRowAndCol[0] ,colAndRow[1], colAndRow[0], middleCol, middleRow);
//
//                int[] capturedAt = new int[2];
//                if(captured){
//                    capturedAt[0] = middleRow;
//                    capturedAt[1] = middleCol;
//                }else{
//                    capturedAt[0] = -1;
//                    capturedAt[1] = -1;
//                }
//                databaseManipulations.updateArrays(gameInit.getBoardArray(), pickupRowAndCol,colAndRow, capturedAt,false, null);
//
//            }else {
                userHasCaptures = false;

                // Does have one more to capture, but updating board anyway, as the turn wast taken
                // Update board array
                // Switches the turn and updates the corresponding node's setSelectable value
                Log.d(TAG, "checkAndUpdate: Color before update:" + turnManager.currentPlayer.getColor());
                turnManager.switchTurn();
                turnManager.UpdateSelectableNodesMultiplayer(gameInit.getArFragment(), isHost);
                Log.d(TAG, "checkAndUpdate: Color after update:" + turnManager.currentPlayer.getColor());
                // Update board array
                updateBoardStartArray(pickupRowAndCol[1], pickupRowAndCol[0] ,colAndRow[1], colAndRow[0], middleCol, middleRow);


                int[] capturedAt = new int[2];
                if(captured){
                    capturedAt[0] = middleRow;
                    capturedAt[1] = middleCol;
                }else{
                    capturedAt[0] = -1;
                    capturedAt[1] = -1;
                }
                databaseManipulations.updateArrays(gameInit.getBoardArray(), pickupRowAndCol,colAndRow, capturedAt, true,turnManager.currentPlayer.getColor());


                if(helperFunctions.isGameOver(gameInit.getBoardArray())){
                    Context context = gameInit.getContext();
                    Intent intent = new Intent(context, EndGameActivity.class);
                    intent.putExtra("winner", winner);
                    context.startActivity(intent);
                }else{
                    if (databaseManipulations.turnChangeListener == null) {
                        Log.d(TAG, "checkAndUpdate: TURN CHANGE LISTENER NOT NULL ASSIGNING NEW LISTENER");
                        databaseManipulations.listenerToUpdateBoard();
                    }
                }
            }
//        }
    }

    public void checkForAvailableCapturesOnStartTurn(){

        capturePositions = helperFunctions.getCapturePositions(turnManager.currentPlayer);
        List<Vector3> turnPos = new ArrayList<>();

        if(!capturePositions.isEmpty()) {
            for (int[] position : capturePositions) {
                Log.d(TAG, "checkAndUpdate: " + Arrays.toString(position));
                int currentRow = position[2];
                int currentCol = position[3];
                TransformableNode[][] nodeArray = gameInit.getNodesArray();
                nodesToExclude.add(nodeArray[currentRow][currentCol]);
                Vector3 squarePos = helperFunctions.getSquarePosition(position[1], position[0], gameInit.getSquareWorldPositions());
                if (squarePos != null) {
                    turnPos.add(squarePos);
                }
            }
            helperFunctions.updateNodes(nodesToExclude);
            if (!turnPos.isEmpty()) {
                for (int i = 0; i < turnPos.size(); i++) {
                    gameInit.greenHighlightArray[i].setEnabled(true);
                    gameInit.greenHighlightArray[i].setWorldPosition(turnPos.get(i));
                }
            }
            Log.d(TAG, "checkAndUpdate: " + (capturePositions));
        }

    }


    public boolean onTouchMethodMultiplayer(HitTestResult hitTestResult, MotionEvent motionEvent){
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
                        pickUpPosition = new Vector3(selectedNode.getWorldPosition());
                        break;
                    case MotionEvent.ACTION_MOVE:
//                        isInsideBoard(selectedNode.getWorldPosition());
                        break;
                    case MotionEvent.ACTION_UP:
                        pieceDroppedMultiplayer(selectedNode);
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

    private void updateBoardStartArray(int initialCol, int initialRow, int destCol, int destRow, int middleCol, int middleRow) {

        TransformableNode[][] nodesArray = gameInit.getNodesArray();
        int[][] boardArray = gameInit.getBoardArray();
        // Get the piece value from the initial square

        int pieceValue = boardArray[initialRow][initialCol];
        TransformableNode nodeValue = nodesArray[initialRow][initialCol];

        if(captured){
            boardArray[middleRow][middleCol] = 0;
            nodesArray[middleRow][middleCol] = null;
        }
        // Set the initial square to 0 (unoccupied)
        boardArray[initialRow][initialCol] = 0;
        nodesArray[initialRow][initialCol] = null;

        // Update the destination square with the moved piece value and update array in GameInit
        nodesArray[destRow][destCol] = nodeValue;
        gameInit.setNodesArray(nodesArray);
        boardArray[destRow][destCol] = pieceValue;
        gameInit.setBoardArray(boardArray);

        // Update both arrays here


    }

    public boolean hasCaptures(int col, int row, TransformableNode node) {
        int[] rowOffsets = {-1, -1, 1, 1};
        int[] colOffsets = {-1, 1, -1, 1};
        int[][] boardArray = gameInit.getBoardArray();
        int currentPiece;
        int opponentPiece;
        if(node.getName() == "redPiece"){
            currentPiece = 2;
        }else{
            currentPiece = 1;
        }

        // Iterate through all possible capture directions
        for (int direction = 0; direction < rowOffsets.length; direction++) {
            int newRow = row + 2 * rowOffsets[direction];
            int newCol = col + 2 * colOffsets[direction];
            if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {

                int midRow = row + rowOffsets[direction];
                int midCol = col + colOffsets[direction];
                int middlePiece = boardArray[midRow][midCol];

                if(currentPiece == 1){
                    opponentPiece = 2;
                }else{
                    opponentPiece = 1;
                }
                // Check if the adjacent square contains an opponent's piece and if the capture square is empty
                if (middlePiece == opponentPiece && boardArray[newRow][newCol] == 0 && lastTurnWasCapture) {
                    userHasCaptures = true;
                    nodesToExclude.clear();
                    nodesToExclude.add(node);
                    helperFunctions.updateNodes(nodesToExclude);
                    gameInit.greenHighlightNode.setEnabled(true);
                    gameInit.greenHighlightNode.setWorldPosition(helperFunctions.getSquarePosition(newRow, newCol, gameInit.getSquareWorldPositions()));
                    captureAt[0] = newRow;
                    captureAt[1] = newCol;
                    return true;
                }
            }

        }
        return false;
    }

    private boolean isValidMove(int initialCol, int initialRow, int destCol, int destRow, Node node) {
        int[][] boardArray = gameInit.getBoardArray();
        int rowDiff = Math.abs(destRow - initialRow);
        int colDiff = Math.abs(destCol - initialCol);
        middleRow = (initialRow + destRow) / 2;
        middleCol = (initialCol + destCol) / 2;
        int opponentPiece;
        if(node.getName().equals("redPiece")){
            opponentPiece = 1;
        }else{
            opponentPiece = 2;
        }

        if(!capturePositions.isEmpty()){
            for(int[] captures : capturePositions){
                if(captures[0] == destCol && captures[1] == destRow) {
                    captured = true;
                    lastTurnWasCapture = true;
                    Log.d(TAG, "isValidMove: Get middle and delete");
                    TransformableNode[][] nodeArray = gameInit.getNodesArray();
                    helperFunctions.removePieceFromScene(nodeArray[middleRow][middleCol]);
                    return true;
                }
            }
            if(rowDiff == 0 && colDiff == 0){
                samePlace = true;
                return false;
            }
            return false;
        }

        if(userHasCaptures && destCol != captureAt[1] && destRow != captureAt[0]){
            Log.d(TAG, "isValidMove: " + destCol + " " + destRow);
            Log.d(TAG, "isValidMove: " + captureAt[1] + " " + captureAt[0]);
            return false;
        }

        if(rowDiff == 0 && colDiff == 0){
            Log.d(TAG, "isValidMove: Same place turn");
            samePlace = true;
            return false;
        }

        // Check if the move is diagonal
        if (rowDiff != colDiff) {
            Log.d(TAG, "isValidMove: Move is not diagonal");
            Log.d(TAG, "isValidMove: row dif " + rowDiff + " col dif: " + colDiff);
            return false;
        }

        // Check if the move length is valid (1 or 2 squares)
        if (rowDiff > 2 || colDiff > 2) {
            Log.d(TAG, "isValidMove: move is more than 1 or 2 squares in length");
            return false;
        }

        // Check if the destination square is occupied
        if (helperFunctions.isSquareOccupied(destRow, destCol)) {
            Log.d(TAG, "isValidMove: Space is OCCUPIED: " + destRow + "  " + destCol);
            // If the move is 1 square long, it's invalid because the destination is occupied
            if (rowDiff == 1 && colDiff == 1) {
                Log.d(TAG, "isValidMove: the move is invalid, as the space is occupied");
                return false;
            }
            // If the move is 2 squares long, it's invalid because the destination is occupied
            else if (rowDiff == 2 && colDiff == 2) {
                Log.d(TAG, "isValidMove: the move is invalid, as the space is occupied (2 squares)");
                return false;
            }
        }

        // If the move is 2 squares long, check if the player can capture the piece
        if (rowDiff == 2 && colDiff == 2) {


            // Check if the piece in the middle square belongs to the opponent
            if (boardArray[middleRow][middleCol] != opponentPiece) {
                Log.d(TAG, "isValidMove: Middle array value:" + boardArray[middleRow][middleCol]);
                Log.d(TAG, "isValidMove: Opponent piece" + opponentPiece);
                Log.d(TAG, "isValidMove: middle: " + middleRow + " " + middleCol);
                captured = false;
                return false;
            }
            else{
                // Get the node in the middle and delete it from the board
                captured = true;
                lastTurnWasCapture = true;
                Log.d(TAG, "isValidMove: Get middle and delete");
                TransformableNode[][] nodeArray = gameInit.getNodesArray();
                helperFunctions.removePieceFromScene(nodeArray[middleRow][middleCol]);
                return true;
            }
        }
        lastTurnWasCapture = false;
        return true;

    }

    // TODO: need to update the board
    public void updateBoard(int[][] boardArray, TransformableNode[][] nodesArray){

    }

}
