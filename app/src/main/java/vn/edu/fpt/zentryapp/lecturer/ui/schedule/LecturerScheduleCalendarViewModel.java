package vn.edu.fpt.zentryapp.lecturer.ui.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.api.LecturerApiService;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CalendarSession;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ApiResponseDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.CalendarClassDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.CalendarDayDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.MonthlyCalendarDataDto;

public class LecturerScheduleCalendarViewModel extends ViewModel {
    private final String TAG = "LecturerScheduleCalendar";

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<CalendarSession>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _selectedDate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _hasSessionsForDate = new MutableLiveData<>();

    // API service
    private LecturerApiService apiService;
    private AuthManager authManager;

    // Cache monthly data
    private Map<String, List<CalendarSession>> monthlySessionsCache = new HashMap<>();
    private Calendar currentSelectedDate;
    private int currentMonth = -1;
    private int currentYear = -1;

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<CalendarSession>> sessions() { return _sessions; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<String> selectedDate() { return _selectedDate; }
    public LiveData<Boolean> hasSessionsForDate() { return _hasSessionsForDate; }

    public void init(Context context, AuthManager authManager) {
        this.authManager = authManager;
        this.apiService = ApiClient.getClient(context).create(LecturerApiService.class);

        // Initialize with today's date
        currentSelectedDate = Calendar.getInstance();
        loadMonthlyCalendarAndSelectDate(currentSelectedDate);
    }

    /**
     * Load monthly calendar data and then select specific date
     */
    private void loadMonthlyCalendarAndSelectDate(Calendar selectedDate) {
        int month = selectedDate.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
        int year = selectedDate.get(Calendar.YEAR);

        // Check if we need to load new month data
        if (currentMonth != month || currentYear != year) {
            loadMonthlyCalendar(month, year, selectedDate);
        } else {
            // Use cached data
            selectDateFromCache(selectedDate);
        }
    }

    /**
     * Load monthly calendar data from API
     */
    private void loadMonthlyCalendar(int month, int year, Calendar selectedDate) {
        _isLoading.setValue(true);

        String lecturerId = authManager.getCurrentUserId();

        Call<ApiResponseDto<MonthlyCalendarDataDto>> call = apiService.getMonthlyCalendar(lecturerId, month, year);
        call.enqueue(new Callback<ApiResponseDto<MonthlyCalendarDataDto>>() {
            @Override
            public void onResponse(Call<ApiResponseDto<MonthlyCalendarDataDto>> call,
                                   Response<ApiResponseDto<MonthlyCalendarDataDto>> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseDto<MonthlyCalendarDataDto> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        processMonthlyCalendar(apiResponse.getData(), month, year);
                        selectDateFromCache(selectedDate);
                    } else {
                        _errorMessage.setValue(apiResponse.getError() != null ?
                                apiResponse.getError() : "Failed to load calendar");
                    }
                } else {
                    _errorMessage.setValue("Failed to load calendar: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponseDto<MonthlyCalendarDataDto>> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    /**
     * Process monthly calendar data and cache it
     */
    private void processMonthlyCalendar(MonthlyCalendarDataDto calendarData, int month, int year) {
        currentMonth = month;
        currentYear = year;
        monthlySessionsCache.clear();

        if (calendarData.getCalendarDays() == null) return;

        for (CalendarDayDto dayDto : calendarData.getCalendarDays()) {
            List<CalendarSession> sessions = mapCalendarDayToSessions(dayDto);

            // Create date key for caching
            String dateKey = extractDateKey(dayDto.getDate());
            if (dateKey != null) {
                monthlySessionsCache.put(dateKey, sessions);
            }
        }

        Log.d(TAG, "Cached " + monthlySessionsCache.size() + " days for month " + month + "/" + year);
    }

    /**
     * Map calendar day DTO to sessions
     */
    private List<CalendarSession> mapCalendarDayToSessions(CalendarDayDto dayDto) {
        List<CalendarSession> sessions = new ArrayList<>();

        if (dayDto.getClasses() == null) return sessions;

        // Parse session date from dayDto.getDate()
        Date sessionDate = parseApiDate(dayDto.getDate());

        for (CalendarClassDto classDto : dayDto.getClasses()) {
            CalendarSession session = new CalendarSession();
            session.setSessionId(classDto.getSessionId());
            session.setClassSectionId(classDto.getClassSectionId());
            session.setCourseName(classDto.getCourseName());
            session.setSectionCode(classDto.getSectionCode());
            session.setClassName(classDto.getCourseName() + " - " + classDto.getSectionCode());
            session.setRoom(classDto.getRoomName());
            session.setBuilding(classDto.getBuilding());
            session.setSessionDate(sessionDate);

            // Parse start time and create full datetime
            Date startTime = parseSessionTime(dayDto.getDate(), classDto.getStartTime());
            session.setStartTime(startTime);

            // Estimate end time (assume 1.5 hour sessions)
            if (startTime != null) {
                Calendar endCal = Calendar.getInstance();
                endCal.setTime(startTime);
                endCal.add(Calendar.HOUR_OF_DAY, 1);
                endCal.add(Calendar.MINUTE, 30);
                session.setEndTime(endCal.getTime());
            }

            // Set session type and status
            session.setSessionType("LECTURE"); // Default type
            updateSessionStatus(session);

            sessions.add(session);
        }

        return sessions;
    }

    /**
     * Parse API date string to Date object
     */
    private Date parseApiDate(String dateString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return format.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing API date: " + dateString, e);
            return null;
        }
    }

    /**
     * Parse session time and combine with date
     */
    private Date parseSessionTime(String dateString, String timeString) {
        try {
            // Extract date part
            String datePart = dateString.substring(0, 10); // "2025-08-11"
            String fullDateTime = datePart + " " + timeString; // "2025-08-11 16:26:29"

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return format.parse(fullDateTime);
        } catch (ParseException | StringIndexOutOfBoundsException e) {
            Log.e(TAG, "Error parsing session time: " + timeString, e);
            return null;
        }
    }

    /**
     * Extract date key for caching (YYYY-MM-DD format)
     */
    private String extractDateKey(String dateString) {
        try {
            return dateString.substring(0, 10); // "2025-08-11"
        } catch (StringIndexOutOfBoundsException e) {
            Log.e(TAG, "Error extracting date key from: " + dateString, e);
            return null;
        }
    }

    /**
     * Select date from cached data
     */
    private void selectDateFromCache(Calendar selectedDate) {
        this.currentSelectedDate = (Calendar) selectedDate.clone();

        // Update selected date display
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        _selectedDate.setValue(dateFormat.format(selectedDate.getTime()));

        // Get sessions for selected date
        String dateKey = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.DAY_OF_MONTH));

        List<CalendarSession> sessions = monthlySessionsCache.get(dateKey);
        if (sessions == null) {
            sessions = new ArrayList<>();
        }

        _sessions.setValue(sessions);
        _hasSessionsForDate.setValue(!sessions.isEmpty());

        Log.d(TAG, "Selected date: " + dateKey + ", found " + sessions.size() + " sessions");
    }

    /**
     * Update session status based on current time
     */
    private void updateSessionStatus(CalendarSession session) {
        if (session.getStartTime() == null) {
            session.setStatus("UPCOMING");
            return;
        }

        long currentTime = System.currentTimeMillis();
        long startTime = session.getStartTime().getTime();
        long endTime = session.getEndTime() != null ? session.getEndTime().getTime() : startTime + (90 * 60 * 1000); // +90 minutes

        if (currentTime < startTime) {
            session.setStatus("UPCOMING");
        } else if (currentTime >= startTime && currentTime <= endTime) {
            session.setStatus("ONGOING");
        } else {
            session.setStatus("COMPLETED");
        }
    }

    /**
     * Load sessions for specific date (called from calendar date selection)
     */
    public void loadSessionsForDate(int year, int month, int dayOfMonth) {
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, dayOfMonth);
        loadMonthlyCalendarAndSelectDate(selectedDate);
    }

    /**
     * Load sessions for specific date
     */
    public void loadSessionsForDate(Calendar selectedDate) {
        loadMonthlyCalendarAndSelectDate(selectedDate);
    }

    /**
     * Refresh current month data
     */
    public void refreshSessions() {
        if (currentSelectedDate != null) {
            // Force reload by resetting current month/year
            currentMonth = -1;
            currentYear = -1;
            loadMonthlyCalendarAndSelectDate(currentSelectedDate);
        }
    }
}
