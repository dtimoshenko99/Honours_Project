// This version is implementing the next hierarchy:
// Anchor - AnchorNode - BoardAnchor + TransNodes (each for a piece)
// This version keeps track of the position of all squares

// The problem: When attaching to a boardnode on piece down - the piece jumps
// into the middle of the board - even if setting the local position of it
// Another problem is that when detaching from a board node the TempAnchor node
// is not set to board's 0,0,0 and moving alongside the piece - hence there is
// no way to get the right local position to determine which square the piece is set to

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener,
        Node.OnTapListener,
        Node.OnTouchListener{

    private final String RED_NODE = "redPiece";
    private final String BLACK_NODE = "blackPiece";
    private final String  TAG = "onTap";


    private ArFragment arFragment;
    private Renderable whitePieces, blackPieces, board;
    private Anchor anchor, anchor2;
    Node boardNode;
    AnchorNode anchorNode;

    AnchorNode tempNode;

    Vector3 localLastKnown;
    Vector3 worldLastKnown;
    Vector3 pickUpPosition;

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

    private int occupiedSquare;
    // Map to hold Position to Square variables
    Map<Integer, Vector3> squareLocalPositionsMap = new HashMap<>();
    List<Vector3> squareWorldPositions = new ArrayList<>();

    Map<Vector3, Integer> isOcupied = new HashMap<>();

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
                    createPieceNode(squareX, squareY, squareZ, BLACK_NODE);
                }
                // Create and populate red nodes
                else if (pieceType == 2) {
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


    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
//        Node selct = hitTestResult.getNode();
//
//        Log.d("onTap", "onTap: The node has been tapped" + motionEvent + hitTestResult + selct);
//        selct.getUp();
    }

    @Override
    public boolean onTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
        Node node = hitTestResult.getNode();

        if(node instanceof TransformableNode){
            if(!nodeSelected) {
                selectedNode = (TransformableNode) node;
            }

            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    if(!nodeSelected){
                        pickUpPosition = new Vector3(selectedNode.getWorldPosition());
                        nodeSelected = true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(nodeSelected){
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    if(nodeSelected){
                        pieceDropped(selectedNode);
                        nodeSelected = false;
                    }
                    break;
            }
            return true;
        }
        return false;

    }

    private void pieceDropped(TransformableNode node){
        Log.d(TAG, "--------------------------------------------");
        worldLastKnown = node.getWorldPosition();
        Vector3 worldSquarePos = getClosestSquarePosition(worldLastKnown);
        int[] colandrow = getRowColFromWorldPosition(worldLastKnown, squareWorldPositions, 8,8);
        Log.d(TAG, "onTouch: COLANDROW" + colandrow[0] + ", " + colandrow[1]);
        node.setWorldPosition(worldSquarePos);
        Log.d(TAG, "pieceDropped: " + "Local: " + node.getLocalPosition() + "World: " + worldLastKnown);
        Log.d(TAG, "--------------------------------------------");
        int[] pickupColandRow = getRowColFromWorldPosition(pickUpPosition, squareWorldPositions, 8,8);
        updateBoardStartArray(pickupColandRow[1], pickupColandRow[0], colandrow[1], colandrow[0]);
    }

    private void updateBoardStartArray(int initialRow, int initialCol, int destRow, int destCol) {
        boardArray[initialRow][initialCol] = 0;

        // Update the destination square with the value representing the moved piece
        int pieceValue = (destRow % 2 == destCol % 2) ? 1 : 2;
        boardArray[destRow][destCol] = pieceValue;
        Log.d(TAG, "updateBoardStartArray: " + Arrays.toString(boardArray));
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

    private void createPieceNode(float x, float y, float z, String node ) {
        tempNode = new AnchorNode(anchor2);
        tempNode.setParent(arFragment.getArSceneView().getScene());
        TransformableNode piece = new TransformableNode(arFragment.getTransformationSystem());
        piece.setParent(tempNode);

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
        tempNode.setWorldPosition(new Vector3(x, y, z));
        Log.d(TAG, "createRedNodes: Black NODE WORLD + LOCAL: " + piece.getWorldPosition() + ", " + piece.getLocalPosition());

    }


}