// This version creates anchors and nodes on positions of each square and
// Detaches to temporary node, when a user moves a piece
// and attaches to a square node when a user sets a piece on a board.

// Although it doesn't work
// Tried this on 20th of March and the piece stoped appearing on the board...
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
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener,
        Node.OnTapListener,
        Node.OnTouchListener{

    private final String TAG = "onTap";
    private ArFragment arFragment;
    private Renderable whitePieces, blackPieces, board;
    private Anchor anchor, anchor2;
    Node boardNode;
    AnchorNode anchorNode, tempNode;

    Vector3 localLastKnown;
    Vector3 worldLastKnown;
    Pose lastPose;
    Vector3 pickUpWorldPos;

    Vector3 pickUpPosition;

    AnchorNode selectedAnchorNode;
    TransformableNode transformableNode;
    boolean nodeSelected = false;
    // For later
    private int playerTurn; // turn: 1- black, 2- white

    // Starting position to the board
    private final int[][] boardStart = {
            {0, 1, 0, 1, 0, 1, 0, 1},
            {1, 0, 1, 0, 1, 0, 1, 0},
            {0, 1, 0, 1, 0, 1, 0, 1},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {2, 0, 2, 0, 2, 0, 2, 0},
            {0, 2, 0, 2, 0, 2, 0, 2},
            {2, 0, 2, 0, 2, 0, 2, 0}
    }; // 1 - black pieces, 2 - white

    // Vector3 array to hold positions of all squares
    ArrayList<Vector3> squareLocalPositions = new ArrayList<>();
    ArrayList<Vector3> squareWorldPositions = new ArrayList<>();
    Map<AnchorNode, Vector3> nodePositions = new HashMap<>();
    // Map to hold Position to Square variables
    AnchorNode pieceAnchor;

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

        // Load models into renderable variables
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

            // Define the anchor node for the piece
            pieceAnchor = new AnchorNode(anchor2);
            arFragment.getArSceneView().getScene().addChild(pieceAnchor);

            anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());
//            anchorNode.setSelectable(false);

            boardNode = new Node();
            boardNode.setParent(anchorNode);
            boardNode.setRenderable(this.board);
            boardNode.setLocalScale(new Vector3(0.02f, 0.02f, 0.02f));
            boardNode.setLocalPosition(new Vector3(0f, -0.02f, 0f));
//            Log.d("RedPiece", "populateBoard: Board postions in render: " + boardNode.getLocalPosition());



            loadModels();
            populateBoard();

        } else {
            Toast.makeText(this, "The board is already placed, you can change the position by moving the board", Toast.LENGTH_LONG).show();
            return;
        }

    }

    // POPULATE BOARD WITH PIECES
    private void populateBoard() {
        float boardX = boardNode.getLocalPosition().x - 0.41f;
        float boardY = boardNode.getLocalPosition().y + 0.05f;
        float boardZ = boardNode.getLocalPosition().z - 0.41f;
        float squareSize = 0.118f;
        boolean isPlaced = false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {

                // Get local position of the squares relative to the board
                float squareX = boardX + col * squareSize;
                float squareY = boardY;
                float squareZ = boardZ + row * squareSize;

                // Get world position of the squares relative to the board's position
                Vector3 boardWorldPosition = boardNode.getWorldPosition();
                float worldSquareX = boardWorldPosition.x + (col * squareSize) - (3.5f * squareSize);
                float worldSquareY = boardWorldPosition.y;
                float worldSquareZ = boardWorldPosition.z + (row * squareSize) - (3.5f * squareSize);


                squareWorldPositions.add(new Vector3(worldSquareX, worldSquareY, worldSquareZ));
                squareLocalPositions.add(new Vector3(squareX, squareY, squareZ));
                // Create anchor and anchorNode for each of the squares
                Anchor squareAnchor = arFragment.getArSceneView().getSession().createAnchor(new Pose(new float[]{worldSquareX, worldSquareY, worldSquareZ},
                        new float[]{0,0,0,0}));
                AnchorNode squareNode = new AnchorNode(squareAnchor);
                squareNode.setWorldPosition(new Vector3(worldSquareX, worldSquareY,worldSquareZ));
                nodePositions.put(squareNode, new Vector3(worldSquareX,worldSquareY,worldSquareZ));

                // TODO: need to create an array to hold all anchors and their position to then map them to square numbers
                // Placing just one piece for testing
//                if(!isPlaced){
//                    createPieceNode(0,0,0, "blackNode", squareNode);
//                    isPlaced = true;
//                }

                int pieceType = boardStart[row][col];

                // Create and Populate black nodes
                if (pieceType == 1) {
                    createPieceNode(0,0,0, "blackNode", squareNode);
                }
                // Create and populate red nodes
                else if (pieceType == 2) {
                    createPieceNode(0,0,0, "redPiece", squareNode);
                }
                else{
                    createPieceNode(0,0,0, "testPiece", squareNode);
                }
            }
        }
        Log.d(TAG, "populateBoard: Square Positions: " + squareLocalPositions.toString());
    }

    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
//        Node select = hitTestResult.getNode();
//        select.setLocalPosition(new Vector3(0,0,0));
//        Log.d("onTap", "onTap: The node has been tapped" + motionEvent + hitTestResult + select);
//        selct.getUp();
    }

    @Override
    public boolean onTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
        Node node = hitTestResult.getNode();

        if(node instanceof TransformableNode){
            if(!nodeSelected){
                transformableNode = (TransformableNode) node;
            }


            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    if(!nodeSelected){
                        // Detatch node from it's AnchorNode, which is in this case the SquareNode
//                    AnchorNode detach = (AnchorNode) transformableNode.getParent();
//                    detach.setAnchor(null);

                        // Get the pickup position of an AnchorNode, TransformableNode and the Pose of an Anchor
                        selectedAnchorNode = (AnchorNode) transformableNode.getParent();;
                        pickUpWorldPos = selectedAnchorNode.getWorldPosition();
                        pickUpPosition = transformableNode.getLocalPosition();
                        lastPose = anchor.getPose();

                        tempNode.setWorldPosition(pickUpPosition);
                        transformableNode.setParent(tempNode);
                        transformableNode.setLocalPosition(pickUpPosition);


                        // Debugging purposes
                        Log.d(TAG, "Position of the Anchor Node on Pickup: " + anchorNode.getWorldPosition());
                        Log.d(TAG, "Position of the anchor on Pickup" + anchor.getPose());
                        nodeSelected = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if(nodeSelected) {
                        Vector3 newWorldPos = selectedAnchorNode.getWorldPosition();
                        Vector3 closestPos = getClosestSquarePosition(newWorldPos);
                        AnchorNode squareNode = getClosestNodePosition(closestPos);
                        transformableNode.setParent(squareNode);
                        transformableNode.setLocalPosition(new Vector3(0,0,0));



                        // Debugging purposes
                        Log.d(TAG, "Action UP: The node has been UPPED"
                                + motionEvent
                                + hitTestResult
                                + transformableNode.getName()
                                + "Local: " + transformableNode.getLocalPosition()
                                + "World: " + transformableNode.getWorldPosition());
                        nodeSelected = false;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if(nodeSelected) {
                        // To get the last known position of a piece to then assign the values
                        // to a piece when user releases it
                        localLastKnown = transformableNode.getLocalPosition();
                        worldLastKnown = transformableNode.getWorldPosition();

                        // Debugging purposes
                        Log.d(TAG, "Action MOVE: The node has been MOVED"
                                + motionEvent
                                + hitTestResult
                                + transformableNode.getName()
                                + "Local: " + transformableNode.getLocalPosition()
                                + "World: " + transformableNode.getWorldPosition());
                    }
                    break;
            }
            return true;
        }
        return true;

    }

    private AnchorNode getClosestNodePosition(Vector3 position){
        AnchorNode squareAnchorNode = null;
        for (Map.Entry<AnchorNode, Vector3> entry : nodePositions.entrySet()) {
            if(entry.getValue().equals(position)){
                Log.d(TAG, "getClosestNodePosition: " + entry.getKey() + entry.getValue());
                squareAnchorNode = entry.getKey();
            }
        }
        return squareAnchorNode;
    }


    private Vector3 getClosestSquarePosition(Vector3 pieceWorldPosition) {
        double threshold = 0.15;
        //  squareLocalPositionsMap
        Vector3 squareWorldPosition = null;
        double distance;

        Log.d(TAG, "getClosestSquarePosition: Starting interation of the closest square");
        for (int i = 0; i < squareWorldPositions.size(); i++){
            squareWorldPosition = squareWorldPositions.get(i);

            Log.d(TAG, "getClosestSquarePosition: stored position: " + squareWorldPosition);
            Log.d(TAG, "getClosestSquarePosition: piece position: " + pieceWorldPosition);

            // Get distance between square and a piece position
            distance = Math.sqrt(Math.pow(pieceWorldPosition.x - squareWorldPosition.x, 2)
                    + Math.pow(pieceWorldPosition.y - squareWorldPosition.y, 2)
                    + Math.pow(pieceWorldPosition.z - squareWorldPosition.z, 2));

            if (distance < threshold) {
                Log.d(TAG, "distance is less: " + pieceWorldPosition + "Local: " + pieceWorldPosition);
                // Should check which space
                return pieceWorldPosition;

            }

        }
        return worldLastKnown;
    }


    private void createPieceNode(float x, float y, float z, String node, AnchorNode pieceAnchorNode) {
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
        piece.setLocalPosition(new Vector3(x, y, z));
        piece.setName(node);
        piece.setOnTapListener(this);
        piece.setOnTouchListener(this);
        Log.d(TAG, "createRedNodes: Piece Local on creation " + piece.getLocalPosition());
        Log.d(TAG, "createPieceNode: Piece world on creation " + piece.getWorldPosition());
    }

}
