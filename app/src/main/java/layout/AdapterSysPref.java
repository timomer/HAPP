package layout;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.plugins.PluginBase;

import java.util.List;


/**
 * Created by Tim on 18/01/2017.
 * ArrayAdapter that highlights the Default Sys Profile Pref with an *
 */

public class AdapterSysPref<T> extends ArrayAdapter{

    private final String mDefaultPrefValue;
    private final int mResource;
    private final Context mContext;

    public AdapterSysPref(Context context, List<T> objects, String defaultPrefValue){
        super(context, R.layout.spinner_dropdown_item, objects);
        this.mContext           =   context;
        this.mDefaultPrefValue  =   defaultPrefValue;
        this.mResource          =   R.layout.spinner_dropdown_item;


    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getItemViewWithDefaultPrefHighlighted(position, convertView, parent, false);
    }

    @Override
    public View getDropDownView (int position, View convertView, ViewGroup parent){
        return getItemViewWithDefaultPrefHighlighted(position, convertView, parent, true);
    }

    private View getItemViewWithDefaultPrefHighlighted(int position,  View convertView, ViewGroup parent, boolean dropDownText){
        View view;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            view = inflater.inflate(mResource, parent, false);
        } else {
            view = convertView;
        }

        CheckedTextView checkedTextView = (CheckedTextView) view.findViewById(android.R.id.text1);
        String strValue;
        try {
            strValue    =   getItem(position).toString();
        } catch (NullPointerException n){
            strValue    =   "";
        }

        if (mDefaultPrefValue != null) {
            if (PluginBase.class.isAssignableFrom(getItem(position).getClass())) {
                if (MainApp.getPluginByName(mDefaultPrefValue).getPluginDisplayName().equals(strValue)){
                    strValue = strValue + " *";
                }
            } else {
                if (mDefaultPrefValue.equals(strValue)) {
                    strValue = strValue + " *";
                }
            }
        }

        checkedTextView.setText(strValue);

        return view;
    }
}
