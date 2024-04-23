package com.skz81.simplenfc2http;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// import android.util.Log;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private int nextItemId = 1;
    // private static final String TAG = "AutoWatS.ViewPagerAdapter";
    private final List<PageData> visiblePages = new ArrayList<>();
    private final Map<Fragment, PageData> pages = new HashMap<>();
    private FragmentActivity activity;

    public ViewPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
        activity = fa;
    }

    // Note: add, remove, show and hide page operations are "cached"
    // When done manipulating the set, please call updateViewPager()
    public void addPage(Fragment fragment, String title, boolean visible) {
        pages.put(fragment, new PageData(fragment, title, visible));
    }

    public void removePage(Fragment fragment, String title, boolean visible) {
        pages.remove(fragment);
    }

    public void showPage(Fragment fragment) {
        pages.get(fragment).visible = true;
    }

    public void hidePage(Fragment fragment) {
        pages.get(fragment).visible = false;
    }

    public CharSequence getPageTitle(int position) {
        return visiblePages.get(position).name;
    }

    @NonNull
    @Override
    public Fragment createFragment(int index) {
        return visiblePages.get(index).fragment;
    }

    public void updateViewPager() {
        visiblePages.clear();
        for (PageData page : pages.values()) {
            if(page.visible) {
                visiblePages.add(page);
            } else {
            }
        }
        // NOTE: unclean hack, but should work for simple use cases
        visiblePages.sort(Comparator.comparingLong(pageData -> pageData.itemId));

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {return visiblePages.size();}

    @Override
    public long getItemId(int position) {
        PageData page = visiblePages.get(position);
        // Log.i(TAG, "getItemId(" + position + ")=" +
        //            ((page == null) ? "NO_ID"
        //                                : String.valueOf(page.itemId)));
        return page == null ? View.NO_ID : page.itemId;
    }

    @Override
    public boolean containsItem(long itemId) {
        for (PageData page: visiblePages) {
            if (page.itemId == itemId) {
                return true;
            }
        };
        return false;
    }

    private class PageData {
        private Fragment fragment;
        private boolean visible;
        private String name;
        private long itemId;
        // private int position;

        public PageData(Fragment fragment, String name, boolean visible) {
            this.fragment = fragment;
            this.visible = visible;
            this.name = name;
            this.itemId = nextItemId;
            nextItemId++;
        }
    }
}
