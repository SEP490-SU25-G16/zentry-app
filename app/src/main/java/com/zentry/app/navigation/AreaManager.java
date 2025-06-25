package com.zentry.app.navigation;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.fragment.NavHostFragment;

import com.zentry.app.R;
import com.zentry.app.model.entity.UserRole;

public class AreaManager {

    /**
     * Switch to appropriate area based on user role after successful login
     * This method is called from SignInFragment after successful authentication.
     * @param fragment The current SignInFragment.
     * @param userRoleEnum User's role (STUDENT or TEACHER)
     */
    public static void navigateToUserArea(Fragment fragment, UserRole userRoleEnum) {
        NavController navController = NavHostFragment.findNavController(fragment);

        switch (userRoleEnum) {
            case STUDENT:
                // Navigate from SignInFragment to the student area graph
                navController.navigate(R.id.action_signInFragment_to_student_area);
                break;

            case TEACHER: // Assuming TEACHER corresponds to "lecturer" role string
                // Navigate from SignInFragment to the lecture area graph
                navController.navigate(R.id.action_signInFragment_to_lecture_area);
                break;

            default:
                throw new IllegalArgumentException("Unknown user role enum for switching area: " + userRoleEnum);
        }
    }

    // You can remove navigateToStudentArea and navigateToLecturerArea if they are only called from SignInFragment
    // and consolidate the logic into navigateToUserArea.
    // However, if you need separate helper methods, they would now be:
    /*
    public static void navigateToStudentArea(Fragment fragment) {
        NavController navController = NavHostFragment.findNavController(fragment);
        navController.navigate(R.id.action_signInFragment_to_student_area); // Make sure this is called from signInFragment
    }

    public static void navigateToLecturerArea(Fragment fragment) {
        NavController navController = NavHostFragment.findNavController(fragment);
        navController.navigate(R.id.action_signInFragment_to_lecture_area); // Make sure this is called from signInFragment
    }
    */

    /**
     * Logout and return to Auth Area (main_nav_graph).
     * This method clears the current navigation graph and sets the main auth graph.
     * @param fragment The current fragment from which logout is initiated.
     */
    public static void logout(Fragment fragment) {
        NavController navController = NavHostFragment.findNavController(fragment);
        // Navigate to the signInFragment which is the start destination of the main nav_graph
        // This will pop up all current fragments/graphs and reset to sign in.
        navController.navigate(R.id.signInFragment, null, new androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true) // Pop up all fragments from the current graph and the main nav_graph itself
                .build());
    }

    /**
     * Helper method to navigate to the Sign In screen directly from any fragment.
     * This method can be used for explicit navigation to sign-in without logout logic.
     * @param fragment The current fragment.
     */
    public static void navigateToSignIn(Fragment fragment) {
        NavController navController = NavHostFragment.findNavController(fragment);
        // This is similar to logout, ensuring we go back to the sign-in screen
        navController.navigate(R.id.signInFragment, null, new androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true) // Pop up all fragments from the current graph and the main nav_graph itself
                .build());
    }


    /**
     * Get appropriate bottom navigation menu based on role string
     * @param roleString User's role as a string (e.g., "student", "lecturer")
     * @return Menu resource ID
     */
    public static int getBottomNavigationMenu(String roleString) {
        UserRole userRoleEnum = getUserRoleFromString(roleString);
        if (userRoleEnum != null) {
            switch (userRoleEnum) {
                case STUDENT:
                    return R.menu.bottom_nav_student;
                case TEACHER:
                    return R.menu.bottom_nav_lecture;
                default:
                    return 0;
            }
        }
        return 0; // No bottom nav for unknown role
    }

    /**
     * Helper method to convert role string to UserRole enum.
     * @param roleString The role as a string (e.g., "student", "lecturer").
     * @return The corresponding UserRole enum, or null if not recognized.
     */
    public static UserRole getUserRoleFromString(String roleString) {
        if (roleString == null) {
            return null;
        }
        switch (roleString.toLowerCase()) {
            case "student":
                return UserRole.STUDENT;
            case "lecturer": // Ensure this matches the string returned by your API for teachers
                return UserRole.TEACHER; // Use TEACHER if your enum is TEACHER
            default:
                return null;
        }
    }
}