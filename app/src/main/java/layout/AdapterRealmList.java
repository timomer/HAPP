package layout;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;

import com.hypodiabetic.happplus.R;

import io.realm.OrderedRealmCollection;
import io.realm.RealmBaseAdapter;
import io.realm.RealmModel;

/**
 * Created by Tim on 25/01/2017.
 * Simple Realm List Adapter for use with Spinners that auto update with a Realm data source
 */

public class AdapterRealmList<T extends RealmModel> extends RealmBaseAdapter<T> implements ListAdapter {

    private Context mContext;
    private int mResource;

    public AdapterRealmList(Context context, OrderedRealmCollection<T> realmResults) {
        super(context,realmResults);
        this.mContext           =   context;
        this.mResource          =   R.layout.spinner_dropdown_item;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
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

        checkedTextView.setText(strValue);

        return view;
    }
}