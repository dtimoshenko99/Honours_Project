package abertay.uad.ac.uk.myapplication;

public class dumb {


//    private void setpiece2() {
//        TransformableNode whitePieces2 = new TransformableNode(arFragment.getTransformationSystem());
////        whitePieces2.setParent(pieceAnchor);
//        whitePieces2.setRenderable(this.whitePieces);
//        Quaternion oldRotation1 = whitePieces2.getLocalRotation();
//        Quaternion newRotation1 = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f),  90);
//        whitePieces2.setLocalRotation(Quaternion.multiply(oldRotation1,newRotation1));
//        whitePieces2.getScaleController().setMaxScale(0.004f);
//        whitePieces2.getScaleController().setMinScale(0.003f);
//        whitePieces2.setLocalPosition(new Vector3(0.06f, 0.05f, 0.42f));
//        whitePieces2.setEnabled(true);
//        pieceNode.addChild(whitePieces2);
//        whitePieces2.setOnTapListener((hitTestResult, motionEvent) -> {
//        });
//
//    }
//
//
//    private void setpiece1() {
//        TransformableNode whitePieces = new TransformableNode(arFragment.getTransformationSystem());
////        whitePieces.setParent(pieceAnchor);
//        whitePieces.setRenderable(this.whitePieces);
//        Quaternion oldRotation = whitePieces.getLocalRotation();
//        Quaternion newRotation = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f),  90);
//        whitePieces.setLocalRotation(Quaternion.multiply(oldRotation,newRotation));
//        whitePieces.getScaleController().setMaxScale(0.004f);
//        whitePieces.getScaleController().setMinScale(0.003f);
//        whitePieces.setLocalPosition(new Vector3(0.3f, 0.05f, 0.42f));
//        //-0.42 left low corner, next: -0.18, next : 0.06, next: 0.3, last: 0.54
//        whitePieces.setEnabled(true);
//        pieceNode.addChild(whitePieces);
//    }
//}
//
//        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
//
//        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
//            Anchor anchor = hitResult.createAnchor();
//
//            ModelRenderable.builder()
//                    .setSource(this, Uri.parse("models/board/scene.gltf"))
//                    .setIsFilamentGltf(true)
//                    .build()
//                    .thenAccept(modelRenderable -> addModelToScene(anchor, modelRenderable))
//                    .exceptionally(throwable -> {
//                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                        builder.setMessage(throwable.getMessage())
//                                .show();
//                        return null;
//                    });
//        });
//    }
//
//    private void addModelToScene(Anchor anchor, ModelRenderable modelRenderable) {
//        AnchorNode anchorNode = new AnchorNode(anchor);
//        TransformableNode transNode = new TransformableNode(arFragment.getTransformationSystem());
//        transNode.getScaleController().setMinScale(0.4f);
//        transNode.getScaleController().setMaxScale(1f);
//        transNode.setLocalScale(new Vector3(0.55f, 0.55f, 0.55f));
//        transNode.setParent(anchorNode);
//        transNode.setRenderable(modelRenderable);
//        arFragment.getArSceneView().getScene().addChild(anchorNode);
//        transNode.select();
//    }





//        Node titleNode = new Node();
//        titleNode.setParent(model);
//        titleNode.setEnabled(false);
//        titleNode.setLocalPosition(new Vector3(0.0f, .5f, 0.0f));
//        titleNode.setRenderable(viewRenderable);
//        titleNode.setEnabled(true);



    //        ViewRenderable.builder()
//                .setView(this, R.layout.view_model_title)
//                .build()
//                .thenAccept(viewRenderable -> {
//                    MainActivity activity = weakActivity.get();
//                    if (activity != null) {
//                        activity.viewRenderable = viewRenderable;
//                    }
//                })
//                .exceptionally(throwable -> {
//                    Toast.makeText(this, "Unable to load model", Toast.LENGTH_LONG).show();
//                    return null;
//                });
}

//    public void loadWhitePieces() {
//        WeakReference<MainActivity> weakActivity = new WeakReference<>(this);
//        ModelRenderable.builder()
//                .setSource(this, Uri.parse("models/pieces/scene.gltf"))
//                .setIsFilamentGltf(true)
//                .build()
//                .thenAccept(whitePieces -> {
//                    MainActivity activity = weakActivity.get();
//                    if(activity != null){
//                        activity.whitePieces = whitePieces;
//                    }
//                })
//                .exceptionally(throwable -> {
//                    Toast.makeText(this, "Unable to load white pieces", Toast.LENGTH_LONG).show();
//                    return null;
//                });
//    }
//
//     Test to find out which pieces I already have
//    public void loadBlackPieces() {
//        WeakReference<MainActivity> weakActivity = new WeakReference<>(this);
//        ModelRenderable.builder()
//                .setSource(this, Uri.parse("models/board/pieces/scene.gltf"))
//                .setIsFilamentGltf(true)
//                .setAsyncLoadEnabled(true)
//                .build()
//                .thenAccept(piecesModel -> {
//                    MainActivity activity = weakActivity.get();
//                    if(activity != null){
//                        activity.blackPieces = blackPieces;
//                    }
//                })
//                .exceptionally(throwable -> {
//                    Toast.makeText(this, "Unable to load white pieces", Toast.LENGTH_LONG).show();
//                    return null;
//                });
//    }

//|| whitePieces == null

//
//    // CREATE BLACK PIECES
//    private Node createBlackPieceNode() {
////        Log.d("POPULATE", "CREATING BLACK PIECES");
////        WeakReference<MainActivity> weakActivity = new WeakReference<>(this);
////        ModelRenderable.builder()
////                .setSource(this, Uri.parse("models/pieces/scene.gltf"))
////                .setIsFilamentGltf(true)
////                .setAsyncLoadEnabled(true)
////                .build()
////                .thenAccept(piecesModel -> {
////                    MainActivity activity = weakActivity.get();
////                    if (activity != null) {
////                        activity.blackPieces = blackPieces;
////                    }
////                    pieceNode.setRenderable(piecesModel);
////                })
////                .exceptionally(throwable -> {
////                    Toast.makeText(this, "Unable to load white pieces", Toast.LENGTH_LONG).show();
////                    return null;
////                });
////        Log.d("POPULATE", "RETURNING RED PIECE NODE:" + pieceNode);
////        return pieceNode;
//        pieceNode.setRenderable(blackPieces);
//        return pieceNode;
//    }
//
//    // CREATE RED PIECES
//    private Node createRedPieceNode() {
////        WeakReference<MainActivity> weakActivity = new WeakReference<>(this);
////        ModelRenderable.builder()
////                .setSource(this, Uri.parse("models/pieces/scene.gltf"))
////                .setIsFilamentGltf(true)
////                .setAsyncLoadEnabled(true)
////                .build()
////                .thenAccept(piecesModel -> {
////                    MainActivity activity = weakActivity.get();
////                    if (activity != null) {
////                        activity.whitePieces = whitePieces;
////                    }
////                    pieceNode.setRenderable(piecesModel);
////                })
////                .exceptionally(throwable -> {
////                    Toast.makeText(this, "Unable to load white pieces", Toast.LENGTH_LONG).show();
////                    return null;
////                });
////        Log.d("POPULATE", "RETURNING RED PIECE NODE:" + pieceNode);
////        return pieceNode;
//        pieceNode.setRenderable(whitePieces);
//        return pieceNode;
//    }
//}

//            TransformableNode redPieces = new TransformableNode(arFragment.getTransformationSystem());
//            redPieces.setRenderable(blackPieces);
//            redPieces.setSelectable(true);
//            redPieces.setEnabled(true);
//            Quaternion oldRotation = redPieces.getLocalRotation();
//            Quaternion newRotation = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f),  90);
//            redPieces.setLocalRotation(Quaternion.multiply(oldRotation,newRotation));
//            redPieces.getScaleController().setMinScale(0.003f);
//            redPieces.getScaleController().setMaxScale(0.004f);
////            redPieces.setLocalScale(new Vector3( 0.004f, 0.004f, 0.004f));
//            redPieces.setLocalPosition(new Vector3(-0.4f, 0.1f, 0f));
//
//            pieceAnchor.addChild(redPieces);
//
//            TransformableNode redP = new TransformableNode(arFragment.getTransformationSystem());
//            redP.setRenderable(blackPieces);
//            redP.setSelectable(true);
//            redP.setEnabled(true);
//            Quaternion oldRotation1 = redP.getLocalRotation();
//            Quaternion newRotation1 = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f),  90);
//            redP.setLocalRotation(Quaternion.multiply(oldRotation1,newRotation1));
//            redP.getScaleController().setMinScale(0.003f);
//            redP.getScaleController().setMaxScale(0.004f);
////            redPieces.setLocalScale(new Vector3( 0.004f, 0.004f, 0.004f));
//            redP.setLocalPosition(new Vector3(0.4f, 0.1f, 0f));
//
//            pieceAnchor.addChild(redP);
//            Toast.makeText(this, "Selectable: " + redPieces.isSelectable(), Toast.LENGTH_SHORT).show();

//            populateBoard();