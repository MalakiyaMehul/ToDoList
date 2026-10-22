package com.example.todolist;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.todolist.Fragment.CalendarFragment;
import com.example.todolist.Fragment.FocusFragment;
import com.example.todolist.Fragment.IndexFragment;
import com.example.todolist.Fragment.ProfileFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {
    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new IndexFragment();
            case 1:
                return new CalendarFragment();
            case 2:
                return new FocusFragment();
            case 3:
                return new ProfileFragment();
            default:
                return new IndexFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
