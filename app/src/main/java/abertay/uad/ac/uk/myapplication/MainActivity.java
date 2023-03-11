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
    private Anchor anchor, anchor2, tempAnchor;
    Node boardNode;
    AnchorNode anchorNode;

    Vector3 localLastKnown;
    Vector3 worldLastKnown;
    Pose lastPose;
    Vector3 pickUpWorldPos;

    Vector3 pickUpPosition;

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
    ArrayList<Vector3> squarePositions = new ArrayList<>();
    ArrayList<Vector3> piecePositions = new ArrayList<>();
//    Map<Vector3, Vector3> nodePositions = new HashMap<>();
    Map<Vector3, Vector3> nodePositions = new HashMap<>();
    // Map to hold Position to Square variables
    Vector3 anchorOriginalPosition;
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
        int squares = 0;
        boolean isPlaced = false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares += 1;
                float squareX = boardX + col * squareSize;
                float squareY = boardY;
                float squareZ = boardZ + row * squareSize;

                squarePositions.add(new Vector3(squareX, squareY, squareZ));
                // TODO: need to create an array to hold all anchors and their position to then map them to square numbers

                if(!isPlaced){
                    createPieceNode(squareX, squareY, squareZ, "blackNode");
                    isPlaced = true;
                }else{
                }

//                int pieceType = boardStart[row][col];
//
//                // Create and Populate black nodes
//                if (pieceType == 1) {
//                    createPieceNode(squareX, squareY, squareZ, "blackNode");
//                }
//                // Create and populate red nodes
//                else if (pieceType == 2) {
//                    createPieceNode(squareX, squareY, squareZ, "redPiece");
//                }
            }
        }
        Log.d(TAG, "populateBoard: Square Positions: " + squarePositions.toString());
    }

    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
        Node select = hitTestResult.getNode();
        select.setLocalPosition(new Vector3(0,0,0));
        Log.d("onTap", "onTap: The node has been tapped" + motionEvent + hitTestResult + select);
//        selct.getUp();
    }

    @Override
    public boolean onTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
        Node node = hitTestResult.getNode();


        if(node instanceof TransformableNode){
            TransformableNode transformableNode = (TransformableNode) node;

            switch (motionEvent.getAction()){

                case MotionEvent.ACTION_DOWN:
                    // Detatch node from it's AnchorNode, which is in this case the SquareNode
//                    AnchorNode detach = (AnchorNode) transformableNode.getParent();
//                    detach.setAnchor(null);
                    // Debugging purposes

                    AnchorNode pickUpWorld = (AnchorNode) transformableNode.getParent();
                    pickUpWorldPos = pickUpWorld.getWorldPosition();
                    pickUpPosition = transformableNode.getLocalPosition();
                    lastPose = anchor.getPose();
                    Log.d(TAG, "Position of the Anchor Node on Pickup: " + anchorNode.getWorldPosition());
                    Log.d(TAG, "Position of the anchor on Pickup" + anchor.getPose());
                    break;
                case MotionEvent.ACTION_UP:

//                    Vector3 newPosition = transformableNode.getLocalPosition();
//                    Pose anchorPose = pieceAnchor.getAnchor().getPose();
//                    Vector3 worldPosition = new Vector3(anchorPose.tx() + newPosition.x, anchorPose.ty() + newPosition.y, anchorPose.tz() + newPosition.z);
//                    Vector3 localPosition = pieceAnchor.worldToLocalPoint(worldPosition);
//                    transformableNode.setLocalPosition(localPosition);
//                    Vector3 closest = getClosestSquarePosition(localLastKnown);
//                    Log.d(TAG, "onTouch: "+ findNodeAtPosition(closest));


                    AnchorNode parent = (AnchorNode) transformableNode.getParent();
                    if(parent.getWorldPosition().equals(pickUpWorldPos)){
                        transformableNode.setLocalPosition(new Vector3(0,0.05f,0));
                        Log.d(TAG, "Position of the AnchorNode Stayed the same:");
                    }else{
                        transformableNode.setLocalPosition(transformableNode.getLocalPosition());
                        Log.d(TAG, "Position of the AnchorNode DID NOT stay the same ");
                    }
//                    tempNode.setWorldPosition(anchorWorldPos);


                    // Debugging purposes
                    Log.d("onTap", "Action UP: The node has been UPPED"
                            + motionEvent
                            + hitTestResult
                            + transformableNode.getName()
                            + "Local: " + transformableNode.getLocalPosition()
                            + "World: " + transformableNode.getWorldPosition());
                    break;

                case MotionEvent.ACTION_MOVE:
                    // To get the last known position of a piece to then assign the values
                    // to a piece when user releases it
                    localLastKnown = transformableNode.getLocalPosition();
                    worldLastKnown = transformableNode.getWorldPosition();

                    // Debugging purposes
                    Log.d("onTap", "Action MOVE: The node has been MOVED"
                            + motionEvent
                            + hitTestResult
                            + transformableNode.getName()
                            + "Local: " + transformableNode.getLocalPosition()
                            + "World: " + transformableNode.getWorldPosition());
                    break;
            }
            return true;
        }
        return true;

    }

    private Vector3 getClosestSquarePosition(Vector3 pieceLocalPosition) {
        double threshold = 0.15;
        //  squareLocalPositionsMap
        Vector3 squareLocalPos = null;
        double distance;
        for (int i = 0; i < squarePositions.size(); i++){
            squareLocalPos = squarePositions.get(i);


            // Get distance between square and a piece position
            distance = Math.sqrt(Math.pow(pieceLocalPosition.x - squareLocalPos.x, 2)
                    + Math.pow(pieceLocalPosition.y - squareLocalPos.y, 2)
                    + Math.pow(pieceLocalPosition.z - squareLocalPos.z, 2));

            if (distance < threshold) {
                Log.d("onTap", "distance is less: " + squareLocalPos + "Local: " + squareLocalPos);
                // Should check which space
                return squareLocalPos;

            }

        }
        return pieceLocalPosition;
    }

    public AnchorNode findNodeAtPosition(Vector3 position) {
        for (Node node : arFragment.getArSceneView().getScene().getChildren()) {
            if (node.getWorldPosition().equals(position)) {
                Log.d(TAG, "findNodeAtPosition: " + node);
                return (AnchorNode) node;
            }
        }
        return null;
    }


    private void createPieceNode(float x, float y, float z, String node) {
        TransformableNode piece = new TransformableNode(arFragment.getTransformationSystem());
        piece.setParent(pieceAnchor);
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
        Log.d(TAG, "createRedNodes: Black NODE Local " + piece.getLocalPosition());
    }

}