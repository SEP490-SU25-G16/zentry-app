package vn.edu.fpt.zentryapp.helper;

import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class BottomNavigationHelper {

    private final FragmentManager fragmentManager;
    private final int containerId;
    private final Map<Integer, String> tabTags = new HashMap<>();
    private final Map<Integer, Integer> tabGraphs = new HashMap<>();
    private final BottomNavigationView bottomNavigationView;
    private int currentTabId;

    public BottomNavigationHelper(FragmentManager fragmentManager, int containerId,
                                  BottomNavigationView bottomNavigationView) {
        this.fragmentManager = fragmentManager;
        this.containerId = containerId;
        this.bottomNavigationView = bottomNavigationView;
        setupBottomNavigation();
    }

    public void addTab(int menuItemId, int graphId) {
        String tag = "tab_" + menuItemId;
        tabTags.put(menuItemId, tag);
        tabGraphs.put(menuItemId, graphId);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (currentTabId == itemId) {
            // If same tab clicked, pop to root of that tab
            NavHostFragment currentFragment = getCurrentNavHostFragment();
            if (currentFragment != null) {
                NavController navController = currentFragment.getNavController();
                navController.popBackStack(navController.getGraph().getStartDestination(), false);
            }
            return true;
        }

        switchTab(itemId);
        return true;
    }

    private void switchTab(int tabId) {
        String tag = tabTags.get(tabId);
        if (tag == null) return;

        // Hide current fragment
        if (currentTabId != 0) {
            String currentTag = tabTags.get(currentTabId);
            Fragment currentFragment = fragmentManager.findFragmentByTag(currentTag);
            if (currentFragment != null) {
                fragmentManager.beginTransaction()
                        .hide(currentFragment)
                        .commit();
            }
        }

        // Show or create target fragment
        Fragment targetFragment = fragmentManager.findFragmentByTag(tag);
        if (targetFragment == null) {
            // Create new NavHostFragment for this tab
            targetFragment = NavHostFragment.create(tabGraphs.get(tabId));
            fragmentManager.beginTransaction()
                    .add(containerId, targetFragment, tag)
                    .commit();

            // Wait for fragment to be added, then pass arguments
            fragmentManager.executePendingTransactions();
            NavHostFragment navHostFragment = (NavHostFragment) targetFragment;
            NavController navController = navHostFragment.getNavController();

            navController.navigate(navController.getGraph().getStartDestination());
        } else {
            fragmentManager.beginTransaction()
                    .show(targetFragment)
                    .commit();
        }

        currentTabId = tabId;

        // Update bottom navigation selection
        bottomNavigationView.getMenu().findItem(tabId).setChecked(true);
    }

    public void selectInitialTab(int tabId) {
        switchTab(tabId);
    }

    private NavHostFragment getCurrentNavHostFragment() {
        if (currentTabId == 0) return null;
        String tag = tabTags.get(currentTabId);
        return (NavHostFragment) fragmentManager.findFragmentByTag(tag);
    }

    public boolean handleBackPress() {
        NavHostFragment currentFragment = getCurrentNavHostFragment();
        if (currentFragment != null) {
            NavController navController = currentFragment.getNavController();
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() != navController.getGraph().getStartDestination()) {
                navController.popBackStack();
                return true;
            }
        }
        return false;
    }
}
