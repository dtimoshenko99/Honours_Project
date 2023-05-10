package abertay.uad.ac.uk.myapplication;

import android.util.Log;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HelperFunctions{
    GameInit gameInit;
    GameLogic gameLogic;

    String TAG = "onTap";

    public HelperFunctions(GameInit gameInit, GameLogic gameLogic){
        this.gameInit = gameInit;
        this.gameLogic = gameLogic;
    }

    public List<int[]> getCapturePositions(GameTurnManager.Player currentPlayer) {
        List<int[]> capturePositions = new ArrayList<>();
        int[] rowOffsets = {-1, -1, 1, 1};
        int[] colOffsets = {-1, 1, -1, 1};
        int[][] boardArray = gameInit.getBoardArray();
        int currentPiece;
        int opponentPiece;

        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                if(boardArray[row][col] != 0){
                    if(currentPlayer.getColor().equals("red")) {
                        currentPiece = 2;
                    } else {
                        currentPiece = 1;
                    }

                    if (boardArray[row][col] == currentPiece) {
                        for (int direction = 0; direction < rowOffsets.length; direction++) {
                            int newRow = row + 2 * rowOffsets[direction];
                            int newCol = col + 2 * colOffsets[direction];
                            if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {

                                int midRow = row + rowOffsets[direction];
                                int midCol = col + colOffsets[direction];
                                int middlePiece = boardArray[midRow][midCol];

                                opponentPiece = currentPiece == 1 ? 2 : 1;
                                // Check if the adjacent square contains an opponent's piece and if the capture square is empty
                                if (middlePiece == opponentPiece && boardArray[newRow][newCol] == 0) {
                                    capturePositions.add(new int[]{newCol, newRow, row , col});
                                }
                            }
                        }
                    }
                }
            }
        }

        return capturePositions;
    }

    void updateNodes(List<TransformableNode> nodesToExclude) {
        TransformableNode[][] nodeArray = gameInit.getNodesArray();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                TransformableNode node = nodeArray[row][col];
                if (node != null && !nodesToExclude.contains(node)) {
                    // Set setSelectable property to false for this node
                    node.setSelectable(false);
                }
            }
        }
    }

    public Vector3 getSquarePosition(int row, int col, List<Vector3> squareWorldPositions) {
        int index = row * 8 + col;
        return squareWorldPositions.get(index);
    }

    // Function to remove a piece from a scene
    void removePieceFromScene(TransformableNode capturedPiece) {
        // Set the renderable of the captured piece to null
        if (capturedPiece != null) {
            TransformableNode renderableNode = capturedPiece;
            Log.d("onTap", "removePieceFromScene: removing node: " + capturedPiece.getName() + "  " + capturedPiece.getParentNode()) ;
            renderableNode.setRenderable(null);
            capturedPiece.getParent().removeChild(capturedPiece);

        }
    }

    public static int[] getRowColFromWorldPosition(Vector3 worldPosition, List<Vector3> squareWorldPositions, int numRows, int numCols) {
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

    private static float euclideanDistance(Vector3 v1, Vector3 v2) {
        float dx = v2.x - v1.x;
        float dy = v2.y - v1.y;
        float dz = v2.z - v1.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // Check if a position is inside the board
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
            return true;
        }else{
            return false;
        }

    }

    boolean isSquareOccupied(int row, int col) {
        int[][] boardArray = gameInit.getBoardArray();
        if(boardArray[row][col] == 0){
            return false;
        }else{
            return true;
        }
    }

    boolean isGameOver(int[][] boardArray) {
        boolean redHasPieces = false;
        boolean blackHasPieces = false;

        // Iterate board Array and see if there are still pieces left
        for (int[] ints : boardArray) {
            for (int piece : ints) {
                if (piece != 0) {
                    // Check if a player has pieces
                    if (piece == 1) {
                        redHasPieces = true;
                    } else if (piece == 2) {
                        blackHasPieces = true;
                    }
                }
            }
        }

        if(blackHasPieces && redHasPieces){
            return false;
        }else if(!blackHasPieces){
            gameLogic.setWinner("red");
            return true;
        }else {
            gameLogic.setWinner("black");
            return true;
        }

    }

    public void updateBoardArrayFromPositions(int wasRow, int wasCol, int nowRow, int nowCol, int capturedRow, int capturedCol, int[][] boardArray) {
        if (wasCol != -1 && wasRow != -1) {
            int piece = boardArray[wasRow][wasCol];
            // Reset the previous square
            boardArray[wasRow][wasCol] = 0;

            // Move the piece to the new square
            boardArray[nowRow][nowCol] = piece;

            // Remove the captured piece
            if (capturedCol != -1 && capturedRow != -1) {
                boardArray[capturedRow][capturedCol] = 0;
            }
        }

        Log.d(TAG, "updateBoardArrayFromPositions: THIS SHOULD BE UPDATED ARRAY" + Arrays.deepToString(boardArray));
        gameInit.setBoardArray(boardArray);
    }

    public void updateNodesArray(TransformableNode[][] nodesArray, int wasRow, int wasCol, int nowRow, int nowCol, int capturedRow, int capturedCol) {
        Log.d("onTap", "updateNodesArray: nodesArray: " + Arrays.deepToString(nodesArray));
        TransformableNode[][] array = nodesArray;
        TransformableNode node = array[wasRow][wasCol];

        Log.d("onTap", "updateNodesArray: " + wasRow + wasCol+ nowRow+nowCol+ capturedRow+capturedCol);
        if (capturedRow != -1 && capturedCol != -1) { // Check if there is a captured node
            removePieceFromScene(array[capturedRow][capturedCol]);
            Log.d(TAG, "updateNodesArray: " + capturedRow + " " + capturedCol);
            array[capturedRow][capturedCol] = null; // Delete the captured node
        }

        if (wasRow != nowRow || wasCol != nowCol) { // Check if the node has moved
            array[nowRow][nowCol] = node; // Move the node to the new position
            array[wasRow][wasCol] = null; // Delete the node from the previous position

        }
        Log.d("onTap", "updateNodesArray: nodesArray: " + Arrays.deepToString(nodesArray));
        gameInit.setNodesArray(array);
    }

    public void updateGameBoard(int[][] boardArray, TransformableNode[][] nodesArray) {

        for (int row = 0; row < boardArray.length; row++) {
            for (int col = 0; col < boardArray[row].length; col++) {
                TransformableNode node = nodesArray[row][col];
//                Log.d("onTap", "updateGameBoard: Row:" + row  + " " + " Col: " + col  + " Node: " + node);
                if (boardArray[row][col] != 0) { // If the square is occupied
                    Log.d(TAG, "updateGameBoard: row:" + row + " col: " + col );
                    Vector3 newPos = getSquarePosition(row, col, gameInit.getSquareWorldPositions());
                    node.setWorldPosition(newPos);
                }
            }
        }
    }
}
