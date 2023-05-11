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

        // Iterate through the board array
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                // Check if the square is occupied
                if (boardArray[row][col] != 0) {
                    // Determine the current piece based on the player's color
                    if (currentPlayer.getColor().equals("red")) {
                        currentPiece = 2;
                    } else {
                        currentPiece = 1;
                    }

                    // Check if the current piece belongs to the current player
                    if (boardArray[row][col] == currentPiece) {
                        // Iterate through all possible capture directions
                        for (int direction = 0; direction < rowOffsets.length; direction++) {
                            int newRow = row + 2 * rowOffsets[direction];
                            int newCol = col + 2 * colOffsets[direction];
                            if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {

                                int midRow = row + rowOffsets[direction];
                                int midCol = col + colOffsets[direction];
                                int middlePiece = boardArray[midRow][midCol];

                                // Determine the opponent's piece based on the current piece
                                opponentPiece = (currentPiece == 1) ? 2 : 1;

                                // Check if the adjacent square contains an opponent's piece and if the capture square is empty
                                if (middlePiece == opponentPiece && boardArray[newRow][newCol] == 0) {
                                    capturePositions.add(new int[]{newCol, newRow, row, col});
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

        // Iterate through the node array
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                // Get the node at the current position
                TransformableNode node = nodeArray[row][col];

                // Check if the node is not null and not in the list of nodes to exclude
                if (node != null && !nodesToExclude.contains(node)) {
                    // Set the 'setSelectable' property of the node to false
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
            renderableNode.setRenderable(null);
            capturedPiece.getParent().removeChild(capturedPiece);

        }
    }

    public int[] getRowColFromWorldPosition(Vector3 worldPosition, List<Vector3> squareWorldPositions, int numRows, int numCols) {
        // Initialize variables to store the closest row and column, and the minimum distance
        int closestRow = -1;
        int closestCol = -1;
        float minDistance = Float.MAX_VALUE;

        // Iterate through each row and column
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                // Calculate the index of the square in the squareWorldPositions list
                int index = row * numCols + col;

                // Get the square position from the squareWorldPositions list
                Vector3 squarePosition = squareWorldPositions.get(index);

                // Calculate the distance between the world position and the square position
                float distance = euclideanDistance(worldPosition, squarePosition);

                // Check if the calculated distance is smaller than the current minimum distance
                if (distance < minDistance) {
                    // Update the minimum distance and closest row and column
                    minDistance = distance;
                    closestRow = row;
                    closestCol = col;
                }
            }
        }

        // Return the closest row and column as an array
        return new int[]{closestRow, closestCol};
    }


    private static float euclideanDistance(Vector3 v1, Vector3 v2) {
        // Calculate the differences in X, Y, and Z coordinates
        float dx = v2.x - v1.x;
        float dy = v2.y - v1.y;
        float dz = v2.z - v1.z;

        // Calculate the squared sum of the coordinate differences
        float squaredDistance = dx * dx + dy * dy + dz * dz;

        // Calculate the square root of the squared distance to get the Euclidean distance
        float distance = (float) Math.sqrt(squaredDistance);

        return distance;
    }


    // Check if a position is inside the board
    boolean isInsideBoard(Vector3 worldPosition) {
        // Get the board node and its properties
        Node boardNode = gameInit.getBoardNode();
        Vector3 boardCenter = boardNode.getWorldPosition();
        float boardWidth = 1.0f;
        float boardLength = 1.0f;

        // Calculate half the width and length of the board
        float halfWidth = boardWidth / 2;
        float halfLength = boardLength / 2;

        // Calculate the minimum and maximum X and Z coordinates of the board
        float minX = boardCenter.x - halfWidth;
        float maxX = boardCenter.x + halfWidth;
        float minZ = boardCenter.z - halfLength;
        float maxZ = boardCenter.z + halfLength;

        // Check if the world position is within the bounds of the board
        if ((worldPosition.x >= minX && worldPosition.x <= maxX) && (worldPosition.z >= minZ && worldPosition.z <= maxZ)) {
            return true; // Inside the board
        } else {
            return false; // Outside the board
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
        boolean whiteHasPieces = false;

        // Iterate through the board array and check if there are still pieces left
        for (int[] row : boardArray) {
            for (int piece : row) {
                if (piece != 0) {
                    // Check if a player has pieces
                    if (piece == 1) {
                        redHasPieces = true;
                    } else if (piece == 2) {
                        whiteHasPieces = true;
                    }
                }
            }
        }

        // Check the conditions for game over
        if (whiteHasPieces && redHasPieces) {
            // Game is not over, both players still have pieces
            return false;
        } else if (!whiteHasPieces) {
            // Game over, white player has no pieces, set the winner as "red"
            gameLogic.setWinner("red");
            return true;
        } else {
            // Game over, red player has no pieces, set the winner as "white"
            gameLogic.setWinner("white");
            return true;
        }
    }


    public void updateBoardArrayFromPositions(int wasRow, int wasCol, int nowRow, int nowCol, int capturedRow, int capturedCol, int[][] boardArray) {
        // Check if a piece was moved from a previous square
        if (wasCol != -1 && wasRow != -1) {
            int piece = boardArray[wasRow][wasCol];

            // Reset the previous square (set it to 0, indicating it's unoccupied)
            boardArray[wasRow][wasCol] = 0;

            // Move the piece to the new square
            boardArray[nowRow][nowCol] = piece;

            // Remove the captured piece, if any
            if (capturedCol != -1 && capturedRow != -1) {
                boardArray[capturedRow][capturedCol] = 0;
            }
        }

        // Update the board array in GameInit
        gameInit.setBoardArray(boardArray);
    }


    public void updateNodesArray(TransformableNode[][] nodesArray, int wasRow, int wasCol, int nowRow, int nowCol, int capturedRow, int capturedCol) {
        TransformableNode[][] array = nodesArray;
        TransformableNode node = array[wasRow][wasCol];

        // Check if there is a captured node
        if (capturedRow != -1 && capturedCol != -1) {
            // Remove node from scene
            removePieceFromScene(array[capturedRow][capturedCol]);
            // Delete the captured node
            array[capturedRow][capturedCol] = null;
        }

        // Check if the node has moved
        if (wasRow != nowRow || wasCol != nowCol) {
            // Move the node to the new position
            array[nowRow][nowCol] = node;
            // Delete the node from the previous position
            array[wasRow][wasCol] = null;
        }
        // Update the board array in GameInit
        gameInit.setNodesArray(array);
    }

    public void updateGameBoard(int[][] boardArray, TransformableNode[][] nodesArray) {
        // Iterate through the board array
        for (int row = 0; row < boardArray.length; row++) {
            for (int col = 0; col < boardArray[row].length; col++) {
                TransformableNode node = nodesArray[row][col];

                // Check if the square is occupied
                if (boardArray[row][col] != 0) {
                    // Get the new position for the piece based on the row and column
                    Vector3 newPos = getSquarePosition(row, col, gameInit.getSquareWorldPositions());
                    node.setWorldPosition(newPos);
                }
            }
        }
    }

}
