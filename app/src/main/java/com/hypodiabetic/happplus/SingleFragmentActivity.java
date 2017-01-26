package com.hypodiabetic.happplus;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.hypodiabetic.happplus.plugins.PluginBase;
import com.hypodiabetic.happplus.plugins.devices.PluginDevice;


public class SingleFragmentActivity extends FragmentActivity {

    private final static String TAG = "SingleFragmentActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            String pluginName   =   extras.getString(Intents.extras.PLUGIN_NAME);

            if (pluginName != null){
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                PluginBase plugin = MainApp.getPluginByName(pluginName);

                if (plugin != null){
                    fragmentTransaction.add(R.id.fragmentHolder, plugin);
                    fragmentTransaction.commit();

                } else {
                    Log.d(TAG, "onCreate: exiting, cannot find plugin");
                    this.finish();
                }
            } else {
                Log.d(TAG, "onCreate: exiting, missing Intent Data");
                this.finish();
            }
        } else {
            Log.d(TAG, "onCreate: exiting, missing Intent Extras");
            this.finish();
        }

    }

}
