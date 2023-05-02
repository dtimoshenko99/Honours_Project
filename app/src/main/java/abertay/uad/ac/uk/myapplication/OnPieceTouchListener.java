package abertay.uad.ac.uk.myapplication;

import android.view.MotionEvent;

import com.google.ar.sceneform.HitTestResult;

public interface OnPieceTouchListener {
    boolean onPieceTouch(HitTestResult hitTestResult, MotionEvent motionEvent);
}
