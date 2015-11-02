package com.hypodiabetic.happ;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Treatments;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;

public class BolusWizardActivity extends Activity {

    private TextView reqInsulinbiob;
    private EditText treatmentValue;
    private EditText carbs;
    private TextView reqInsulinCarbs;
    private TextView reqInsulinBg;
    private TextView sugBolus;
    private Button buttonAccept;
    private EditText suggestedBolus;
    private TextView ReqInsulinBgText;

    private TextView bwDisplayIOBCorr;
    private TextView bwDisplayCarbCorr;
    private TextView bwDisplayBGCorr;

    Treatments bolusTreatment = new Treatments();
    Treatments carbTratment = new Treatments();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bolus_wizard);

        //Run Bolus Wizard on suggested carb amount change
        carbs           = (EditText) findViewById(R.id.wizardCarbValue);
        carbs.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                run_bw();
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        Intent intent = getIntent();
        treatmentValue = (EditText) findViewById(R.id.wizardCarbValue);
        treatmentValue.setText(intent.getStringExtra("CARB_VALUE"));



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bolus_wizard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void run_bw(){
        JSONObject reply = BolusWizard.run_NS_BW(this.getBaseContext());
        TextView sysMsg;
        sysMsg = (TextView) findViewById(R.id.wizardCalc);
        sysMsg.setText("NS bwp: " + reply.toString());

        reqInsulinbiob  = (TextView) findViewById(R.id.wizardNetIOB);
        carbs           = (EditText) findViewById(R.id.wizardCarbValue);
        reqInsulinCarbs = (TextView) findViewById(R.id.wizardReqInsulinCarbs);
        reqInsulinBg    = (TextView) findViewById(R.id.wizardReqInsulinBg);
        sugBolus        = (TextView) findViewById(R.id.wizardSugBolus);
        suggestedBolus  = (EditText) findViewById(R.id.wizardSuggestedBolus);
        ReqInsulinBgText= (TextView) findViewById(R.id.wizardReqInsulinBgText);

        bwDisplayIOBCorr    = (TextView) findViewById(R.id.bwDisplayIOBCorr);
        bwDisplayCarbCorr   = (TextView) findViewById(R.id.bwDisplayCarbCorr);
        bwDisplayBGCorr     = (TextView) findViewById(R.id.bwDisplayBGCorr);
        Double carbValue = 0D;

        if (!carbs.getText().toString().equals("")){
            try {
                carbValue = NumberFormat.getNumberInstance(java.util.Locale.UK).parse(carbs.getText().toString()).doubleValue();
            } catch (ParseException e){
                Crashlytics.logException(e);
            }
        }

        JSONObject bw = BolusWizard.bw(this.getBaseContext(), carbValue);


            //Bolus Wizard Display
            bwDisplayIOBCorr.setText(   bw.optString("net_biob", "") + "U");
            bwDisplayCarbCorr.setText(  bw.optString("insulin_correction_carbs", "") + "U");
            bwDisplayBGCorr.setText(    bw.optString("insulin_correction_bg", "") + "U");

            //Bolus Wizard Calculations
            reqInsulinbiob.setText(bw.optString("net_biob_maths", ""));
            reqInsulinCarbs.setText(bw.optString("insulin_correction_carbs_maths", ""));
            ReqInsulinBgText.setText(bw.optString("bgCorrection", "") + " bg correction");
            reqInsulinBg.setText(bw.optString("insulin_correction_bg_maths", ""));
            sugBolus.setText(bw.optString("suggested_bolus_maths", ""));
            suggestedBolus.setText(bw.optString("suggested_bolus", ""));

            Date dateNow = new Date();
            if (bw.has("suggested_bolus")) {
                try {
                    if (NumberFormat.getNumberInstance(java.util.Locale.UK).parse(bw.optString("suggested_bolus", "")).doubleValue() > 0) {
                        bolusTreatment.datetime = dateNow.getTime();
                        bolusTreatment.datetime_display = dateNow.toString();
                        bolusTreatment.note = "bolus";
                        bolusTreatment.type = "Insulin";
                        bolusTreatment.value = bw.optDouble("suggested_bolus", 0D);
                    }
                } catch (ParseException e){
                    Crashlytics.logException(e);
                }
            }
            if (carbValue > 0){
                carbTratment.datetime         = dateNow.getTime();
                carbTratment.datetime_display = dateNow.toString();
                carbTratment.note             = "";
                carbTratment.type             = "Carbs";
                carbTratment.value            = carbValue;
            }



        buttonAccept = (Button) findViewById(R.id.wizardAccept);
        if (carbTratment.value == null && bolusTreatment.value == null){
            buttonAccept.setEnabled(false);
        } else {
            buttonAccept.setEnabled(true);
        }
    }

    public void wizardAccept(View view){

        if (suggestedBolus.getText().toString().trim().length() != 0 && Double.parseDouble(suggestedBolus.getText().toString()) > 0) {
            try {
                bolusTreatment.value = NumberFormat.getNumberInstance(java.util.Locale.UK).parse(suggestedBolus.getText().toString()).doubleValue();
            } catch (ParseException e){
                Crashlytics.logException(e);
            }
            pumpAction.setBolus(bolusTreatment, carbTratment, view.getContext());                   //Action the suggested Bolus
        } else if (carbTratment.value > 0) {
            carbTratment.save();
            Toast.makeText(this, carbTratment.value + "g saved, no Bolus suggested", Toast.LENGTH_SHORT).show();

            //Return to the home screen (if not already on it)
            Intent intentHome = new Intent(view.getContext(), MainActivity.class);
            intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            view.getContext().startActivity(intentHome);
        }

        //finish();

    }

    public void wizardCancel(View view){
        finish();
    }
}
