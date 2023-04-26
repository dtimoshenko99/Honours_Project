package abertay.uad.ac.uk.myapplication;



import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import org.checkerframework.checker.nullness.qual.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener,
        Node.OnTapListener,
        Node.OnTouchListener{

    private final String  TAG = "onTap";

    private AnchorNode initialAnchorNode;
    private ArFragment arFragment;
    private Renderable redPiece, blackPieces, board;
    private Anchor anchor, anchor2;
    Node boardNode;
    AnchorNode anchorNode;

    AnchorNode tempNode;


    Vector3 localLastKnown;
    Vector3 worldLastKnown;
    Vector3 pickUpPosition;

    GameTurnManager turnManager = new GameTurnManager();

    private boolean nodeSelected = false;
    private TransformableNode selectedNode;
    // For later
    private int playerTurn; // turn: 1- black, 2- white

    // Starting position to the board

    private final int[][] boardArray = {
            {0, 1, 0, 1, 0, 1, 0, 1},
            {1, 0, 1, 0, 1, 0, 1, 0},
            {0, 1, 0, 1, 0, 1, 0, 1},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {2, 0, 2, 0, 2, 0, 2, 0},
            {0, 2, 0, 2, 0, 2, 0, 2},
            {2, 0, 2, 0, 2, 0, 2, 0}
    }; // 1 - black pieces, 2 - red

    // Map to hold Position to Square variables
    List<Vector3> squareWorldPositions = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }

        loadModels();
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
            arFragment.setOnViewCreatedListener(this);
            arFragment.setOnTapArPlaneListener(this);
        }
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        }
    }

    @Override
    public void onViewCreated(ArSceneView arSceneView) {
        arFragment.setOnViewCreatedListener(null);

        // Fine adjust the maximum frame rate
        arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL);
    }

    public void loadModels() {
        WeakReference<MainActivity> weakActivity = new WeakReference<>(this);
        ModelRenderable.builder()
                .setSource(this, Uri.parse("models/board/scene.gltf"))
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build()
                .thenAccept(model -> {
                    MainActivity activity = weakActivity.get();
                    if (activity != null) {
                        activity.board = model;
                    }
                })
                .exceptionally(throwable -> {
                    Toast.makeText(
                            this, "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
        WeakReference<MainActivity> weakActivity1 = new WeakReference<>(this);
        ModelRenderable.builder()
                .setSource(this, Uri.parse("models/pieces/scene.gltf"))
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build()
                .thenAccept(piecesModel -> {
                    MainActivity activity = weakActivity1.get();
                    if (activity != null) {
                        activity.blackPieces = piecesModel;
                    }
                })
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load white pieces" + throwable, Toast.LENGTH_LONG).show();
                    return null;
                });
    }


    @Override
    public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
        if (board == null) {
            Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the Anchor
        // If board is already placed then restrict from spawning additional boards
        if (anchor == null) {
            anchor = hitResult.createAnchor();
            anchor2 = hitResult.createAnchor();


            anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            Quaternion rotationAnchor = Quaternion.axisAngle(new Vector3(0.0f, 0f, 0.0f),  0);
            anchorNode.setLocalRotation(rotationAnchor);


            boardNode = new Node();
            boardNode.setParent(anchorNode);
            boardNode.setRenderable(this.board);
            boardNode.setLocalScale(new Vector3(0.02f, 0.02f, 0.02f));
            boardNode.setLocalPosition(new Vector3(0f, -0.02f, 0f));

            // Rotate the board 90 degrees
            Quaternion rotation = Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f),  90);
            boardNode.setLocalRotation(rotation);

            // Load models of the board and pieces
            loadModels();
            // Populate the board with pieces
            populateBoard();

            //Decide who plays first and update Node's selectivity
            turnManager.switchTurnAndUpdateSelectableNodes(arFragment);
        } else {
            Toast.makeText(this, "The board is already placed, you can change the position by moving the board", Toast.LENGTH_LONG).show();
        }

    }

    // POPULATE BOARD WITH PIECES
    private void populateBoard() {
        float boardX = boardNode.getWorldPosition().x - 0.415f;
        float boardY = boardNode.getWorldPosition().y + 0.05f;
        float boardZ = boardNode.getWorldPosition().z - 0.415f;
        float squareSize = 0.235f;
        boolean placed = false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                float squareX = boardX + col * (squareSize / 2);
                float squareY = boardY;
                float squareZ = boardZ + row * (squareSize / 2);


                squareWorldPositions.add(new Vector3(squareX, squareY, squareZ));
//                if(placed != true){
//                    createPieceNode(squareX, squareY, squareZ, BLACK_NODE);
//                    placed = true;
//                }
                int pieceType = boardArray[row][col];
                // Create and Populate black nodes
                if (pieceType == 1) {
                    String BLACK_NODE = "blackPiece";
                    createPieceNode(squareX, squareY, squareZ, BLACK_NODE);
                }
                // Create and populate red nodes
                else if (pieceType == 2) {
                    String RED_NODE = "redPiece";
                    createPieceNode(squareX, squareY, squareZ, RED_NODE);
                }
            }


        }
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
        Log.d(TAG, "isSquareOccupied: " + boardArray[row][col]);
        Log.d(TAG, "updateBoardStartArray: " + Arrays.deepToString(boardArray));
        if(boardArray[row][col] == 0){
            return false;
        }else{
            return true;
        }
    }

    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {

    }

    @Override
    public boolean onTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
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
//                        isInsideBoard(selectedNode.getWorldPosition());
                        pieceDropped(selectedNode);
                        // Need check logic here
                        // TODO: need to implement a "checkersGame" class, where to check whether a piece has more turn
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

    private void pieceDropped(TransformableNode node){
        worldLastKnown = node.getWorldPosition();
        Vector3 worldSquarePos = getClosestSquarePosition(worldLastKnown);
        Vector3 pickUpSquarePos = getClosestSquarePosition(pickUpPosition);
        int[] colAndRow = getRowColFromWorldPosition(worldLastKnown, squareWorldPositions, 8,8);
        Log.d(TAG, "onTouch: ROW AND COL" + colAndRow[0] + ", " + colAndRow[1]);

        int[] pickupRowAndCol = getRowColFromWorldPosition(pickUpPosition, squareWorldPositions, 8,8);
        // Check if the move was valid
        boolean isValid = isValidMove(pickupRowAndCol[1], pickupRowAndCol[0], colAndRow[1], colAndRow[0]);

        if(isValid){
            selectedNode.setParent(initialAnchorNode);
            selectedNode.setLocalPosition(new Vector3(0,0,0));
        }else{
            Log.d(TAG, "pieceDropped: isValid?; " + isValid);
            node.setWorldPosition(worldSquarePos);
            updateBoardStartArray(pickupRowAndCol[1], pickupRowAndCol[0], colAndRow[1], colAndRow[0]);

            // Switches the turn and updates the corresponding node's setSelectable value
            turnManager.switchTurnAndUpdateSelectableNodes(arFragment);
            initialAnchorNode = null;
        }



    }

    private void updateBoardStartArray(int initialCol, int initialRow, int destCol, int destRow) {
        // Get the piece value from the initial square
        int pieceValue = boardArray[initialRow][initialCol];
        // Set the initial square to 0 (unoccupied)
        boardArray[initialRow][initialCol] = 0;

        // Update the destination square with the moved piece value
        boardArray[destRow][destCol] = pieceValue;

    }

    private Vector3 getClosestSquarePosition(Vector3 pieceWorldPosition){
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

    private boolean isValidMove(int initialCol, int initialRow, int destCol, int destRow) {
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

        // TODO: the following functionality is not working as intended - need to make it work
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
        }

        return true;
    }


    private void createPieceNode(float x, float y, float z, String node ) {
        Pose pose = Pose.makeTranslation(x, y, z);
        Anchor pieceAnchor = arFragment.getArSceneView().getSession().createAnchor(pose);


        AnchorNode pieceAnchorNode = new AnchorNode(pieceAnchor);
        pieceAnchorNode.setParent(arFragment.getArSceneView().getScene());

        TransformableNode piece = new TransformableNode(arFragment.getTransformationSystem());
        piece.setParent(pieceAnchorNode);

        piece.setRenderable(blackPieces);
        piece.setSelectable(true);
        piece.setEnabled(true);
        Quaternion oldRotation1 = piece.getLocalRotation();
        Quaternion newRotation1 = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f),  90);
        piece.setLocalRotation(Quaternion.multiply(oldRotation1,newRotation1));
        piece.getScaleController().setMinScale(0.002f);
        piece.getScaleController().setMaxScale(0.003f);
        piece.setLocalPosition(new Vector3(0, 0.05f, 0));
        piece.setWorldPosition(new Vector3(x, y, z));
        piece.setName(node);
        piece.setOnTapListener(this);
        piece.setOnTouchListener(this);
        pieceAnchorNode.setWorldPosition(new Vector3(x, y, z));
    }

    private boolean isInsideBoard(Vector3 worldPosition) {
        Vector3 boardCenter = boardNode.getWorldPosition(); // Replace with the actual center world position of your board
        float boardWidth = 1.0f; // Replace with the actual width of your board
        float boardLength = 1.0f; // Replace with the actual length of your board

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

}