package com.hypodiabetic.happ.Objects;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Tim on 21/08/2015.
 */
public class Pref_Lists_Repo {

    public static final String PREFS_NAME = "PRODUCT_APP";
    public static final String BASALS = "Basals";

    public Pref_Lists_Repo() {
        super();
    }

    // This four methods are used for maintaining favorites.
    public void saveBasals(Context context, List<Basal> favorites) {
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        editor = settings.edit();

        Gson gson = new Gson();
        String jsonBasals = gson.toJson(favorites);

        editor.putString(BASALS, jsonBasals);

        editor.commit();
    }

    public void addBasal(Context context, Basal basal) {
        List<Basal> basals = getBasals(context);
        if (basals == null)
            basals = new ArrayList<Basal>();
        basals.add(basal);
        saveBasals(context, basals);
    }

    public void removeBasal(Context context, Basal basal) {
        ArrayList<Basal> basals = getBasals(context);
        if (basals != null) {
            basals.remove(basal);
            saveBasals(context, basals);
        }
    }

    public ArrayList<Basal> getBasals(Context context) {
        SharedPreferences settings;
        List<Basal> basals;

        settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);

        if (settings.contains(BASALS)) {
            String jsonBasals = settings.getString(BASALS, null);
            Gson gson = new Gson();
            Basal[] basalItems = gson.fromJson(jsonBasals,
                    Basal[].class);

            basals = Arrays.asList(basalItems);
            basals = new ArrayList<Basal>(basals);
        } else
            return null;

        return (ArrayList<Basal>) basals;
    }
}
