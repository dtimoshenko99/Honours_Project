package abertay.uad.ac.uk.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;

import com.google.ar.core.HitResult;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Handler;

public class GameLogic {

    private boolean nodeSelected = false;
    private TransformableNode selectedNode;
    private Vector3 pickUpPosition;
    private AnchorNode initialAnchorNode;
    private GameTurnManager turnManager;
    private GameInit gameInit;
    private String winner;

    private Vector3 worldLastKnown;
    private final String TAG = "onTap";

    public GameLogic(GameTurnManager turnManager, GameInit gameInit) {
        this.turnManager = turnManager;
        this.gameInit = gameInit;
    }

    private List<Vector3> squareWorldPositions;

    public void pieceDropped(TransformableNode node){

        squareWorldPositions = gameInit.getSquareWorldPositions();
        worldLastKnown = node.getWorldPosition();
        Vector3 worldSquarePos = getClosestSquarePosition(worldLastKnown);
        Vector3 pickUpSquarePos = getClosestSquarePosition(pickUpPosition);
        int[] colAndRow = getRowColFromWorldPosition(worldLastKnown, squareWorldPositions, 8,8);
        Log.d(TAG, "onTouch: ROW AND COL" + colAndRow[0] + ", " + colAndRow[1]);

        int[] pickupRowAndCol = getRowColFromWorldPosition(pickUpPosition, squareWorldPositions, 8,8);
        // Check if the move was valid
        boolean isValid = isValidMove(pickupRowAndCol[1], pickupRowAndCol[0], colAndRow[1], colAndRow[0]);

        if(!isValid){
            selectedNode.setParent(initialAnchorNode);
            selectedNode.setLocalPosition(new Vector3(0,0,0));
        }else{
            Log.d(TAG, "pieceDropped: isValid?; " + isValid);
            node.setWorldPosition(worldSquarePos);
            updateBoardStartArray(pickupRowAndCol[1], pickupRowAndCol[0], colAndRow[1], colAndRow[0]);

            // Switches the turn and updates the corresponding node's setSelectable value
            turnManager.switchTurnAndUpdateSelectableNodes(gameInit.getArFragment());

            // Update textview with the text of whose turn it is
            turnManager.updateTurnIndicator();
            initialAnchorNode = null;
            if(isGameOver(gameInit.getBoardArray())){
                Context context = gameInit.getContext();
                Intent intent = new Intent(context, EndGameActivity.class);
                intent.putExtra("winner", "red");
                context.startActivity(intent);
            }
        }
    }

    public boolean onTouchMethod(HitTestResult hitTestResult, MotionEvent motionEvent){
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
                        initialAnchorNode = (AnchorNode) selectedNode.getParent();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        isInsideBoard(selectedNode.getWorldPosition());
                        break;
                    case MotionEvent.ACTION_UP:
                        // TODO: Need to implement here a check if the position is outside the board with the function above - return to pickup without further operations
                        // The function is ready, jsut need to push back the piece if it is out of bounds
//                        isInsideBoard(selectedNode.getWorldPosition());
                        pieceDropped(selectedNode);
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

    private void updateBoardStartArray(int initialCol, int initialRow, int destCol, int destRow) {

        TransformableNode[][] nodesArray = gameInit.getNodesArray();
        int[][] boardArray = gameInit.getBoardArray();
        // Get the piece value from the initial square
        int pieceValue = boardArray[initialRow][initialCol];
        TransformableNode nodeValue = nodesArray[initialRow][initialCol];
        // Set the initial square to 0 (unoccupied)
        boardArray[initialRow][initialCol] = 0;
        nodesArray[initialRow][initialCol] = null;

        // Update the destination square with the moved piece value
        nodesArray[destRow][destCol] = nodeValue;
        gameInit.setNodesArray(nodesArray);
        boardArray[destRow][destCol] = pieceValue;
        gameInit.setBoardArray(boardArray);

    }

    boolean isInsideBoard(Vector3 worldPosition) {
        Node boardNode = gameInit.getBoardNode();
        Vector3 boardCenter = boardNode.getWorldPosition();
        float boardWidth = 1.0f;
        float boardLength = 1.0f;

        float halfWidth = boardWidth / 2;
        float halfLength = boardLength / 2;

        float minX = boardCenter.x - halfWidth;
        float maxX = boardCenter.x + halfWidth;
        float minZ = boardCenter.z - halfLength;
        float maxZ = boardCenter.z + halfLength;

        if((worldPosition.x >= minX && worldPosition.x <= maxX) && (worldPosition.z >= minZ && worldPosition.z <= maxZ)){
            Log.d(TAG, "isInsideBoard: Inside the board");
        }else{
            Log.d(TAG, "isInsideBoard: outside the board");
        }
        return (worldPosition.x >= minX && worldPosition.x <= maxX) && (worldPosition.z >= minZ && worldPosition.z <= maxZ);
    }

    private Vector3 getClosestSquarePosition(Vector3 pieceWorldPosition){
        squareWorldPositions = gameInit.getSquareWorldPositions();
        double threshold = 0.15;
        Vector3 closestSquare;
        //  squareLocalPositionsMap
        for (Vector3 squareWorldPos : squareWorldPositions){

            // Get distance between square and a piece position
            double distance = Math.sqrt(Math.pow(pieceWorldPosition.x - squareWorldPos.x, 2)
                    + Math.pow(pieceWorldPosition.y - squareWorldPos.y, 2)
                    + Math.pow(pieceWorldPosition.z - squareWorldPos.z, 2));

            if (distance < threshold){
                Log.d("onTap", "distance is less: " + squareWorldPos + "Piecepos: " + pieceWorldPosition);
                // Should check which space
                closestSquare = squareWorldPos;
                return closestSquare;

            }

        }
        Log.d("onTap", "returning last known: " + pickUpPosition);
        return pickUpPosition;
    }

    private int[] getRowColFromWorldPosition(Vector3 worldPosition, List<Vector3> squareWorldPositions, int numRows, int numCols) {
        int closestRow = -1;
        int closestCol = -1;
        float minDistance = Float.MAX_VALUE;

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                int index = row * numCols + col;
                Vector3 squarePosition = squareWorldPositions.get(index);
                float distance = euclideanDistance(worldPosition, squarePosition);

                if (distance < minDistance) {
                    minDistance = distance;
                    closestRow = row;
                    closestCol = col;
                }
            }
        }

        return new int[]{closestRow, closestCol};
    }

    private float euclideanDistance(Vector3 v1, Vector3 v2) {
        float dx = v2.x - v1.x;
        float dy = v2.y - v1.y;
        float dz = v2.z - v1.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean isSquareOccupied(int row, int col) {
        int[][] boardArray = gameInit.getBoardArray();
        TransformableNode[][] nodesArr = gameInit.getNodesArray();
        Log.d(TAG, "isSquareOccupied: nodes" + nodesArr[row][col]);
        Log.d(TAG, "isSquareOccupied: " + boardArray[row][col]);
        Log.d(TAG, "isSquareOccupied: " + Arrays.deepToString(boardArray));
        if(boardArray[row][col] == 0){
            return false;
        }else{
            return true;
        }
    }

    public boolean hasLegalMoves() {
        int[] rowOffsets = {-1, -1, 1, 1};
        int[] colOffsets = {-1, 1, -1, 1};
        int[][] boardArray = gameInit.getBoardArray();

        // Iterate through all the squares on the board
        for (int row = 0; row < boardArray.length; row++) {
            for (int col = 0; col < boardArray[row].length; col++) {

                int currentPiece = boardArray[row][col];

                if (currentPiece != 0) {
                    // Iterate through all possible move directions
                    for (int direction = 0; direction < rowOffsets.length; direction++) {
                        int newRow = row + rowOffsets[direction];
                        int newCol = col + colOffsets[direction];

                        // Check if the new position is inside the board
                        if (newRow >= 0 && newRow < boardArray.length && newCol >= 0 && newCol < boardArray[row].length) {

                            int adjacentPiece = boardArray[newRow][newCol];

                            // Check if the adjacent square is empty
                            if (adjacentPiece == 0) {
                                return true;
                            }

                            // Check if the adjacent square contains an opponent's piece
                            if (isOpponentPiece(currentPiece, adjacentPiece)) {
                                int captureRow = newRow + rowOffsets[direction];
                                int captureCol = newCol + colOffsets[direction];

                                // Check if the capture position is inside the board
                                if (captureRow >= 0 && captureRow < boardArray.length && captureCol >= 0 && captureCol < boardArray[row].length) {
                                    // Check if the capture square is empty
                                    if (boardArray[captureRow][captureCol] == 0) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // No legal moves are possible
        return false;
    }


    private boolean isOpponentPiece(int piece1, int piece2) {
        return (piece1 > 0 && piece2 < 0) || (piece1 < 0 && piece2 > 0);
    }

    public boolean isGameOver(int[][] boardArray) {
        boolean redHasPieces = false;
        boolean blackHasPieces = false;
        boolean redHasMoves = false;
        boolean blackHasMoves = false;

        for (int row = 0; row < boardArray.length; row++) {
            for (int col = 0; col < boardArray[row].length; col++) {
                int piece = boardArray[row][col];
                if (piece != 0) {
                    // Check if the player has pieces
                    if (piece == 1) {
                        redHasPieces = true;
                    } else if (piece == 2) {
                        blackHasPieces = true;
                    }

                    // Check if the player has legal captures
                    if (hasLegalMoves()) {
                        if (piece == 1) {
                            redHasMoves = true;
                        } else if (piece == 2) {
                            blackHasMoves = true;
                        }
                    }
                }
            }
        }

        if(blackHasPieces && redHasPieces){
            return false;
        }else if(!blackHasPieces){
            winner = "red";
            return true;
        }else if(!redHasPieces){
            winner = "black";
            return true;
        }else if(blackHasMoves && redHasMoves){
            return false;
        }else if(!blackHasMoves){
            winner = "red";
            return true;
        }else if(!redHasMoves){
            winner = "black";
            return true;
        }

        return false;
    }

    private boolean isValidMove(int initialCol, int initialRow, int destCol, int destRow) {
        int[][] boardArray = gameInit.getBoardArray();
        int rowDiff = Math.abs(destRow - initialRow);
        int colDiff = Math.abs(destCol - initialCol);

        Log.d(TAG, "isValidMove: Initialcol+row: " + initialCol + "  " + initialRow);
        Log.d(TAG, "isValidMove: destCol and Row " + destCol + "  " + destRow);
        Log.d(TAG, "isValidMove: rowDiff" + rowDiff);
        Log.d(TAG, "isValidMove: colDiff" + colDiff);

        // Check if the move is diagonal
        if (rowDiff != colDiff) {
            Log.d(TAG, "isValidMove: Move is not diagonal");
            return false;
        }

        // Check if the move length is valid (1 or 2 squares)
        if (rowDiff > 2 || colDiff > 2) {
            Log.d(TAG, "isValidMove: move is more than 1 or 2 squares in length");
            return false;
        }


        int currentPlayerPiece = boardArray[initialRow][initialCol];
        int opponentPiece = (currentPlayerPiece == 1) ? 2 : 1;

        // Check if the destination square is occupied
        if (isSquareOccupied(destRow, destCol)) {
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
            int middleRow = (initialRow + destRow) / 2;
            int middleCol = (initialCol + destCol) / 2;

            // Check if the piece in the middle square belongs to the opponent
            if (boardArray[middleRow][middleCol] != opponentPiece) {
                Log.d(TAG, "isValidMove:  If the move is 2 squares long, check if the player can capture the piece");
                return false;
            }
            else{
                // Get the node in the middle and delete it from the board
                TransformableNode[][] nodeArray = gameInit.getNodesArray();
                removePieceFromScene(nodeArray[middleRow][middleCol]);
                return true;
            }
        }
        return true;
    }

    // Function to remove a piece from a scene
    private void removePieceFromScene(TransformableNode capturedPiece) {
        // Set the renderable of the captured piece to null
        if (capturedPiece != null) {
            TransformableNode renderableNode = capturedPiece;
            Log.d(TAG, "removePieceFromScene: removing node: " + capturedPiece.getName() + "  " + capturedPiece.getParentNode()) ;
            renderableNode.setRenderable(null);
            capturedPiece.getParent().removeChild(capturedPiece);

        }
    }
}