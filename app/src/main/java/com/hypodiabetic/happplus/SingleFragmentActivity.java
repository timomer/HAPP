package com.hypodiabetic.happplus;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceNotifyFragmentBackPress;
import com.hypodiabetic.happplus.plugins.PluginManager;

import layout.FragmentCannotLoadFragment;
import layout.FragmentEventEntry;
import layout.FragmentProfileEditor24H;


public class SingleFragmentActivity extends FragmentActivity {

    private final static String TAG = "SingleFragmentActivity";

    public final static String FRAGMENT_EVENT_ENTRY     =   "FragmentEventEntry";
    public final static String FRAGMENT_PROFILE_EDITOR  =   "FragmentProfileEditor";

    private String fragmentTag;

    
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
                fragmentTag = fragmentName;
                switch (fragmentName){
                    case FRAGMENT_EVENT_ENTRY:
                        fragmentTransaction.add(R.id.fragmentHolder, FragmentEventEntry.newInstance(), fragmentTag);
                        fragmentTransaction.commit();
                        break;
                    case FRAGMENT_PROFILE_EDITOR:
                        fragmentTransaction.add(R.id.fragmentHolder, FragmentProfileEditor24H.newInstance(extras.getString(FragmentProfileEditor24H.ARG_PREF_NAME), extras.getString(FragmentProfileEditor24H.ARG_PREF_PLUGIN)),fragmentTag);

                        fragmentTransaction.commit();
                        break;
                    default:
                        Log.d(TAG, "onCreate: exiting, unknown Fragment: " + fragmentName);
                        this.finish();
                }

            } else if (pluginName != null){


                AbstractPluginBase plugin = PluginManager.getPluginByName(pluginName);

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

    @Override
    public void onBackPressed() {
        //Notify the Fragment user has Back Pressed
        final InterfaceNotifyFragmentBackPress fragment = (InterfaceNotifyFragmentBackPress) getSupportFragmentManager().findFragmentByTag(fragmentTag);

        if (fragment != null){
            if(fragment.onBackPress()){
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        return false;
    }
}
