package abertay.uad.ac.uk.myapplication;

import static abertay.uad.ac.uk.myapplication.GameTurnManager.Player.RED;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class GameTurnManager {
    public enum Player {
        RED("redPiece", "red"),
        BLACK("blackPiece", "black");

        private String nodeName;
        private String color;

        Player(String nodeName, String color) {

            this.nodeName = nodeName;
            this.color = color;
        }

        public String getNodeName() {
            return nodeName;
        }
        public String getColor(){ return color;}
    }

    public boolean isHost;
    public String userColor;

    public Player currentPlayer;
    private Context context;

    public GameTurnManager(Context context) {
        int random = (Math.random() <= 0.5) ? 1 : 2;
        if(random == 1){
            currentPlayer = Player.RED;
        } else{
            currentPlayer = Player.BLACK;
        }
        this.context = context;
    }

    public GameTurnManager(Context context, boolean isHost){
        this.context = context;
        this.isHost = isHost;
        currentPlayer = Player.BLACK;
    }

    public String getUserColor(){
        if(isHost){
            userColor = "black";
        }else{
            userColor = "red";
        }
        return userColor;
    }

    public boolean isMoveAllowed(String nodeName) {
        if (currentPlayer == Player.RED && nodeName.equals("redPiece")) {
            return true;
        } else if (currentPlayer == Player.BLACK && nodeName.equals("blackPiece")) {
            return true;
        } else {
            return false;
        }
    }

    public void switchTurn() {
        Log.d("onTap", "switchTurn: player was: " + currentPlayer);
        if (currentPlayer == Player.RED) {
            currentPlayer = Player.BLACK;
        } else {
            currentPlayer = Player.RED;
        }

        Log.d("onTap", "switchTurn: player now: " + currentPlayer);
    }

    private void updateSelectableNodes(ArFragment arFragment ,String playersNode) {
        // Iterate through all nodes in the scene
        for (Node node : arFragment.getArSceneView().getScene().getChildren()) {
            // Check if the node is a TransformableNode
            if (node instanceof TransformableNode) {
                TransformableNode transformableNode = (TransformableNode) node;

                // If the node's name matches the current player's name, make it selectable
                if (transformableNode.getName().equals(playersNode)) {
                    transformableNode.setSelectable(true);
                } else {
                    // Otherwise, make it not selectable
                    transformableNode.setSelectable(false);
                }
            }
            // Iterate the hierarchy
            for (Node node1 : node.getChildren()) {
                // Check if the node is a TransformableNode
                if (node1 instanceof TransformableNode) {
                    TransformableNode transformableNode = (TransformableNode) node1;

                    // If the node's name matches the current player's name, make it selectable
                    if (transformableNode.getName().equals(playersNode)) {
                        transformableNode.setSelectable(true);
                    } else {
                        // Otherwise, make it not selectable
                        transformableNode.setSelectable(false);
                    }
                }
            }
        }

    }

    public void updateAllNodesSelectableToFalse(ArFragment arFragment) {
        // Iterate through all nodes in the scene
        for (Node node : arFragment.getArSceneView().getScene().getChildren()) {
            // Check if the node is a TransformableNode
            if (node instanceof TransformableNode) {
                TransformableNode transformableNode = (TransformableNode) node;
                transformableNode.setSelectable(false);

            }
            // Iterate the hierarchy
            for (Node node1 : node.getChildren()) {
                // Check if the node is a TransformableNode
                if (node1 instanceof TransformableNode) {
                    TransformableNode transformableNode = (TransformableNode) node1;
                    transformableNode.setSelectable(false);

                }
            }
        }
    }


    public void switchTurnAndUpdateSelectableNodes(ArFragment arFragment) {
        switchTurn();
        String nodeName = (currentPlayer == Player.RED) ? "redPiece" : "blackPiece";
        updateSelectableNodes(arFragment, nodeName);
    }

    public void updateTurnIndicator() {
        TextView turnIndicator = (TextView) ((Activity)context).findViewById(R.id.turnIndicator);
        String currentPlayerName = currentPlayer == RED ? "Red" : "Black";
        turnIndicator.setText("Turn: " + currentPlayerName);
    }

    public void UpdateSelectableNodesMultiplayer(ArFragment arFragment, boolean isHost) {
        String nodeName;
        if(currentPlayer == Player.RED && isHost){
            updateAllNodesSelectableToFalse(arFragment);
        } else if(currentPlayer == Player.RED && !isHost){
            nodeName = "redPiece";
            updateSelectableNodes(arFragment, nodeName);
        } else if(currentPlayer == Player.BLACK && isHost){
            nodeName = "blackPiece";
            updateSelectableNodes(arFragment, nodeName);
        }else if(currentPlayer == Player.BLACK && !isHost){
            updateAllNodesSelectableToFalse(arFragment);
        }
        updateTurnIndicatorMultiplayer();
    }

    public void updateTurnIndicatorMultiplayer() {
        TextView turnIndicator = (TextView) ((Activity)context).findViewById(R.id.turnIndicator);
        String currentPlayerName = null;
        if(currentPlayer == Player.RED && isHost){
            currentPlayerName = "Red's Turn, Please Wait";
        } else if(currentPlayer == Player.RED && !isHost){
            currentPlayerName = "Your Turn - Red";
        } else if(currentPlayer == Player.BLACK && isHost){
            currentPlayerName = "Your Turn - Black";
        }else if(currentPlayer == Player.BLACK && !isHost){
            currentPlayerName = "Black's Turn, Please Wait";
        }
        turnIndicator.setText("Turn: " + currentPlayerName);
    }
}