package layout;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tim on 12/02/2017.
 * Dynamic Fragment Page Adapter that loads Fragments from its own internal list and
 * handling of null fragments. Extended from {@link HAPPFragmentStatePagerAdapter} to handel
 * dynamic loading of Plugin Fragments
 */

public class DynamicFragmentPagerAdapter extends HAPPFragmentStatePagerAdapter {

    public DynamicFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    /**
     * Contains all the fragments.
     */
    private List<Fragment> fragments = new ArrayList<>();

    /**
     * Contains all the tab titles.
     */
    private List<String> tabTitles = new ArrayList<>();

    /**
     * Adds the fragment to the list, also adds the fragment's tab title.
     * @param fragment New instance of the Fragment to be associated with this tab.
     * @param tabTitle A String containing the tab title for this Fragment.
     */
    public void addFragment(Fragment fragment, String tabTitle) {
        fragments.add(fragment);
        tabTitles.add(tabTitle);
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // If Fragment already exists, the FragmentPagerAdapter uses the cached copy (no need to handle yourself with find tag by ID, etc)
        // http://stackoverflow.com/questions/6976027/reusing-fragments-in-a-fragmentpageradapter
        Fragment fragment = fragments.get(position);
        if (fragment == null) {
            //Fragment cannot be found
            return FragmentCannotLoadFragment.newInstance("'" + tabTitles.get(position) + "' " + MainApp.getInstance().getString(R.string.fragment_missing_cannot_find));
        } else {
            return fragment;
        }
    }


    @Override
    public int getCount() {
        if (fragments == null) return 0;
        return fragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles.get(position);
    }
}

