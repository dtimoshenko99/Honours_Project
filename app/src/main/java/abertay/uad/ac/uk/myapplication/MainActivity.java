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

    private ArFragment arFragment;
    private Renderable whitePieces, blackPieces, board;
    private Anchor anchor, anchor2;
    Node boardNode;
    AnchorNode anchorNode;

    Vector3 localLastKnown;
    Vector3 worldLastKnown;

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
    // Map to hold Position to Square variables
    Map<Integer, ArrayList> squarePositionsMap = new HashMap<>();


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

            AnchorNode pieceAnchor = new AnchorNode(anchor2);
            pieceAnchor.setParent(arFragment.getArSceneView().getScene());

            anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());
//            anchorNode.setSelectable(false);

            Node pieceNode = new TransformableNode(arFragment.getTransformationSystem());
            pieceNode.setParent(anchorNode);

            boardNode = new Node();
            boardNode.setParent(anchorNode);
            boardNode.setRenderable(this.board);
            boardNode.setLocalScale(new Vector3(0.02f, 0.02f, 0.02f));
            boardNode.setLocalPosition(new Vector3(0f, 0f, 0f));
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

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares += 1;
                float squareX = boardX + col * squareSize;
                float squareY = boardY;
                float squareZ = boardZ + row * squareSize;

                // TODO: need to create an array to hold all anchors and their position to then map them to square numbers
                squarePositions.add(new Vector3(squareX, squareY, squareZ));


                int pieceType = boardStart[row][col];

                // Create and Populate black nodes
                if (pieceType == 1) {
                    TransformableNode blackPiece = new TransformableNode(arFragment.getTransformationSystem());
                    blackPiece.setRenderable(blackPieces);
                    blackPiece.setSelectable(true);
                    blackPiece.setEnabled(true);
                    Quaternion oldRotation1 = blackPiece.getLocalRotation();
                    Quaternion newRotation1 = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f),  90);
                    blackPiece.setLocalRotation(Quaternion.multiply(oldRotation1,newRotation1));
                    blackPiece.getScaleController().setMinScale(0.002f);
                    blackPiece.getScaleController().setMaxScale(0.003f);
                    blackPiece.setLocalPosition(new Vector3(squareX, squareY, squareZ));
                    blackPiece.setName("blackpiece");
                    blackPiece.setOnTapListener(this::onTap);
                    blackPiece.setOnTouchListener(this);


                    AnchorNode blackSquare = new AnchorNode(anchor);
                    blackSquare.setParent(arFragment.getArSceneView().getScene());
                    blackSquare.addChild(blackPiece);
                }
                // Create and populate red nodes
                else if (pieceType == 2) {
                    TransformableNode redPiece = new TransformableNode(arFragment.getTransformationSystem());
                    redPiece.setRenderable(blackPieces);
                    redPiece.setSelectable(true);
                    redPiece.setEnabled(true);
                    Quaternion oldRotation1 = redPiece.getLocalRotation();
                    Quaternion newRotation1 = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f),  90);
                    redPiece.setLocalRotation(Quaternion.multiply(oldRotation1,newRotation1));
                    redPiece.getScaleController().setMinScale(0.002f);
                    redPiece.getScaleController().setMaxScale(0.003f);
                    redPiece.setLocalPosition(new Vector3(squareX, squareY, squareZ));
                    redPiece.setOnTapListener(this::onTap);
                    redPiece.setOnTouchListener(this);
                    redPiece.setName("redpiece");

                    Log.d("redPiece", "PUT PIECE ON: X= " + squareX + "Y= " + squareY + "Z= " + squareZ );
                    AnchorNode redSquare = new AnchorNode(anchor);
                    redSquare.setParent(arFragment.getArSceneView().getScene());
                    redSquare.addChild(redPiece);
                }

            }
        }
    }

    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
        Node selct = hitTestResult.getNode();

        Log.d("onTap", "onTap: The node has been tapped" + motionEvent + hitTestResult + selct);
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
                    AnchorNode detach = (AnchorNode) transformableNode.getParent();
                    detach.setAnchor(null);

                    // Debugging purposes
                    Log.d("onTap", "ACTION DOWN: The node has been DOWNED"
                            + detach.getName()
                            + motionEvent
                            + hitTestResult
                            + node
                            + node.getName());
                    break;
                case MotionEvent.ACTION_UP:

                    Vector3 worldPosition = worldLastKnown;
                    float[] pos = new float[] {worldPosition.x, worldPosition.y + 0.05f, worldPosition.z};
                    float[] iden = new float[] {Quaternion.identity().x, Quaternion.identity().y, Quaternion.identity().z, Quaternion.identity().w};
                    Anchor anchorSet = arFragment.getArSceneView().getSession().createAnchor(new Pose(pos, iden));
                    AnchorNode anchorSetNode = new AnchorNode(anchorSet);
                    anchorSetNode.setParent(arFragment.getArSceneView().getScene());
                    transformableNode.setParent(anchorSetNode);
                    transformableNode.setLocalPosition(localLastKnown);
                    transformableNode.setRenderable(blackPieces);

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

    // TODO: three functions
    // 1. Create association between square number and square local position - Done, created a map
    // 2. position relative to the board - take coordinates and provide closest square number
    // 3. take square number and return coordinates

    private void positionToSquare(){
        int squareN = 1;
        for (int i = 0; i < 8; i++){
            for (int j =0; j < 8; j++){
                squarePositionsMap.put(squareN, squarePositions);
                squareN++;
            }
        }
    }
    //
    private void getClosestSquareNumber(Vector3 position){

    }

    private void getPieceSquare(){

    }
}