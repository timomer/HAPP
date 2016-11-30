package com.hypodiabetic.happ;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.RealmManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

        String[] integrationTypes = {Constants.treatmentService.INSULIN_INTEGRATION_APP, "ns_client"};
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
        realmManager.closeRealm();
        super.onDestroy();
    }
    @Override
    public void onPause(){
        realmManager.closeRealm();
        super.onPause();
    }

    public class mySimpleAdapterIntegration extends SimpleAdapter {
        public mySimpleAdapterIntegration(Context context, List<HashMap<String, String>> items, int resource, String[] from, int[] to) {
            super(context, items, resource, from, to);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            //Shows Integration details image
            ImageView integrationImage  = (ImageView) view.findViewById(R.id.integrationIcon);
            TextView integrationState   = (TextView) view.findViewById(R.id.integrationState);
            integrationImage.setBackgroundResource(tools.getIntegrationStatusImg(integrationState.getText().toString()));

            return view;
        }
    }

    public void reloadList(String intergartion, String happObject, int hoursOld){
        ArrayList<HashMap<String, String>> integrationList = new ArrayList<>();
        SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd MMM HH:mm", getResources().getConfiguration().locale);
        List<Integration> integrations = Integration.getIntegrationsHoursOld(intergartion, happObject, hoursOld, realmManager.getRealm());

        for (Integration integration : integrations){                                                    //Convert from a List<Object> Array to ArrayList
            HashMap<String, String> integrationItem = new HashMap<String, String>();

            if (!integration.getState().equals("deleted")) {
                integrationItem.put("integrationID",        integration.getId());
                integrationItem.put("integrationType",      integration.getType());
                integrationItem.put("integrationDateTime",  sdfDateTime.format(integration.getTimestamp()));
                integrationItem.put("integrationDetails",   integration.getDetails());
                integrationItem.put("integrationState",     integration.getState());

                integrationList.add(integrationItem);
            }
        }

        mySimpleAdapterIntegration adapter = new mySimpleAdapterIntegration(MainActivity.getInstance(), integrationList, R.layout.integration_list_layout,
                new String[]{"integrationID", "integrationType", "integrationDateTime", "integrationDetails", "integrationState"},
                new int[]{R.id.integrationID, R.id.integrationType, R.id.integrationDateTime, R.id.integrationDetails, R.id.integrationState});
        integrationReportList.setAdapter(adapter);

        integrationItemCount.setText(getString(R.string.count) + ": " + integrationList.size());

        integrationReportList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
                Dialog dialog = new Dialog(parent.getContext());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.integration_list_layout_details);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);

                SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd MMM HH:mm", getResources().getConfiguration().locale);
                TextView integrationID  = (TextView) view.findViewById(R.id.integrationID);
                Integration integration = Integration.getIntegrationByID(integrationID.getText().toString(), realmManager.getRealm());

                TextView integrationTypeDetails         = (TextView) dialog.findViewById(R.id.integrationTypeDetails);
                TextView integrationCreatedDetails      = (TextView) dialog.findViewById(R.id.integrationCreatedDetails);
                TextView integrationUpdatedDetails      = (TextView) dialog.findViewById(R.id.integrationUpdatedDetails);
                TextView integrationStateDetails        = (TextView) dialog.findViewById(R.id.integrationStateDetails);
                TextView integrationActionDetails       = (TextView) dialog.findViewById(R.id.integrationActionDetails);
                TextView integrationWhatDetails         = (TextView) dialog.findViewById(R.id.integrationWhatDetails);
                TextView integrationIDDetails           = (TextView) dialog.findViewById(R.id.integrationIDDetails);
                TextView integrationRemoteIDDetails     = (TextView) dialog.findViewById(R.id.integrationRemoteIDDetails);
                TextView integrationDetailsDetails      = (TextView) dialog.findViewById(R.id.integrationDetailsDetails);
                TextView integrationToSyncDetails       = (TextView) dialog.findViewById(R.id.integrationToSyncDetails);
                TextView integrationAuthIDDetails       = (TextView) dialog.findViewById(R.id.integrationAuthIDDetails);
                TextView integrationRemoteVar1Details   = (TextView) dialog.findViewById(R.id.integrationRemoteVar1Details);
                integrationTypeDetails.setText      (integration.getType());
                integrationCreatedDetails.setText   ("Created: " + sdfDateTime.format(integration.getTimestamp()));
                integrationUpdatedDetails.setText   ("Updated: " + sdfDateTime.format(integration.getDate_updated()));
                integrationStateDetails.setText     ("State: " + integration.getState());
                integrationActionDetails.setText    ("Action: " + integration.getAction());
                integrationWhatDetails.setText      (integration.getObjectSummary(realmManager.getRealm()));
                integrationIDDetails.setText        ("Local ID: " + integration.getId());
                integrationRemoteIDDetails.setText  ("Remote ID: " + integration.getRemote_id());
                integrationDetailsDetails.setText   (integration.getDetails());
                integrationToSyncDetails.setText    ("To Sync: " + integration.getToSync());
                integrationAuthIDDetails.setText    ("Auth ID: " + integration.getAuth_code());
                integrationRemoteVar1Details.setText("Remote Var1: " + integration.getRemote_var1());

                dialog.show();
            }

        });
    }

}
