package com.hypodiabetic.happ;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.RealmManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;

public class Integration_Report extends AppCompatActivity {

    Spinner integrationType;
    Spinner happObjectType;
    Spinner numHours;
    ListView integrationReportList;
    TextView integrationItemCount;
    RealmManager realmManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_integration__report);
        realmManager = new RealmManager();

        integrationType         =   (Spinner) findViewById(R.id.integrationType);
        happObjectType          =   (Spinner) findViewById(R.id.HAPPObjectType);
        numHours                =   (Spinner) findViewById(R.id.integrationHours);
        integrationReportList   =   (ListView) findViewById(R.id.integrationReportList);
        integrationItemCount    =   (TextView) findViewById(R.id.integrationItemCount);

        String[] integrationHours = {"4", "8", "12", "24", "48"};
        ArrayAdapter<String> stringArrayAdapterHours= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, integrationHours);
        numHours.setAdapter(stringArrayAdapterHours);
        numHours.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                reloadList(integrationType.getSelectedItem().toString(), happObjectType.getSelectedItem().toString(), Integer.parseInt(numHours.getSelectedItem().toString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        String[] integrationTypes = {"insulin_integration_app", "ns_client"};
        ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, integrationTypes);
        integrationType.setAdapter(stringArrayAdapter);
        integrationType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                reloadList(integrationType.getSelectedItem().toString(), happObjectType.getSelectedItem().toString(), Integer.parseInt(numHours.getSelectedItem().toString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        String[] happObjectTypes = {"bolus_delivery", "temp_basal", "treatment_carbs"};
        ArrayAdapter<String> stringArrayAdapter2= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, happObjectTypes);
        happObjectType.setAdapter(stringArrayAdapter2);
        happObjectType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                reloadList(integrationType.getSelectedItem().toString(), happObjectType.getSelectedItem().toString(), Integer.parseInt(numHours.getSelectedItem().toString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        realmManager.closeRealm();
    }

    public void reloadList(String intergartion, String happObject, int hoursOld){
        ArrayList<HashMap<String, String>> integrationList = new ArrayList<>();
        Calendar integrationDate  = Calendar.getInstance();
        SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd MMM HH:mm", getResources().getConfiguration().locale);
        List<Integration> integrations = Integration.getIntegrationsHoursOld(intergartion, happObject, hoursOld, realmManager.getRealm());

        for (Integration integration : integrations){                                                    //Convert from a List<Object> Array to ArrayList
            HashMap<String, String> integrationItem = new HashMap<String, String>();

            //ObjectToSync objectSyncDetails = new ObjectToSync(integration);

            if (integration.getState().equals("delete_me")) {
                realmManager.getRealm().beginTransaction();
                integration.deleteFromRealm();
                realmManager.getRealm().commitTransaction();
            } else {

                if (integration.getTimestamp() != null) {
                    integrationDate.setTime(integration.getTimestamp());
                } else {
                    integrationDate.setTime(new Date(0));                                                 //Bad integration
                }
                integrationItem.put("integrationType",      integration.getType());
                integrationItem.put("integrationWhat",      "Request sent: " + integration.getObjectSummary(realmManager.getRealm()));
                integrationItem.put("integrationDateTime",  sdfDateTime.format(integrationDate.getTime()));
                integrationItem.put("integrationState",     "state:" + integration.getState());
                integrationItem.put("integrationAction",    "action:" + integration.getAction());
                integrationItem.put("integrationRemoteID",  "remote_id:" + integration.getRemote_id());
                integrationItem.put("integrationDetails",   integration.getDetails());
                integrationItem.put("integrationID",        "id:" + integration.getId());

                integrationList.add(integrationItem);
            }
        }

        SimpleAdapter adapter = new SimpleAdapter(MainActivity.getInstace(), integrationList, R.layout.integration_list_layout,
                new String[]{"integrationType", "integrationWhat", "integrationDateTime", "integrationState", "integrationAction", "integrationRemoteID", "integrationDetails", "integrationID"},
                new int[]{R.id.integrationType, R.id.integrationWhat, R.id.integrationDateTime, R.id.integrationState, R.id.integrationAction, R.id.integrationRemoteID, R.id.integrationDetails, R.id.integrationID});
        integrationReportList.setAdapter(adapter);

        integrationItemCount.setText("Count: " + integrationList.size());
    }

}
