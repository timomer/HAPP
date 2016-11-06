package com.hypodiabetic.happ.integration.Objects;

import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.Objects.Bolus;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.R;
import com.hypodiabetic.happ.tools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;

/**
 * Created by Tim on 14/02/2016.
 * This class collects and reviews updates from Insulin Integration App, as multiple updates may hit HAPP at once, this class combines them
 */
public class InsulinIntegrationNotify {

    ArrayList<HashMap<String, String>> detailList;                                                  //Details of Integrations
    ArrayList<HashMap<String, String>> detailListErrorsOnly;                                        //Details of Integrations with errors
    List<Integration> recentlyUpdated;                                                              //All Integrations recently updated
    List<Integration> withErrors;                                                                   //All Integrations with errors that have not been Acknowledged
    String snackbarMsg;                                                                             //Summary String for Snackbar
    String errorMsg;                                                                                //Summary String of error items
    public Boolean foundError;                                                                      //Where errors found?
    public Boolean haveUpdates;                                                                     //Updates found?
    private Realm realm;
    private Pump pump;
    private static String TAG = "InsulinIntegratNotify";

    public InsulinIntegrationNotify(Realm realm){
        Log.d(TAG, "InsulinIntegrationNotify: START");
        this.realm              =   realm;
        detailList              =   new ArrayList<>();
        detailListErrorsOnly    =   new ArrayList<>();
        recentlyUpdated         =   Integration.getUpdatedInLastMins(1, Constants.treatmentService.INSULIN_INTEGRATION_APP, realm);
        withErrors              =   Integration.getIntegrationsWithErrors(Constants.treatmentService.INSULIN_INTEGRATION_APP, realm);
        snackbarMsg             =   "";
        errorMsg                =   "";
        foundError              =   false;
        haveUpdates             =   false;
        pump                    =   new Pump(new Profile(new Date()), realm);
        SimpleDateFormat sdfDateTime    = new SimpleDateFormat("dd MMM HH:mm", MainApp.instance().getResources().getConfiguration().locale);
        SimpleDateFormat sdfTime        = new SimpleDateFormat("HH:mm", MainApp.instance().getResources().getConfiguration().locale);
        if (recentlyUpdated.size() > 0) haveUpdates = true;

        for (Integration integration : recentlyUpdated) {
            if (!integration.getState().equals("error") && !integration.getState().equals("error_ack")) {     //Deal with errors later
                HashMap<String, String> detailListItem = new HashMap<String, String>();

                if (!integration.getState().equals("delete_me")) {

                    switch (integration.getLocal_object()) {
                        case "bolus_delivery":
                            Bolus bolus = Bolus.getBolus(integration.getLocal_object_id(), realm);
                            detailListItem.put("value", tools.formatDisplayInsulin(bolus.getValue(), 2));
                            detailListItem.put("summary", bolus.getType());
                            snackbarMsg += integration.getState().toUpperCase() + ": " + tools.formatDisplayInsulin(bolus.getValue(), 2) + " " + integration.getType() + " " + sdfTime.format(integration.getDate_updated()) + "\n";
                            break;

                        case "temp_basal":
                            TempBasal tempBasal = TempBasal.getTempBasalByID(integration.getLocal_object_id(), realm);
                            pump.setNewTempBasal(null, tempBasal);
                            detailListItem.put("value", tools.formatDisplayBasal(tempBasal.getRate(), true));
                            detailListItem.put("summary", "(" + pump.getTempBasalPercent() + "%) " + tempBasal.getDuration() + "mins");
                            snackbarMsg += integration.getState().toUpperCase() + ": " + tools.formatDisplayBasal(tempBasal.getRate(), false) + " (" + pump.getTempBasalPercent() + "%) " + tempBasal.getDuration() + "mins " + sdfTime.format(integration.getDate_updated()) + "\n";
                            break;
                    }
                    detailListItem.put("happObjectType",    integration.getLocal_object());
                    detailListItem.put("state",             integration.getState().toUpperCase());
                    detailListItem.put("details",           integration.getDetails());
                    detailListItem.put("action",            "action:" + integration.getAction());
                    detailListItem.put("date",              sdfDateTime.format(integration.getDate_updated()));
                    detailListItem.put("intID",             "INT ID:" + integration.getId());
                    detailList.add(detailListItem);
                }
            }
        }

        if (withErrors.size() > 0) foundError = true;
        for (Integration integrationWithError : withErrors) {
            HashMap<String, String> detailListItem = new HashMap<String, String>();

            if (!integrationWithError.getState().equals("deleted")) {

                switch (integrationWithError.getLocal_object()) {
                    case "bolus_delivery":
                        Bolus bolus = Bolus.getBolus(integrationWithError.getLocal_object_id(), realm);
                        detailListItem.put("value", tools.formatDisplayInsulin(bolus.getValue(), 2));
                        detailListItem.put("summary", bolus.getType());
                        errorMsg += integrationWithError.getState().toUpperCase() + ": " + tools.formatDisplayInsulin(bolus.getValue(), 2) + " " + bolus.getType() + "\n";
                        break;

                    case "temp_basal":
                        TempBasal tempBasal = TempBasal.getTempBasalByID(integrationWithError.getLocal_object_id(), realm);
                        pump.setNewTempBasal(null, tempBasal);
                        detailListItem.put("value", tools.formatDisplayBasal(tempBasal.getRate(), true));
                        detailListItem.put("summary", "(" + pump.getTempBasalPercent() + "%) " + tempBasal.getDuration() + "mins");
                        errorMsg += integrationWithError.getState().toUpperCase() + ": " + tools.formatDisplayBasal(tempBasal.getRate(), false) + " (" + pump.getTempBasalPercent() + "%) " + tempBasal.getDuration() + "mins\n";
                        break;
                }
                detailListItem.put("happObjectType",    integrationWithError.getLocal_object());
                detailListItem.put("state",             integrationWithError.getState().toUpperCase());
                detailListItem.put("details",           integrationWithError.getDetails());
                detailListItem.put("action",            "action:" + integrationWithError.getAction());
                detailListItem.put("date",              sdfDateTime.format(integrationWithError.getDate_updated()));
                detailListItem.put("intID",             "INT ID:" + integrationWithError.getId());
                detailListErrorsOnly.add(detailListItem);
            }
        }
        Log.d(TAG, "InsulinIntegrationNotify: FINISH");
    }

    public Snackbar getSnackbar(View v){
        if (recentlyUpdated.size() > 0) {

            int snackbarLength = Snackbar.LENGTH_LONG;

            Snackbar snackbar = Snackbar.make(v, snackbarMsg, snackbarLength);
            View snackbarView = snackbar.getView();
            TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
            //textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,10F);
            textView.setMaxLines(recentlyUpdated.size());
            snackbar.setAction("DETAILS", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Dialog dialog = new Dialog(view.getContext());
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.integration_dialog);
                    dialog.setCancelable(true);
                    dialog.setCanceledOnTouchOutside(true);

                    ListView list = (ListView) dialog.findViewById(R.id.integrationList);
                    mySimpleAdapter adapter = new mySimpleAdapter(MainActivity.getInstance(), detailList, R.layout.integration_list_layout_insulin_summary,
                            new String[]{"value", "summary", "state", "details", "happObjectType", "action", "date"},
                            new int[]{R.id.insulinSummaryAmount, R.id.insulinSummarySummary, R.id.insulinSummaryState, R.id.insulinSummaryDetails, R.id.insulinSummaryHappObjectType, R.id.insulinSummaryAction, R.id.insulinSummaryDate});
                    list.setAdapter(adapter);

                    dialog.show();
                }
            });

            return snackbar;
        } else {
            return null;
        }
    }

    public Dialog showErrorDetailsDialog(View view){
        final Dialog dialog = new Dialog(view.getContext());
        dialog.setTitle("Error: Insulin Actions");
        dialog.setContentView(R.layout.integration_dialog);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView msg = (TextView) dialog.findViewById(R.id.integrationMsg);
        msg.setText(R.string.InsulinIntegrationNotify_actions_failed);
        msg.setVisibility(View.VISIBLE);

        Button buttonOK = (Button) dialog.findViewById(R.id.integrationOK);
        buttonOK.setText(R.string.InsulinIntegrationNotify_acknowledge);
        buttonOK.setVisibility(View.VISIBLE);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Integration integrationWithError : withErrors) {
                    realm.beginTransaction();
                    integrationWithError.setState   ("error_ack");                                    //User has Acknowledged the errors
                    realm.commitTransaction();
                }
                Notifications.clear("INSULIN_UPDATE");
                dialog.dismiss();
            }
        });

        ListView list = (ListView) dialog.findViewById(R.id.integrationList);
        mySimpleAdapter adapter = new mySimpleAdapter(MainActivity.getInstance(), detailListErrorsOnly, R.layout.integration_list_layout_insulin_summary,
                new String[]{"value", "summary", "state", "details", "happObjectType", "action", "date", "intID"},
                new int[]{R.id.insulinSummaryAmount, R.id.insulinSummarySummary, R.id.insulinSummaryState, R.id.insulinSummaryDetails, R.id.insulinSummaryHappObjectType, R.id.insulinSummaryAction, R.id.insulinSummaryDate, R.id.insulinSummaryINTID});
        list.setAdapter(adapter);

        return dialog;
    }

    public NotificationCompat.Builder getErrorNotification(){

        if (foundError) {
            Context c = MainApp.instance();
            String title = "Error: Insulin Actions";
            String msg = errorMsg;
            Bitmap bitmap = Bitmap.createBitmap(320,320, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.RED);

            Intent intent_open_activity = new Intent(c, MainActivity.class);
            PendingIntent pending_intent_open_activity = PendingIntent.getActivity(c, 2, intent_open_activity, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c);
            notificationBuilder.setSmallIcon(R.drawable.alert_circle);
            notificationBuilder.setContentTitle(title);
            notificationBuilder.setContentText(msg);
            notificationBuilder.setContentIntent(pending_intent_open_activity);
            notificationBuilder.setPriority(Notification.PRIORITY_MAX);
            notificationBuilder.setCategory(Notification.CATEGORY_ALARM);
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
            notificationBuilder.setVibrate(new long[]{500, 1000, 500, 500, 500, 1000, 500});

            return notificationBuilder;

        } else {
            return null;
        }
    }

    public class mySimpleAdapter extends SimpleAdapter {
        private Context mContext;

        public mySimpleAdapter(Context context, List<HashMap<String, String>> items, int resource, String[] from, int[] to) {
            super(context, items, resource, from, to);
            this.mContext = context;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            ImageView imageView = (ImageView) view.findViewById(R.id.insulinSummaryStateImage);
            TextView textView   = (TextView) view.findViewById(R.id.insulinSummaryState);
            imageView.setBackgroundResource(tools.getIntegrationStatusImg(textView.getText().toString()));

            TextView happObject = (TextView) view.findViewById(R.id.insulinSummaryHappObjectType);
            TextView value = (TextView) view.findViewById(R.id.insulinSummaryAmount);
            switch (happObject.getText().toString()){
                case "bolus_delivery":
                    value.setBackgroundResource(R.drawable.insulin_treatment_round);
                    value.setMaxLines(1);
                    break;
                case "temp_basal":
                    value.setBackgroundResource(R.drawable.insulin_basal_square);
                    value.setMaxLines(2);
                    break;
            }
            return view;
        }
    }
}

