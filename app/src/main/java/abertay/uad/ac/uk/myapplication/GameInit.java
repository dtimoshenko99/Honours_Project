package abertay.uad.ac.uk.myapplication;

import static com.facebook.FacebookSdk.getApplicationContext;
import static abertay.uad.ac.uk.myapplication.GameTurnManager.Player.RED;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.List;

public class GameInit {
    private Context context;
    private ArFragment arFragment;
    public Renderable redPiece, blackPieces, board;
    private Anchor mainAnchor;
    private Node boardNode;
    private AnchorNode anchorNode;
    public TransformableNode redHighlightNode, greenHighlightNode, secondGreenHighlightNode, thirdGreenHighlightNode, fourthGreenHighlightNode;
    public TransformableNode[] greenHighlightArray = new TransformableNode[4];
    GameTurnManager turnManager;
    private OnPieceTouchListener onPieceTouchListener;
    private TransformableNode[][] nodesArray;
    ModelRenderable cubeRenderable;
    private String gameType;
    public List<Integer> occupiedSquares = new ArrayList<>();

    private int[][] boardArray = {
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
    private List<Vector3> squareWorldPositions = new ArrayList<>();

    public List<Vector3> getSquareWorldPositions(){
        return squareWorldPositions;
    }

    public GameInit(Context context, ArFragment arFragment, GameTurnManager turnManager, OnPieceTouchListener onPieceTouchListener, String gameType) {
        this.context = context;
        this.arFragment = arFragment;
        this.turnManager = turnManager;
        this.onPieceTouchListener = onPieceTouchListener;
        nodesArray = new TransformableNode[8][8];
        this.gameType = gameType;
    }

    public Node getBoardNode(){
        return boardNode;
    }

    public int[][] getBoardArray(){
        return boardArray;
    }

    public void setBoardArray(int[][] boardArraySet){
        this.boardArray = boardArraySet;
    }

    public ArFragment getArFragment(){
        return this.arFragment;
    }

    public Context getContext(){
        return context;
    }

    public TransformableNode[][] getNodesArray(){
        return nodesArray;
    }

    public void setNodesArray(TransformableNode[][] nodesArray) {
        this.nodesArray = nodesArray;
    }

    public void loadModels() {
        ModelRenderable.builder()
                .setSource(context, Uri.parse("models/board/scene.gltf"))
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build()
                .thenAccept(model -> {

                board = model;
                })
                .exceptionally(throwable -> {
                    Toast.makeText(
                            context, "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
//        WeakReference<SinglePlayerActivity> weakActivity1 = new WeakReference<>(this);
        ModelRenderable.builder()
                .setSource(context, Uri.parse("models/pieces/scene.gltf"))
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build()
                .thenAccept(piecesModel -> {
                    blackPieces = piecesModel;
                    redPiece = piecesModel;
                })
                .exceptionally(throwable -> {
                    Toast.makeText(context, "Unable to load white pieces" + throwable, Toast.LENGTH_LONG).show();
                    return null;
                });

        MaterialFactory.makeTransparentWithColor(context, new Color(1f, 0, 0, 1f))
                .thenAccept(material -> {
                    cubeRenderable = ShapeFactory.makeCube(new Vector3(0.118f, 0.001f, 0.118f), Vector3.zero(), material);

                    // Attach the cube renderable to the anchor node at the position of the highlighted square
                    AnchorNode highlightAnchorNode = new AnchorNode(mainAnchor);
                    highlightAnchorNode.setParent(arFragment.getArSceneView().getScene());
                    redHighlightNode = new TransformableNode(arFragment.getTransformationSystem());
                    redHighlightNode.setParent(highlightAnchorNode);
                    redHighlightNode.setRenderable(cubeRenderable);
                    redHighlightNode.setSelectable(false);
                    redHighlightNode.setEnabled(false);
                });

        MaterialFactory.makeTransparentWithColor(context, new Color(0f, 1f, 0, 1f))
                .thenAccept(material -> {
                    cubeRenderable = ShapeFactory.makeCube(new Vector3(0.118f, 0.001f, 0.118f), Vector3.zero(), material);

                    // Attach the cube renderable to the anchor node at the position of the highlighted square
                    AnchorNode highlightAnchorNode = new AnchorNode(mainAnchor);
                    highlightAnchorNode.setParent(arFragment.getArSceneView().getScene());
                    greenHighlightNode = new TransformableNode(arFragment.getTransformationSystem());
                    greenHighlightNode.setParent(highlightAnchorNode);
                    greenHighlightNode.setRenderable(cubeRenderable);
                    greenHighlightNode.setEnabled(false);
                    greenHighlightNode.setSelectable(false);
                    greenHighlightArray[0] = greenHighlightNode;
                });
        MaterialFactory.makeTransparentWithColor(context, new Color(0f, 1f, 0, 1f))
                .thenAccept(material -> {
                    cubeRenderable = ShapeFactory.makeCube(new Vector3(0.118f, 0.001f, 0.118f), Vector3.zero(), material);

                    // Attach the cube renderable to the anchor node at the position of the highlighted square
                    AnchorNode highlightAnchorNode = new AnchorNode(mainAnchor);
                    highlightAnchorNode.setParent(arFragment.getArSceneView().getScene());
                    secondGreenHighlightNode = new TransformableNode(arFragment.getTransformationSystem());
                    secondGreenHighlightNode.setParent(highlightAnchorNode);
                    secondGreenHighlightNode.setRenderable(cubeRenderable);
                    secondGreenHighlightNode.setEnabled(false);
                    secondGreenHighlightNode.setSelectable(false);
                    greenHighlightArray[1] = secondGreenHighlightNode;
                });
        MaterialFactory.makeTransparentWithColor(context, new Color(0f, 1f, 0, 1f))
                .thenAccept(material -> {
                    cubeRenderable = ShapeFactory.makeCube(new Vector3(0.118f, 0.001f, 0.118f), Vector3.zero(), material);

                    // Attach the cube renderable to the anchor node at the position of the highlighted square
                    AnchorNode highlightAnchorNode = new AnchorNode(mainAnchor);
                    highlightAnchorNode.setParent(arFragment.getArSceneView().getScene());
                    thirdGreenHighlightNode = new TransformableNode(arFragment.getTransformationSystem());
                    thirdGreenHighlightNode.setParent(highlightAnchorNode);
                    thirdGreenHighlightNode.setRenderable(cubeRenderable);
                    thirdGreenHighlightNode.setSelectable(false);
                    thirdGreenHighlightNode.setEnabled(false);
                    greenHighlightArray[2] = thirdGreenHighlightNode;
                });
        MaterialFactory.makeTransparentWithColor(context, new Color(0f, 1f, 0, 1f))
                .thenAccept(material -> {
                    cubeRenderable = ShapeFactory.makeCube(new Vector3(0.118f, 0.001f, 0.118f), Vector3.zero(), material);

                    // Attach the cube renderable to the anchor node at the position of the highlighted square
                    AnchorNode highlightAnchorNode = new AnchorNode(mainAnchor);
                    highlightAnchorNode.setParent(arFragment.getArSceneView().getScene());
                    fourthGreenHighlightNode = new TransformableNode(arFragment.getTransformationSystem());
                    fourthGreenHighlightNode.setParent(highlightAnchorNode);
                    fourthGreenHighlightNode.setRenderable(cubeRenderable);
                    fourthGreenHighlightNode.setEnabled(false);
                    fourthGreenHighlightNode.setSelectable(false);
                    greenHighlightArray[3] = fourthGreenHighlightNode;
                });
    }



    public void createAnchors(HitResult hitResult, ArFragment arFragment){
        if (board == null) {
            Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mainAnchor == null) {

            // Get the HitResult position as a float[] array
            float[] hitPositionArray = hitResult.getHitPose().getTranslation();

            // Convert the float[] array to a Vector3
            Vector3 hitPosition = new Vector3(hitPositionArray[0], hitPositionArray[1], hitPositionArray[2]);

            // Modify the X-axis (or any other axis)
            float newX = 0; // Set the board in the centre of the screen
            Vector3 newPosition = new Vector3(newX, hitPosition.y, hitPosition.z);

            // Create a new translation Pose
            Pose newTranslationPose = Pose.makeTranslation(newPosition.x, newPosition.y, newPosition.z);

            // Create a new rotation Pose from the original HitResult rotation
            Pose newRotationPose = Pose.makeRotation(0, -0.05f, 0, 0);

            // Combine the translation and rotation Poses
            Pose newPose = newTranslationPose.compose(newRotationPose);

            // Create an Anchor at the new Pose
            mainAnchor = arFragment.getArSceneView().getSession().createAnchor(newPose);

            anchorNode = new AnchorNode(mainAnchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            // Create a Node to hold anchorNode
            boardNode = new Node();
            boardNode.setParent(anchorNode);
            boardNode.setRenderable(this.board);
            boardNode.setLocalScale(new Vector3(0.02f, 0.02f, 0.02f));
            boardNode.setLocalPosition(new Vector3(0f, -0.10f, 0f));

            // Rotate the board 90 degrees
            Quaternion rotationLocal = Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f),  90);
            boardNode.setLocalRotation(rotationLocal);

            // Disable white dots which show the place on the plane that the board can be placed on
            arFragment.getArSceneView().getPlaneRenderer().setVisible(false);

            // Populate the board with pieces
            populateBoard();

            if(!gameType.equals("MultiPlayer")){
                turnManager.switchTurnAndUpdateSelectableNodes(arFragment);
                turnManager.updateTurnIndicator();
            }
            //Decide who plays first and update Node's selectivity

        } else {
            Toast.makeText(context, "The board is already placed, you can change the position by moving the board", Toast.LENGTH_LONG).show();
        }
    }

    // POPULATE BOARD WITH PIECES
    private void populateBoard() {
        float boardX = boardNode.getWorldPosition().x - 0.415f;
        float boardY = boardNode.getWorldPosition().y + 0.05f;
        float boardZ = boardNode.getWorldPosition().z - 0.415f;
        float squareSize = 0.235f;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                float squareX = boardX + col * (squareSize / 2);
                float squareY = boardY;
                float squareZ = boardZ + row * (squareSize / 2);


                squareWorldPositions.add(new Vector3(squareX, squareY, squareZ));
                int pieceType = boardArray[row][col];
                // Create and Populate black nodes
                if (pieceType == 1) {
                    String BLACK_NODE = "blackPiece";
                    createPieceNode(squareX, squareY, squareZ, BLACK_NODE, row, col);
                }
                // Create and populate red nodes
                else if (pieceType == 2) {
                    String RED_NODE = "redPiece";
                    createPieceNode(squareX, squareY, squareZ, RED_NODE, row, col);
                }
            }
        }
    }

    private void createPieceNode(float x, float y, float z, String node, int row, int col) {
        Pose pose = Pose.makeTranslation(x, y, z);
        Anchor pieceAnchor = arFragment.getArSceneView().getSession().createAnchor(pose);


        AnchorNode pieceAnchorNode = new AnchorNode(pieceAnchor);
        pieceAnchorNode.setParent(arFragment.getArSceneView().getScene());

        TransformableNode piece = new TransformableNode(arFragment.getTransformationSystem());
        piece.setParent(pieceAnchorNode);

        nodesArray[row][col] = piece;

        if(node.equals("redPiece")){
            piece.setRenderable(redPiece);
        }else{
            piece.setRenderable(blackPieces);
        }
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
        piece.setOnTouchListener((hitTestResult, event) -> onPieceTouchListener.onPieceTouch(hitTestResult, event));
        pieceAnchorNode.setWorldPosition(new Vector3(x, y, z));
    }

}
