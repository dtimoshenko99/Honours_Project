 VERSION 07.03 3:40 - after the meeting

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

    private final String  TAG = "onTap";
    private ArFragment arFragment;
    private Renderable whitePieces, blackPieces, board;
    private Anchor anchor, anchor2;
    Node boardNode;
    AnchorNode anchorNode;

    AnchorNode tempNode;
    Anchor tempAnchor;

    Anchor squareAnchor;
    AnchorNode squareNode;

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
            boardNode.setLocalPosition(new Vector3(0f, 0f, 0f));

            // Create temporary anchor to hold piece Movement
            float[] nodePose = new float[]{anchorNode.getWorldPosition().x, anchorNode.getWorldPosition().y, anchorNode.getWorldPosition().z};
            float[] nodeIden = new float[]{Quaternion.identity().x, Quaternion.identity().y, Quaternion.identity().z, Quaternion.identity().w};
            tempAnchor = arFragment.getArSceneView().getSession().createAnchor(new Pose(nodePose, nodeIden));
            tempNode = new AnchorNode(tempAnchor);
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
        float squareSize = 0.118f;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                float squareX = boardX + col * (squareSize / 2);
                float squareY = boardY;
                float squareZ = boardZ + row * (squareSize / 2);

                // World position of each square
                Vector3 boardWorldPosition = boardNode.getWorldPosition();
                float squareWorldX = boardWorldPosition.x + (col * squareSize) - (3.5f * squareSize);
                float squareWorldY = boardWorldPosition.y;
                float squareWorldZ = boardWorldPosition.z + (row * squareSize) - (3.5f * squareSize);

                isOcupied.put(new Vector3(squareWorldX, squareWorldY, squareWorldZ), boardArray[row][col]);

                squareLocalandWorldPositionMap.put
                        (new Vector3(squareWorldX, squareWorldY, squareWorldZ),new Vector3(squareX, squareY, squareZ));


//                float[] boardRotation = new float[]{boardNode.getWorldRotation().x, boardNode.getWorldRotation().y, boardNode.getWorldRotation().z, boardNode.getWorldRotation().w};
//                squareAnchor = arFragment.getArSceneView().getSession()
//                        .createAnchor(new Pose(new float[]{squareWorldX, squareWorldY, squareWorldZ}, new float[]{0,0,0,0}));
//                squareNode = new AnchorNode(squareAnchor);
//                squareNode.setParent(arFragment.getArSceneView().getScene());

                int pieceType = boardArray[row][col];
                // Create and Populate black nodes
                if (pieceType == 1) {
                    createBlackNodes(squareX, squareY, squareZ);
                }
                // Create and populate red nodes
                else if (pieceType == 2) {
                    createRedNodes(squareX, squareY, squareZ);
                }

            }

        }
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
                        float tempX = selectedNode.getWorldPosition().x;
                        float tempY = selectedNode.getWorldPosition().y;
                        float tempZ = selectedNode.getWorldPosition().z;

                        tempNode.setWorldPosition(new Vector3(tempX, tempY, tempZ));
                        selectedNode.setParent(tempNode);
                        selectedNode.setLocalPosition(new Vector3(0 ,0.1f,0));

                        pickUpPosition = new Vector3[]{selectedNode.getLocalPosition(), selectedNode.getWorldPosition()};
                        Log.d(TAG, "onTouch: pickUpPosition Local:" + pickUpPosition + "pickUpPosition World: " + selectedNode.getWorldPosition());
                        nodeSelected = true;

                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(nodeSelected){
//                        localLastKnown = selectedNode.getLocalPosition();
//                        localLastKnown.y += 0.05f;
//                        selectedNode.setLocalPosition(localLastKnown);
//                        worldLastKnown = selectedNode.getWorldPosition();
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    if(nodeSelected){
                        localLastKnown = selectedNode.getLocalPosition();
                        worldLastKnown = selectedNode.getWorldPosition();
                        Vector3[] worldAndLocalSquarePos = getClosestSquarePosition(worldLastKnown,localLastKnown);
                        Node closestNode = findNodeAtPosition(worldAndLocalSquarePos[0]);
                        selectedNode.setParent(closestNode);
//                        Log.d(TAG, "onTouch: worldAndLocalSquarePos" + Arrays.toString(worldAndLocalSquarePos));
//                        selectedNode.setWorldPosition(worldAndLocalSquarePos[0]);
//                        selectedNode.setLocalPosition(worldAndLocalSquarePos[1]);
                        // Debugging purposes

                        Log.d("onTap", "Action UP: The node has been UPPED"
                                + motionEvent
                                + hitTestResult
                                + selectedNode.getName()
                                + "Local: " + selectedNode.getLocalPosition()
                                + "World: " + selectedNode.getWorldPosition());
                        nodeSelected = false;
                    }
                    break;
            }
            return true;
        }
        return true;

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

    private Vector3[] getClosestSquarePosition(Vector3 pieceWorldPosition, Vector3 pieceLocalPosition){
        double threshold = 0.15;
        Vector3[] worldAndLocal = new Vector3[2];
        //  squareLocalPositionsMap
        for (Map.Entry<Vector3, Vector3> entry : squareLocalandWorldPositionMap.entrySet()){
            Vector3 squareWorldPos = entry.getKey();
            Vector3 squareLocalPos = entry.getValue();


            // Get distance between square and a piece position
            double distance = Math.sqrt(Math.pow(pieceWorldPosition.x - squareWorldPos.x, 2)
                    + Math.pow(pieceWorldPosition.y - squareWorldPos.y, 2)
                    + Math.pow(pieceWorldPosition.z - squareWorldPos.z, 2));

            if (distance < threshold){
                Log.d("onTap", "distance is less: " + squareWorldPos + "Local: " + squareLocalPos);
                // Should check which space
                worldAndLocal[0] = squareWorldPos;
                worldAndLocal[1] = squareLocalPos;
//                return worldAndLocal;
                if (!isSpaceOccupied(worldAndLocal)) {
                    Log.d(TAG, "getClosestSquarePosition: isSpaceOccupied returned false - placing on new position ");
                    return worldAndLocal;
                }
                else{
                    Log.d(TAG, "getClosestSquarePosition: isSpaceOccupied returned true - placing on pickup position ");
                    return pickUpPosition;
                }
            }

        }
        Log.d("onTap", "returning last known: " + localLastKnown);
        worldAndLocal[0] = pieceWorldPosition;
        worldAndLocal[1] = pieceLocalPosition;
        return worldAndLocal;
    }

    private boolean isSpaceOccupied(Vector3[] positions) {
        for(Map.Entry<Vector3, Integer> square : isOcupied.entrySet()){
            int squareValue = square.getValue();
            Vector3 worldPose = positions[0];
            Vector3 squareKey = square.getKey();
//            Log.d(TAG, "isSpaceOccupied: worldPose" + worldPose);
//            Log.d(TAG, "isSpaceOccupied: get Key" + square.getKey());
            if(worldPose.equals(squareKey)){
//                Log.d(TAG, "isSpaceOccupied: " + square.getKey());
//                Log.d(TAG, "isSpaceOccupied: HELP MßE PLEASE");
                switch(squareValue){
                    case 0:
                        Log.d(TAG, "isSpaceOccupied: returning false" );
                        return false;
                    case 1:
                    case 2:
                        Log.d(TAG, "isSpaceOccupied: returning true");
                        return true;
                }
            }
        }
        return true;
    }


    private void createRedNodes(float x, float y, float z) {

        TransformableNode redPiece = new TransformableNode(arFragment.getTransformationSystem());
        redPiece.setParent(anchorNode);
        redPiece.setRenderable(blackPieces);
        redPiece.setSelectable(true);
        redPiece.setEnabled(true);
        Quaternion oldRotation1 = redPiece.getLocalRotation();
        Quaternion newRotation1 = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f),  90);
        redPiece.setLocalRotation(Quaternion.multiply(oldRotation1,newRotation1));
        redPiece.getScaleController().setMinScale(0.002f);
        redPiece.getScaleController().setMaxScale(0.003f);
        redPiece.setLocalPosition(new Vector3(x, y, z));
        redPiece.setOnTapListener(this);
        redPiece.setOnTouchListener(this);
        redPiece.setName("redpiece");
        Log.d(TAG, "createRedNodes: Red NODE WORLD + LOCAL: " + redPiece.getWorldPosition() + ", " + redPiece.getLocalPosition());


//        redPiece.setLocalPosition(new Vector3(0,0.05f,0));

    }

    private void createBlackNodes(float x, float y, float z) {

        TransformableNode blackPiece = new TransformableNode(arFragment.getTransformationSystem());
        blackPiece.setParent(anchorNode);
        blackPiece.setRenderable(blackPieces);
        blackPiece.setSelectable(true);
        blackPiece.setEnabled(true);
        Quaternion oldRotation1 = blackPiece.getLocalRotation();
        Quaternion newRotation1 = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f),  90);
        blackPiece.setLocalRotation(Quaternion.multiply(oldRotation1,newRotation1));
        blackPiece.getScaleController().setMinScale(0.002f);
        blackPiece.getScaleController().setMaxScale(0.003f);
        blackPiece.setLocalPosition(new Vector3(x, y, z));
        blackPiece.setName("blackpiece");
        blackPiece.setOnTapListener(this);
        blackPiece.setOnTouchListener(this);
        Log.d(TAG, "createRedNodes: Black NODE WORLD + LOCAL: " + blackPiece.getWorldPosition() + ", " + blackPiece.getLocalPosition());
        blackPiece.setParent(squareNode);

//        blackPiece.setLocalPosition(new Vector3(0,0.05f,0));
    }

}
