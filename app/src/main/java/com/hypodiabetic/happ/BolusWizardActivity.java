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

import org.json.JSONObject;

public class BolusWizardActivity extends Activity {

    private EditText treatmentValue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bolus_wizard);

        Intent intent = getIntent();
        treatmentValue = (EditText) findViewById(R.id.wizardValue);
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

    public void runBW(View view){
        JSONObject reply = BolusWizard.run_bw(view.getContext());

        TextView sysMsg;
        sysMsg = (TextView) findViewById(R.id.wizardCalc);
        sysMsg.setText(reply.toString());
    }
}
