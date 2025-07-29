package vn.edu.fpt.zentryapp.student.ui.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lombok.Getter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.student.data.api.StudentScheduleApiService;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentClassSectionData;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentDailyScheduleResponse;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleSession;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionData;

public class StudentScheduleViewModel extends ViewModel {
    private static final String TAG = "StudentScheduleVM";

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<StudentScheduleSession>> _schedules = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> _userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> _greeting = new MutableLiveData<>();
    private final MutableLiveData<String> _subGreeting = new MutableLiveData<>();

    // API Service
    private StudentScheduleApiService apiService;
    private AuthManager authManager;
    private Context context;

    // Public getters
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<StudentScheduleSession>> schedules() { return _schedules; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<String> successMessage() { return _successMessage; }
    public LiveData<UserProfile> userProfile() { return _userProfile; }
    public LiveData<String> greeting() { return _greeting; }
    public LiveData<String> subGreeting() { return _subGreeting; }

    public void init(Context context, AuthManager authManager) {
        this.context = context;
        this.authManager = authManager;
        this.apiService = ApiClient.getClient(context).create(StudentScheduleApiService.class);

        loadUserProfile();
        loadSchedules();
        generateGreeting();
    }

    public void loadSchedules() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

        if (authManager == null || !authManager.isLoggedIn()) {
            _errorMessage.setValue("User not logged in");
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        String studentId = authManager.getCurrentUserId();
        String todayDate = getTodayDateString();

        Log.d(TAG, "Loading daily schedule for student: " + studentId + ", date: " + todayDate);

        apiService.getDailySchedule(studentId, todayDate)
                .enqueue(new Callback<StudentDailyScheduleResponse>() {
                    @Override
                    public void onResponse(Call<StudentDailyScheduleResponse> call, Response<StudentDailyScheduleResponse> response) {
                        _isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            StudentDailyScheduleResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                List<StudentScheduleSession> sessions = mapApiDataToSessions(apiResponse.getData());
                                _schedules.setValue(sessions);
                                _successMessage.setValue("Schedules loaded successfully");
                                Log.d(TAG, "✅ Loaded " + sessions.size() + " student sessions");
                            } else {
                                String error = apiResponse.getError() != null ? apiResponse.getError() : "Unknown API error";
                                _errorMessage.setValue(error);
                                Log.e(TAG, "❌ API Error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue(error);
                            Log.e(TAG, "❌ " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<StudentDailyScheduleResponse> call, Throwable t) {
                        _isLoading.setValue(false);
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "❌ Network Error", t);
                    }
                });
    }

    /**
     * 🔧 MAP API data sang StudentScheduleSession objects
     */
    private List<StudentScheduleSession> mapApiDataToSessions(List<StudentClassSectionData> apiData) {
        List<StudentScheduleSession> sessions = new ArrayList<>();

        if (apiData == null || apiData.isEmpty()) {
            Log.d(TAG, "No API data to map");
            return sessions;
        }

        String todayDate = getTodayDateString();
        Log.d(TAG, "Mapping sessions for date: " + todayDate);

        for (StudentClassSectionData classSection : apiData) {
            // Find active session based on status and date
            SessionData activeSession = findActiveSession(classSection.getSessions(), todayDate);

            if (activeSession != null) {
                try {
                    // Parse times từ class section (daily time)
                    String startTimeStr = classSection.getStartTime(); // "06:21:50"
                    String endTimeStr = classSection.getEndTime();     // "06:51:50"

                    // Clean lecturer name
                    String lecturerName = cleanLecturerName(classSection.getLecturerName());

                    // Create session object
                    StudentScheduleSession session = new StudentScheduleSession(
                            activeSession.getSessionId(),          // sessionId
                            classSection.getCourseName(),          // className
                            classSection.getSectionCode(),         // grade
                            classSection.getWeekday(),             // day
                            startTimeStr,                          // startTime (string)
                            endTimeStr,                            // endTime (string)
                            classSection.getRoomName(),            // room
                            lecturerName,                          // instructor
                            classSection.getCourseCode()           // classCode
                    );

                    sessions.add(session);
                    Log.d(TAG, "✅ Mapped session: " + classSection.getCourseCode() +
                            " [" + classSection.getSectionCode() + "] - " + activeSession.getStatus());

                } catch (Exception e) {
                    Log.e(TAG, "❌ Error mapping session: " + classSection.getCourseCode(), e);
                }
            } else {
                Log.w(TAG, "⚠️ No active session found for: " + classSection.getCourseCode() +
                        " [" + classSection.getSectionCode() + "]");
            }
        }

        Log.d(TAG, "Total mapped sessions: " + sessions.size());
        return sessions;
    }

    /**
     * 🔧 FIND active session (similar to lecturer logic but for student)
     */
    private SessionData findActiveSession(List<SessionData> sessions, String todayDate) {
        if (sessions == null || sessions.isEmpty()) {
            Log.d(TAG, "No sessions to search");
            return null;
        }

        // Priority 1: Find session with "Active" status
        for (SessionData session : sessions) {
            if ("Active".equalsIgnoreCase(session.getStatus())) {
                Log.d(TAG, "Found Active session: #" + session.getSessionNumber());
                return session;
            }
        }

        // Priority 2: Find session that belongs to today
        for (SessionData session : sessions) {
            if (isSessionToday(session.getStartTime(), todayDate)) {
                Log.d(TAG, "Found session for today: #" + session.getSessionNumber());
                return session;
            }
        }

        // Priority 3: Find next upcoming session
        SessionData nextSession = findNextUpcomingSession(sessions);
        if (nextSession != null) {
            Log.d(TAG, "Using next upcoming session: #" + nextSession.getSessionNumber());
            return nextSession;
        }

        Log.w(TAG, "No suitable session found");
        return null;
    }

    /**
     * 🔧 CHECK if session belongs to today
     */
    private boolean isSessionToday(String sessionStartTime, String todayDate) {
        if (sessionStartTime == null || todayDate == null) return false;

        try {
            // Extract date part from ISO datetime (2025-07-29T07:51:50Z -> 2025-07-29)
            String sessionDate = sessionStartTime.substring(0, 10);
            return todayDate.equals(sessionDate);
        } catch (Exception e) {
            Log.e(TAG, "Error checking if session is today: " + sessionStartTime, e);
            return false;
        }
    }

    /**
     * 🔧 FIND next upcoming session
     */
    private SessionData findNextUpcomingSession(List<SessionData> sessions) {
        SessionData nextSession = null;
        Date earliestDate = null;

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

        for (SessionData session : sessions) {
            if ("Pending".equalsIgnoreCase(session.getStatus())) {
                try {
                    Date sessionStart = isoFormat.parse(session.getStartTime());
                    if (earliestDate == null || sessionStart.before(earliestDate)) {
                        earliestDate = sessionStart;
                        nextSession = session;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing session date for upcoming check", e);
                }
            }
        }

        return nextSession;
    }

    /**
     * 🔧 CLEAN lecturer name (remove "- Lecturer" suffix)
     */
    private String cleanLecturerName(String fullName) {
        if (fullName == null) return "Unknown Lecturer";

        // Remove "- Lecturer" suffix nếu có
        if (fullName.endsWith(" - Lecturer")) {
            return fullName.substring(0, fullName.length() - " - Lecturer".length());
        }

        return fullName;
    }

    /**
     * 🔧 GET today's date in yyyy-MM-dd format
     */
    private String getTodayDateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    // ==================== EXISTING METHODS ====================

    private void loadUserProfile() {
        if (authManager != null && authManager.isLoggedIn()) {
            String email = authManager.getCurrentUserEmail();
            String role = authManager.getCurrentUserRole();
            String userId = authManager.getCurrentUserId();

            String name = extractNameFromEmail(email);
            _userProfile.setValue(new UserProfile(userId, name, email, role));
        }
    }

    private void generateGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        String subGreeting;

        if (hour < 12) {
            greeting = "Good Morning";
            subGreeting = "Ready for today's classes?";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
            subGreeting = "Check your upcoming classes";
        } else {
            greeting = "Good Evening";
            subGreeting = "Review tomorrow's schedule";
        }

        if (authManager != null && authManager.isLoggedIn()) {
            String email = authManager.getCurrentUserEmail();
            String name = extractNameFromEmail(email);
            greeting = "Hi, " + name;
        }

        _greeting.setValue(greeting);
        _subGreeting.setValue(subGreeting);
    }

    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "Student";
        }

        String username = email.split("@")[0];
        if (username.length() > 0) {
            return username.substring(0, 1).toUpperCase() + username.substring(1);
        }

        return "Student";
    }


    @Getter
    public static class UserProfile {
        private final String id;
        private final String name;
        private final String email;
        private final String role;

        public UserProfile(String id, String name, String email, String role) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
        }
    }
}
