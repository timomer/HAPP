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

import com.hypodiabetic.happ.Objects.Treatments;

import org.json.JSONException;
import org.json.JSONObject;

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
        JSONObject reply = BolusWizard.run_bw(this.getBaseContext());
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
        Double carbValue = 0D;

        if (!carbs.getText().toString().equals("")) carbValue = Double.parseDouble(carbs.getText().toString());

        JSONObject bw = BolusWizard.bw(this.getBaseContext(), carbValue);

        try {
            reqInsulinbiob.setText("BolusIOB(" + bw.getString("biob") + ") - (COB(" + bw.getString("cob") + ") / Carb Ratio(" + bw.getString("carbRatio") + "g) = " + bw.getString("net_biob") + "U");
            reqInsulinCarbs.setText("Carbs(" + carbValue + "g) / Carb Ratio(" + bw.getString("carbRatio") + "g) = " + bw.getString("insulin_correction_carbs") + "U");
            ReqInsulinBgText.setText(bw.getString("bgCorrection") + " Bg Correction:");
            if (bw.getString("bgCorrection").equals("High")){
                reqInsulinBg.setText("eventualBG(" + bw.getString("eventualBG") + ") - Max BG(" + bw.getString("max_bg") + ") / ISF(" + bw.getString("isf") + ") = " + bw.getString("insulin_correction_bg") + "U");
            } else if (bw.getString("bgCorrection").equals("Low")){
                reqInsulinBg.setText("Target BG(" + bw.getString("target_bg") + ") - " + "eventualBG(" + bw.getString("eventualBG") + ") / ISF(" + bw.getString("isf") + ") = " + bw.getString("insulin_correction_bg") + "U");
            } else {
                reqInsulinBg.setText("NA - BG within Target");
            }
            sugBolus.setText("Carb Corr(" + bw.getString("insulin_correction_carbs") + ") + BG Corr(" + bw.getString("insulin_correction_bg") + ") - Net Bolus(" + bw.getString("net_biob") + ") = ");
            suggestedBolus.setText(bw.getString("suggested_bolus"));

            Date dateNow = new Date();
            if (bw.getDouble("suggested_bolus") > 0){
                bolusTreatment.datetime         = dateNow.getTime();
                bolusTreatment.datetime_display = dateNow.toString();
                bolusTreatment.note             = "bolus";
                bolusTreatment.type             = "Insulin";
                bolusTreatment.value            = bw.getDouble("suggested_bolus");
            }
            if (carbValue > 0){
                carbTratment.datetime         = dateNow.getTime();
                carbTratment.datetime_display = dateNow.toString();
                carbTratment.note             = "";
                carbTratment.type             = "Carbs";
                carbTratment.value            = carbValue;
            }

        } catch (JSONException e) {
        }

        buttonAccept = (Button) findViewById(R.id.wizardAccept);
        if (carbTratment.value == null && bolusTreatment.value == null){
            buttonAccept.setEnabled(false);
        } else {
            buttonAccept.setEnabled(true);
        }
    }

    public void wizardAccept(View view){

        bolusTreatment.value = Double.parseDouble(suggestedBolus.getText().toString());
        pumpAction.setBolus(bolusTreatment, carbTratment, view.getContext());                       //Action the suggested Bolus

        //finish();
    }

    public void wizardCancel(View view){
        finish();
    }
}
