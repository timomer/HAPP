package com.hypodiabetic.happ.integration.openaps.dev;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Tim on 21/11/2015.
 * Allows running native OpenAPS determine-basal.js in J2V8
 * From project by LorelaiL https://github.com/timomer/AndroidAPS
 * Files include:
 * DetermineBasalAdapterJS.java
 * ScriptReader.java
 */
public class ScriptReader {


    private final Context mContext;

    public ScriptReader(Context context) {
        mContext = context;
    }

    public byte[] readFile(String fileName) throws IOException {

        AssetManager assetManager = mContext.getAssets();
        InputStream is = assetManager.open(fileName);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        byte[] bytes = buffer.toByteArray();
        is.close();
        buffer.close();


        return bytes;

    }

}
