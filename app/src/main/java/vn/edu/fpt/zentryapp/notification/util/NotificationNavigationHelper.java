package vn.edu.fpt.zentryapp.notification.util;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class NotificationNavigationHelper {
    
    private static final String TAG = "NotificationNavHelper";
    
    /**
     * Handle back navigation từ StudentSettingNotificationFragment về NotificationFragment
     * @param fragment Fragment hiện tại (StudentSettingNotificationFragment)
     * @return true nếu đã handle back navigation, false nếu không
     */
    public static boolean handleBackNavigation(Fragment fragment) {
        Log.d(TAG, "handleBackNavigation called");
        
        if (fragment == null || fragment.getArguments() == null) {
            Log.d(TAG, "Fragment or arguments is null");
            return false;
        }
        
        Bundle args = fragment.getArguments();
        String sourceFragment = args.getString("source_fragment");
        boolean shouldNavigateBack = args.getBoolean("should_navigate_back_to_notification", false);
        
        Log.d(TAG, "Source: " + sourceFragment + ", ShouldNavigateBack: " + shouldNavigateBack);
        
        // Chỉ handle khi được navigate từ NotificationFragment
        if ("NotificationFragment".equals(sourceFragment) && shouldNavigateBack) {
            FragmentActivity activity = fragment.getActivity();
            if (activity != null) {
                FragmentManager fragmentManager = activity.getSupportFragmentManager();
                
                // Thử pop back stack với name đã định nghĩa
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    Log.d(TAG, "Back stack count: " + fragmentManager.getBackStackEntryCount());
                    try {
                        fragmentManager.popBackStackImmediate("NotificationToSettings", 0);
                        Log.d(TAG, "Pop back stack with name succeeded");
                        return true;
                    } catch (Exception e) {
                        Log.d(TAG, "Pop back stack with name failed, trying general pop");
                        // Nếu không tìm thấy back stack entry, thử pop back thường
                        try {
                            fragmentManager.popBackStackImmediate();
                            Log.d(TAG, "General pop back stack succeeded");
                            return true;
                        } catch (Exception ex) {
                            Log.d(TAG, "General pop back stack failed");
                            // Continue to fallback
                        }
                    }
                }
                
                // Fallback: tạo mới NotificationFragment và navigate
                Log.d(TAG, "Using fallback navigation");
                try {
                    Class<?> notificationFragmentClass = Class.forName("vn.edu.fpt.zentryapp.notification.ui.NotificationFragment");
                    Fragment notificationFragment = (Fragment) notificationFragmentClass.newInstance();
                    
                    // Tìm container
                    int containerId = findFragmentContainer(activity, fragment);
                    Log.d(TAG, "Container ID: " + containerId);
                    
                    fragmentManager.beginTransaction()
                            .replace(containerId, notificationFragment)
                            .commit();
                    
                    Log.d(TAG, "Fallback navigation succeeded");
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Fallback navigation failed", e);
                    e.printStackTrace();
                }
            }
        }
        
        Log.d(TAG, "No navigation handled");
        return false;
    }
    
    /**
     * Tìm fragment container
     */
    private static int findFragmentContainer(FragmentActivity activity, Fragment currentFragment) {
        // Thử tìm container của fragment hiện tại
        if (currentFragment.getView() != null) {
            ViewParent parent = currentFragment.getView().getParent();
            if (parent instanceof ViewGroup) {
                ViewGroup parentGroup = (ViewGroup) parent;
                int parentId = parentGroup.getId();
                if (parentId != -1) {
                    return parentId;
                }
            }
        }
        
        // Fallback: thử các container ID có thể tồn tại
        try {
            // Kiểm tra nav_host_fragment nếu tồn tại
            if (activity.findViewById(vn.edu.fpt.zentryapp.R.id.nav_host_fragment) != null) {
                return vn.edu.fpt.zentryapp.R.id.nav_host_fragment;
            }
        } catch (Exception e) {
            // ID không tồn tại
        }
        
        // Fallback cuối cùng: sử dụng android.R.id.content
        return android.R.id.content;
    }
} 