package vn.edu.fpt.zentryapp.student.ui.schedule;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentScheduleCalendarBinding;
import vn.edu.fpt.zentryapp.student.adapter.CalendarEventAdapter;
import vn.edu.fpt.zentryapp.student.data.model.response.CalendarEvent;

public class StudentScheduleCalendarFragment extends Fragment implements CalendarEventAdapter.OnEventClickListener {

    private FragmentStudentScheduleCalendarBinding binding;
    private StudentScheduleCalendarViewModel viewModel;
    private CalendarEventAdapter eventAdapter;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentScheduleCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel với context
        viewModel = new ViewModelProvider(this).get(StudentScheduleCalendarViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager);

        setupRecyclerView();
        setupCalendar();
        setupToolbar();
        observeViewModel();
    }

    private void setupRecyclerView() {
        eventAdapter = new CalendarEventAdapter();
        eventAdapter.setOnEventClickListener(this);

        binding.rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEvents.setAdapter(eventAdapter);
    }

    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((calendarView, year, month, dayOfMonth) -> {
            viewModel.loadEventsForDate(year, month, dayOfMonth);
        });
    }

    private void setupToolbar() {
        binding.ivStudentScheduleCalendarBack.setOnClickListener(v ->
                navController.navigateUp());
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe events
        viewModel.events().observe(getViewLifecycleOwner(), events -> {
            if (events != null) {
                eventAdapter.setEvents(events);

                boolean isEmpty = events.isEmpty();
                binding.rvEvents.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                Log.d("StudentCalendar", "Loaded " + events.size() + " events");
            }
        });

        // Observe selected date
        viewModel.selectedDate().observe(getViewLifecycleOwner(), selectedDate -> {
            if (selectedDate != null) {
                // Update UI with selected date if needed
                Log.d("StudentCalendar", "Selected date: " + selectedDate);
            }
        });

        // Observe success messages
        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.d("CalendarFragment", message);
            }
        });

        // Observe errors
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                Log.e("CalendarFragment", "Error: " + error);

                if (error.contains("network") || error.contains("connection")) {
                    showRetryDialog();
                }
            }
        });
    }

    private void showRetryDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Connection Error")
                .setMessage("Unable to load events. Would you like to retry?")
                .setPositiveButton("Retry", (dialog, which) -> {
                    viewModel.refreshCalendar();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEventClick(CalendarEvent event) {
        String eventInfo = String.format("%s\n%s\n%s",
                event.getDisplayTitle(),
                event.getDisplaySubtitle(),
                event.getFormattedDate());

        Toast.makeText(requireContext(), eventInfo, Toast.LENGTH_LONG).show();
        viewModel.onEventClicked(event);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        if (viewModel != null) {
            viewModel.refreshCalendar();
        }
    }
}
