package vn.edu.fpt.zentryapp.student.ui.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.student.data.model.response.CalendarEvent;

public class StudentScheduleCalendarViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<CalendarEvent>> _events = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _selectedDate = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<CalendarEvent>> events() { return _events; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<String> successMessage() { return _successMessage; }
    public LiveData<String> selectedDate() { return _selectedDate; }

    private AuthManager authManager;

    public void init(AuthManager authManager) {
        this.authManager = authManager;
        loadEventsForToday();
    }

    public void loadEventsForDate(int year, int month, int dayOfMonth) {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        // Create selected date
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth);
        Date selectedDate = calendar.getTime();

        SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        _selectedDate.setValue(formatter.format(selectedDate));

        new Handler().postDelayed(() -> {
            try {
                List<CalendarEvent> eventsForDate = generateMockEventsForDate(selectedDate);
                _events.setValue(eventsForDate);
                _successMessage.setValue("Events loaded successfully");
            } catch (Exception e) {
                _errorMessage.setValue("Failed to load events: " + e.getMessage());
            } finally {
                _isLoading.setValue(false);
            }
        }, 500);
    }

    public void loadEventsForToday() {
        Calendar today = Calendar.getInstance();
        loadEventsForDate(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );
    }

    private List<CalendarEvent> generateMockEventsForDate(Date date) {
        List<CalendarEvent> events = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        // Generate different events based on day of week
        switch (dayOfWeek) {
            case Calendar.MONDAY:
            case Calendar.WEDNESDAY:
            case Calendar.FRIDAY:
                events.add(new CalendarEvent(
                        "E001",
                        "Math Class",
                        "Grade 07",
                        date,
                        "08:00",
                        "class",
                        "#FF4081"
                ));
                events.add(new CalendarEvent(
                        "E002",
                        "Physics Lab",
                        "Laboratory Session",
                        date,
                        "10:30",
                        "class",
                        "#2196F3"
                ));
                break;

            case Calendar.TUESDAY:
            case Calendar.THURSDAY:
                events.add(new CalendarEvent(
                        "E003",
                        "Chemistry Class",
                        "Grade 08",
                        date,
                        "09:00",
                        "class",
                        "#4CAF50"
                ));
                events.add(new CalendarEvent(
                        "E004",
                        "Meeting",
                        "Parent-Teacher Conference",
                        date,
                        "14:00",
                        "meeting",
                        "#FF9800"
                ));
                break;

            case Calendar.SATURDAY:
                events.add(new CalendarEvent(
                        "E005",
                        "Study Group",
                        "Mathematics Review",
                        date,
                        "10:00",
                        "study",
                        "#9C27B0"
                ));
                break;

            default: // Sunday
                events.add(new CalendarEvent(
                        "E006",
                        "Free Day",
                        "No scheduled classes",
                        date,
                        "All Day",
                        "free",
                        "#607D8B"
                ));
                break;
        }

        return events;
    }

    public void onEventClicked(CalendarEvent event) {
        // TODO: Handle event click for navigation or details
    }
}
