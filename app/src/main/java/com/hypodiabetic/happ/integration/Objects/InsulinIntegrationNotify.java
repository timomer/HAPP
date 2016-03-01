package com.hypodiabetic.happ.integration.Objects;

import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.R;
import com.hypodiabetic.happ.WearDisplayActivity;
import com.hypodiabetic.happ.tools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public InsulinIntegrationNotify(){
        detailList              =   new ArrayList<>();
        detailListErrorsOnly    =   new ArrayList<>();
        recentlyUpdated         =   Integration.getUpdatedInLastMins(1,"insulin_integration_app");
        withErrors              =   Integration.getIntegrationsWithErrors("insulin_integration_app");
        snackbarMsg             =   "";
        errorMsg                =   "";
        foundError              =   false;
        SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd MMM HH:mm", MainApp.instance().getResources().getConfiguration().locale);

        for (Integration integration : recentlyUpdated) {
            if (!integration.state.equals("error") && !integration.state.equals("error_ack")) {     //Deal with errors later
                ObjectToSync integrationWithDetails = new ObjectToSync(integration);
                HashMap<String, String> detailListItem = new HashMap<String, String>();

                if (integrationWithDetails.state.equals("delete_me")) {
                    integration.delete();
                } else {

                    switch (integrationWithDetails.happ_object_type) {
                        case "bolus_delivery":
                            detailListItem.put("value", tools.formatDisplayInsulin(integrationWithDetails.value1, 2));
                            detailListItem.put("summary", integrationWithDetails.value3);
                            snackbarMsg += integrationWithDetails.state.toUpperCase() + ": " + tools.formatDisplayInsulin(integrationWithDetails.value1, 2) + " " + integrationWithDetails.value3 + "\n";
                            break;

                        case "temp_basal":
                            detailListItem.put("value", tools.formatDisplayBasal(integrationWithDetails.value1, true));
                            detailListItem.put("summary", "(" + integrationWithDetails.value2 + "%) " + integrationWithDetails.value3 + "mins");
                            snackbarMsg += integrationWithDetails.state.toUpperCase() + ": " + tools.formatDisplayBasal(integrationWithDetails.value1, false) + " (" + integrationWithDetails.value2 + "%) " + integrationWithDetails.value3 + "mins\n";
                            break;
                    }
                    detailListItem.put("happObjectType", integrationWithDetails.happ_object_type);
                    detailListItem.put("state", integrationWithDetails.state.toUpperCase());
                    detailListItem.put("details", integrationWithDetails.details);
                    detailListItem.put("action", "action:" + integrationWithDetails.action);
                    detailListItem.put("date", sdfDateTime.format(integration.date_updated));
                    detailListItem.put("intID", "INT ID:" + integration.getId());
                    detailList.add(detailListItem);
                }
            }
        }

        if (withErrors.size() > 0) foundError = true;
        for (Integration integrationWithError : withErrors) {
            ObjectToSync integrationWithDetails = new ObjectToSync(integrationWithError);
            HashMap<String, String> detailListItem = new HashMap<String, String>();

            if (integrationWithDetails.state.equals("delete_me")) {
                integrationWithError.delete();
            } else {

                switch (integrationWithDetails.happ_object_type) {
                    case "bolus_delivery":
                        detailListItem.put("value", tools.formatDisplayInsulin(integrationWithDetails.value1, 2));
                        detailListItem.put("summary", integrationWithDetails.value3);
                        errorMsg += integrationWithDetails.state.toUpperCase() + ": " + tools.formatDisplayInsulin(integrationWithDetails.value1, 2) + " " + integrationWithDetails.value3 + "\n";
                        break;

                    case "temp_basal":
                        detailListItem.put("value", tools.formatDisplayBasal(integrationWithDetails.value1, true));
                        detailListItem.put("summary", "(" + integrationWithDetails.value2 + "%) " + integrationWithDetails.value3 + "mins");
                        errorMsg += integrationWithDetails.state.toUpperCase() + ": " + tools.formatDisplayBasal(integrationWithDetails.value1, false) + " (" + integrationWithDetails.value2 + "%) " + integrationWithDetails.value3 + "mins\n";
                        break;
                }
                detailListItem.put("happObjectType", integrationWithDetails.happ_object_type);
                detailListItem.put("state", integrationWithDetails.state.toUpperCase());
                detailListItem.put("details", integrationWithDetails.details);
                detailListItem.put("action", "action:" + integrationWithDetails.action);
                detailListItem.put("date", sdfDateTime.format(integrationWithError.date_updated));
                detailListItem.put("intID", "INT ID:" + integrationWithError.getId());
                detailListErrorsOnly.add(detailListItem);
            }
        }
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
                    mySimpleAdapter adapter = new mySimpleAdapter(MainActivity.getInstace(), detailList, R.layout.integration_list_layout_insulin_summary,
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
        msg.setText("These actions failed, they will NOT be resent and must be manually actioned.");
        msg.setVisibility(View.VISIBLE);

        Button buttonOK = (Button) dialog.findViewById(R.id.integrationOK);
        buttonOK.setText("Acknowledge");
        buttonOK.setVisibility(View.VISIBLE);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Integration integrationWithError : withErrors) {
                    integrationWithError.state  =   "error_ack";                                    //User has Acknowledged the errors
                    integrationWithError.save();
                }
                Notifications.clear("INSULIN_UPDATE");
                dialog.dismiss();
            }
        });

        ListView list = (ListView) dialog.findViewById(R.id.integrationList);
        mySimpleAdapter adapter = new mySimpleAdapter(MainActivity.getInstace(), detailListErrorsOnly, R.layout.integration_list_layout_insulin_summary,
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

            Intent displayIntent = new Intent(c, WearDisplayActivity.class);

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
            TextView textView = (TextView) view.findViewById(R.id.insulinSummaryState);
            switch (textView.getText().toString().toLowerCase()) {
                case "sent":
                    imageView.setBackgroundResource(R.drawable.arrow_right_bold_circle);
                    break;
                case "received":
                    imageView.setBackgroundResource(R.drawable.information);
                    break;
                case "delayed":
                    imageView.setBackgroundResource(R.drawable.clock);
                    break;
                case "delivered":
                case "set":
                case "canceled":
                    imageView.setBackgroundResource(R.drawable.checkbox_marked_circle);
                    break;
                case "error":
                    imageView.setBackgroundResource(R.drawable.alert_circle);
                    break;
                default:
                    imageView.setBackgroundResource(R.drawable.alert_circle);
                    break;
            }

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

