package abertay.uad.ac.uk.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.firebase.firestore.FirebaseFirestore;

import org.checkerframework.checker.nullness.qual.NonNull;

public class MultiPlayerActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener,
        Node.OnTapListener,
        OnPieceTouchListener
{

    private final String gameType = "MultiPlayer";

    GameInit gameInitialization;
    GameTurnManager turnManager;
    GameLogic gameLogic;
    MultiplayerGameLogic multiplayerGameLogic;
    FirebaseFirestore db;
    private ArFragment arFragment;
    public String lobbyId, hostUserId, guestUserId;
    public boolean isHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_player);

        db = FirebaseFirestore.getInstance();
        Intent intent = getIntent();
        lobbyId = intent.getStringExtra("lobbyId");
        db.collection("lobbies").document(lobbyId).delete();
        if(intent.getStringExtra("hostUserId") != null){
            hostUserId = intent.getStringExtra("hostUserId");
            isHost = true;
        }else{
            guestUserId = intent.getStringExtra("guestUserId");
            isHost = false;
        }

        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
            arFragment.setOnViewCreatedListener(this);
            arFragment.setOnTapArPlaneListener(this);
        }
        turnManager = new GameTurnManager(this, isHost);
        gameInitialization = new GameInit(this, arFragment, turnManager, this, gameType);
        gameInitialization.loadModels();
        gameLogic = new GameLogic(turnManager, gameInitialization);
        multiplayerGameLogic = new MultiplayerGameLogic(turnManager, gameLogic, gameInitialization, db, lobbyId, isHost);
    }


    @Override
    public void onViewCreated(ArSceneView arSceneView) {
        arFragment.setOnViewCreatedListener(null);

        // Fine adjust the maximum frame rate
        arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL);
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        }
    }

    @Override
    public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
        gameInitialization.createAnchors(hitResult, arFragment);
        multiplayerGameLogic.gameStart();
    }

    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
        // do nothing on tap
    }

    @Override
    public boolean onPieceTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
        return multiplayerGameLogic.onTouchMethodMultiplayer(hitTestResult, motionEvent);
    }
}