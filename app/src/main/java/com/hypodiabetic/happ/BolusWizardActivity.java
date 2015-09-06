package com.hypodiabetic.happ;

import android.app.Activity;
import android.content.Intent;
import android.preference.EditTextPreference;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class BolusWizardActivity extends Activity {

    private EditText treatmentValue;
    private TextView biobValue;
    private TextView cobValue;
    private TextView netBIOB;
    private EditText carbs;
    private TextView reqInsulinCarbs;
    private TextView reqInsulinBg;
    private TextView sugBolus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bolus_wizard);

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

    public void wizardAccept(View view){



        JSONObject reply = BolusWizard.run_bw(view.getContext());

        TextView sysMsg;
        sysMsg = (TextView) findViewById(R.id.wizardCalc);
        sysMsg.setText(reply.toString());


        biobValue       = (TextView) findViewById(R.id.wizardIOB);
        cobValue        = (TextView) findViewById(R.id.wizardCOB);
        netBIOB         = (TextView) findViewById(R.id.wizardNetIOB);
        carbs           = (EditText) findViewById(R.id.wizardCarbValue);
        reqInsulinCarbs = (TextView) findViewById(R.id.wizardReqInsulinBg);
        reqInsulinBg    = (TextView) findViewById(R.id.wizardReqInsulinBg);
        sugBolus        = (TextView) findViewById(R.id.wizardSugBolus);
        Integer carbValue = 0;

        if (!carbs.getText().equals("")) carbValue = Integer.getInteger(carbs.getText().toString());


        JSONObject bw = BolusWizard.bw(view.getContext(), carbValue);

        try {
            biobValue.setText("Bolus IOB: " + bw.getString("biob"));
            cobValue.setText("COB: " + bw.getString("cob"));
            netBIOB.setText("Net Bolus IOB: " + bw.getString("net_biob"));
            reqInsulinCarbs.setText("Carbs(" + carbValue + ") / Carb Ratio(" + bw.getString("carbRatio") + ") = " + bw.getString("insulin_correction_carbs"));
            reqInsulinBg.setText(bw.getString("bg") + " - " + bw.getString("target_bg") + " * " + bw.getString("carbratio") + " = " + bw.getString("insulin_correction_bg"));
            sugBolus.setText(bw.getString("insulin_correction_carbs") + " + " + bw.getString("insulin_correction_bg") + " - " + bw.getString("net_biob") + " = " + bw.getString("suggested_bolus"));
        } catch (JSONException e) {
        }



    }
}
