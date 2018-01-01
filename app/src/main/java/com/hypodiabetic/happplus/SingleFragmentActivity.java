package com.hypodiabetic.happplus;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceHasActionBar;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceNotifyFragmentBackPress;
import com.hypodiabetic.happplus.plugins.PluginManager;

import layout.FragmentCannotLoadFragment;
import layout.FragmentEventEntry;
import layout.FragmentProfileEditor24H;

/**
 * Activity container for displaying Fragments
 */


public class SingleFragmentActivity extends AppCompatActivity  {

    private final static String TAG = "SingleFragmentActivity";
    Toolbar toolbar;
    public final static String FRAGMENT_EVENT_ENTRY     =   "FragmentEventEntry";
    public final static String FRAGMENT_PROFILE_EDITOR  =   "FragmentProfileEditor";

    private String fragmentTag;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().hide();

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
                        setActionBar();
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
                    if (InterfaceHasActionBar.class.isAssignableFrom(plugin.getClass())) { setActionBar();}
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

    private void setActionBar(){
        getSupportActionBar().show();
    }

    @Override
    public void onBackPressed() {
        //Notify the Fragment user has Back Pressed
        final Fragment loadedFragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);

        if (loadedFragment != null) {
            if (InterfaceNotifyFragmentBackPress.class.isAssignableFrom(loadedFragment.getClass())) {
                final InterfaceNotifyFragmentBackPress fragment = (InterfaceNotifyFragmentBackPress) loadedFragment;
                if (fragment.onBackPress()) {
                    super.onBackPressed();
                }
            } else {
                super.onBackPressed();
            }
        }else {
            super.onBackPressed();
        }
    }


}
