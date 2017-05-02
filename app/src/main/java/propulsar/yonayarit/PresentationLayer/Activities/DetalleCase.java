package propulsar.yonayarit.PresentationLayer.Activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import propulsar.yonayarit.DomainLayer.Adapters.StateCaseAdapter;
import propulsar.yonayarit.DomainLayer.Objects.StateCase;
import propulsar.yonayarit.DomainLayer.WS;
import propulsar.yonayarit.R;

public class DetalleCase extends AppCompatActivity implements WS.OnWSRequested, SwipeRefreshLayout.OnRefreshListener{

    TextView folio;
    TextView categoria;
    TextView tipo;
    TextView titulo;
    TextView desc;
    TextView fecha;

    int caseID;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private android.support.v7.widget.LinearLayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private ArrayList<StateCase> states;

    // ---------------------------------------- //
    // -------------- LIFECYCLE --------------- //
    // ---------------------------------------- //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_case);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarDetalleCase);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        folio = (TextView)findViewById(R.id.detCase_folio);
        categoria = (TextView)findViewById(R.id.detCase_categoria);
        desc = (TextView)findViewById(R.id.detCase_descripcion);
        fecha = (TextView)findViewById(R.id.detCase_fecha);
        tipo = folio = (TextView)findViewById(R.id.detCase_tipo);
        titulo = folio = (TextView)findViewById(R.id.detCase_titulo);

        caseID = getIntent().getIntExtra("CaseId",-1);

        mRecyclerView = (RecyclerView)findViewById(R.id.statesRecyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        states = new ArrayList<StateCase>();
        mAdapter = new StateCaseAdapter(states, this);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout=(SwipeRefreshLayout)findViewById(R.id.statesSwipeRefresh);

        mSwipeRefreshLayout.setOnRefreshListener(this);

        mSwipeRefreshLayout.setRefreshing(true);

        getDetail();
    }

    // ---------------------------------------------------------- //
    // -------------- WEB SERVICES IMPLEMENTATION --------------- //
    // ---------------------------------------------------------- //

    @Override
    public void wsAnswered(JSONObject json) {
        Log.d("WSDEB",json.toString());
        int ws=0; int status=-1;
        try{status=json.getInt("status");}catch(Exception e){e.printStackTrace();}
        if(status!=0){/*ERRRRRRROOOOOOOORRRRRRR*/}

        mSwipeRefreshLayout.setRefreshing(false);

        try {
            ws = json.getInt("ws");
            switch (ws) {
                case WS.WS_getCaseDetail:{
                    JSONObject data = json.getJSONObject("data");

                    folio.setText("Folio: "+data.getString("Folio"));
                    categoria.setText(data.getString("CategoryDescription"));
                    desc.setText(data.getString("Description"));
                    fecha.setText(data.getString("Date").split("T")[0]);
                    titulo.setText(data.getString("Title"));
                    tipo.setText(data.getString("ComplaintTypeDescription"));

                    setDetailsList(data.getJSONArray("ChangeStatus"));
                }
            }
        }catch(Exception e){e.printStackTrace();}
    }

    // ------------------------------------------ //
    // -------------- OWN METHODS --------------- //
    // ------------------------------------------ //

    private void getDetail(){
        SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPrefName), 0);
        int userID = sharedPreferences.getInt("userID",0);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("UserId",userID);
        params.put("ComplaintId",caseID);
        WS.getInstance(DetalleCase.this).getCaseDetail(params,this);
    }

    private void setDetailsList(JSONArray arrayStates) throws Exception {
        for(int i=0; i<arrayStates.length(); i++){
            JSONObject jsonState = arrayStates.getJSONObject(i);
            Log.d("adjfhdska","will print = "+jsonState.toString());
            StateCase stateCase = new StateCase();
            stateCase.setComplaintID(""+jsonState.getInt("ComplaintId"));
            stateCase.setComplaintStatusID(""+jsonState.getInt("ComplaintStatusId"));
            stateCase.setColor(jsonState.getString("StatusColor"));
            stateCase.setDate(jsonState.getString("ChangeDate"));
            stateCase.setDescription(jsonState.getString("Description"));

            states.add(stateCase);
            /*
            try {
                JSONObject jsonState = arrayStates.getJSONObject(i);
            }catch(Exception e){e.printStackTrace();}
            */
        }

        mAdapter.notifyDataSetChanged();
    }

    // ----------------------------------------------------------- //
    // -------------- SWIPE REFRESH IMPLEMENTATION --------------- //
    // ----------------------------------------------------------- //

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        states.clear();
        getDetail();
    }
}
