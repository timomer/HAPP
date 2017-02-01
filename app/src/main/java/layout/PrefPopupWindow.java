package layout;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.database.Profile;
import com.hypodiabetic.happplus.database.RealmHelper;
import com.hypodiabetic.happplus.database.dbHelperProfile;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.devices.SysProfileDevice;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

/**
 * Created by Tim on 20/01/2017.
 * Pref Popup Window where new value for this pref can be selected
 * Plugin owner of this pref is notified of the change
 */

public class PrefPopupWindow extends android.widget.PopupWindow {

    private Context mContext;
    private final SysPref sysPref;
    private final AbstractPluginBase plugin;
    private ListView sysPrefItems;
    private EditText sysPrefTextValue;
    private final TextView textView;
    private TextView sysPrefPopupDesc;
    private TextView sysPrefPopupSubTitle;
    private TextView sysPrefPopupTitle;
    private Button btnSave;
    private Spinner sysPrefSysProfiles;

    public PrefPopupWindow(Context context, final SysPref sysPref, AbstractPluginBase plugin, final TextView textView) {
        super(context);

        this.mContext =   context;
        this.sysPref    =   sysPref;
        this.plugin     =   plugin;
        this.textView   =   textView;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView       =   inflater.inflate(R.layout.pref_popup, null);
        setContentView(popupView);

        sysPrefPopupTitle           =   (TextView) popupView.findViewById(R.id.sysPrefPopupTitle);
        sysPrefPopupSubTitle        =   (TextView) popupView.findViewById(R.id.sysPrefPopupSubTitle);
        sysPrefPopupDesc            =   (TextView) popupView.findViewById(R.id.sysPrefPopupDesc);
        sysPrefSysProfiles       =   (Spinner) popupView.findViewById(R.id.sysPrefSysProfiles);
        sysPrefItems                    =   (ListView) popupView.findViewById(R.id.sysPrefItems);
        btnSave                         =   (Button) popupView.findViewById(R.id.sysPrefSave);
        Button btnCancel                =   (Button) popupView.findViewById(R.id.sysPrefCancel);
        sysPrefTextValue                =   (EditText) popupView.findViewById(R.id.sysPrefText);

        sysPrefItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Save pref, if a list of Plugins is displayed, save the plugin name and not toString value
                if (AbstractPluginBase.class.isAssignableFrom(parent.getItemAtPosition(position).getClass())){
                    AbstractPluginBase pluginBase = (AbstractPluginBase) sysPref.getPefValues().get(position);
                    updatePref(pluginBase.getPluginName());
                } else {
                    updatePref(sysPref.getPefValues().get(position).toString());
                }
            }
        });
        sysPrefSysProfiles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Updated the highlighted pref with the value set in the selected profile
                Profile profile = (Profile) parent.getSelectedItem();
                setPrefList(new SysPref<>(
                        sysPref.getPrefName(),
                        sysPref.getPrefDisplayName(),
                        sysPref.getPrefDescription(),
                        sysPref.getPefValues(),
                        sysPref.getPrefDisplayValues(),
                        PluginPref.PREF_TYPE_LIST,
                        profile.getId()).getDefaultStringValue());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updatePref(sysPrefTextValue.getText().toString());
            }
        });

        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        setWidth(WindowManager.LayoutParams.WRAP_CONTENT);

        // Closes the popup window when touch outside of it - when looses focus
        setOutsideTouchable(true);
        setFocusable(true);

        // Removes default black background
        setBackgroundDrawable((new ColorDrawable(ContextCompat.getColor(context, android.R.color.background_light))));
    }

    // Attaches the view to its parent anchor-view at position x and y
    public void show(View anchor, int x, int y) {
        loadUI();
        showAtLocation(anchor, Gravity.CENTER, x, y);
    }


    private void loadUI(){
        // Setup the UI
        RealmHelper realmHelper = new RealmHelper();
        sysPrefPopupTitle.setText(      sysPref.getPrefDisplayName());
        String subTitle =               sysPref.getSysProfileName() + " " + mContext.getString(R.string.device_profile);
        sysPrefPopupSubTitle.setText(   subTitle);
        sysPrefPopupDesc.setText(       sysPref.getPrefDescription());

        //Customise UI based on Pref Type
        switch (sysPref.getPrefType()){
            case PluginPref.PREF_TYPE_LIST:
                btnSave.setVisibility(View.GONE);
                sysPrefTextValue.setVisibility(View.GONE);
                sysPrefSysProfiles.setAdapter(  new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1 , dbHelperProfile.getProfileList(realmHelper.getRealm(), Profile.TYPE_SYS_PROFILE)));
                sysPrefSysProfiles.setSelection(Utilities.getIndex(sysPrefSysProfiles, SysProfileDevice.DEFAULT_SYS_PROFILE_NAME), false);
                sysPrefSysProfiles.setPrompt(mContext.getString(R.string.pref_compare_profile));
                setPrefList(sysPref.getDefaultStringValue());
                break;
            case PluginPref.PREF_TYPE_DOUBLE:
                sysPrefItems.setVisibility(View.GONE);

                break;
            case PluginPref.PREF_TYPE_INT:
                sysPrefItems.setVisibility(View.GONE);

                break;
            case PluginPref.PREF_TYPE_STRING:
                sysPrefItems.setVisibility(View.GONE);

                break;
        }

        realmHelper.closeRealm();
    }

    private void updatePref(String newValue){
        sysPref.update(newValue);
        textView.setText(sysPref.getPrefDisplayValue());
        plugin.refreshPrefs(sysPref);

        Intent prefUpdate = new Intent(Intents.newLocalEvent.NEW_LOCAL_EVENT_PREF_UPDATE);
        prefUpdate.putExtra(Intents.extras.PLUGIN_NAME, plugin.getPluginName());
        prefUpdate.putExtra(Intents.extras.PLUGIN_TYPE, plugin.getPluginType());
        prefUpdate.putExtra(Intents.extras.PLUGIN_PREF_NAME, sysPref.getPrefName());
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(prefUpdate);

        dismiss();
    }


    private void setPrefList(String prefOptionToHighlight){
        sysPrefItems.setAdapter(        new AdapterSysPref<>(mContext, sysPref.getPrefDisplayValues(), prefOptionToHighlight));
    }

}
