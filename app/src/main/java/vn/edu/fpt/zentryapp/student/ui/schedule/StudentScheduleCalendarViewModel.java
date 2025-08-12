package vn.edu.fpt.zentryapp.student.ui.schedule;

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
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ApiResponseDto;
import vn.edu.fpt.zentryapp.student.data.api.StudentApiService;
import vn.edu.fpt.zentryapp.student.data.model.response.CalendarEvent;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.StudentCalendarClassDto;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.StudentCalendarDayDto;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.StudentMonthlyCalendarDataDto;

public class StudentScheduleCalendarViewModel extends ViewModel {
    private final String TAG = "StudentScheduleCalendar";

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<CalendarEvent>> _events = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _selectedDate = new MutableLiveData<>();

    // API service
    private StudentApiService apiService;
    private AuthManager authManager;

    // Cache monthly data
    private Map<String, List<CalendarEvent>> monthlyEventsCache = new HashMap<>();
    private Calendar currentSelectedDate;
    private int currentMonth = -1;
    private int currentYear = -1;

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<CalendarEvent>> events() { return _events; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<String> successMessage() { return _successMessage; }
    public LiveData<String> selectedDate() { return _selectedDate; }

    public void init(Context context, AuthManager authManager) {
        this.authManager = authManager;
        this.apiService = ApiClient.getClient(context).create(StudentApiService.class);

        // Initialize with today's date
        currentSelectedDate = Calendar.getInstance();
        loadMonthlyCalendarAndSelectDate(currentSelectedDate);
    }

    public void init(AuthManager authManager) {
        // Backward compatibility method - context will be needed
        this.authManager = authManager;
        loadEventsForToday();
    }

    /**
     * Load events for specific date
     */
    public void loadEventsForDate(int year, int month, int dayOfMonth) {
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, dayOfMonth);
        loadMonthlyCalendarAndSelectDate(selectedDate);
    }

    /**
     * Load events for today
     */
    public void loadEventsForToday() {
        Calendar today = Calendar.getInstance();
        loadEventsForDate(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );
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

        String studentId = authManager.getCurrentUserId();

        Call<ApiResponseDto<StudentMonthlyCalendarDataDto>> call = apiService.getStudentMonthlyCalendar(studentId, month, year);
        call.enqueue(new Callback<ApiResponseDto<StudentMonthlyCalendarDataDto>>() {
            @Override
            public void onResponse(Call<ApiResponseDto<StudentMonthlyCalendarDataDto>> call,
                                   Response<ApiResponseDto<StudentMonthlyCalendarDataDto>> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseDto<StudentMonthlyCalendarDataDto> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        processMonthlyCalendar(apiResponse.getData(), month, year);
                        selectDateFromCache(selectedDate);
                        _successMessage.setValue("Calendar loaded successfully");
                    } else {
                        _errorMessage.setValue(apiResponse.getError() != null ?
                                apiResponse.getError() : "Failed to load calendar");
                    }
                } else {
                    _errorMessage.setValue("Failed to load calendar: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponseDto<StudentMonthlyCalendarDataDto>> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    /**
     * Process monthly calendar data and cache it
     */
    private void processMonthlyCalendar(StudentMonthlyCalendarDataDto calendarData, int month, int year) {
        currentMonth = month;
        currentYear = year;
        monthlyEventsCache.clear();

        if (calendarData.getCalendarDays() == null) return;

        for (StudentCalendarDayDto dayDto : calendarData.getCalendarDays()) {
            List<CalendarEvent> events = mapCalendarDayToEvents(dayDto);

            // Create date key for caching
            String dateKey = extractDateKey(dayDto.getDate());
            if (dateKey != null) {
                monthlyEventsCache.put(dateKey, events);
            }
        }

        Log.d(TAG, "Cached " + monthlyEventsCache.size() + " days for month " + month + "/" + year);
    }

    /**
     * Map calendar day DTO to events
     */
    private List<CalendarEvent> mapCalendarDayToEvents(StudentCalendarDayDto dayDto) {
        List<CalendarEvent> events = new ArrayList<>();

        if (dayDto.getClasses() == null) return events;

        // Parse event date from dayDto.getDate()
        Date eventDate = parseApiDate(dayDto.getDate());

        for (StudentCalendarClassDto classDto : dayDto.getClasses()) {
            CalendarEvent event = new CalendarEvent();
            event.setEventId(classDto.getSessionId());
            event.setSessionId(classDto.getSessionId());
            event.setClassSectionId(classDto.getClassSectionId());
            event.setCourseName(classDto.getCourseName());
            event.setSectionCode(classDto.getSectionCode());
            event.setRoomName(classDto.getRoomName());
            event.setBuilding(classDto.getBuilding());
            event.setEventDate(eventDate);
            event.setStartTime(classDto.getStartTime());

            // Estimate end time (assume 1.5 hour classes)
            String endTime = calculateEndTime(classDto.getStartTime());
            event.setEndTime(endTime);

            // Set display properties
            event.setTitle(event.getFullCourseName());
            event.setDescription(event.getDisplaySubtitle());
            event.setEventType("class");
            event.setColor(getRandomClassColor());

            events.add(event);
        }

        return events;
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
     * Calculate end time based on start time (add 1.5 hours)
     */
    private String calculateEndTime(String startTime) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Date start = timeFormat.parse(startTime);

            if (start != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(start);
                cal.add(Calendar.HOUR_OF_DAY, 1);
                cal.add(Calendar.MINUTE, 30);
                return timeFormat.format(cal.getTime());
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing start time: " + startTime, e);
        }
        return startTime; // Return original if parsing fails
    }

    /**
     * Get random color for class events
     */
    private String getRandomClassColor() {
        String[] colors = {
                "#FF4081", "#2196F3", "#4CAF50", "#FF9800",
                "#9C27B0", "#607D8B", "#795548", "#E91E63"
        };
        int index = (int) (Math.random() * colors.length);
        return colors[index];
    }

    /**
     * Extract date key for caching (YYYY-MM-DD format)
     */
    private String extractDateKey(String dateString) {
        try {
            return dateString.substring(0, 10); // "2025-08-12"
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

        // Get events for selected date
        String dateKey = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.DAY_OF_MONTH));

        List<CalendarEvent> events = monthlyEventsCache.get(dateKey);
        if (events == null) {
            events = new ArrayList<>();
        }

        _events.setValue(events);

        Log.d(TAG, "Selected date: " + dateKey + ", found " + events.size() + " events");
    }

    /**
     * Handle event click
     */
    public void onEventClicked(CalendarEvent event) {
        Log.d(TAG, "Event clicked: " + event.getTitle());
        // TODO: Navigate to session details or handle event click
    }

    /**
     * Refresh current month data
     */
    public void refreshCalendar() {
        if (currentSelectedDate != null) {
            // Force reload by resetting current month/year
            currentMonth = -1;
            currentYear = -1;
            loadMonthlyCalendarAndSelectDate(currentSelectedDate);
        }
    }
}
