package com.hypodiabetic.happ.integration.nsclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hypodiabetic.happ.Intents;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Objects.Bg;
import com.hypodiabetic.happ.tools;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

import io.realm.Realm;

/**
 * Created by Tim on 10/08/2016.
 */
public class NSClientIncoming {
    private static String TAG = "NSClientIncoming";

    public static void New_sgv(Intent intent, Realm realm) {

        Log.d(TAG, "New NSClient SGV Received");

        Bundle bundles = intent.getExtras();
        if (bundles == null) return;

        try {
            // TODO: 10/08/2016 poss drop this, as NSClient only sends "sgvs" 
            if (bundles.containsKey("sgv")) {
                String sgvstring = bundles.getString("sgv");
                JSONObject sgvJson = new JSONObject(sgvstring);
                NSSgv nsSgv = new NSSgv(sgvJson);

                Bg bg = nsSgv2Bg(nsSgv);

                realm.beginTransaction();
                realm.copyToRealm(bg);
                realm.commitTransaction();

                Log.d(TAG, "New BG saved, sending out UI Update");

                Intent updateIntent = new Intent(Intents.UI_UPDATE);
                updateIntent.putExtra("UPDATE", "NEW_BG");
                LocalBroadcastManager.getInstance(MainApp.instance()).sendBroadcast(updateIntent);
            }

            if (bundles.containsKey("sgvs")) {
                String sgvstring = bundles.getString("sgvs");
                JSONArray jsonArray = new JSONArray(sgvstring);
                int newBGCount=0;
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject sgvJson = jsonArray.getJSONObject(i);
                    NSSgv nsSgv = new NSSgv(sgvJson);
                    Bg bg = nsSgv2Bg(nsSgv);

                    if (!Bg.haveBGTimestamped(bg.getDatetime(), realm)){
                        realm.beginTransaction();
                        realm.copyToRealm(bg);
                        realm.commitTransaction();
                        newBGCount++;
                    } else {
                        Log.d(TAG, "Already have a BG with this timestamp, ignoring");
                    }
                }

                if (newBGCount > 0) {
                    Log.d(TAG, (jsonArray.length() - newBGCount) + " BG(s) ignored");
                    Log.d(TAG, newBGCount + " New BG(s) saved, sending out UI Update");

                    Intent updateIntent = new Intent(Intents.UI_UPDATE);
                    updateIntent.putExtra("UPDATE", "NEW_BG");
                    LocalBroadcastManager.getInstance(MainApp.instance()).sendBroadcast(updateIntent);
                } else {
                    Log.d(TAG, jsonArray.length() + " BG(s) ignored");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error processing new SGV from NSClient: " + e.getLocalizedMessage());
        }
    }

    private static Bg nsSgv2Bg(NSSgv nsSgv){
        Bg bg = new Bg();
        bg.setDirection(nsSgv.getDirection());
        bg.setBattery(0);
        bg.setBgdelta(0);
        bg.setDatetime(new Date(nsSgv.getMills()));
        bg.setSgv(nsSgv.getMgdl().toString());

        return bg;
    }
}
