package com.hypodiabetic.happplus;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;

import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;

import layout.FragmentCannotLoadFragment;
import layout.FragmentEventEntry;


public class SingleFragmentActivity extends FragmentActivity {

    private final static String TAG = "SingleFragmentActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            String pluginName   =   extras.getString(Intents.extras.PLUGIN_NAME);
            String fragmentName =   extras.getString(Intents.extras.FRAGMENT_NAME);

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            if (fragmentName != null){
                switch (fragmentName){
                    case "FragmentEventEntry":
                        fragmentTransaction.add(R.id.fragmentHolder, FragmentEventEntry.newInstance());
                        fragmentTransaction.commit();
                        break;
                    default:
                        Log.d(TAG, "onCreate: exiting, unknown Fragment: " + fragmentName);
                        this.finish();
                }

            } else if (pluginName != null){


                AbstractPluginBase plugin = MainApp.getPluginByName(pluginName);

                if (plugin != null){
                    fragmentTransaction.add(R.id.fragmentHolder, plugin);
                    fragmentTransaction.commit();

                } else {
                    Log.d(TAG, "onCreate: cannot find plugin");
                    fragmentTransaction.add(R.id.fragmentHolder, FragmentCannotLoadFragment.newInstance("'" + pluginName + "' " + MainApp.getInstance().getString(R.string.fragment_missing_cannot_find)));
                    fragmentTransaction.commit();
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
