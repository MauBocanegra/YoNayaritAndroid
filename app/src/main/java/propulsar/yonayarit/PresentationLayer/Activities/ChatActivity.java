package propulsar.yonayarit.PresentationLayer.Activities;

import android.animation.Animator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import propulsar.yonayarit.DomainLayer.Adapters.GalleryAdapter;
import propulsar.yonayarit.DomainLayer.Adapters.MsgAdapter;
import propulsar.yonayarit.DomainLayer.Objects.Msg;
import propulsar.yonayarit.DomainLayer.Objects.OtherMsg;
import propulsar.yonayarit.DomainLayer.Objects.OwnMsg;
import propulsar.yonayarit.DomainLayer.WS;
import propulsar.yonayarit.Manifest;
import propulsar.yonayarit.R;

public class ChatActivity extends AppCompatActivity implements
        WS.OnWSRequested,
        OnMapReadyCallback,
        GalleryAdapter.GalleryLoadedListener{

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private android.support.v7.widget.LinearLayoutManager mLayoutManager;
    private SwipeRefreshLayout swipeRefreshLayout;

    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION=123;
    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE=321;
    final int MY_PERMISSIONS_REQUEST_CAMERA=231;
    final int PHOTO_ACTIVITY_REQUEST=31;
    final int GALLERY_CHOOSER=12;
    GoogleApiClient mGoogleApiClient;

    private ArrayList<Msg> messages;
    int userID;
    private int skipIni=0; private int takeIni=10;
    private int skip=skipIni;
    private int take=takeIni;

    int scaleIni = 0;
    int scaleFin = 1;
    boolean enabled = false;

    Handler h;
    Runnable getMessagesRepeatedly;

    private EditText editText;
    private FloatingActionButton fab;
    private ProgressBar progress;
    private View buttonLocation;
    private View buttonImg;
    private FloatingActionButton buttonCam;
    private FloatingActionButton buttonGallery;
    private View cardEditText;

    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private View mapContainer;
    private MarkerOptions mMarkerOptions;
    private Marker mMarker;
    private boolean locationJustSent;
    private double lat; private double lon;
    private int initTime;

    private View galleryContainer;
    private GalleryAdapter mGalleryAdapter;
    private GridView gallery;
    private View progressGalleryLoaded;

    private int visibleItemCount;
    private int totalItemCount;
    private int pastVisiblesItems;
    private boolean topRequested=false;

    // ----------------------------------------------- //
    // ----------------- LYFE CYCLE ------------------ //
    // ----------------------------------------------- //


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:{
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation();
                } else {
                    (WS.getInstance(ChatActivity.this)).showSucces("Para enviar tu ubicación debes permitirnos obtenerla",buttonLocation);
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:{
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showGallery();
                }else{
                    (WS.getInstance(ChatActivity.this)).showSucces("Para enviar una imágen debes permitir el acceso a tus archivos",buttonImg);
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_CAMERA:{
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showCamera();
                }else{
                    (WS.getInstance(ChatActivity.this)).showSucces("Para capturar una foto debes permitir el acceso a la misma",buttonImg);
                }
                return;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPrefName), 0);
        userID = sharedPreferences.getInt("userID",0);

        //Primero instanciamos el mapa ya que es lo mas tardado
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(ChatActivity.this);
        mapContainer = findViewById(R.id.mapContainer);


        //Asignamos el toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarChat);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Preparamos el adapter de los mensajes asi como las vistas que lo contienen
        messages = new ArrayList<Msg>();
        mRecyclerView = (RecyclerView)findViewById(R.id.msgRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MsgAdapter(messages, ChatActivity.this);
        mRecyclerView.setAdapter(mAdapter);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.chatSwipeRefreshLayout);
        swipeRefreshLayout.setEnabled(false);

        //asignamos las vistas a los objetos de la clase
        fab = (FloatingActionButton) findViewById(R.id.fab_sendMessage);
        editText = (EditText)findViewById(R.id.editText_mensaje);
        progress = (ProgressBar)findViewById(R.id.progress_chat);
        buttonLocation = findViewById(R.id.buttonLocation);
        buttonImg = findViewById(R.id.buttonImage);
        cardEditText = findViewById(R.id.cardMensaje);
        galleryContainer = findViewById(R.id.galleryContainer);
        gallery = (GridView)findViewById(R.id.gridview);
        buttonCam = (FloatingActionButton) findViewById(R.id.fab_camera);
        buttonGallery = (FloatingActionButton)findViewById(R.id.fab_gallery);
        progressGalleryLoaded = findViewById(R.id.progressGalleryLoading);

        //Establecemos los clicks de lo clickeable
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickSend(view);
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                Log.d("TextEditListener","i="+i+" key="+keyEvent.toString());
                return false;
            }
        });
        buttonLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickLocation();
            }
        });
        buttonImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(galleryContainer.getVisibility()==View.VISIBLE){
                    hideGallery();
                }else{
                    clickGallery();
                }
            }
        });
        buttonCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickCamera();
            }
        });
        buttonGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickGalleryChooser();
            }
        });

        //Listener del scroll
        mRecyclerView.addOnScrollListener(setScrollListener());
        getMessages(false);


        //Por ultimo realizamos la conexion con google para el awareness
        mGoogleApiClient = new GoogleApiClient.Builder(ChatActivity.this)
                .addApi(Awareness.API)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    protected void onStart() {
        super.onStart();
        //Mandamos a actualizar los mensajes cada n segundos
        h = new Handler(){
            @Override
            public void  handleMessage(Message msg){
                switch(msg.what){
                    case 0:
                        this.removeMessages(0);
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        };

        getMessagesRepeatedly = new Runnable(){
            public void run(){
                //do something
                getMessages(false);
                h.postDelayed(this, initTime+=30000);
            }
        };
        h.postDelayed(getMessagesRepeatedly, 30000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        h.removeCallbacks(getMessagesRepeatedly);
    }

    // ----------------------------------------------- //
    // ----------------- OWN METHODS ----------------- //
    // ----------------------------------------------- //

    private void clickGalleryChooser(){

        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setType("image/*");
        Intent chooserIntent = Intent.createChooser(pickIntent, "Seleccionar imágen");
        startActivityForResult(chooserIntent, GALLERY_CHOOSER);
    }

    private void showCamera(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, PHOTO_ACTIVITY_REQUEST);
        }
    }

    private void clickCamera(){
        if (ContextCompat.checkSelfPermission(ChatActivity.this,
                android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(ChatActivity.this,
                    new String[]{android.Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }else{
            showCamera();
        }
    }

    private void clickGallery(){
        if (ContextCompat.checkSelfPermission(ChatActivity.this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(ChatActivity.this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }else{
            showGallery();
        }
    }

    private void showGallery(){

        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        hideMap();

        if(mGalleryAdapter == null){
            mGalleryAdapter = new GalleryAdapter(ChatActivity.this);
            gallery.setAdapter(mGalleryAdapter);
            mGalleryAdapter.setGalleryLoadedListener(ChatActivity.this);
            gallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    //((ImageView)view)
                    /*
                    Map<String, Object> params = new LinkedHashMap<>();
                    params.put("userID",""+userID);
                    params.put("link","201704261909.jpeg/"+userID+"/3");
                    params.put("name","201704261909");
                    params.put("namefull","201704261909.jpeg");
                    params.put("image",((BitmapDrawable)((ImageView)view).getDrawable()).getBitmap());
                    WS.getInstance(ChatActivity.this).uploadImage(params,ChatActivity.this);
                    */
                    final String tag = ((ImageView)view).getTag().toString();
                    Log.d("UPLOADING","------- requested : "+tag);
                    new AsyncTask<Void,Void,Void>(){
                        @Override
                        protected Void doInBackground(Void... voids) {
                            uploadFile(tag);
                            return null;
                        }
                    }.execute();
                }
            });
        }

        galleryContainer.setVisibility(View.VISIBLE);
    }

    private void hideGallery(){
        galleryContainer.setVisibility(View.GONE);
    }

    private void showMap(){
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        hideGallery();

        Handler h3 = new Handler();
        h3.postDelayed(new Runnable(){
            public void run(){
                fab.setImageDrawable(ContextCompat.getDrawable(ChatActivity.this, R.drawable.ic_add_location));
                buttonImg.setVisibility(View.GONE);
                cardEditText.setVisibility(View.GONE);
                ((ImageView)buttonLocation).setImageDrawable(ContextCompat.getDrawable(ChatActivity.this, R.drawable.ic_back));
                //tituloAccion.setVisibility(View.VISIBLE);
            }
        }, 100);
    }

    private void hideMap(){
        fab.setImageDrawable(ContextCompat.getDrawable(ChatActivity.this, R.drawable.ic_send_material));
        buttonImg.setVisibility(View.VISIBLE);
        cardEditText.setVisibility(View.VISIBLE);
        ((ImageView)buttonLocation).setImageDrawable(ContextCompat.getDrawable(ChatActivity.this, R.drawable.ic_place));


        Handler h3 = new Handler();
        h3.postDelayed(new Runnable(){
            public void run(){
                mapContainer.setVisibility(View.GONE);

                try{ mMap.setMyLocationEnabled(false);}
                catch (SecurityException e){e.printStackTrace();}
            }
        }, 300);
    }

    private void getLocation(){

        if(mapContainer.getVisibility()==View.VISIBLE){
            hideMap();
            return;
        }

        try {
        Awareness.SnapshotApi.getLocation(mGoogleApiClient).setResultCallback(new ResultCallback<LocationResult>() {
            @Override
            public void onResult(@NonNull LocationResult locationResult) {
                if (!locationResult.getStatus().isSuccess()) {
                    Log.e("LocationAwareness", "Could not detect user location");
                    (WS.getInstance(ChatActivity.this)).showError("No pudimos obtener tu ubicación, intenta nuevamente", buttonLocation);
                    return;
                }

                if(mMap==null){
                    (WS.getInstance(ChatActivity.this)).showError("Ocurrió un error al mostrar el mapa, intenta nuevamente", buttonLocation);
                    return;
                }

                showMap();

                try{ mMap.setMyLocationEnabled(true);}
                catch (SecurityException e){e.printStackTrace();}

                Location location = locationResult.getLocation();
                lat=location.getLatitude();
                lon=location.getLongitude();
                mMarkerOptions = new MarkerOptions().position(new LatLng(location.getLatitude(),location.getLongitude()));
                mapContainer.setVisibility(View.VISIBLE);
                CameraPosition mCameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(),location.getLongitude()))
                        .zoom(15)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
                if(mMarker==null)
                mMarker = mMap.addMarker(mMarkerOptions);

                Handler h2 = new Handler();
                h2.postDelayed(new Runnable(){
                    public void run(){
                        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                            @Override
                            public void onCameraMove() {
                                LatLng latlon = mMap.getCameraPosition().target;
                                mMarker.setPosition(latlon);
                                lat=latlon.latitude; lon=latlon.longitude;
                            }
                        });
                    }
                }, 3000);

            }
        });
        }catch(SecurityException ex){}
    }

    private void onClickLocation(){
        if (ContextCompat.checkSelfPermission(ChatActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(ChatActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }else{
            getLocation();
        }
    }

    private void getMessages(boolean isTopList){

        swipeRefreshLayout.setRefreshing(true);

        if(isTopList){
            skip+=take;
        }else{
            skip=skipIni; take=takeIni;
            messages.clear();
            mAdapter.notifyDataSetChanged();
        }

        SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPrefName), 0);
        userID = sharedPreferences.getInt("userID",0);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("UserId",userID);
        params.put("Skip",skip);
        params.put("Take",take);
        WS.getInstance(ChatActivity.this).getMessages(params,this);
    }

    private void addToList(ArrayList<Msg> newMsgs){

        for(int i=0; i<newMsgs.size(); i++){
            messages.add(newMsgs.get(i));
        }
        mAdapter.notifyDataSetChanged();
        mLayoutManager.scrollToPosition(messages.size());

        swipeRefreshLayout.setRefreshing(false);

        //Log.d("DebCases","casesLength="+benefs.size());
        //mSwipeRefreshLayout.setRefreshing(false);
        //bottomRequested=false;
    }

    //Gestionar el envio de mensajes mediante la accion del FAB
    private void clickSend(View view){

        if(mapContainer.getVisibility()==View.VISIBLE){
            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);

            builder.setTitle("¿Deseas enviar la ubicación establecida por el marcador rojo");
            // Add the buttons
            builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                scaleIni = 1;
                scaleFin = 0;
                fabAnimate(false);
                locationJustSent=true;

                SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPrefName), 0);
                userID = sharedPreferences.getInt("userID",0);
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("UserId",userID);
                params.put("DestinationId",3);
                params.put("Text",""+String.format("%.6f", lat)+","+ String.format("%.6f", lon));
                WS.getInstance(ChatActivity.this).sendMessage(params,ChatActivity.this);

                }
            });
            builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {}
            });

            // Create the AlertDialog
            AlertDialog dialog = builder.create();

            dialog.show();
            return;
        }

        //Obtenemos el texto
        String mensaje = editText.getEditableText().toString();
        if(mensaje.isEmpty()){
            Snackbar snack=Snackbar.make(view, "No puedes mandar mensajes vacios", Snackbar.LENGTH_SHORT);
            View snackBarView = snack.getView();
            snackBarView.setBackgroundColor(ContextCompat.getColor(ChatActivity.this, R.color.colorAccent));
            snack.setAction("Action", null).show();
            snack.show();
            return;
        }

        scaleIni = 1;
        scaleFin = 0;
        fabAnimate(false);

        SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPrefName), 0);
        userID = sharedPreferences.getInt("userID",0);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("UserId",userID);
        params.put("DestinationId",3);
        params.put("Text",mensaje);
        WS.getInstance(ChatActivity.this).sendMessage(params,this);
    }

    //ESCONDE DE MANERA ANIMADA EL FAB Y MUESTRA EL PROGRESS QUE ESTA DETRAS SIEMPRE VISIBLE
    private void fabAnimate(final boolean visible){

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            final Interpolator interpolador = AnimationUtils.loadInterpolator(getBaseContext(),
                    android.R.interpolator.fast_out_slow_in);

            fab.animate()
                .scaleX(scaleIni)
                .scaleY(scaleIni)
                .setInterpolator(interpolador)
                .setDuration(600)
                .setStartDelay(0)
                .setListener(new Animator.AnimatorListener() {

                    @Override
                    public void onAnimationStart(Animator animation){}

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        fab.animate()
                                .scaleY(scaleFin)
                                .scaleX(scaleFin)
                                .setInterpolator(interpolador)
                                .setDuration(600)
                                .start();
                        editText.setEnabled(visible);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation){}
                    @Override
                    public void onAnimationRepeat(Animator animation){}
                });
        }
    }

    // ---------------------------------------------------------- //
    // -------------- WEB SERVICES IMPLEMENTATION --------------- //
    // ---------------------------------------------------------- //

    @Override
    public void wsAnswered(JSONObject json) {
        Log.d("Messages",json.toString());
        int ws=0; int status=-1;
        try{status=json.getInt("status");}catch(Exception e){e.printStackTrace();}
        if(status!=0){/*ERRRRRRROOOOOOOORRRRRRR*/}

        try{
            ws = json.getInt("ws");
            switch (ws){
                case WS.WS_getMessages:{

                    JSONObject data = json.getJSONObject("data");
                    JSONArray newMsgJArray = data.getJSONArray("Data");
                    ArrayList<Msg> newMsgs = new ArrayList<Msg>();

                    for(int i=0; i<newMsgJArray.length();i++){
                        JSONObject newMsgJSONObject = newMsgJArray.getJSONObject(i);
                        Log.d("MSGDEB","sentfrom="+((newMsgJSONObject.getInt("UserId")==userID) ? "mine" : "other"));
                        Msg newMsg=null;
                        if(newMsgJSONObject.getInt("UserId")==userID){
                            newMsg = new OwnMsg();
                        }else{
                            newMsg = new OtherMsg();
                        }

                        newMsg.setId(newMsgJSONObject.getInt("MessageId"));
                        newMsg.setSenderId(newMsgJSONObject.getInt("UserId"));
                        newMsg.setMsg(newMsgJSONObject.getString("Text"));
                        newMsg.setTimeStamp(newMsgJSONObject.getString("CreatedAt"));

                        newMsgs.add(newMsg);
                    }

                    if(newMsgs.size()==0 && !topRequested){
                        findViewById(R.id.msgNoHay).setVisibility(View.VISIBLE);
                    }else{
                        findViewById(R.id.msgNoHay).setVisibility(View.GONE);
                    }

                    addToList(newMsgs);

                    topRequested=false;
                    break;
                }

                case WS.WS_sendMessage:{
                    scaleIni = 0;
                    scaleFin = 1;
                    fabAnimate(true);
                    getMessages(false);
                    if(locationJustSent){locationJustSent=false; hideMap();}
                    editText.setText("");
                    break;
                }
            }
        }catch(Exception e){e.printStackTrace();}
    }

    // ---------------------------------------------------- //
    // -------------- SCROLL IMPLEMENTATION --------------- //
    // ---------------------------------------------------- //

    private RecyclerView.OnScrollListener setScrollListener(){
        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                //Log.d("fdjsklfds","dy="+dy);

                if(topRequested){return;}

                if(dy < 0) //check for scroll down
                {
                    visibleItemCount = mLayoutManager.getChildCount();
                    totalItemCount = mLayoutManager.getItemCount();
                    pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition();

                    //Log.d("fdsafdsafds","visibleItemCount="+visibleItemCount+" total="+totalItemCount+" pastVisible"+pastVisiblesItems);

                    if((visibleItemCount+pastVisiblesItems)==totalItemCount){
                        topRequested=true;
                        getMessages(true);
                    }
                }
            }
        };
    }

    // ------------------------------------------------------- //
    // -------------- MAP READY IMPLEMENTATION --------------- //
    // ------------------------------------------------------- //

    @Override
    public void onMapReady(GoogleMap map) {
        mMap=map;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMinZoomPreference(12.0f);
        mMap.setMaxZoomPreference(17.0f);
    }

    // ------------------------------------------------------------ //
    // -------------- GALLERY LOADED IMPLEMENTATION --------------- //
    // ------------------------------------------------------------ //

    @Override
    public void onGalleryLoaded() {
        progressGalleryLoaded.setVisibility(View.GONE);
    }









    int serverResponseCode;
    public int uploadFile(final String sourceFileUri) {

        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);
        fileName = sourceFile.getName();

        if (!sourceFile.isFile()) {

            Log.e("uploadFile", "Source File not exist");

            return 0;

        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL("http://svcyonayarit.iog.digital/media/UploadPhoto/img/1951/3");
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                //  conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type","multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("fileContents", fileName);


                dos = new DataOutputStream(conn.getOutputStream());


                dos.writeBytes(twoHyphens + boundary + lineEnd);

                //Adding Parameter userdestination
                String name="3";
                dos.writeBytes("Content-Disposition: form-data; name=\"userdestination\"" + lineEnd);
                //dos.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                //dos.writeBytes("Content-Length: " + name.length() + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(name); // mobile_no is String variable
                dos.writeBytes(lineEnd);

                dos.writeBytes(twoHyphens + boundary + lineEnd);

                //Adding Parameter userid
                String phone="1951";
                dos.writeBytes("Content-Disposition: form-data; name=\"userid\"" + lineEnd);
                //dos.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                //dos.writeBytes("Content-Length: " + name.length() + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(phone); // mobile_no is String variable
                dos.writeBytes(lineEnd);

                dos.writeBytes(twoHyphens + boundary + lineEnd);

                //Adding Parameter fileName
                dos.writeBytes("Content-Disposition: form-data; name=\"fileName\"" + lineEnd);
                //dos.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                //dos.writeBytes("Content-Length: " + name.length() + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(fileName); // mobile_no is String variable
                dos.writeBytes(lineEnd);


                //Json_Encoder encode=new Json_Encoder();
                //call to encode method and assigning response data to variable 'data'
                //String data=encode.encod_to_json();
                //response of encoded data
                //System.out.println(data);


                //Adding Parameter filepath
                /*
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                String filepath="http://192.168.1.110/echo/uploads"+fileName;

                dos.writeBytes("Content-Disposition: form-data; name=\"filepath\"" + lineEnd);
                //dos.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                //dos.writeBytes("Content-Length: " + name.length() + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(filepath); // mobile_no is String variable
                dos.writeBytes(lineEnd);
                */


                //Adding Parameter media file(audio,video and image)
                dos.writeBytes(twoHyphens + boundary + lineEnd);

                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""+ fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);
                // create a buffer of maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0)
                {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();



                Log.i("uploadFile", "HTTP Response is : "+ serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Log.d("UPLOAD","Everything wetn fine");
                        }
                    });
                }

                // close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.e("Exception","MalformedURLException Exception : check script url.");
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (final Exception e) {

                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d("Got Exception : ",e.toString());
                    }
                });
                Log.e("UploadException","Exception : " + e.getMessage(), e);
            }
            return serverResponseCode;
        }
    }
}
