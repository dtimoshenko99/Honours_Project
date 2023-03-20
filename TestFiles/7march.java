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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.ref.WeakReference;
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
    Vector3[] pickUpPosition;

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
    Map<Vector3, Vector3> squareLocalandWorldPositionMap = new HashMap<>();

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

            boardNode = new Node();
            boardNode.setParent(anchorNode);
            boardNode.setRenderable(this.board);
            boardNode.setLocalScale(new Vector3(0.02f, 0.02f, 0.02f));
            boardNode.setLocalPosition(new Vector3(0f, -0.02f, 0f));

            // Create temporary anchor to hold piece Movement

            tempNode = new AnchorNode(anchor2);
            tempNode.setParent(arFragment.getArSceneView().getScene());

            // Rotate the board 90 degrees
            Quaternion oldRotation1 = boardNode.getLocalRotation();
            Quaternion newRotation1 = Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f),  90);
            boardNode.setLocalRotation(Quaternion.multiply(oldRotation1,newRotation1));

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
        float boardX = boardNode.getLocalPosition().x - 0.415f;
        float boardY = boardNode.getLocalPosition().y + 0.05f;
        float boardZ = boardNode.getLocalPosition().z - 0.415f;
        float squareSize = 0.235f;
        boolean placed = false;
        Log.d(TAG, "--------------------------------------------");
        Log.d(TAG, "Populate Board Function START");
        Log.d(TAG, "--------------------------------------------");
        Log.d(TAG, "PopulateBoard: Board Local and World: " + boardNode.getLocalPosition() + boardNode.getWorldPosition());

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                float squareX = boardX + col * (squareSize / 2);
                float squareY = boardY;
                float squareZ = boardZ + row * (squareSize / 2);


                squareLocalandWorldPositionMap.put(new Vector3(anchorNode.getWorldPosition().x, anchorNode.getWorldPosition().y, anchorNode.getWorldPosition().z),new Vector3(squareX, squareY, squareZ));
                Log.d(TAG, "--------------------------------------------");
                createPieceNode(squareX, squareY, squareZ, BLACK_NODE);
                Log.d(TAG, "Piece created at:  " + new Vector3(anchorNode.getWorldPosition().x, anchorNode.getWorldPosition().y, anchorNode.getWorldPosition().z) + new Vector3(squareX, squareY, squareZ));
                Log.d(TAG, "--------------------------------------------");
//                int pieceType = boardArray[row][col];
//                // Create and Populate black nodes
//                if (pieceType == 1) {
//                    createPieceNode(squareX, squareY, squareZ, BLACK_NODE);
//                }
//                // Create and populate red nodes
//                else if (pieceType == 2) {
//                    createPieceNode(squareX, squareY, squareZ, RED_NODE);
//                }
            }


        }
        Log.d(TAG, "--------------------------------------------");
        Log.d(TAG, "Populate Board Function END");
        Log.d(TAG, "--------------------------------------------");
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
                        Log.d(TAG, "--------------------------------------------");
                        Log.d(TAG, "Action Down old parent: " + selectedNode.getParent());
                        selectedNode.setParent(tempNode);
                        Log.d(TAG, "Action Down new parent: " + selectedNode.getParent());
                        pickUpPosition = new Vector3[]{selectedNode.getLocalPosition(), selectedNode.getWorldPosition()};
                        nodeSelected = true;
                        Log.d(TAG, "Pick UP Position: " + Arrays.toString(pickUpPosition));
                        Log.d(TAG, "--------------------------------------------");
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(nodeSelected){
//                        localLastKnown = selectedNode.getLocalPosition();
//                        localLastKnown.y += 0.05f;
//                        selectedNode.setLocalPosition(localLastKnown);
//                        worldLastKnown = selectedNode.getWorldPosition();
                        tempNode.setWorldPosition(new Vector3(0,0,0));
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    if(nodeSelected){
                        Log.d(TAG, "--------------------------------------------");
                        localLastKnown = selectedNode.getLocalPosition();
                        worldLastKnown = selectedNode.getWorldPosition();
                        Log.d(TAG, "new positions: Local:" + localLastKnown + " World: " + worldLastKnown);
//                        Vector3[] worldAndLocalSquarePos = getClosestSquarePosition(worldLastKnown,localLastKnown);
                        Log.d(TAG, "Old Parent: " + selectedNode.getParent());
                        selectedNode.setParent(anchorNode);

                        selectedNode.setLocalPosition(new Vector3(0,0.5f, 0));
                        Log.d(TAG, "New Parent: " + selectedNode.getParent());
//                        selectedNode.setLocalPosition(worldAndLocalSquarePos[1]);
//                        Log.d(TAG, "Placing on Local and World: "+ Arrays.toString(worldAndLocalSquarePos));
                        Log.d(TAG, "--------------------------------------------");
                        nodeSelected = false;
                    }
                    break;
            }
            return true;
        }
        return true;

    }


    private Vector3[] getClosestSquarePosition(Vector3 pieceWorldPosition, Vector3 pieceLocalPosition){
        double threshold = 0.15;
        Vector3[] worldAndLocal = new Vector3[2];
        //  squareLocalPositionsMap
        for (Map.Entry<Vector3, Vector3> entry : squareLocalandWorldPositionMap.entrySet()){
            Vector3 squareWorldPos = entry.getKey();
            Vector3 squareLocalPos = entry.getValue();


            // Get distance between square and a piece position
            double distance = Math.sqrt(Math.pow(pieceLocalPosition.x - squareLocalPos.x, 2)
                    + Math.pow(pieceLocalPosition.y - squareLocalPos.y, 2)
                    + Math.pow(pieceLocalPosition.z - squareLocalPos.z, 2));

            if (distance < threshold){
                Log.d("onTap", "distance is less: " + squareWorldPos + "Local: " + squareLocalPos);
                // Should check which space
                worldAndLocal[0] = squareWorldPos;
                worldAndLocal[1] = squareLocalPos;
                return worldAndLocal;

            }

        }
        Log.d("onTap", "returning last known: " + localLastKnown);
        worldAndLocal[0] = pieceWorldPosition;
        worldAndLocal[1] = pieceLocalPosition;
        return worldAndLocal;
    }

    private void createPieceNode(float x, float y, float z, String node ) {
        TransformableNode piece = new TransformableNode(arFragment.getTransformationSystem());
        piece.setParent(anchorNode);
        piece.setRenderable(blackPieces);
        piece.setSelectable(true);
        piece.setEnabled(true);
        Quaternion oldRotation1 = piece.getLocalRotation();
        Quaternion newRotation1 = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f),  90);
        piece.setLocalRotation(Quaternion.multiply(oldRotation1,newRotation1));
        piece.getScaleController().setMinScale(0.002f);
        piece.getScaleController().setMaxScale(0.003f);
        piece.setLocalPosition(new Vector3(x, y, z));
        piece.setName(node);
        piece.setOnTapListener(this);
        piece.setOnTouchListener(this);
        Log.d(TAG, "createRedNodes: Black NODE WORLD + LOCAL: " + piece.getWorldPosition() + ", " + piece.getLocalPosition());

    }


}
