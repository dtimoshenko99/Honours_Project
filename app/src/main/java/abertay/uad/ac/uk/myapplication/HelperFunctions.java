package abertay.uad.ac.uk.myapplication;

import android.util.Log;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.List;

public class HelperFunctions{
    GameInit gameInit;
    GameLogic gameLogic;

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

    public  Vector3 getSquarePosition(int row, int col, List<Vector3> squareWorldPositions) {
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

    public List<Integer> translateBoardArrayToList(int[][] boardArray) {
        List<Integer> occupiedSquares = new ArrayList<>();

        for (int i = 0; i < boardArray.length; i++) {
            for (int j = 0; j < boardArray[i].length; j++) {
                if (boardArray[i][j] != 0) { // Square is occupied
                    int squareNumber = i * boardArray.length + j; // Calculate square number
                    occupiedSquares.add(squareNumber);
                }
            }
        }

        return occupiedSquares;
    }

    public List<Integer> translateNodeArrayToList(TransformableNode[][] nodesArray) {
        List<Integer> occupiedSquares = new ArrayList<>();

        // Iterate through all nodes in the array
        for (int i = 0; i < nodesArray.length; i++) {
            for (int j = 0; j < nodesArray[i].length; j++) {
                TransformableNode node = nodesArray[i][j];
                if (node != null && node.isEnabled()) {
                    // If the node is enabled, add its square number to the list
                    int squareNumber = i * nodesArray.length + j;
                    occupiedSquares.add(squareNumber);
                }
            }
        }

        return occupiedSquares;
    }

}
