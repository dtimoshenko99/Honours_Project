package abertay.uad.ac.uk.myapplication;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameLogic {

    private boolean nodeSelected = false;
    private TransformableNode selectedNode;
    private Vector3 pickUpPosition;
    private final GameTurnManager turnManager;
    private HelperFunctions helperFunctions;
    private final GameInit gameInit;
    public String winner;
    public int middleRow, middleCol;
    public int[] capturedAt = new int[2];
    List<int[]> capturePositions = new ArrayList<>();
    List<TransformableNode> nodesToExclude = new ArrayList<>();
    public Vector3 worldSquarePos;

    // Variables to hold state of the game
    public boolean lastTurnWasCapture = false;
    public boolean captured = false;
    public boolean userHasCaptures = false;
    public boolean wronglyPlacedPiece = false;
    public boolean samePlace = false;
    private int[] captureAt = new int[2];
    public int[] returnTo = new int[2];
    private List<TransformableNode> kingNodes = new ArrayList<>();


    public GameLogic(GameTurnManager turnManager, GameInit gameInit) {
        this.turnManager = turnManager;
        this.gameInit = gameInit;
        this.helperFunctions = new HelperFunctions(gameInit, this);
    }

    public void setWinner(String winner){
        this.winner = winner;
    }

    private List<Vector3> squareWorldPositions;

    public void pieceDropped(TransformableNode node) {
        // Get the world positions of the game board squares
        squareWorldPositions = gameInit.getSquareWorldPositions();

        // Get the last known world position of the dropped node
        Vector3 worldLastKnown = node.getWorldPosition();

        // Get the closest square position to the dropped node
        worldSquarePos = getClosestSquarePosition(worldLastKnown);
        // Get the column and row indices of the dropped position
        int[] colAndRow = helperFunctions.getRowColFromWorldPosition(worldLastKnown, squareWorldPositions, 8, 8);

        // Get the column and row indices of the pickup position
        int[] pickupRowAndCol = helperFunctions.getRowColFromWorldPosition(pickUpPosition, squareWorldPositions, 8, 8);

        // Check if the dropped position is inside the game board
        boolean isInsideBoard = helperFunctions.isInsideBoard(worldLastKnown);

        // Check if the move from pickup position to dropped position is valid
        boolean isTurnValid = isValidMove(pickupRowAndCol[1], pickupRowAndCol[0], colAndRow[1], colAndRow[0], selectedNode);

        // Check and update the game state based on the move validity and position
        checkAndUpdate(isTurnValid, isInsideBoard, pickupRowAndCol, colAndRow);
    }

    public void checkAndUpdate(boolean isTurnValid, boolean isInsideBoard, int[] pickupRowAndCol, int[] colAndRow) {
        // Check if the move was invalid
        if (!isTurnValid || !isInsideBoard) {
            // Set the right position to return to
            if (samePlace) {
                Toast.makeText(gameInit.getContext(), "This is not valid, please move diagonally", Toast.LENGTH_SHORT).show();
                selectedNode.setLocalPosition(new Vector3(0,0.01f, 0));
                samePlace = false;
            } else if (!wronglyPlacedPiece) {
                // Store the pickup position to return the piece to
                returnTo[0] = pickupRowAndCol[1];
                returnTo[1] = pickupRowAndCol[0];
                // Show the red highlight node at the pickup position
                gameInit.redHighlightNode.setEnabled(true);
                gameInit.redHighlightNode.setWorldPosition(new Vector3(pickUpPosition.x, pickUpPosition.y - 0.02f, pickUpPosition.z));
                selectedNode.setLocalPosition(new Vector3(0,0.01f, 0));
                wronglyPlacedPiece = true;
                Toast.makeText(gameInit.getContext(), "This is not a valid move, please place the piece back.", Toast.LENGTH_SHORT).show();
            } else if (colAndRow[1] == returnTo[0] && colAndRow[0] == returnTo[1] && wronglyPlacedPiece) {
                // Hide the red highlight node and reset the return position
                gameInit.redHighlightNode.setEnabled(false);
                returnTo[0] = 0;
                returnTo[1] = 0;
                wronglyPlacedPiece = false;
                selectedNode.setLocalPosition(new Vector3(0,0.01f, 0));
                Toast.makeText(gameInit.getContext(), "Thank you.", Toast.LENGTH_SHORT).show();
            }
        } else if (!wronglyPlacedPiece) {
            // Reset the red highlight node and capture positions
            if (gameInit.redHighlightNode != null) {
                gameInit.redHighlightNode.setEnabled(false);
            }
            if (!capturePositions.isEmpty()) {
                capturePositions.clear();
            }
            // Disable green highlight nodes
            if (gameInit.greenHighlightArray.length != 0) {
                for (TransformableNode node : gameInit.greenHighlightArray) {
                    node.setEnabled(false);

                }
            }

            if (hasCaptures(colAndRow[1], colAndRow[0], selectedNode) && lastTurnWasCapture) {
                // Update the position of the selected node and update the board array for a capture move
                updateBoardStartArray(pickupRowAndCol[1], pickupRowAndCol[0], colAndRow[1], colAndRow[0], middleCol, middleRow);
            } else {
                userHasCaptures = false;

                // Update the position of the selected node and switch the turn
                turnManager.switchTurnAndUpdateSelectableNodes(gameInit.getArFragment());
                updateBoardStartArray(pickupRowAndCol[1], pickupRowAndCol[0], colAndRow[1], colAndRow[0], middleCol, middleRow);
                // Update the turn indicator
                turnManager.updateTurnIndicator();

                // Check for available captures for the new turn
                checkForAvailableCapturesOnStartTurn();

                // Check if the game is over
                if(helperFunctions.isGameOver(gameInit.getBoardArray())){
                    Context context = gameInit.getContext();
                    Intent intent = new Intent(context, EndGameActivity.class);
                    if(gameInit.gameType.equals("SinglePlayer")){
                        String winner = turnManager.currentPlayer.getColor().equals("red") ? "white" : "red";
                        intent.putExtra("winner", winner);
                    }else{
                        intent.putExtra("winner", winner);
                    }
                    intent.putExtra("type", "singleplayer");
                    context.startActivity(intent);
                }
            }
        }
    }

    public void checkForAvailableCapturesOnStartTurn() {

        // Get the capture positions for the current player
        capturePositions = helperFunctions.getCapturePositions(turnManager.currentPlayer);
        List<Vector3> turnPos = new ArrayList<>();

        if (!capturePositions.isEmpty()) {
            // Iterate over each capture position
            for (int[] position : capturePositions) {

                // Get the row and column of the current position
                int currentRow = position[2];
                int currentCol = position[3];

                // Get the node array from GameInit
                TransformableNode[][] nodeArray = gameInit.getNodesArray();

                // Add the current node to the nodesToExclude list
                nodesToExclude.add(nodeArray[currentRow][currentCol]);

                // Get the world position of the square corresponding to the capture position
                Vector3 squarePos = helperFunctions.getSquarePosition(position[1], position[0], gameInit.getSquareWorldPositions());

                // Add the square position to the turnPos list if it exists
                if (squarePos != null) {
                    turnPos.add(squarePos);
                }
            }

            // Update the nodes to exclude in the helper functions
            helperFunctions.updateNodes(nodesToExclude);

            if (!turnPos.isEmpty()) {
                // Enable the green highlight nodes and set their world positions
                for (int i = 0; i < turnPos.size(); i++) {
                    gameInit.greenHighlightArray[i].setEnabled(true);
                    gameInit.greenHighlightArray[i].setWorldPosition(turnPos.get(i));
                }
            }
        }
    }



    // Custom onTouchMethod
    public boolean onTouchMethod(HitTestResult hitTestResult, MotionEvent motionEvent) {
        Node node = hitTestResult.getNode();

        // Check if the node is a TransformableNode
        if (node instanceof TransformableNode) {
            String nodeName = node.getName();

            // Check if the move is allowed for the current player
            boolean moveAllowed = turnManager.isMoveAllowed(nodeName);

            if (moveAllowed) {
                if (!nodeSelected) {
                    // Set the selected node if no node is currently selected
                    selectedNode = (TransformableNode) node;
                    nodeSelected = true;
                }

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Store the pickup position when the node is touched
                        pickUpPosition = new Vector3(selectedNode.getWorldPosition());
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // Additional logic for when the node is being moved (not included in the commented code)
                        break;
                    case MotionEvent.ACTION_UP:
                        // Perform actions when the node is released/dropped
                        Log.d("onTap", "onTouchMethod: WORLD:" + selectedNode.getWorldPosition());
                        pieceDropped(selectedNode);
                        helperFunctions.updateGameBoard(gameInit.getBoardArray(), gameInit.getNodesArray());
                        ArSceneView scene = gameInit.getArFragment().getArSceneView();

                        scene.getUpdatedPlanes();
                        Log.d("onTap", "onTouchMethod: WORLD V2:" + selectedNode.getWorldPosition());
                        nodeSelected = false;
                        break;
                }
            } else {
                // Reset the node selection if the move is not allowed for the current player
                nodeSelected = false;
            }

            return true;
        }

        // Return false if the node is not a TransformableNode
        return false;
    }

    public void updateBoardStartArray(int initialCol, int initialRow, int destCol, int destRow, int middleCol, int middleRow) {
        // Get the nodes array and board array from GameInit
        TransformableNode[][] nodesArray = gameInit.getNodesArray();
        int[][] boardArray = gameInit.getBoardArray();

        // Get the piece value and node from the initial square
        int pieceValue = boardArray[initialRow][initialCol];
        TransformableNode nodeValue = nodesArray[initialRow][initialCol];

        // Check if a piece was captured during the move
        if (captured) {
            // Clear the captured piece from the board and nodes array
            boardArray[middleRow][middleCol] = 0;
            nodesArray[middleRow][middleCol] = null;
        }

        // Set the initial square to 0 (unoccupied) in the board array and null in the nodes array
        boardArray[initialRow][initialCol] = 0;
        nodesArray[initialRow][initialCol] = null;

        // Update the destination square with the moved piece value and update the arrays in GameInit
        nodesArray[destRow][destCol] = nodeValue;
        gameInit.setNodesArray(nodesArray);
        boardArray[destRow][destCol] = pieceValue;
        gameInit.setBoardArray(boardArray);
    }

    Vector3 getClosestSquarePosition(Vector3 pieceWorldPosition) {
        // Get the world positions of the game board squares
        squareWorldPositions = gameInit.getSquareWorldPositions();

        // Set the threshold distance to determine the closest square
        double threshold = 0.15;

        Vector3 closestSquare;

        // Iterate through each square world position
        for (Vector3 squareWorldPos : squareWorldPositions) {

            // Calculate the Euclidean distance between the piece and the square position
            double distance = Math.sqrt(Math.pow(pieceWorldPosition.x - squareWorldPos.x, 2)
                    + Math.pow(pieceWorldPosition.y - squareWorldPos.y, 2)
                    + Math.pow(pieceWorldPosition.z - squareWorldPos.z, 2));

            // Check if the distance is within the threshold
            if (distance < threshold) {
                closestSquare = squareWorldPos;
                return closestSquare;
            }
        }

        // If no closest square is found within the threshold, return the pickup position as a fallback
        return pickUpPosition;
    }

    public boolean hasCaptures(int col, int row, TransformableNode node) {
        int[] rowOffsets = {-1, -1, 1, 1};
        int[] colOffsets = {-1, 1, -1, 1};
        int[][] boardArray = gameInit.getBoardArray();
        int currentPiece;
        int opponentPiece;

        // Determine the current piece type based on the node name
        if (node.getName().equals("redPiece")) {
            currentPiece = 2;
        } else {
            currentPiece = 1;
        }

        // Iterate through all possible capture directions
        for (int direction = 0; direction < rowOffsets.length; direction++) {
            int newRow = row + 2 * rowOffsets[direction];
            int newCol = col + 2 * colOffsets[direction];

            // Check if the new position is within the board boundaries
            if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                int midRow = row + rowOffsets[direction];
                int midCol = col + colOffsets[direction];
                int middlePiece = boardArray[midRow][midCol];

                // Determine the opponent's piece type
                if (currentPiece == 1) {
                    opponentPiece = 2;
                } else {
                    opponentPiece = 1;
                }

                // Check if the adjacent square contains an opponent's piece and the capture square is empty
                if (middlePiece == opponentPiece && boardArray[newRow][newCol] == 0 && lastTurnWasCapture) {
                    userHasCaptures = true;
                    nodesToExclude.clear();
                    nodesToExclude.add(node);
                    helperFunctions.updateNodes(nodesToExclude);

                    // Enable the green highlight node and set its position to the capture square
                    if (gameInit.greenHighlightNode != null) {
                        gameInit.greenHighlightNode.setEnabled(true);
                        gameInit.greenHighlightNode.setWorldPosition(helperFunctions.getSquarePosition(newRow, newCol, gameInit.getSquareWorldPositions()));
                    }

                    captureAt[0] = newRow;
                    captureAt[1] = newCol;
                    return true;
                }
            }
        }

        return false;
    }


    public boolean isValidMove(int initialCol, int initialRow, int destCol, int destRow, Node node) {
        int[][] boardArray = gameInit.getBoardArray();
        int rowDiff = Math.abs(destRow - initialRow);
        int colDiff = Math.abs(destCol - initialCol);
        middleRow = (initialRow + destRow) / 2;
        middleCol = (initialCol + destCol) / 2;
        int allowedDirection = turnManager.currentPlayer == GameTurnManager.Player.WHITE ? 1 : -1;
        int moveDirection = destRow - initialRow;
        int opponentPiece;
        boolean selectedIsKingNode = false;
        if(node.getName().equals("redPiece")){
            opponentPiece = 1;
        }else{
            opponentPiece = 2;
        }

        if(destRow == 7 && !kingNodes.contains(selectedNode) && allowedDirection == 1){
            kingNodes.add(selectedNode);
        }else if(destRow == 0 && !kingNodes.contains(selectedNode) && allowedDirection == -1){
            kingNodes.add(selectedNode);
        }

        if(kingNodes.contains(selectedNode)){
            selectedIsKingNode = true;
        }

        // If at the start of the round user has enemy pieces to capture and doesn't do it the move is invalid
        // If he does do it, but the move is not diagonal the move is invalid
        // If evrything is right - delete nodes and return that the move is valid
        if(!capturePositions.isEmpty()){
            for(int[] captures : capturePositions){
                if(captures[0] == destCol && captures[1] == destRow) {
                    if(rowDiff != colDiff){
                        return false;
                    }else if(rowDiff == 1 && colDiff == 1){
                        return false;
                    }
                    captured = true;
                    lastTurnWasCapture = true;
                    TransformableNode[][] nodeArray = gameInit.getNodesArray();
                    helperFunctions.removePieceFromScene(nodeArray[middleRow][middleCol]);
                    capturedAt[0] = middleRow;
                    capturedAt[1] = middleCol;
                    if(destRow == 7){
                        kingNodes.add(selectedNode);
                    }
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
            if(rowDiff != colDiff){
                return false;
            }
            return false;
        }

        if(rowDiff == 0 && colDiff == 0){
            samePlace = true;
            return false;
        }

        // Check if the move is diagonal
        if (rowDiff != colDiff) {
            return false;
        }

        // Check if the move length is valid (1 or 2 squares)
        if (rowDiff > 2 || colDiff > 2) {
            return false;
        }

        // Check if the destination square is occupied
        if (helperFunctions.isSquareOccupied(destRow, destCol)) {
            // If the move is 1 square long, it's invalid because the destination is occupied
            if (rowDiff == 1 && colDiff == 1) {
                return false;
            }
            // If the move is 2 squares long, it's invalid because the destination is occupied
            else if (rowDiff == 2 && colDiff == 2) {
                return false;
            }
        }

        // If the move is 2 squares long, check if the player can capture the piece
        if (rowDiff == 2 && colDiff == 2) {


            // Check if the piece in the middle square belongs to the opponent
            if (boardArray[middleRow][middleCol] != opponentPiece) {
                captured = false;
                return false;
            }
            else{
                // Get the node in the middle and delete it from the board
                captured = true;
                lastTurnWasCapture = true;
                TransformableNode[][] nodeArray = gameInit.getNodesArray();
                helperFunctions.removePieceFromScene(nodeArray[middleRow][middleCol]);
                capturedAt[0] = middleRow;
                capturedAt[1] = middleCol;
                if(destRow == 7){
                    kingNodes.add(selectedNode);
                }
                return true;
            }
        }

        if(moveDirection * allowedDirection <= 0 && !selectedIsKingNode){
            return false;
        }

        capturedAt[0] = -1;
        capturedAt[1] = -1;
        lastTurnWasCapture = false;

        return true;

    }


}