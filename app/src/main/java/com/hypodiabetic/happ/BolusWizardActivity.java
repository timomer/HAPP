package com.hypodiabetic.happ;

import android.app.Activity;
import android.content.Intent;
import android.preference.EditTextPreference;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Date;

public class BolusWizardActivity extends Activity {

    private EditText treatmentValue;
    private TextView biobValue;
    private TextView cobValue;
    private TextView netBIOB;
    private EditText carbs;
    private TextView reqInsulinCarbs;
    private TextView reqInsulinBg;
    private TextView sugBolus;

    Treatments bolusTreatment = new Treatments();
    Treatments carbTratment = new Treatments();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        biobValue       = (TextView) findViewById(R.id.wizardIOB);
        cobValue        = (TextView) findViewById(R.id.wizardCOB);
        netBIOB         = (TextView) findViewById(R.id.wizardNetIOB);
        carbs           = (EditText) findViewById(R.id.wizardCarbValue);
        reqInsulinCarbs = (TextView) findViewById(R.id.wizardReqInsulinCarbs);
        reqInsulinBg    = (TextView) findViewById(R.id.wizardReqInsulinBg);
        sugBolus        = (TextView) findViewById(R.id.wizardSugBolus);
        Double carbValue = 0D;

        if (!carbs.getText().toString().equals("")) carbValue = Double.parseDouble(carbs.getText().toString());

        JSONObject bw = BolusWizard.bw(this.getBaseContext(), carbValue);

        try {
            biobValue.setText("Bolus IOB: " + bw.getString("biob"));
            cobValue.setText("COB: " + bw.getString("cob"));
            netBIOB.setText("Net Bolus IOB: " + bw.getString("net_biob"));
            reqInsulinCarbs.setText("Carb Correction: Carbs(" + carbValue + "g) / Carb Ratio(" + bw.getString("carbRatio") + "g) = " + bw.getString("insulin_correction_carbs") + "U");
            reqInsulinBg.setText("BG Correction: BG(" + bw.getString("bg") + ") - Traget BG(" + bw.getString("target_bg") + ") / ISF(" + bw.getString("isf") + ") = " + bw.getString("insulin_correction_bg"));
            sugBolus.setText("Suggested Bolus: " + bw.getString("insulin_correction_carbs") + " + " + bw.getString("insulin_correction_bg") + " - " + bw.getString("net_biob") + " = " + bw.getString("suggested_bolus"));

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
    }

    public void wizardAccept(View view){

        String toastMsg="Saved ";

        if (bolusTreatment.value != null){
            bolusTreatment.save();
            toastMsg += bolusTreatment.value + "U ";
        }
        if (carbTratment.value != null){
            carbTratment.save();
            toastMsg += carbTratment.value + "g ";
        }

        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();

    }
}
