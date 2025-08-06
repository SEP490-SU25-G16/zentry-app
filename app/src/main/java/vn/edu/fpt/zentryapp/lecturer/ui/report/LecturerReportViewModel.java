package vn.edu.fpt.zentryapp.lecturer.ui.report;

import android.os.Handler;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerReportClassSection;

public class LecturerReportViewModel extends ViewModel {

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<LecturerReportClassSection>> _classrooms = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<List<LecturerReportClassSection>> classrooms() {
        return _classrooms;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }
    private AuthManager authManager;

    public void init(AuthManager authManager) {
        this.authManager = authManager;
        loadClassrooms();
    }

    /**
     * Load classrooms data (main method for this screen)
     */
    public void loadClassrooms() {
        _isLoading.setValue(true);

        // Simulate network delay
        new Handler().postDelayed(() -> {
            List<LecturerReportClassSection> mockClassrooms = generateMockClassrooms();
            _classrooms.setValue(mockClassrooms);
            _isLoading.setValue(false);
        }, 1000);
    }

    /**
     * Generate mock classrooms data matching the new model structure
     */
    private List<LecturerReportClassSection> generateMockClassrooms() {
        List<LecturerReportClassSection> classrooms = new ArrayList<>();

        // Mathematics classes
        classrooms.add(new LecturerReportClassSection(
                "Mathematics -G701",
                20,
                15,
                20,
                90.0,
                "SE1801 - Room DE-201",
                "Mon 08:00-09:30",
                "ACTIVE"
        ));

        classrooms.add(new LecturerReportClassSection(
                "Mathematics -G702",
                20,
                15,
                20,
                75.0,
                "SE1802 - Room DE-203",
                "Tue 09:45-11:15",
                "ACTIVE"
        ));

        classrooms.add(new LecturerReportClassSection(
                "Mathematics -G703",
                18,
                12,
                18,
                85.0,
                "SE1803 - Room DE-105",
                "Wed 13:30-15:00",
                "ACTIVE"
        ));

        // Computer Science classes
        classrooms.add(new LecturerReportClassSection(
                "Data Structures -G801",
                25,
                18,
                22,
                88.0,
                "CS1801 - Room IT-301",
                "Thu 15:15-16:45",
                "ACTIVE"
        ));

        classrooms.add(new LecturerReportClassSection(
                "Web Development -G901",
                22,
                16,
                20,
                92.0,
                "CS1802 - Room IT-302",
                "Fri 17:00-18:30",
                "ACTIVE"
        ));

        // Physics classes
        classrooms.add(new LecturerReportClassSection(
                "Physics Lab -P501",
                15,
                10,
                15,
                67.0,
                "PH1801 - Lab P-201",
                "Mon 14:00-16:00",
                "ACTIVE"
        ));

        return classrooms;
    }

    /**
     * Handle classroom item click
     */
    public void onClassroomClicked(LecturerReportClassSection classroom) {
        // Log the click for debugging
        android.util.Log.d("LecturerReport", "Classroom clicked: " + classroom.getCourseName());

        // This method can be extended to handle specific business logic
        // before navigation (e.g., tracking analytics, preparing data, etc.)
    }

    /**
     * Search classrooms by name
     */
    public void searchClassrooms(String query) {
        List<LecturerReportClassSection> currentList = _classrooms.getValue();
        if (currentList == null) return;

        if (query == null || query.trim().isEmpty()) {
            // Show all classrooms if query is empty
            loadClassrooms();
            return;
        }

        List<LecturerReportClassSection> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (LecturerReportClassSection classroom : currentList) {
            if (classroom.getCourseName().toLowerCase().contains(lowerQuery) ||
                    classroom.getClassInfo().toLowerCase().contains(lowerQuery)) {
                filteredList.add(classroom);
            }
        }

        _classrooms.setValue(filteredList);
    }

    /**
     * Get classrooms by status
     */
    public void filterByStatus(String status) {
        List<LecturerReportClassSection> currentList = _classrooms.getValue();
        if (currentList == null) return;

        if (status == null || status.equals("ALL")) {
            loadClassrooms();
            return;
        }

        List<LecturerReportClassSection> filteredList = new ArrayList<>();
        for (LecturerReportClassSection classroom : currentList) {
            if (status.equals(classroom.getStatus())) {
                filteredList.add(classroom);
            }
        }

        _classrooms.setValue(filteredList);
    }

}
