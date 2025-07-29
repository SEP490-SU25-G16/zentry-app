package vn.edu.fpt.zentryapp.lecturer.ui.schedule;

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

import lombok.AllArgsConstructor;
import lombok.Getter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.api.LecturerScheduleApiService;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ClassSectionData;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.DailyScheduleResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerScheduleSession;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionData;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.StartSessionResponse;

public class LecturerScheduleViewModel extends ViewModel {
    private static final String TAG = "LecturerScheduleVM";

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<LecturerScheduleSession>> _todaySessions = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> _userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> _greeting = new MutableLiveData<>();
    private final MutableLiveData<String> _currentDate = new MutableLiveData<>();
    // 🔧 THÊM LiveData cho start session result
    private final MutableLiveData<Boolean> _isStartingSession = new MutableLiveData<>(false);
    private final MutableLiveData<String> _startSessionSuccess = new MutableLiveData<>();

    // API Service
    private LecturerScheduleApiService apiService;
    private AuthManager authManager;
    private Context context;

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<LecturerScheduleSession>> todaySessions() { return _todaySessions; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<UserProfile> userProfile() { return _userProfile; }
    public LiveData<String> greeting() { return _greeting; }
    public LiveData<String> currentDate() { return _currentDate; }
    public LiveData<Boolean> isStartingSession() { return _isStartingSession; }
    public LiveData<String> startSessionSuccess() { return _startSessionSuccess; }

    public void init(Context context, AuthManager authManager) {
        this.context = context;
        this.authManager = authManager;
        this.apiService = ApiClient.getClient(context).create(LecturerScheduleApiService.class);

        loadUserProfile();
        loadTodaySessions();
        generateGreeting();
        updateCurrentDate();
    }

    /**
     * Load today's scheduled sessions from API
     */
    private void loadTodaySessions() {
        if (authManager == null || !authManager.isLoggedIn()) {
            _errorMessage.setValue("User not logged in");
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        String lecturerId = authManager.getCurrentUserId();
        String todayDate = getTodayDateString();

        Log.d(TAG, "Loading daily schedule for lecturer: " + lecturerId + ", date: " + todayDate);

        apiService.getDailySchedule(lecturerId, todayDate)
                .enqueue(new Callback<DailyScheduleResponse>() {
                    @Override
                    public void onResponse(Call<DailyScheduleResponse> call, Response<DailyScheduleResponse> response) {
                        _isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            DailyScheduleResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                List<LecturerScheduleSession> sessions = mapApiDataToSessions(apiResponse.getData());
                                _todaySessions.setValue(sessions);
                                Log.d(TAG, "✅ Loaded " + sessions.size() + " sessions");
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
                    public void onFailure(Call<DailyScheduleResponse> call, Throwable t) {
                        _isLoading.setValue(false);
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "❌ Network Error", t);
                    }
                });
    }

    /**
     * Map API data to LecturerScheduleSession objects
     */
    private List<LecturerScheduleSession> mapApiDataToSessions(List<ClassSectionData> apiData) {
        List<LecturerScheduleSession> sessions = new ArrayList<>();

        if (apiData == null || apiData.isEmpty()) {
            Log.d(TAG, "No API data to map");
            return sessions;
        }

        String todayDate = getTodayDateString(); // yyyy-MM-dd format
        Log.d(TAG, "Mapping sessions for date: " + todayDate);

        for (ClassSectionData classSection : apiData) {
            // Find active session based on status and date
            SessionData activeSession = findActiveSession(classSection.getSessions(), todayDate);

            if (activeSession != null) {
                try {
                    // Parse times
                    Date startTime = parseDateTime(activeSession.getStartTime());
                    Date endTime = parseDateTime(activeSession.getEndTime());
                    Date currentDate = new Date();

                    // Determine status based on session status and current time
                    String status = determineSessionStatus(activeSession.getStatus(), startTime, endTime, currentDate);

                    // Create session object
                    LecturerScheduleSession session = new LecturerScheduleSession(
                            activeSession.getSessionId(),        // sessionId
                            classSection.getCourseCode(),        // courseCode
                            classSection.getCourseName(),        // courseName
                            classSection.getSectionCode(),       // sectionCode
                            classSection.getRoomName(),          // room
                            startTime,                           // startTime
                            endTime,                             // endTime
                            currentDate,                         // currentDate
                            status,                              // status
                            determineCanStartSession(classSection.isCanStartSession(), status, startTime, currentDate), // canStartInstant
                            isSessionCompleted(status)           // canViewDetail
                    );

                    sessions.add(session);
                    Log.d(TAG, "✅ Mapped session: " + classSection.getCourseCode() +
                            " [" + classSection.getSectionCode() + "] - " + status +
                            " (Session #" + activeSession.getSessionNumber() + ")");

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
     * Find active session based on status and date info
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

        // Priority 2: Find session that belongs to today (based on startTime)
        for (SessionData session : sessions) {
            if (isSessionToday(session.getStartTime(), todayDate)) {
                Log.d(TAG, "Found session for today: #" + session.getSessionNumber());
                return session;
            }
        }

        // Priority 3: If no session for today, find the next upcoming session
        SessionData nextSession = findNextUpcomingSession(sessions);
        if (nextSession != null) {
            Log.d(TAG, "Using next upcoming session: #" + nextSession.getSessionNumber());
            return nextSession;
        }

        Log.w(TAG, "No suitable session found");
        return null;
    }

    /**
     * Check if session belongs to today
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
     * Find next upcoming session (status = Pending and future date)
     */
    private SessionData findNextUpcomingSession(List<SessionData> sessions) {
        SessionData nextSession = null;
        Date earliestDate = null;

        for (SessionData session : sessions) {
            if ("Pending".equalsIgnoreCase(session.getStatus())) {
                try {
                    Date sessionStart = parseDateTime(session.getStartTime());
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
     * Enhanced datetime parsing with better error handling
     */
    private Date parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            Log.w(TAG, "Empty datetime string, using current time");
            return new Date();
        }

        try {
            // Handle ISO format: 2025-07-29T07:51:50Z
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            Date parsed = isoFormat.parse(dateTimeString.trim());

            if (parsed == null) {
                Log.w(TAG, "Parsed date is null for: " + dateTimeString);
                return new Date();
            }

            return parsed;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing datetime: " + dateTimeString, e);
            return new Date(); // Fallback to current time
        }
    }

    /**
     * Enhanced status determination with buffer time
     */
    private String determineSessionStatus(String apiStatus, Date startTime, Date endTime, Date currentTime) {
        long currentMillis = currentTime.getTime();
        long startMillis = startTime.getTime();
        long endMillis = endTime.getTime();

        // Buffer time: 15 minutes before session starts
        long bufferTime = 15 * 60 * 1000; // 15 minutes in milliseconds

        if ("Active".equalsIgnoreCase(apiStatus)) {
            if (currentMillis < (startMillis - bufferTime)) {
                return "UPCOMING";
            } else if (currentMillis >= (startMillis - bufferTime) && currentMillis < startMillis) {
                return "STARTING_SOON";
            } else if (currentMillis >= startMillis && currentMillis <= endMillis) {
                return "ONGOING";
            } else {
                return "COMPLETED";
            }
        } else if ("Pending".equalsIgnoreCase(apiStatus)) {
            // For pending sessions, determine based on time
            if (currentMillis > endMillis) {
                return "COMPLETED";
            } else if (currentMillis >= startMillis && currentMillis <= endMillis) {
                return "ONGOING"; // API might not be updated yet
            } else if (currentMillis >= (startMillis - bufferTime)) {
                return "STARTING_SOON";
            } else {
                return "UPCOMING";
            }
        } else {
            // Default fallback
            return "UPCOMING";
        }
    }

    /**
     * Determine if session can be started based on multiple factors
     */
    private boolean determineCanStartSession(boolean apiCanStart, String status, Date startTime, Date currentTime) {
        // If API explicitly says cannot start, respect it
        if (!apiCanStart) return false;

        // Check if status allows starting
        if ("ONGOING".equals(status) || "STARTING_SOON".equals(status)) {
            return true;
        }

        // Additional time-based check: can start 30 minutes before session
        long bufferTime = 30 * 60 * 1000; // 30 minutes
        long currentMillis = currentTime.getTime();
        long startMillis = startTime.getTime();

        return currentMillis >= (startMillis - bufferTime);
    }

    /**
     * Check if session is completed (can view details)
     */
    private boolean isSessionCompleted(String status) {
        return "COMPLETED".equals(status) || "ONGOING".equals(status);
    }

    /**
     * Find the active session (status = "Active") or first session if none active
     */
    private SessionData findActiveSession(List<SessionData> sessions) {
        if (sessions == null || sessions.isEmpty()) return null;

        // First try to find active session
        for (SessionData session : sessions) {
            if ("Active".equalsIgnoreCase(session.getStatus())) {
                return session;
            }
        }

        // If no active session, return first session (for display purposes)
        return sessions.get(0);
    }

    /**
     * Get today's date in yyyy-MM-dd format
     */
    private String getTodayDateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    /**
     * Refresh sessions
     */
    public void refreshSessions() {
        loadTodaySessions();
    }

    /**
     * Load user profile (unchanged)
     */
    private void loadUserProfile() {
        if (authManager != null && authManager.isLoggedIn()) {
            String email = authManager.getCurrentUserEmail();
            String role = authManager.getCurrentUserRole();
            String userId = authManager.getCurrentUserId();

            String name = extractNameFromEmail(email);
            _userProfile.setValue(new UserProfile(userId, name, email, role));
        }
    }

    /**
     * Generate greeting (unchanged)
     */
    private void generateGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }

        if (authManager != null && authManager.isLoggedIn()) {
            String email = authManager.getCurrentUserEmail();
            String name = extractNameFromEmail(email);
            greeting += ", " + name + "!";
        }

        _greeting.setValue(greeting);
    }

    /**
     * Update current date (unchanged)
     */
    private void updateCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        _currentDate.setValue(currentDate);
    }

    /**
     * Handle start instant class (unchanged)
     */
    public void startInstantClass(LecturerScheduleSession session) {
        if (!session.isCanStartInstant()) {
            _errorMessage.setValue("Cannot start this session at the moment");
            return;
        }

        _errorMessage.setValue("Starting class: " + session.getCourseName());
    }

    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "Lecturer";
        }

        String username = email.split("@")[0];
        if (username.length() > 0) {
            return username.substring(0, 1).toUpperCase() + username.substring(1);
        }

        return "Lecturer";
    }

    /**
     * 🔧 THÊM method để start session via API
     */
    public void startSessionViaAPI(LecturerScheduleSession session) {
        if (session == null) {
            _errorMessage.setValue("Invalid session");
            return;
        }

        _isStartingSession.setValue(true);
        _errorMessage.setValue(null);

        String sessionId = session.getSessionId();
        Log.d(TAG, "Starting session via API: " + sessionId);

        apiService.startSession(sessionId)
                .enqueue(new Callback<StartSessionResponse>() {
                    @Override
                    public void onResponse(Call<StartSessionResponse> call, Response<StartSessionResponse> response) {
                        _isStartingSession.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            StartSessionResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                Log.d(TAG, "✅ Session started successfully: " + apiResponse.getMessage());

                                // 🔧 CẬP NHẬT session status thành ACTIVE
                                updateSessionStatusToActive(session);

                                // Notify success
                                String successMessage = apiResponse.getMessage() != null ?
                                        apiResponse.getMessage() : "Session started successfully";
                                _startSessionSuccess.setValue(successMessage);

                                // 🔧 REFRESH sessions để lấy data mới từ server
                                refreshSessions();

                            } else {
                                String error = apiResponse.getError() != null ?
                                        apiResponse.getError() : "Failed to start session";
                                _errorMessage.setValue(error);
                                Log.e(TAG, "❌ Start session API error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue(error);
                            Log.e(TAG, "❌ Start session HTTP error: " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<StartSessionResponse> call, Throwable t) {
                        _isStartingSession.setValue(false);
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "❌ Start session network error", t);
                    }
                });
    }

    /**
     * 🔧 CẬP NHẬT session status thành ACTIVE locally
     */
    private void updateSessionStatusToActive(LecturerScheduleSession session) {
        // Update local session status
        session.setStatus("ACTIVE");
        session.setCanViewDetail(true);

        // Update trong list hiện tại
        List<LecturerScheduleSession> currentSessions = _todaySessions.getValue();
        if (currentSessions != null) {
            for (LecturerScheduleSession s : currentSessions) {
                if (s.getSessionId().equals(session.getSessionId())) {
                    s.setStatus("ACTIVE");
                    s.setCanViewDetail(true);
                    break;
                }
            }
            // Trigger UI update
            _todaySessions.setValue(currentSessions);
        }

        Log.d(TAG, "Updated session status to ACTIVE: " + session.getSessionId());
    }

    /**
     * 🔧 CLEAR success message
     */
    public void clearStartSessionSuccess() {
        _startSessionSuccess.setValue(null);
    }

    // Inner Classes (unchanged)
    @Getter
    @AllArgsConstructor
    public static class UserProfile {
        private final String id;
        private final String name;
        private final String email;
        private final String role;
    }
}
