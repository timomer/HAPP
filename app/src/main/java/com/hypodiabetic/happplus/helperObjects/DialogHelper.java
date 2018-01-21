package com.hypodiabetic.happplus.helperObjects;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.ClipboardManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;

/**
 * Created by Tim on 11/02/2017.
 */

public class DialogHelper {

    public static void newWithCopyToClipboard(final String msg, final Context context){
            new AlertDialog.Builder(context)
                    .setMessage(msg)
                    .setPositiveButton(context.getText(R.string.dialog_copy_to_clipboard), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            final android.content.ClipboardManager clipboardManager = (android.content.ClipboardManager) context
                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                            final android.content.ClipData clipData = android.content.ClipData
                                    .newPlainText("happ_data", msg);
                            clipboardManager.setPrimaryClip(clipData);

                            Toast.makeText(MainApp.getInstance(), context.getText(R.string.dialog_copy_to_clipboard), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .show();

            //if (msg.length() > 100) {
            //    TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
            //    textView.setTextSize(10);
            //}

    }


}
