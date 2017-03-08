package layout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hypodiabetic.happplus.R;

/**
 * Returened when DynamicFragmentPageAdapter tries to load a Null fragment
 */
public class FragmentCannotLoadFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";

    // TODO: Rename and change types of parameters
    private String mParam1;

    public FragmentCannotLoadFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param errorMsg Message displayed in the Fragment
     * @return A new instance of fragment FragmentMissingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentCannotLoadFragment newInstance(String errorMsg) {
        FragmentCannotLoadFragment fragment = new FragmentCannotLoadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, errorMsg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view   =   inflater.inflate(R.layout.fragment_missing, container, false);

        TextView textView   =   (TextView) view.findViewById(R.id.missingFragmentText);
        textView.setText(mParam1);
        return view;
    }

}
