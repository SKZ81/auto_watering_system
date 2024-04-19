package com.skz81.simplenfc2http;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.List;

// TBRemoved
import android.util.Log;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "AutoWatS.ViewPagerAdapter";
    private final List<Fragment> fragmentList = new ArrayList<>();
    private final List<String> fragmentTitleList = new ArrayList<>();
    private FragmentActivity activity;

    public ViewPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
        activity = fa;
    }

    public void addFragment(Fragment fragment, String title) {
        fragmentList.add(fragment);
        fragmentTitleList.add(title);
    }

    public CharSequence getPageTitle(int position) {
        return fragmentTitleList.get(position);
    }

    @Override
    public Fragment createFragment(int index) {
        Log.i(TAG, "createFragment, index:" + index);
        return fragmentList.get(index);
    }

    public void removeFragments(Fragment... toBeRemoved) {
        for(Fragment fragment : toBeRemoved) {
            int index = fragmentList.indexOf(fragment);
            if (index != -1) {
                Log.i(TAG, "Removing fragment : " + fragment.toString() + ", index: "+ index);
                fragmentList.remove(index);
                fragmentTitleList.remove(index);
            } else {
                Log.w(TAG, "Can't remove not found fragment: " + fragment.toString());
            }
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // changeData();
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {return fragmentList.size();}
}
